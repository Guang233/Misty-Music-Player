import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    // 1. 核心：使用 KMP 插件
    alias(libs.plugins.kotlinMultiplatform)
    // 2. 必须：作为被引用的库，这里要用 androidLibrary 而非 androidApplication
    alias(libs.plugins.androidLibrary)
    // 3. 序列化：添加 Kotlin 序列化插件
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    // 与 composeApp 对齐，定义 Android 目标
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    // 与 composeApp 对齐，定义 JVM (Desktop) 目标
    jvm()

    sourceSets {
        // 公共逻辑：MistySong 等模型类放在这里
        commonMain.dependencies {
            // 如果需要 JSON 解析，可以加上这个
            implementation(libs.kotlinx.serialization.json)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

android {
    namespace = "com.guang.misty.model"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}