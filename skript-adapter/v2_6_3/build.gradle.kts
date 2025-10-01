plugins {
    kotlin("jvm")
    alias(libs.plugins.ksp)
}

repositories {
    maven("https://repo.skriptlang.org/releases")
    mavenCentral()
}

dependencies {
    compileOnly(libs.spigot)
    compileOnly(libs.skript.api.v263)

    implementation(libs.bundles.koin)

    implementation(project(":lang"))
    implementation(project(":compiler:frontend"))
    implementation(project(":compiler:backend"))
    implementation(project(":skript-adapter:common"))

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(8)
}