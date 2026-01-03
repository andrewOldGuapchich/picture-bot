plugins {
    kotlin("jvm") version "2.0.10"
    application
    java
}

group = "com.andrew.tg"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.telegram:telegrambots:6.8.0")
    implementation("com.fasterxml.jackson.core:jackson-core:2.20.1")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.15.2")
    implementation("org.slf4j:slf4j-simple:2.0.9")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
}
tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(17)
}

application {
    mainClass.set("com.andrew.tg.Main")
}

tasks {
    jar {
        manifest {
            attributes["Main-Class"] = "com.andrew.tg.Main"
        }
        from(configurations.runtimeClasspath.get().map {
            if (it.isDirectory) it else zipTree(it)
        })

        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }
}