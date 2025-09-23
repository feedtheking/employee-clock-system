plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
    id("org.jetbrains.kotlin.kapt")
}

android {
    namespace = "com.ttri.clockapp"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.ttri.clockapp"
        minSdk = 23
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

    buildFeatures {
        viewBinding = true
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
    // AndroidX + Material
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // Firebase BoM (keeps all Firebase libs on the same version)
    implementation(platform("com.google.firebase:firebase-bom:33.1.2"))

    // Firebase Services
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-analytics-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx") // ✅ Firestore

    // Room (database)
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1") // annotation processor

    // WorkManager (background sync)
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    implementation("com.google.firebase:firebase-storage-ktx")


}
apply(plugin = "com.google.gms.google-services")
