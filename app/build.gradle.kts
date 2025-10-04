plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.pawansingh.pdfeditor"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.pawansingh.pdfeditor"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

    // For editing, annotations, conversion
    implementation("com.tom-roush:pdfbox-android:2.0.27.0")

    // For displaying PDF (built-in, no native .so files)
//    implementation("androidx.pdf:pdf-renderer:1.0.1")

//    implementation("androidx.pdf:pdf")
    // implementation("com.github.barteksc:android-pdf-viewer:3.2.0-beta.1")

    // Image Picker
    implementation("com.github.dhaval2404:imagepicker:2.1")

//    implementation("com.github.chrisbanes:PhotoView:2.3.0")
//    implementation("com.otaliastudios:zoomlayout:1.9.0")

    // Lifecycle (for ViewModel)
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.9.4")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.9.4")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}