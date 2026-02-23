import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.changelog.Changelog
import org.jetbrains.changelog.markdownToHTML

plugins {
    id("java")
    // Because the root project applied KMP, Kotlin is already loaded globally.
    // We just apply it here without specifying a version.
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.compose")

    // We can now safely use the aliases because the root project resolved the classpath
    id("org.jetbrains.intellij.platform")
//    id("org.jetbrains.changelog")
}

group = providers.gradleProperty("pluginGroup").getOrElse("com.yourcompany.plugin")
version = providers.gradleProperty("pluginVersion").getOrElse("1.0.0")

kotlin {
    jvmToolchain(21)

    compilerOptions {
        freeCompilerArgs.addAll(
            listOf(
                "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi"
            )
        )
    }
}

repositories {
    mavenCentral()

    // IntelliJ Platform Gradle Plugin Repositories Extension
    intellijPlatform {
        defaultRepositories()
    }
    google()
}

dependencies {
    testImplementation(libs.junit)
    testImplementation(libs.opentest4j)
    testImplementation(libs.hamcrest)
    testImplementation(libs.composeuitest)
    testImplementation(libs.jewelstandalone)

    // Workaround for running tests on Windows and Linux
    testImplementation(libs.skikoAwtRuntimeAll)
    
    // Core MVVMate Dependencies
    implementation(project(":core"))
    
    // koog agents dependency
    implementation(libs.koog.agents) {
        exclude(group = "org.slf4j")
    }

    // Remote Debug Dependencies
    implementation(project(":remote-debug"))
    implementation(libs.java.websocket)
    implementation(libs.ktor.serialization.kotlinx.json)

    intellijPlatform {
        intellijIdea(providers.gradleProperty("platformVersion"))

        // Compose support dependencies
        @Suppress("UnstableApiUsage")
        composeUI()

        // Plugin Dependencies
        bundledPlugins(providers.gradleProperty("platformBundledPlugins").map { it.split(',') })
        plugins(providers.gradleProperty("platformPlugins").map { it.split(',') })

        testFramework(TestFrameworkType.Platform)
    }
}

intellijPlatform {
    pluginConfiguration {
        name = providers.gradleProperty("pluginName")
        version = providers.gradleProperty("pluginVersion")

        // Extracts description from the README.md located in the studio-plugin directory
        description = "This is the description"

        ideaVersion {
            sinceBuild = providers.gradleProperty("pluginSinceBuild")
        }
    }

    signing {
        certificateChain = providers.environmentVariable("CERTIFICATE_CHAIN")
        privateKey = providers.environmentVariable("PRIVATE_KEY")
        password = providers.environmentVariable("PRIVATE_KEY_PASSWORD")
    }

    publishing {
        token = providers.environmentVariable("PUBLISH_TOKEN")
        channels = providers.gradleProperty("pluginVersion")
            .map { listOf(it.substringAfter('-', "").substringBefore('.').ifEmpty { "default" }) }
    }

    pluginVerification {
        ides {
            create(providers.gradleProperty("platformType"), providers.gradleProperty("platformVersion"))
        }
    }
}

tasks {
    publishPlugin {
    }
}

// Do not run Plugin Verifier in the template itself
tasks.named("verifyPlugin") {
    enabled = false
}