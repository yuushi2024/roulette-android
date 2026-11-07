plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.yuushi2024.roulette"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.yuushi2024.roulette"
        minSdk = 21
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            val storeFilePath = System.getenv("ROULETTE_KEYSTORE") ?: "${rootDir}/keystore/release.jks"
            storeFile = file(storeFilePath)
            storePassword = System.getenv("ROULETTE_STORE_PASSWORD") ?: "roulette2024"
            keyAlias = System.getenv("ROULETTE_KEY_ALIAS") ?: "roulette"
            keyPassword = System.getenv("ROULETTE_KEY_PASSWORD") ?: "roulette2024"
        }
    }

    // The signing key is not committed, so a fresh clone can still build (unsigned)
    // until scripts/build-release.sh creates one.
    val releaseKeystore = file(
        System.getenv("ROULETTE_KEYSTORE") ?: "${rootDir}/keystore/release.jks",
    )

    buildTypes {
        release {
            isMinifyEnabled = false
            isShrinkResources = false
            if (releaseKeystore.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    packaging {
        resources.excludes += setOf("META-INF/*.kotlin_module")
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.4")
    implementation("androidx.activity:activity-ktx:1.9.1")

    testImplementation("junit:junit:4.13.2")
}
