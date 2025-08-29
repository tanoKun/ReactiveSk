group = "com.github.tanokun"
version = "developing"

plugins {
    kotlin("jvm") version "2.2.0"
}

kotlin {
    jvmToolchain(8)
}

repositories {
    mavenCentral()
}