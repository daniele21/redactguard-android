pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
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
