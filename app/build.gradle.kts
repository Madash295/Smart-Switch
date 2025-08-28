plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.madash.smartswitch"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.madash.smartswitch"
        minSdk = 24
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
        compose = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation("androidx.navigation:navigation-compose:2.9.3")
    implementation(libs.androidx.material)
    implementation(libs.androidx.material.icons.extended)
    implementation("androidx.datastore:datastore-preferences:1.1.7")
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation("com.google.accompanist:accompanist-navigation-animation:0.36.0")
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.core.splashscreen)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    implementation("com.google.accompanist:accompanist-pager:0.34.0")
    implementation("com.google.accompanist:accompanist-pager-indicators:0.34.0")
    implementation("androidx.compose.material3:material3:1.3.2")
    implementation("com.google.accompanist:accompanist-systemuicontroller:0.36.0")
    val lottieVersion = "6.0.0" // Use the latest stable version
    implementation(libs.lottie.compose)
}