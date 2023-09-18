plugins {
    id("com.android.library")
    id("kotlin-android")
    id("kotlin-kapt")
}

android {
    namespace = "com.bwksoftware.android.seasync.domain"
    compileSdk = 33

    defaultConfig {
        namespace = "com.bwksoftware.android.seasync.domain"
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
    implementation(libs.appcompat)
    androidTestImplementation(libs.junit4)
    androidTestImplementation(libs.androidx.test.espresso.core) {
        exclude(group = "androidx.annotation", module = "annotation")
    }

    implementation(libs.rxkotlin)
    implementation(libs.retrofit.core)
    implementation(libs.kotlin.stdlib)
}
