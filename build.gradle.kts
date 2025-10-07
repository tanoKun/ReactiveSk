

plugins {
    kotlin("jvm") version "2.2.0"
}

kotlin {
    jvmToolchain(8)
}

repositories {
    mavenCentral()
}

allprojects {
    group = "com.github.tanokun"
    version = "developing"

    repositories {
        mavenCentral()

        maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
        maven("https://oss.sonatype.org/content/repositories/snapshots")
        maven("https://repo.skriptlang.org/releases")

        maven("https://jitpack.io")
    }
}