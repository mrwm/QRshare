plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.wchung.qrshare"
    compileSdk = 36

    dependenciesInfo {
        // Disables dependency metadata when building APKs.
        includeInApk = false
        // Disables dependency metadata when building Android App Bundles.
        includeInBundle = false
    }

    defaultConfig {
        applicationId = "com.wchung.qrshare"
        minSdk = 21 // Lowest possible that androidx.appcompat 1.7.0 supports (Android 5)
        targetSdk = 36
        versionCode = 21
        versionName = "1.0.$versionCode"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isShrinkResources = true
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            ndk.debugSymbolLevel = "FULL"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.activity)
    implementation(libs.ui.android)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation(libs.core) // used in place of xzing library
}
