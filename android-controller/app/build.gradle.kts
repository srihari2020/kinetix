plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.kinetix.controller.v2"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.kinetix.controller.v2"
        minSdk = 23
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"

        ndk {
            abiFilters.addAll(listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64"))
        }
    }

    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }

    // 🔥 FIX: Java 17 compatibility
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    // 🔥 FIX: Important for native libs (WebRTC, etc.)
    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
    }

    signingConfigs {
        create("release") {
            storeFile = file("kinetix.jks")
            storePassword = "kinetix123"
            keyAlias = "kinetix"
            keyPassword = "kinetix123"
            enableV1Signing = true
            enableV2Signing = true
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")

            isDebuggable = false
            isJniDebuggable = false

            // 🔥 IMPORTANT (DON’T ENABLE MINIFY)
            isMinifyEnabled = false
            isShrinkResources = false

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }

        debug {
            isDebuggable = true
        }
    }
}

dependencies {

    // Android Core
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.recyclerview:recyclerview:1.3.2")

    // 🔥 WebSocket (VERY IMPORTANT)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // JSON
    implementation("com.google.code.gson:gson:2.10.1")

    // 🔥 WebRTC (heavy lib → increases APK size properly)
    implementation("io.getstream:stream-webrtc-android:1.1.1")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
}