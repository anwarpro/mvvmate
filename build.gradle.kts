plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.android.kotlin.multiplatform.library) apply false
    alias(libs.plugins.jetbrainsCompose) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
//    alias(libs.plugins.jreleaser) apply false
    alias(libs.plugins.dokka) apply false
    alias(libs.plugins.vanniktech.mavenPublish) apply false

    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0" apply false

    alias(libs.plugins.intelliJPlatform) apply false
    alias(libs.plugins.changelog) apply false

}