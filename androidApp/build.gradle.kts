import com.android.build.api.variant.FilterConfiguration
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.koin.compiler)
}

val keystorePropertiesFile = rootProject.file("key.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.isFile) {
        keystorePropertiesFile.inputStream().use { load(it) }
    }
}
val hasReleaseSigning = keystorePropertiesFile.isFile

kotlin {
    jvmToolchain(21)
}

android {
    namespace = "com.xingheyuzhuan.shiguangschedule"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.xingheyuzhuan.shiguangschedule"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 44
        versionName = "2.1.0-njupt.12"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = rootProject.file(
                    keystoreProperties.getProperty("storeFile")
                        ?: throw GradleException("key.properties 缺少必填项：storeFile")
                ).also {
                    if (!it.isFile) {
                        throw GradleException("找不到签名文件：${it.absolutePath}")
                    }
                }
                storePassword = keystoreProperties.getProperty("storePassword")
                    ?: throw GradleException("key.properties 缺少必填项：storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                    ?: throw GradleException("key.properties 缺少必填项：keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
                    ?: throw GradleException("key.properties 缺少必填项：keyPassword")
                storeType = keystoreProperties.getProperty("storeType") ?: "PKCS12"
            }
        }
    }

    buildTypes {
        release {
            optimization {
                enable = true
            }
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    splits {
        abi {
            isEnable = true
            exclude("mips", "mips64", "armeabi", "riscv64", "x86")
            isUniversalApk = false
            include("armeabi-v7a", "arm64-v8a", "x86_64")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    androidResources {
        generateLocaleConfig = true
        localeFilters += listOf("zh", "zh-rCN", "zh-rTW", "en")
    }
}

dependencies {
    implementation(project(":shared"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.core.remoteviews)
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.animation)
    implementation(libs.androidx.datastore.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.datetime)
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.core)
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.workmanager)
    implementation(libs.koin.compose)
    implementation(libs.koin.compose.viewmodel)
    implementation(libs.koin.annotations)
    debugImplementation(libs.compose.ui.tooling)
}

androidComponents {
    onVariants { variant ->
        val buildType = variant.buildType ?: ""
        val versionName = android.defaultConfig.versionName ?: ""

        variant.outputs.forEach { output ->
            val abiFilter = output.filters.find {
                it.filterType == FilterConfiguration.FilterType.ABI
            }?.identifier ?: "universal"

            // 动态设置输出的 APK 文件名
            output.outputFileName.set("shiguangschedule-v${versionName}-${abiFilter}-${buildType}.apk")
        }
    }
}