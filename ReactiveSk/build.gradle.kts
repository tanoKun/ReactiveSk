import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    `java-library`
    alias(libs.plugins.bukkit.yaml)
    alias(libs.plugins.shadow)
    alias(libs.plugins.antlr)
}

group = "com.github.tanokun.reactivesk.v263"
version = "v1.0.0"

repositories {
    mavenCentral()
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    maven("https://oss.sonatype.org/content/repositories/snapshots")
    maven("https://repo.skriptlang.org/releases")
    maven("https://repo.songoda.com/repository/public/")
    maven("https://jitpack.io")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(8)
    }
}

tasks.named<JavaCompile>("compileJava") {
    sourceCompatibility = "8"
    targetCompatibility = "8"
    options.encoding = "UTF-8"
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions.jvmTarget.set(JvmTarget.JVM_1_8)
}

tasks.generateTestGrammarSource {
    isEnabled = false
}

tasks.generateGrammarSource {
    isEnabled = false
}

tasks {
    shadowJar {
        dependsOn(subprojects.map { it.tasks.named("test") })

        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        archiveFileName = "ReactiveSk-${project.version}.jar"

        dependencies {
            exclude(dependency("com.ibm.icu:icu4j"))
        }

        mergeServiceFiles()
    }

    test {
        useJUnitPlatform()
    }
}

dependencies {
    compileOnly(libs.spigot)
    compileOnly(libs.skript.api.v263)

    implementation(libs.bundles.mccoroutine)
    implementation(libs.kotlinx.coroutines)

    implementation(libs.bytebuddy.agent)
    implementation(libs.bytebuddy)

    implementation(libs.bundles.reactivesk.all)

    testImplementation(kotlin("test"))
}

bukkit {
    main = "com.github.tanokun.reactivesk.v263.ReactiveSkAddon"
    name = "ReactiveSk"

    depend = listOf("Skript")
    version = project.version.toString()
}