package com.guang.misty.player

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.annotation.OptIn
import androidx.core.app.NotificationCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

/**
 * Misty 媒体播放服务
 * 
 * 用于在后台播放音乐并在通知栏显示播放控制
 * 集成 MediaSession 以支持系统媒体控制
 */
@OptIn(UnstableApi::class)
class MistyPlaybackService : MediaSessionService() {
    
    private var mediaSession: MediaSession? = null
    
    companion object {
        private const val NOTIFICATION_CHANNEL_ID = "misty_playback_channel"
        
        // 共享的 ExoPlayer 实例，供 AndroidMistyPlayer 使用
        @Volatile
        var sharedPlayer: ExoPlayer? = null
            private set
        
        // 共享的 MediaSession，供外部获取元数据
        @Volatile
        var sharedSession: MediaSession? = null
            private set
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // 必须尽快调用 startForeground()，否则 Android 会 ANR
        createNotificationChannel()
        val notification = createPlaceholderNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            startForeground(1, notification)
        }
        return super.onStartCommand(intent, flags, startId)
    }
    
    override fun onCreate() {
        super.onCreate()
        
        // 创建通知渠道
        createNotificationChannel()
        
        // 创建 ExoPlayer
        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                true // handleAudioFocus
            )
            .setHandleAudioBecomingNoisy(true)
            .build()
        
        sharedPlayer = player
        
        // 创建 MediaSession
        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(createPendingIntent())
            .setCallback(MistyMediaSessionCallback())
            .build()
        
        sharedSession = mediaSession
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Music Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Misty music playback notifications"
                setShowBadge(false)
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }
    }
    
    private fun createPlaceholderNotification(): Notification {
        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Misty")
            .setContentText("正在准备播放...")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setSilent(true)
            .build()
    }
    
    private fun createPendingIntent(): PendingIntent {
        val intent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        
        return PendingIntent.getActivity(this, 0, intent, flags)
    }
    
    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }
    
    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }
    
    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        sharedPlayer = null
        sharedSession = null
        super.onDestroy()
    }
    
    /**
     * MediaSession 回调，处理自定义命令
     */
    private inner class MistyMediaSessionCallback : MediaSession.Callback {
        
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo
        ): MediaSession.ConnectionResult {
            val sessionCommands = MediaSession.ConnectionResult.DEFAULT_SESSION_AND_LIBRARY_COMMANDS.buildUpon()
                .build()
            
            return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                .setAvailableSessionCommands(sessionCommands)
                .build()
        }
        
        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>
        ): ListenableFuture<MutableList<MediaItem>> {
            val updatedItems = mediaItems.map { item ->
                item.buildUpon()
                    .setUri(item.requestMetadata.mediaUri)
                    .build()
            }.toMutableList()
            
            return Futures.immediateFuture(updatedItems)
        }
    }
}
