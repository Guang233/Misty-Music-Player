import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    // 1. 核心：使用 KMP 插件
    alias(libs.plugins.kotlinMultiplatform)
    // 2. 必须：作为被引用的库，这里要用 androidLibrary 而非 androidApplication
    alias(libs.plugins.androidLibrary)
    // 3. 序列化：添加 Kotlin 序列化插件
    alias(libs.plugins.kotlinSerialization)
    // 4. Compose Multiplatform：用于资源管理
    alias(libs.plugins.composeMultiplatform)
    // 5. Compose Compiler：用于 Compose 编译
    alias(libs.plugins.composeCompiler)
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
        commonMain.dependencies {
            implementation(project(":core:model"))
            implementation(project(":core:network"))
            implementation(project(":core:crypto"))
            implementation(libs.quickjs.kt)
            implementation(libs.quickjs.kt.converter)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.coroutinesCore)
            // Compose Resources 用于读取资源文件
            implementation(compose.components.resources)
        }
        androidMain.dependencies {
            implementation(libs.kotlinx.coroutinesAndroid)
        }
        jvmMain.dependencies {
            implementation(libs.kotlinx.coroutinesSwing)
            // Compose Runtime for resource loading
            implementation(compose.runtime)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        jvmTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutinesTest)
        }
    }
}

android {
    namespace = "com.guang.misty.engine"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}