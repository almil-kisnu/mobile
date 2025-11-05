plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("kotlin-parcelize")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "com.almil.dessertcakekinian"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.almil.dessertcakekinian"
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
    implementation("io.ktor:ktor-client-android:2.3.0")   // ✅
    implementation("io.ktor:ktor-client-core:2.3.0")      // ✅

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")   // ✅

    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")   // ✅

    implementation("io.github.jan-tennert.supabase:gotrue-kt:2.0.0")    // ✅ auth (login/register)
    implementation("io.github.jan-tennert.supabase:postgrest-kt:2.0.0") // ✅ database query
    implementation("io.github.jan-tennert.supabase:storage-kt:2.0.0")   // ⚠️ kalau gak upload file bisa dibuang
    implementation("io.github.jan-tennert.supabase:realtime-kt:2.0.0")  // ⚠️ kalau gak butuh realtime listener bisa dibuang

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.navigation.fragment.ktx)
    implementation(libs.navigation.ui.ktx)
    implementation(libs.androidx.legacy.support.v4)
    implementation(libs.androidx.recyclerview)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}