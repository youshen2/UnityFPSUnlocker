import java.util.Properties

plugins {
    alias(libs.plugins.agp.app)
    alias(libs.plugins.compose.compiler)
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}
val storeFilePath: String? = localProperties.getProperty("STORE_FILE") ?: System.getenv("STORE_FILE")
val storePasswordValue: String? =
    localProperties.getProperty("STORE_PASSWORD") ?: System.getenv("STORE_PASSWORD")
val keyAliasValue: String? = localProperties.getProperty("KEY_ALIAS") ?: System.getenv("KEY_ALIAS")
val keyPasswordValue: String? =
    localProperties.getProperty("KEY_PASSWORD") ?: System.getenv("KEY_PWD")

android {
    namespace = "io.github.hexstr.UnityFPSUnlocker"
    compileSdk = 37
    buildToolsVersion = "37.0.0"
    ndkVersion = "29.0.14206865"

    defaultConfig {
        applicationId = "io.github.hexstr.UnityFPSUnlocker"
        minSdk = 28
        targetSdk = 36
        versionCode = (project.findProperty("versionCode") as String?)?.toIntOrNull()
            ?: System.getenv("VERSION_CODE")?.toIntOrNull() ?: 2
        versionName = (project.findProperty("versionName") as String?)
            ?: System.getenv("VERSION_NAME") ?: "2.0.0-api101"
        buildConfigField("String", "BUILD_TIME", "\"${System.currentTimeMillis()}\"")

        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86", "x86_64")
        }
        externalNativeBuild {
            cmake {
                cppFlags += "-std=c++17"
            }
        }
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    signingConfigs {
        if (storeFilePath != null && storePasswordValue != null &&
            keyAliasValue != null && keyPasswordValue != null
        ) {
            register("release") {
                storeFile = file(storeFilePath)
                storePassword = storePasswordValue
                keyAlias = keyAliasValue
                keyPassword = keyPasswordValue
                storeType = "pkcs12"
                enableV1Signing = true
                enableV2Signing = true
                enableV3Signing = true
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles("proguard-rules.pro")
            signingConfig = if (storeFilePath != null) {
                signingConfigs["release"]
            } else {
                signingConfigs["debug"]
            }
        }
        debug {
            isMinifyEnabled = false
            isShrinkResources = false
        }
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        resources {
            merges += "META-INF/xposed/*"
            excludes += setOf(
                "META-INF/*.kotlin_module",
                "META-INF/*.version",
                "META-INF/*.proto",
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE*",
                "META-INF/NOTICE*",
            )
        }
        jniLibs {
            useLegacyPackaging = true
        }
    }

    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }
}

dependencies {
    compileOnly(libs.libxposed.api)
    implementation(libs.libxposed.service)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core)
    implementation(libs.miuix.ui)
    implementation(libs.miuix.preference)
    implementation(libs.miuix.icons)
    implementation(libs.navigationevent.compose)
}
