import org.gradle.jvm.tasks.Jar

plugins {
    java
    val kotlinVersion = "1.4.0"
    val springBootVersion = "2.3.3.RELEASE"
    id("org.springframework.boot") version springBootVersion
    id("io.spring.dependency-management") version "1.0.10.RELEASE"
    kotlin("jvm") version kotlinVersion
    kotlin("plugin.spring") version kotlinVersion
    kotlin("plugin.jpa") version kotlinVersion
}

buildscript {
    val kotlinVersion = "1.4.0"
    val springBootVersion = "2.3.3.RELEASE"
    repositories {
        jcenter()
        mavenCentral()
    }
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
        classpath("org.jetbrains.kotlin:kotlin-allopen:$kotlinVersion")
        classpath("org.springframework.boot:spring-boot-gradle-plugin:$springBootVersion")
    }
}

group = "pro.komdosh"
version = "0.0.1"

repositories {
    jcenter()
    mavenCentral()
}

dependencies {
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    compileOnly("org.springframework.boot:spring-boot-starter-web") {
        exclude("com.fasterxml.jackson.core", "jackson-databind")
        exclude("org.jboss.logging", "jboss-logging")
    }
    compileOnly("org.springframework.boot:spring-boot-starter-data-jpa")
    compileOnly("org.springframework.boot:spring-boot-starter-validation")

    //DTO Mapping
    implementation("org.mapstruct:mapstruct:1.3.1.Final")
    annotationProcessor("org.mapstruct:mapstruct-processor:1.3.1.Final")

    implementation("com.github.spullara.mustache.java:compiler:0.9.6")
    annotationProcessor("com.google.auto.service:auto-service:1.0-rc5")
    compileOnly("com.google.auto.service:auto-service:1.0-rc5")
}


tasks {
    register("fatJar", Jar::class.java) {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        from(configurations.runtimeClasspath.get()
                .onEach { println("add from dependencies: ${it.name}") }
                .map { if (it.isDirectory) it else zipTree(it) })
        val sourcesMain = sourceSets.main.get()
        sourcesMain.allSource.forEach { println("add from sources: ${it.name}") }
        from(sourcesMain.output)
    }
}

tasks {
    "build" {
        dependsOn("fatJar")
    }
}
