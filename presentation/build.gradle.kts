plugins {
    id("com.android.library")
    id("kotlin-android")
    id("kotlin-kapt")
}

android {
    namespace = "com.bwksoftware.android.seasync.presentation"
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

    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation(project(":domain"))
    implementation(project(":data"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.material)
    implementation(libs.androidx.cardview)
    implementation(libs.picasso)
    implementation(libs.imagezoom)
    androidTestImplementation(libs.junit4)
    androidTestImplementation(libs.androidx.test.espresso.core) {
        exclude(group = "androidx.annotation", module = "annotation")
    }

    implementation(libs.dagger)
    kapt(libs.dagger.compiler)
    implementation(libs.rxkotlin)
    implementation(libs.rxandroid)
    implementation(libs.universal.image.loader)
    implementation(libs.http.request)
    implementation(libs.okhttp)
    implementation(libs.picasso2.okhttp3.downloader)
    implementation(libs.recyclerview.fastscroll)
    implementation(libs.kotlin.stdlib)
}
