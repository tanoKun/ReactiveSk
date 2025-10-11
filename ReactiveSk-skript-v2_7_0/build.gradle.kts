import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    `java-library`
    alias(libs.plugins.bukkit.yaml)
    alias(libs.plugins.shadow)
    alias(libs.plugins.antlr)
}

group = "com.github.tanokun.reactivesk.v270"
version = "Developing-1-v2.7.0"

repositories {
    mavenCentral()
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    maven("https://oss.sonatype.org/content/repositories/snapshots")
    maven("https://repo.skriptlang.org/releases")
    maven("https://repo.songoda.com/repository/public/")
    maven("https://jitpack.io")
}

kotlin {
    jvmToolchain(17)
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

tasks.named<JavaCompile>("compileJava") {
    sourceCompatibility = "17"
    targetCompatibility = "17"
    options.encoding = "UTF-8"
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions.jvmTarget.set(JvmTarget.JVM_17)
}

tasks {
    shadowJar {
        dependsOn(subprojects.map { it.tasks.named("test") })

        duplicatesStrategy = DuplicatesStrategy.EXCLUDE

        archiveBaseName.set("ReactiveSk")
        archiveVersion.set("developing")

        dependencies {
            exclude(dependency("com.ibm.icu:icu4j"))
        }

        mergeServiceFiles()
    }
}

tasks.generateTestGrammarSource {
    isEnabled = false
}

tasks.generateGrammarSource {
    isEnabled = false
}

dependencies {
    compileOnly(libs.spigot)
    compileOnly(libs.skript.api.v270)

    implementation(project(":ReactiveSk-skript-v2_6_3"))

    implementation(libs.bundles.mccoroutine)
    implementation(libs.kotlinx.coroutines)

    implementation(libs.bytebuddy.agent)
    implementation(libs.bytebuddy)

    implementation(libs.bundles.reactivesk.all)

    testImplementation(kotlin("test"))
}

bukkit {
    main = "com.github.tanokun.reactivesk.v270.ReactiveSkAddon"
    name = "ReactiveSk"

    apiVersion = "1.13"

    depend = listOf("Skript")
    version = project.version.toString()
}