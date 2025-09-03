plugins {
    kotlin("jvm")
}

group = "com.github.tanokun"
version = "developing"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":compiler:frontend"))
    implementation(project(":lang"))
    implementation(libs.bytebuddy)

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(8)
}