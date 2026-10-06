import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

val backendProperties = Properties().apply {
    rootProject.file("../hoode-admin/backend/public.properties").inputStream().use { load(it) }
}
fun backendString(name: String): String = "\"" + backendProperties.getProperty(name)
    .replace("\\", "\\\\").replace("\"", "\\\"") + "\""

android {
    sourceSets.getByName("main").kotlin.directories.add(rootProject.file("../hoode-admin/backend/android/src/main/java").path)
    namespace = "com.example.hoode_app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.hoodeconnect.app"
        minSdk = 26
        targetSdk = 36
        buildConfigField("String", "SUPABASE_URL", backendString("SUPABASE_URL"))
        buildConfigField("String", "SUPABASE_ANON_KEY", backendString("SUPABASE_ANON_KEY"))
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
    implementation("androidx.exifinterface:exifinterface:1.3.7")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
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
