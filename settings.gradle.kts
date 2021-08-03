
rootProject.name = "poeditor-proxy"

pluginManagement {

    val kotlinVersion: String by settings
    val detektVersion: String by settings
    val jibVersion: String by settings

    plugins {
        kotlin("jvm") version(kotlinVersion)
        kotlin("plugin.serialization") version(kotlinVersion)
        id("io.gitlab.arturbosch.detekt") version(detektVersion)
        id("com.google.cloud.tools.jib") version(jibVersion)
    }
}