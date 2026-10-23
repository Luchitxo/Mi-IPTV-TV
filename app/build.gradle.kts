plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.miiptvvodtv"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.miiptvvodtv"
        minSdk = 21
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    // Jetpack Compose BOM
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation("androidx.compose.material3:material3")

    // Jetpack Compose para TV (Material3 estable)
    implementation(libs.androidx.tv.material)

    // Lifecycle & Activity
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // AndroidX Media3 (ExoPlayer)
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.exoplayer.hls)
    implementation(libs.media3.exoplayer.dash)
    implementation(libs.media3.ui)

    // Coil para carga asíncrona de logotipos
    implementation(libs.coil.compose)

    // Conectividad de Red (Retrofit + Gson)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
}
