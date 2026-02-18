import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import pl.allegro.tech.build.axion.release.domain.PredefinedVersionCreator

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.kotlinKotlinxSerialization)
    alias(libs.plugins.detekt)
    alias(libs.plugins.tinyJib)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.axionRelease)
    application
}

scmVersion {
    tag {
        prefix = "v"
    }
    nextVersion {
        suffix = "SNAPSHOT"
        separator = "-"
    }
    versionCreator = PredefinedVersionCreator.SIMPLE.versionCreator
}

group = "tel.schich"
version = scmVersion.version

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.ktorServerCore)
    implementation(libs.ktorServerNetty)
    implementation(libs.ktorServerCors)
    implementation(libs.ktorServerCachingHeaders)
    implementation(libs.ktorClientCore)
    implementation(libs.ktorClientJava)
    implementation(libs.ktorSerialization)
    implementation(libs.kotlinxCoroutinesCore)
    implementation(libs.kotlinxSerializationJson)
    implementation(libs.kotlinLogging)
    implementation(libs.logbackClassic)
    implementation(libs.cache4k)
}

tasks.test {
    useJUnitPlatform()
}

val javaTarget = 25
kotlin {
    jvmToolchain(javaTarget)
    compilerOptions {
        jvmTarget = JvmTarget.fromTarget(javaTarget.toString())
    }
}

application {
    mainClass.set("tel.schich.poeditorproxy.MainKt")
}

tinyJib {
    from {
        image = "eclipse-temurin:$javaTarget-jre-alpine"
    }
    container {
        ports = listOf("8080")
        mainClass = application.mainClass
    }
    to {
        image = "ghcr.io/pschichtel/$name:$version"
    }
}

ktlint {
    version = "1.4.1"
}
