import org.gradle.jvm.tasks.Jar
import java.io.FileInputStream
import java.util.*

val githubPropertiesFile = rootProject.file("github.properties");
val githubProperties = Properties()
githubProperties.load(FileInputStream(githubPropertiesFile))

plugins {
    java
    id("maven-publish")
    val springBootVersion = "2.4.1"
    id("org.springframework.boot") version springBootVersion
    id("io.spring.dependency-management") version "1.0.11.RELEASE"
}

buildscript {
    val springBootVersion = "2.4.1"
    repositories {
        jcenter()
        mavenCentral()
    }
    dependencies {
        classpath("org.springframework.boot:spring-boot-gradle-plugin:$springBootVersion")
    }
}

group = "pro.komdosh"
description = "searchable-rest-entity"
version = "0.0.6"

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
    implementation("org.apache.commons:commons-lang3:3.10")

    implementation("com.github.spullara.mustache.java:compiler:0.9.7")
    annotationProcessor("com.google.auto.service:auto-service:1.0-rc7")
    compileOnly("com.google.auto.service:auto-service:1.0-rc7")
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

publishing {
    repositories {
        maven {
            name = "SearchableRestEntity"
            url = uri("https://maven.pkg.github.com/Komdosh/SearchableRestEntity")
            credentials {
                username = githubProperties["gpr.user"] as String? ?: System.getenv("USERNAME")
                password = githubProperties["gpr.key"] as String? ?: System.getenv("TOKEN")
            }
        }
    }

    publications {
        create<MavenPublication>("gpr") {
            run {
                groupId = project.group as String?
                artifactId = project.name
                version = project.version as String?
                artifact("$buildDir/libs/$artifactId-$version.jar")
            }
        }
    }
}
