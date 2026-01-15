import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    // 1. 核心：使用 KMP 插件
    alias(libs.plugins.kotlinMultiplatform)
    // 2. 必须：作为被引用的库，这里要用 androidLibrary 而非 androidApplication
    alias(libs.plugins.androidLibrary)
    // 3. 添加序列化插件
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
        // 公共逻辑：网络请求相关代码
        commonMain.dependencies {
            // 依赖 core:model 模块
            implementation(project(":core:model"))
            // Ktor 核心库
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
        }
        androidMain.dependencies {
            // Android 平台特定的 HTTP 客户端
            implementation(libs.ktor.client.android)
        }
        jvmMain.dependencies {
            // JVM 平台特定的 HTTP 客户端
            implementation(libs.ktor.client.cio)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

android {
    namespace = "com.guang.misty.network"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}