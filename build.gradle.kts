import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

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
    implementation("io.ktor:ktor-server-cors-jvm:3.0.1")
    implementation("io.ktor:ktor-server-caching-headers-jvm:3.0.1")
    val ktorVersion = "3.0.1"
    val coroutinesVersion = "1.6.4"
    val serializationVersion = "1.4.1"
    val junitVersion = "5.9.2"

    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
    implementation(kotlin("stdlib-jdk8"))
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("io.ktor:ktor-serialization:$ktorVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$serializationVersion")
    implementation("io.github.microutils:kotlin-logging:3.0.5")
    implementation("ch.qos.logback:logback-classic:1.4.5")
    implementation("io.github.reactivecircus.cache4k:cache4k:0.9.0")

    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersion")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitVersion")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$coroutinesVersion")
    testImplementation(kotlin("test-junit5"))
}

tasks.test {
    useJUnitPlatform()
}

val target = "17"
java.toolchain.languageVersion.set(JavaLanguageVersion.of(target))

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.fromTarget(target))
    }
}

application {
    mainClass.set("tel.schich.poeditorproxy.MainKt")
}

jib {
    from {
        image = "eclipse-temurin:17-jre-alpine"
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
