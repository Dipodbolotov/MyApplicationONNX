plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.example.myapplication"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.myapplication"
        minSdk = 26
        targetSdk = 34
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

//    implementation(libs.androidx.core.ktx)
    implementation("androidx.core:core-ktx:1.13.1")

    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    api("io.kinference", "inference-core", "0.2.26")
    api("io.kinference", "inference-ort", "0.2.26")
    api("io.kinference", "inference-api", "0.2.26")
    api("io.kinference", "serializer-protobuf", "0.2.26")
    api("io.kinference", "utils-common", "0.2.26")
    api("io.kinference", "ndarray-api", "0.2.26")
    api("io.kinference", "ndarray-core", "0.2.26")

    implementation("org.jetbrains.kotlinx:kotlin-deeplearning-api:0.5.2")
    implementation("org.jetbrains.kotlinx:kotlin-deeplearning-dataset:0.5.2")  // Dataset support

    implementation("io.ktor:ktor-client-core:2.3.12")
    implementation("io.ktor:ktor-client-cio:2.3.12") // JVM Engine

    api("org.slf4j:slf4j-api:2.0.9")
    api("org.slf4j:slf4j-simple:2.0.9")

    implementation("ai.djl:api:0.28.0")
    implementation("ai.djl.huggingface:tokenizers:0.28.0")


    implementation("be.tarsos.dsp:core:2.5")
    implementation("be.tarsos.dsp:jvm:2.5")

}
