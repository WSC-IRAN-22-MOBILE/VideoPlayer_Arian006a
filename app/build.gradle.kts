plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.myvideos"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.myvideos"
        minSdk = 33
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
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    dependencies {
        implementation (libs.material.v190)

        implementation (libs.recyclerview)

        implementation (libs.cardview)

        implementation (libs.activity.v161)

        implementation (libs.glide)
        annotationProcessor (libs.compiler)
    }

}