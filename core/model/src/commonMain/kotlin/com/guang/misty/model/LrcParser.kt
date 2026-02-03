package com.guang.misty.model

/**
 * LRC 歌词解析器
 * 
 * 支持：
 * - 逐行格式：[mm:ss.xx]歌词内容
 * - 逐字格式：[00:00.670]私[00:01.260]の[00:01.400]恋...
 * - 多语言合并：相同时间戳按出现顺序分配（第1次=原文，第2次=译文，第3次=罗马音）
 */
object LrcParser {
    
    // 时间戳正则：[mm:ss.xx] 或 [mm:ss.xxx] 或 [mm:ss]
    private val TIME_TAG_REGEX = Regex("""\[(\d{1,2}):(\d{2})(?:[.:](\d{2,3}))?\]""")
    
    // 检测是否为逐字格式（行内有多个时间戳）
    private val WORD_BY_WORD_REGEX = Regex("""\[\d{1,2}:\d{2}[.:]\d{2,3}\][^\[\]]+\[\d{1,2}:\d{2}[.:]\d{2,3}\]""")
    
    /**
     * 解析 LRC 内容
     * 
     * @param lrcContent LRC 文件内容
     * @return 解析后的歌词行列表
     */
    fun parse(lrcContent: String): List<LyricLine> {
        val lines = lrcContent.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() && it.startsWith("[") }
            .filter { !isMetadataLine(it) }
        
        // 第一步：解析所有行，得到 (时间戳, 内容, words?) 的列表
        val rawLines = mutableListOf<RawLyricLine>()
        
        for (line in lines) {
            if (isWordByWord(line)) {
                // 逐字格式
                val parsed = parseWordByWordLine(line)
                if (parsed != null) {
                    rawLines.add(parsed)
                }
            } else {
                // 逐行格式（可能有多个时间戳指向同一内容）
                val parsed = parseLineByLineLine(line)
                rawLines.addAll(parsed)
            }
        }
        
        // 第二步：按时间戳分组，合并原文/译文/罗马音
        val groupedByTime = rawLines.groupBy { it.startTimeMs }
        
        val mergedLines = groupedByTime.map { (startTime, linesAtTime) ->
            val original = linesAtTime.getOrNull(0)
            val translation = linesAtTime.getOrNull(1)
            val romanization = linesAtTime.getOrNull(2)
            
            LyricLine(
                startTimeMs = startTime,
                endTimeMs = 0L, // 稍后计算
                text = original?.text ?: "",
                words = original?.words,
                translation = translation?.text?.takeIf { it.isNotEmpty() },
                romanization = romanization?.text?.takeIf { it.isNotEmpty() }
            )
        }.sortedBy { it.startTimeMs }
        
        // 第三步：计算每行的结束时间（下一行的开始时间）
        return mergedLines.mapIndexed { index, line ->
            val nextStartTime = mergedLines.getOrNull(index + 1)?.startTimeMs
                ?: (line.startTimeMs + 5000) // 最后一行默认持续5秒
            line.copy(endTimeMs = nextStartTime)
        }
    }
    
    /**
     * 从插件返回的 MistyLyricBundle 转换为 LyricLine 列表
     */
    fun fromBundle(bundle: MistyLyricBundle): List<LyricLine> {
        // 分离不同类型的歌词
        val originalLyric = bundle.lyrics.find { it.type == MistyLyricType.ORIGINAL }
        val translationLyric = bundle.lyrics.find { it.type == MistyLyricType.TRANSLATION }
        val romanizationLyric = bundle.lyrics.find { it.type == MistyLyricType.ROMANIZATION }
        
        // 解析原文歌词
        val originalLines = if (originalLyric != null) {
            parseRawLines(originalLyric.content)
        } else {
            emptyList()
        }
        
        // 解析译文歌词
        val translationLines = if (translationLyric != null) {
            parseRawLines(translationLyric.content)
        } else {
            emptyList()
        }
        
        // 解析罗马音歌词
        val romanizationLines = if (romanizationLyric != null) {
            parseRawLines(romanizationLyric.content)
        } else {
            emptyList()
        }
        
        // 按时间戳合并
        val allTimes = (originalLines.map { it.startTimeMs } +
                translationLines.map { it.startTimeMs } +
                romanizationLines.map { it.startTimeMs }).distinct().sorted()
        
        val originalMap = originalLines.associateBy { it.startTimeMs }
        val translationMap = translationLines.associateBy { it.startTimeMs }
        val romanizationMap = romanizationLines.associateBy { it.startTimeMs }
        
        val mergedLines = allTimes.mapNotNull { time ->
            val original = originalMap[time]
            val translation = translationMap[time]
            val romanization = romanizationMap[time]
            
            // 至少要有原文
            if (original == null && translation == null && romanization == null) {
                return@mapNotNull null
            }
            
            LyricLine(
                startTimeMs = time,
                endTimeMs = 0L,
                text = original?.text ?: translation?.text ?: romanization?.text ?: "",
                words = original?.words,
                translation = translation?.text?.takeIf { it.isNotEmpty() },
                romanization = romanization?.text?.takeIf { it.isNotEmpty() }
            )
        }
        
        // 计算结束时间
        return mergedLines.mapIndexed { index, line ->
            val nextStartTime = mergedLines.getOrNull(index + 1)?.startTimeMs
                ?: (line.startTimeMs + 5000)
            line.copy(endTimeMs = nextStartTime)
        }
    }
    
    /**
     * 解析原始行（不合并多语言）
     */
    private fun parseRawLines(content: String): List<RawLyricLine> {
        val lines = content.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() && it.startsWith("[") }
            .filter { !isMetadataLine(it) }
        
        val rawLines = mutableListOf<RawLyricLine>()
        
        for (line in lines) {
            if (isWordByWord(line)) {
                val parsed = parseWordByWordLine(line)
                if (parsed != null) {
                    rawLines.add(parsed)
                }
            } else {
                val parsed = parseLineByLineLine(line)
                rawLines.addAll(parsed)
            }
        }
        
        return rawLines
    }
    
    /**
     * 判断是否为元数据行（如 [ti:标题], [ar:艺术家] 等）
     */
    private fun isMetadataLine(line: String): Boolean {
        val metadataTags = listOf("ti:", "ar:", "al:", "by:", "offset:", "re:", "ve:", "length:")
        return metadataTags.any { line.lowercase().startsWith("[$it") }
    }
    
    /**
     * 判断是否为逐字格式
     */
    private fun isWordByWord(line: String): Boolean {
        return WORD_BY_WORD_REGEX.containsMatchIn(line)
    }
    
    /**
     * 解析逐行格式（支持多时间戳指向同一内容，如 [01:23.45][02:34.56]歌词）
     */
    private fun parseLineByLineLine(line: String): List<RawLyricLine> {
        val result = mutableListOf<RawLyricLine>()
        
        // 找出所有时间戳
        val timeMatches = TIME_TAG_REGEX.findAll(line).toList()
        if (timeMatches.isEmpty()) return emptyList()
        
        // 提取时间戳后的文本
        val lastMatch = timeMatches.last()
        val text = line.substring(lastMatch.range.last + 1).trim()
        
        // 为每个时间戳创建一个条目
        for (match in timeMatches) {
            val timeMs = parseTimeTag(match)
            if (timeMs != null) {
                result.add(RawLyricLine(timeMs, text, null))
            }
        }
        
        return result
    }
    
    /**
     * 解析逐字格式
     * 格式：[00:00.670]私[00:01.260]の[00:01.400]恋...
     */
    private fun parseWordByWordLine(line: String): RawLyricLine? {
        val words = mutableListOf<LyricWord>()
        var currentIndex = 0
        var lineStartTime: Long? = null
        
        while (currentIndex < line.length) {
            // 寻找时间戳
            val timeMatch = TIME_TAG_REGEX.find(line, currentIndex)
            if (timeMatch == null || timeMatch.range.first != currentIndex) {
                // 没有更多时间戳，检查是否有剩余文本
                if (currentIndex < line.length && words.isNotEmpty()) {
                    val remainingText = line.substring(currentIndex).trim()
                    if (remainingText.isNotEmpty()) {
                        // 将剩余文本附加到最后一个词
                        val lastWord = words.removeLast()
                        words.add(lastWord.copy(text = lastWord.text + remainingText))
                    }
                }
                break
            }
            
            val startTime = parseTimeTag(timeMatch) ?: break
            if (lineStartTime == null) {
                lineStartTime = startTime
            }
            
            currentIndex = timeMatch.range.last + 1
            
            // 提取文本直到下一个时间戳
            val nextTimeMatch = TIME_TAG_REGEX.find(line, currentIndex)
            val textEndIndex = nextTimeMatch?.range?.first ?: line.length
            val text = line.substring(currentIndex, textEndIndex)
            
            if (text.isNotEmpty()) {
                val endTime = if (nextTimeMatch != null) {
                    parseTimeTag(nextTimeMatch) ?: startTime
                } else {
                    startTime + 1000 // 最后一个字默认持续1秒
                }
                
                words.add(LyricWord(startTime, endTime, text))
            }
            
            currentIndex = textEndIndex
        }
        
        if (words.isEmpty() || lineStartTime == null) return null
        
        val fullText = words.joinToString("") { it.text }
        return RawLyricLine(lineStartTime, fullText, words)
    }
    
    /**
     * 解析时间戳为毫秒
     */
    private fun parseTimeTag(match: MatchResult): Long? {
        val minutes = match.groupValues[1].toLongOrNull() ?: return null
        val seconds = match.groupValues[2].toLongOrNull() ?: return null
        val millisPart = match.groupValues.getOrNull(3)?.padEnd(3, '0')?.take(3)?.toLongOrNull() ?: 0
        
        return minutes * 60 * 1000 + seconds * 1000 + millisPart
    }
    
    /**
     * 内部使用的原始歌词行
     */
    private data class RawLyricLine(
        val startTimeMs: Long,
        val text: String,
        val words: List<LyricWord>?
    )
}
