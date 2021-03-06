import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

repositories {
    mavenCentral()
    jcenter()
}

plugins {
    kotlin("jvm") version "1.4.30"
}

val cliktVersion = "2.8.0"
val slf4jVersion = "1.7.30"
val guava = "30.1-jre"

dependencies {
    implementation("com.github.ajalt:clikt:$cliktVersion")
    implementation("org.slf4j:slf4j-api:$slf4jVersion")
    implementation("org.slf4j:slf4j-simple:$slf4jVersion")
    implementation("com.google.guava:guava:$guava")
}

tasks {
    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "11"
    }
}