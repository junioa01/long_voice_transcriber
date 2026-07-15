plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.voicetranscriber"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.voicetranscriber"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    
    kotlinOptions {
        jvmTarget = "17"
    }
    
    buildFeatures {
        compose = true
    }
    
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.9"
    }
}

dependencies {
    // Jetpack Compose & Material 3
    val composeBom = platform("androidx.compose:compose-bom:2024.02.02")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    
    // Added Extended Icons for ContentCopy
    implementation("androidx.compose.material:material-icons-extended")
    
    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")
    
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Updated Google AI SDK to 0.9.0 for inlineData support
    implementation("com.google.ai.client.generativeai:generativeai:0.9.0")
}
