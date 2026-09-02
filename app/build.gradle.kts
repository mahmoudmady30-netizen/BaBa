import java.io.FileInputStream
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// This build runs fully offline by design — no online voice API is
// called at runtime, and no API key is baked in by default. These
// BuildConfig fields exist only as an optional, empty-by-default
// extension point: if ElevenLabsService.kt is ever wired back into
// SmartVoiceManager in the future, it reads credentials from here — a
// GitHub Actions secret (env var) or a local, git-ignored
// secrets.properties file — never a hardcoded value in source.
val secretsProperties = Properties().apply {
    val secretsFile = rootProject.file("secrets.properties")
    if (secretsFile.exists()) {
        load(FileInputStream(secretsFile))
    }
}
fun secretOrEnv(key: String): String =
    secretsProperties.getProperty(key) ?: System.getenv(key) ?: ""

// Release signing for the Google Play AAB. Google Play will not accept an
// unsigned or debug-signed bundle, so this reads a real upload keystore
// from either secrets.properties (local builds) or environment variables
// (GitHub Actions secrets) — never a keystore committed to git.
//
// If no keystore is configured, the signing config is simply not applied,
// so debug builds and `assembleDebug` keep working exactly as before
// instead of failing for contributors who don't have the key.
val keystorePath = secretOrEnv("KEYSTORE_FILE")
val keystorePassword = secretOrEnv("KEYSTORE_PASSWORD")
val keyAliasvalue = secretOrEnv("KEY_ALIAS")
val keyPasswordValue = secretOrEnv("KEY_PASSWORD")
val hasReleaseKeystore = keystorePath.isNotBlank() &&
    keystorePassword.isNotBlank() &&
    keyAliasvalue.isNotBlank() &&
    keyPasswordValue.isNotBlank()

android {
    namespace = "com.babakids.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.babakids.app"
        minSdk = 24
        targetSdk = 36
        versionCode = 2
        versionName = "1.0.1"

        buildConfigField("String", "ELEVENLABS_API_KEY", "\"${secretOrEnv("ELEVENLABS_API_KEY")}\"")
        buildConfigField("String", "ELEVENLABS_VOICE_ID", "\"${secretOrEnv("ELEVENLABS_VOICE_ID")}\"")
    }

    signingConfigs {
        if (hasReleaseKeystore) {
            create("release") {
                storeFile = file(keystorePath)
                storePassword = keystorePassword
                keyAlias = keyAliasvalue
                keyPassword = keyPasswordValue
            }
        }
    }

    buildTypes {
        release {
            // Shrinking is off deliberately: this app bundles a large
            // audio asset set and uses reflection-free Compose only, so
            // R8 gains little here while adding real risk of stripping
            // something used only at runtime. Turn on later with proper
            // testing, not blindly before a first release.
            isMinifyEnabled = false
            if (hasReleaseKeystore) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
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
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.activity:activity-compose:1.9.1")

    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
