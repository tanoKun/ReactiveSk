plugins {
    kotlin("jvm")
    antlr
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":lang"))
    implementation(libs.antlr.kotlin.runtime)
    antlr(libs.antlr.tool)

    testImplementation(kotlin("test"))
    implementation(kotlin("test"))
}

tasks.generateGrammarSource {
    maxHeapSize = "64m"
    arguments = arguments + listOf("-visitor", "-long-messages")
}

tasks.compileKotlin {
    dependsOn(tasks.generateGrammarSource)
}

tasks.compileTestKotlin {
    dependsOn(tasks.generateTestGrammarSource)
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(8)
}