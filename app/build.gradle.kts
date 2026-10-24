plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.molina.studio"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.molina.studio"
        minSdk = 26
        targetSdk = 28 // exec binary PRoot dari data dir (lihat blueprint)
        versionCode = 1
        versionName = "0.1.0"
        ndk { abiFilters += listOf("arm64-v8a") }
    }

    signingConfigs {
        // Kunci tetap dari secrets CI; tanpa env ini dipakai keystore debug bawaan.
        val ksPath = System.getenv("MOLINA_KEYSTORE_FILE")
        if (ksPath != null && File(ksPath).isFile) {
            create("molina") {
                storeFile = File(ksPath)
                storeType = "pkcs12"
                storePassword = System.getenv("MOLINA_KEYSTORE_PASSWORD")
                keyAlias = "molina"
                keyPassword = System.getenv("MOLINA_KEYSTORE_PASSWORD")
            }
        }
    }

    buildTypes {
        debug { signingConfigs.findByName("molina")?.let { signingConfig = it } }
        release {
            isMinifyEnabled = false
            signingConfigs.findByName("molina")?.let { signingConfig = it }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
    lint { abortOnError = false }
    packaging { jniLibs { useLegacyPackaging = true } }
}

dependencies {
    implementation(project(":terminal-view"))
    implementation(project(":feature:mpv"))
    implementation(project(":engine:newpipe"))
    implementation(project(":core:common"))
    implementation(project(":core:storage"))
    implementation("org.apache.commons:commons-compress:1.27.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.datastore:datastore-preferences:1.1.1")
}
