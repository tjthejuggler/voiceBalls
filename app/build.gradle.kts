import java.util.Properties

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(localPropertiesFile.inputStream())
}

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.example.voiceballs"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.voiceballs"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // Reads the key from local.properties and creates a BuildConfig field
        buildConfigField(
            "String",
            "PICOVOICE_ACCESS_KEY",
            "\"${localProperties.getProperty("PICOVOICE_ACCESS_KEY") ?: ""}\""
        )
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    
    lint {
        disable += "NullSafeMutableLiveData"
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // For ViewModel and LiveData (UI state management)
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.7.0")

    // For Coroutines (background tasks)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // For Picovoice (Wake Word and Speech-to-Text) - Using versions available on Maven Central
    implementation("ai.picovoice:porcupine-android:3.0.3")
    implementation("ai.picovoice:cheetah-android:1.1.1")
    implementation("ai.picovoice:android-voice-processor:1.0.2")

    // For RecyclerView (displaying lists)
    implementation("androidx.recyclerview:recyclerview:1.3.2")

    // For easy SharedPreferences access
    implementation("androidx.preference:preference-ktx:1.2.1")

    // For JSON serialization/deserialization
    implementation("com.google.code.gson:gson:2.10.1")
}