import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("io.gitlab.arturbosch.detekt")
    id("com.google.cloud.tools.jib")
    id("org.jlleitschuh.gradle.ktlint")
    application
}

group = "tel.schich"
version = "1.0.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    val ktorVersion = "3.0.1"
    val coroutinesVersion = "1.9.0"
    val serializationVersion = "1.7.3"

    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
    implementation(kotlin("stdlib-jdk8"))
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("io.ktor:ktor-serialization:$ktorVersion")
    implementation("io.ktor:ktor-server-cors:$ktorVersion")
    implementation("io.ktor:ktor-server-caching-headers:$ktorVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$serializationVersion")
    implementation("io.github.microutils:kotlin-logging:3.0.5")
    implementation("ch.qos.logback:logback-classic:1.4.5")
    implementation("io.github.reactivecircus.cache4k:cache4k:0.13.0")
}

tasks.test {
    useJUnitPlatform()
}

val javaTarget = 21
kotlin {
    jvmToolchain(javaTarget)
    compilerOptions {
        jvmTarget = JvmTarget.fromTarget(javaTarget.toString())
    }
}

application {
    mainClass.set("tel.schich.poeditorproxy.MainKt")
}

jib {
    from {
        image = "eclipse-temurin:$javaTarget-jre-alpine"
    }
    container {
        ports = listOf("8080")
    }
    to {
        val dockerHubUsername = System.getenv("DOCKERHUB_USERNAME")
        val dockerHubPassword = System.getenv("DOCKERHUB_PASSWORD")
        if (dockerHubUsername != null && dockerHubPassword != null) {
            auth {
                username = dockerHubUsername
                password = dockerHubPassword
            }
        }
        image = "pschichtel/$name:$version"
    }
}

ktlint {
    setVersion("0.48.2")
}
