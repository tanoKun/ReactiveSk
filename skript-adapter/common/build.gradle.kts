plugins {
    kotlin("jvm")
    alias(libs.plugins.ksp)
}

repositories {
    maven("https://repo.skriptlang.org/releases")
    maven("https://repo.destroystokyo.com/repository/maven-public")
    mavenCentral()
}

dependencies {
    compileOnly(libs.skript.api.v263)

    implementation(libs.bundles.koin)

    implementation(libs.bytebuddy)

    implementation(project(":lang"))
    implementation(project(":compiler:frontend"))
    implementation(project(":compiler:backend"))

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(8)
}