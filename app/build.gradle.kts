plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.google.devtools.ksp)
    alias(libs.plugins.google.dagger.hilt)
    alias(libs.plugins.compose.compiler)
    id("com.google.android.libraries.mapsplatform.secrets-gradle-plugin")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
}

android {
    namespace = "com.devidea.aicar"
    compileSdk = 35

    defaultConfig {
        testInstrumentationRunner = "com.devidea.aicar.CustomTestRunner"
        applicationId = "com.devidea.aicar"
        minSdk = 26
        targetSdk = 35
        versionCode = 6
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
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
        viewBinding = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    secrets {
        propertiesFileName = "secrets.properties"
        defaultPropertiesFileName = "local.defaults.properties"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.accompanist.permissions)
    implementation(libs.androidx.runtime.livedata)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.bluetooth)
    implementation (libs.androidx.datastore.preferences)
    implementation(libs.androidx.lifecycle.runtime.compose.android)

    //hilt
    ksp(libs.hilt.compiler)
    implementation (libs.hilt.android)

    implementation (libs.androidx.hilt.navigation.compose)

    //room
    ksp(libs.androidx.room.compiler)
    //implementation(libs.androidx.room.rxjava2)
    implementation(libs.androidx.room.runtime)

    implementation(libs.reorderable)
    implementation (libs.maps.compose)

    implementation (libs.material)
    implementation (libs.gson)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)

    //compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.material3)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.adaptive)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.runtime.livedata)
    implementation(libs.androidx.runtime.rxjava2)
    implementation (libs.play.services.location)

    androidTestImplementation (libs.truth)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    androidTestImplementation(libs.hilt.android.testing)
    //kspAndroidTest (libs.hilt.compiler)
    //androidTestAnnotationProcessor (libs.hilt.compiler)

    debugImplementation(libs.androidx.ui.test.manifest)
    debugImplementation(libs.androidx.ui.tooling)
}