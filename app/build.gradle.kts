plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.hoode_app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.hoodeconnect.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }


    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

dependencies {
    // Core AndroidX
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)

    // Material Design
    implementation(libs.material)

    // Layout
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.viewpager2)
    implementation(libs.androidx.swiperefreshlayout)

    // Fragment
    implementation(libs.androidx.fragment.ktx)

    // Navigation
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)

    // Lifecycle
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // Image loading
    implementation(libs.coil)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}