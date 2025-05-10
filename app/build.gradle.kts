plugins {
    alias(libs.plugins.android.application)  // This should be fine if you have the alias in your `libs.versions.toml`
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.arlifelink"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.arlifelink"
        minSdk = 30
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
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.3.2"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    // Google Play Services
    implementation("com.google.android.gms:play-services-maps:18.1.0")
    implementation ("com.github.bumptech.glide:glide:4.12.0")
    annotationProcessor ("com.github.bumptech.glide:compiler:4.12.0")
    implementation ("de.hdodenhof:circleimageview:3.1.0")
    implementation("com.google.android.gms:play-services-base:18.5.0")
    implementation("com.google.android.gms:play-services-location:21.3.0")
    implementation("com.mapbox.maps:android:11.10.3")
    // Firebase BOM (Bill of Materials)
    implementation(platform("com.google.firebase:firebase-bom:33.9.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-firestore:24.8.1")
    implementation("com.google.firebase:firebase-database:20.2.1")
    implementation("com.google.firebase:firebase-auth")

    // UI Dependencies
    implementation("com.google.android.material:material:1.9.0")
    implementation("androidx.fragment:fragment:1.6.1")

    //AR
    implementation("com.google.ar:core:1.31.0")
    implementation ("de.javagl:obj:0.4.0")
    // AppCompat and other libraries (ensure these are defined in your `libs.versions.toml`)
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)

    // Testing Dependencies
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
