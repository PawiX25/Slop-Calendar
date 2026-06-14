import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.pawix.caltimeline"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.pawix.caltimeline"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2026.05.01"))

    // Core + Compose (Material 3 Expressive lives in material3 1.4.0, pinned by the BOM above)
    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.10.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    // Material 3 Expressive (MaterialExpressiveTheme, expressive color schemes) became
    // public in the 1.5.0 alpha train. alpha18 rides the same Compose 1.11 train as the
    // BOM above (foundation 1.11.x), so it stays compatible with compileSdk 36 / AGP 8.13.
    implementation("androidx.compose.material3:material3:1.5.0-alpha18")
    implementation("androidx.compose.material:material-icons-extended")

    // Glance — Compose-style home screen widget
    implementation("androidx.glance:glance-appwidget:1.1.1")
    implementation("androidx.glance:glance-material3:1.1.1")

    // Persisted calendar selection
    implementation("androidx.datastore:datastore-preferences:1.2.1")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
