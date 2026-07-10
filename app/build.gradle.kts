plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.calmcalories.app"
    compileSdk = 35

    val keystoreFilePath = System.getenv("KEYSTORE_FILE_PATH") ?: project.findProperty("RELEASE_STORE_FILE")?.toString()
    val keystorePassword = System.getenv("KEYSTORE_PASSWORD") ?: project.findProperty("RELEASE_STORE_PASSWORD")?.toString()
    val keyAlias = System.getenv("KEY_ALIAS") ?: project.findProperty("RELEASE_KEY_ALIAS")?.toString()
    val keyPassword = System.getenv("KEY_PASSWORD") ?: project.findProperty("RELEASE_KEY_PASSWORD")?.toString()

    val keystoreFile = if (!keystoreFilePath.isNullOrEmpty()) file(keystoreFilePath) else null
    val hasKeystore = keystoreFile != null && keystoreFile.exists()
    val hasCredentials = !keystorePassword.isNullOrEmpty() && !keyAlias.isNullOrEmpty() && !keyPassword.isNullOrEmpty()

    signingConfigs {
        create("release") {
            if (hasKeystore && hasCredentials) {
                storeFile = keystoreFile
                storePassword = keystorePassword
                this.keyAlias = keyAlias
                this.keyPassword = keyPassword
            }
        }
    }

    defaultConfig {
        applicationId = "com.calmcalories.app"
        minSdk = 28
        targetSdk = 35
        versionCode = project.findProperty("versionCode")?.toString()?.toIntOrNull() ?: 1
        versionName = project.findProperty("versionName")?.toString() ?: "1.0.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (hasKeystore && hasCredentials) {
                signingConfig = signingConfigs.getByName("release")
            } else {
                signingConfig = signingConfigs.getByName("debug")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    aaptOptions {
        noCompress += "litertlm"
    }
}

kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.compose.ui:ui:1.7.8")
    implementation("androidx.compose.ui:ui-tooling-preview:1.7.8")
    implementation("androidx.compose.material3:material3:1.3.1")
    implementation("androidx.compose.material:material-icons-extended:1.7.8")
    implementation("androidx.navigation:navigation-compose:2.8.8")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("androidx.room:room-runtime:2.8.4")
    ksp("androidx.room:room-compiler:2.8.4")

    // LiteRT-LM Kotlin Android API
    implementation("com.google.ai.edge.litertlm:litertlm-android:latest.release")

    debugImplementation("androidx.compose.ui:ui-tooling:1.7.8")
    debugImplementation("androidx.compose.ui:ui-test-manifest:1.7.8")
}
