pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

val harnessConsumerSdkRepositoryOverride =
    providers
        .gradleProperty("harnessConsumerSdkRepository")
        .orNull
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
val harnessConsumerSdkVersionOverride =
    providers
        .gradleProperty("harnessConsumerSdkVersion")
        .orNull
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
check((harnessConsumerSdkRepositoryOverride == null) == (harnessConsumerSdkVersionOverride == null)) {
    "harnessConsumerSdkRepository and harnessConsumerSdkVersion must be provided together"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        harnessConsumerSdkRepositoryOverride?.let { candidateRepository ->
            maven {
                name = "harnessConsumerSdkCandidate"
                url = uri(candidateRepository)
                content {
                    includeGroup("io.github.daniele21.localllm")
                }
            }
        }
        maven {
            name = "harnessConsumerSdk"
            url = uri("https://raw.githubusercontent.com/daniele21/android-local-llm-harness/consumer-sdk-maven/maven")
            content {
                includeGroup("io.github.daniele21.localllm")
            }
        }
    }
}

rootProject.name = "redactguard-android"
include(":app")
