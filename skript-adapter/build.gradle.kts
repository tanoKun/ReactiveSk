plugins {
    kotlin("jvm")
}

group = "com.github.tanokun"
version = "developing"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(8)
}