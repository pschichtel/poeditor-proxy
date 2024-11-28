rootProject.name = "poeditor-proxy"

pluginManagement {
    val kotlinVersion: String by settings
    val detektVersion: String by settings
    val jibVersion: String by settings
    val ktlintVersion: String by settings

    plugins {
        kotlin("jvm") version(kotlinVersion)
        kotlin("plugin.serialization") version(kotlinVersion)
        id("io.gitlab.arturbosch.detekt") version(detektVersion)
        id("com.google.cloud.tools.jib") version(jibVersion)
        id("org.jlleitschuh.gradle.ktlint") version(ktlintVersion)
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}
