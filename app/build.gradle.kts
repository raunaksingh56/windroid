/*
 * WinDroid - App Build Configuration
 * Created by Raunak Singh
 * Updated: signing config reads from environment variables (GitHub Actions)
 */

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace  = "com.raunaksingh.windroid"
    compileSdk = 34

    defaultConfig {
        applicationId  = "com.raunaksingh.windroid"
        minSdk         = 26          // Android 8.0+ (USB Host API stable)
        targetSdk      = 34
        versionCode    = 1
        versionName    = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }
    }

    // ── Signing ──────────────────────────────────────────────────────────────
    // Reads from environment variables set by GitHub Actions.
    // For local builds: create a local.properties or just use debug signing.
    signingConfigs {
        create("release") {
            val storeFile   = System.getenv("SIGNING_STORE_FILE")
            val storePass   = System.getenv("SIGNING_STORE_PASSWORD")
            val keyAlias    = System.getenv("SIGNING_KEY_ALIAS")
            val keyPass     = System.getenv("SIGNING_KEY_PASSWORD")

            if (storeFile != null && storePass != null && keyAlias != null && keyPass != null) {
                this.storeFile     = file(storeFile)
                this.storePassword = storePass
                this.keyAlias      = keyAlias
                this.keyPassword   = keyPass
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled   = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            val releaseCfg = signingConfigs.getByName("release")
            if (releaseCfg.storeFile != null) {
                signingConfig = releaseCfg
            }
        }
        debug {
            applicationIdSuffix = ".debug"
            isDebuggable        = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions { jvmTarget = "17" }

    buildFeatures { compose = true }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.4"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.01.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.animation:animation")

    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    testImplementation("junit:junit:4.13.2")
}
