plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.mehmetdem.dil"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.mehmetdem.dil"
        minSdk = 26
        targetSdk = 36
        versionCode = 4
        versionName = "0.3.1-phase03"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    signingConfigs {
        create("preview") {
            // Public development key: stable installs for preview APKs only.
            // Google Play builds must use Play App Signing, never this key.
            storeFile = file("preview.keystore")
            storePassword = "android"
            keyAlias = "uygulamam-dil-preview"
            keyPassword = "android"
            enableV1Signing = true
            enableV2Signing = true
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("preview")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        resources.excludes += setOf("/META-INF/{AL2.0,LGPL2.1}")
    }
}

dependencies {
    implementation(project(":android:core:model"))
    implementation(project(":android:core:designsystem"))
    implementation(project(":android:core:data"))
    implementation(project(":android:feature:home"))
    implementation(project(":android:feature:lesson"))
    implementation(project(":android:feature:library"))
    implementation(project(":android:feature:profile"))
    implementation(project(":android:feature:onboarding"))

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.kotlinx.coroutines.core)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
