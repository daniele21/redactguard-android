plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.spotless)
}

group = "io.github.daniele21.redactguard"
version = "0.1.0-SNAPSHOT"

allprojects {
    dependencyLocking {
        lockAllConfigurations()
    }
}

spotless {
    kotlin {
        target("**/*.kt")
        targetExclude("**/build/**")
        ktlint()
        trimTrailingWhitespace()
        endWithNewline()
    }
    kotlinGradle {
        target("**/*.gradle.kts")
        targetExclude("**/build/**")
        ktlint()
        trimTrailingWhitespace()
        endWithNewline()
    }
}
