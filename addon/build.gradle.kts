import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
  //  java
    alias(libs.plugins.bukkit.yaml)
    alias(libs.plugins.shadow)
    alias(libs.plugins.antlr)
}

group = "com.github.tanokun"
version = "developing"

kotlin {
    jvmToolchain(8)
}

/*java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}*/

tasks.named<KotlinCompile>("compileKotlin") {
    dependsOn(tasks.named("generateGrammarSource"))
}

tasks.named<AntlrTask>("generateGrammarSource") {
    outputDirectory = file("build/generated-src/antlr/main")
    arguments = arguments + listOf("-package", "com.github.tanokun.addon", "-visitor")
}

tasks.named("generateTestGrammarSource") {
    enabled = false
}

sourceSets {
    main {
        kotlin {
            srcDir("build/generated-src/antlr/main")
        }
    }

    test {
        kotlin {
            srcDir("build/generated-src/antlr/main")
        }
    }
}

repositories {
    mavenCentral()

    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    maven("https://oss.sonatype.org/content/repositories/snapshots")
    maven("https://repo.skriptlang.org/releases")
}

dependencies {
    compileOnly(libs.spigot.api)
    compileOnly(libs.skript.api)
    implementation(libs.antlr.kotlin.runtime)
    implementation(libs.kotlinx.coroutines)
    implementation(libs.bundles.mccoroutine)
    implementation(libs.bytebuddy)
    antlr(libs.antlr.tool)

    testImplementation(libs.skript.api)
    testImplementation(libs.spigot.api)
    testImplementation(libs.mockk)
    testImplementation(kotlin("test"))
}

tasks {
    val copyJarToPlugins by registering(Copy::class) {
        dependsOn(shadowJar)
        from(shadowJar.get().archiveFile)
        into("C:/Users/owner/Desktop/1.12.2 paper/plugins")
    }

    shadowJar {
        dependsOn(subprojects.map { it.tasks.named("test") })

        archiveBaseName.set("ReactiveSk")
        archiveVersion.set("developing")

        dependencies {
            exclude(dependency("com.ibm.icu:icu4j"))
        }

        mergeServiceFiles()
        finalizedBy(copyJarToPlugins)
    }

    test {
        useJUnitPlatform()
    }
}

bukkit {
    main = "com.github.tanokun.addon.ReactiveSkAddon"
    name = "ReactiveSk"

    depend = listOf("Skript")
    version = "1.0.0"
}