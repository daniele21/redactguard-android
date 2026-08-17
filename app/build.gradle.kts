plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

val sharedRuntimeReleaseHostPackage = "io.github.daniele21.localllm.phonetest"
val sharedRuntimeDebugHostPackage = "io.github.daniele21.localllm.phonetest.debug"
val sharedRuntimeHostService = "io.github.daniele21.localllm.phonetest.HarnessSharedRuntimeService"
val sharedRuntimeReleasePermission = "io.github.daniele21.localllm.permission.USE_LOCAL_LLM"
val sharedRuntimeDebugPermission = "io.github.daniele21.localllm.debug.permission.USE_LOCAL_LLM"

android {
    namespace = "io.github.daniele21.redactguard"
    compileSdk =
        libs.versions.compileSdk
            .get()
            .toInt()
    buildToolsVersion = libs.versions.buildTools.get()

    defaultConfig {
        applicationId = "io.github.daniele21.redactguard"
        minSdk =
            libs.versions.minSdk
                .get()
                .toInt()
        targetSdk =
            libs.versions.targetSdk
                .get()
                .toInt()
        versionCode = 1
        versionName = "0.1.0"
        manifestPlaceholders["sharedRuntimePermission"] = sharedRuntimeReleasePermission
        manifestPlaceholders["sharedRuntimeHostPackage"] = sharedRuntimeReleaseHostPackage
        buildConfigField("String", "SHARED_RUNTIME_HOST_PACKAGE", "\"$sharedRuntimeReleaseHostPackage\"")
        buildConfigField("String", "SHARED_RUNTIME_HOST_SERVICE", "\"$sharedRuntimeHostService\"")
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            manifestPlaceholders["sharedRuntimePermission"] = sharedRuntimeDebugPermission
            manifestPlaceholders["sharedRuntimeHostPackage"] = sharedRuntimeDebugHostPackage
            buildConfigField("String", "SHARED_RUNTIME_HOST_PACKAGE", "\"$sharedRuntimeDebugHostPackage\"")
        }
        release {
            isMinifyEnabled = true
            manifestPlaceholders["sharedRuntimePermission"] = sharedRuntimeReleasePermission
            manifestPlaceholders["sharedRuntimeHostPackage"] = sharedRuntimeReleaseHostPackage
            buildConfigField("String", "SHARED_RUNTIME_HOST_PACKAGE", "\"$sharedRuntimeReleaseHostPackage\"")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.pdfbox.android)
    implementation(libs.harness.consumer.android)

    debugImplementation(libs.androidx.compose.ui.tooling)
    testImplementation(libs.junit4)
}
