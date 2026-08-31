import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

fun buildConfigString(value: String): String = "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""

val redactGuardUploadSigningEnvironment =
    mapOf(
        "storeFile" to System.getenv("REDACTGUARD_ANDROID_UPLOAD_STORE_FILE"),
        "storePassword" to System.getenv("REDACTGUARD_ANDROID_UPLOAD_STORE_PASSWORD"),
        "keyAlias" to System.getenv("REDACTGUARD_ANDROID_UPLOAD_KEY_ALIAS"),
        "keyPassword" to System.getenv("REDACTGUARD_ANDROID_UPLOAD_KEY_PASSWORD"),
    )
val redactGuardUploadSigningConfigured =
    redactGuardUploadSigningEnvironment.values.all { !it.isNullOrBlank() }
val redactGuardUploadSigningPartiallyConfigured =
    redactGuardUploadSigningEnvironment.values.any { !it.isNullOrBlank() } && !redactGuardUploadSigningConfigured
val allowUnsignedRelease =
    System.getenv("REDACTGUARD_ALLOW_UNSIGNED_RELEASE").equals("true", ignoreCase = true)

val versionPropertiesFile = file("version.properties")
check(versionPropertiesFile.isFile) { "Missing app/version.properties" }
val versionProperties =
    Properties().apply {
        versionPropertiesFile.inputStream().use { load(it) }
    }
val currentVersionCode =
    versionProperties.getProperty("versionCode")?.toIntOrNull()
        ?: throw GradleException("app/version.properties must define an integer versionCode")
val currentVersionName =
    versionProperties.getProperty("versionName")?.takeIf { it.isNotBlank() }
        ?: throw GradleException("app/version.properties must define a non-empty versionName")
val playVersionCodeOverride =
    System
        .getenv("PLAY_VERSION_CODE")
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
        ?.let { raw ->
            raw.toIntOrNull()?.takeIf { it > 0 }
                ?: throw GradleException("PLAY_VERSION_CODE must be a positive integer")
        }
val effectiveVersionCode = playVersionCodeOverride ?: currentVersionCode

val sourceRevision =
    providers
        .gradleProperty("redactGuardSourceRevision")
        .orElse(providers.environmentVariable("REDACTGUARD_SOURCE_REVISION"))
        .orElse(providers.environmentVariable("GITHUB_SHA"))
        .orElse("unavailable")
        .get()
val sourceDirty =
    providers
        .gradleProperty("redactGuardSourceDirty")
        .orElse(providers.environmentVariable("REDACTGUARD_SOURCE_DIRTY"))
        .orElse("true")
        .get()
        .toBooleanStrictOrNull() ?: true
val buildId =
    providers
        .gradleProperty("redactGuardBuildId")
        .orElse(providers.environmentVariable("REDACTGUARD_BUILD_ID"))
        .orElse(providers.environmentVariable("GITHUB_RUN_ID").map { "gha-$it" })
        .orElse("development")
        .get()

val sharedRuntimeReleaseHostPackage = "io.github.daniele21.localllm.phonetest"
val sharedRuntimeDebugHostPackage = "io.github.daniele21.localllm.phonetest.debug"
val sharedRuntimeHostService = "io.github.daniele21.localllm.phonetest.HarnessSharedRuntimeService"
val sharedRuntimeReleasePermission = "io.github.daniele21.localllm.permission.USE_LOCAL_LLM"
val sharedRuntimeDebugPermission = "io.github.daniele21.localllm.debug.permission.USE_LOCAL_LLM"

gradle.taskGraph.whenReady {
    val packagesRelease =
        allTasks.any { task ->
            task.path == ":app:bundleRelease" || task.path == ":app:assembleRelease"
        }
    if (redactGuardUploadSigningPartiallyConfigured) {
        throw GradleException(
            "RedactGuard release signing is incomplete. Set all REDACTGUARD_ANDROID_UPLOAD_* variables; " +
                "never commit upload-key material.",
        )
    }
    if (packagesRelease && !redactGuardUploadSigningConfigured && !allowUnsignedRelease) {
        throw GradleException(
            "RedactGuard release signing is not configured. Use scripts/build-redactguard-release.sh build, " +
                "or set REDACTGUARD_ALLOW_UNSIGNED_RELEASE=true only for an intentional unsigned CI artifact.",
        )
    }
}

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
        versionCode = effectiveVersionCode
        versionName = currentVersionName
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        manifestPlaceholders["sharedRuntimePermission"] = sharedRuntimeReleasePermission
        manifestPlaceholders["sharedRuntimeHostPackage"] = sharedRuntimeReleaseHostPackage
        buildConfigField("String", "SHARED_RUNTIME_HOST_PACKAGE", buildConfigString(sharedRuntimeReleaseHostPackage))
        buildConfigField("String", "SHARED_RUNTIME_HOST_SERVICE", buildConfigString(sharedRuntimeHostService))
        buildConfigField("String", "REDACTGUARD_BUILD_ID", buildConfigString(buildId))
        buildConfigField("String", "SOURCE_REVISION", buildConfigString(sourceRevision))
        buildConfigField("boolean", "SOURCE_DIRTY", sourceDirty.toString())
    }

    signingConfigs {
        create("upload") {
            if (redactGuardUploadSigningConfigured) {
                storeFile = file(redactGuardUploadSigningEnvironment.getValue("storeFile")!!)
                storePassword = redactGuardUploadSigningEnvironment.getValue("storePassword")
                keyAlias = redactGuardUploadSigningEnvironment.getValue("keyAlias")
                keyPassword = redactGuardUploadSigningEnvironment.getValue("keyPassword")
                storeType = "PKCS12"
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            manifestPlaceholders["sharedRuntimePermission"] = sharedRuntimeDebugPermission
            manifestPlaceholders["sharedRuntimeHostPackage"] = sharedRuntimeDebugHostPackage
            buildConfigField("String", "SHARED_RUNTIME_HOST_PACKAGE", buildConfigString(sharedRuntimeDebugHostPackage))
        }
        release {
            isMinifyEnabled = true
            manifestPlaceholders["sharedRuntimePermission"] = sharedRuntimeReleasePermission
            manifestPlaceholders["sharedRuntimeHostPackage"] = sharedRuntimeReleaseHostPackage
            buildConfigField("String", "SHARED_RUNTIME_HOST_PACKAGE", buildConfigString(sharedRuntimeReleaseHostPackage))
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (redactGuardUploadSigningConfigured) {
                signingConfig = signingConfigs.getByName("upload")
            }
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
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.pdfbox.android)
    implementation(libs.harness.consumer.android)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    testImplementation(libs.junit4)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
}
