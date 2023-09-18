plugins {
    id("com.android.library")
    id("kotlin-android")
    id("kotlin-kapt")
}

tasks.preBuild {
    dependsOn(":domain:build")
}

android {
    namespace = "com.bwksoftware.android.seasync.data"
    compileSdk = 33

    defaultConfig {
        minSdk = 21

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(project(":domain"))

    implementation(libs.appcompat)
    androidTestImplementation(libs.junit4)
    androidTestImplementation(libs.androidx.test.espresso.core) {
        exclude(group = "androidx.annotation", module = "annotation")
    }

    implementation(libs.hilt.android)
    kapt(libs.hilt.ext.compiler)
    implementation(libs.adapter.rxjava2)
    implementation(libs.converter.gson)
    implementation(libs.okhttp.logging)
    implementation(libs.rxkotlin)
    implementation(libs.retrofit.core)
    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlin.gradlePlugin)
}
