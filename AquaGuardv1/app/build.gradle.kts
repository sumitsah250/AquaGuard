plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.boss.aquaguardv1"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.boss.aquaguardv1"
        minSdk = 25
        targetSdk = 35
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
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    implementation("com.microsoft.onnxruntime:onnxruntime-android:1.19.0")

    //for chart
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
}