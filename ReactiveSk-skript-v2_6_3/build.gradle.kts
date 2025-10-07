import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    `java-library`
    alias(libs.plugins.bukkit.yaml)
    alias(libs.plugins.shadow)
    alias(libs.plugins.antlr)
}

group = "com.github.tanokun.reactivesk.v263"
version = "Developing-3-v2.6.3"

repositories {
    mavenCentral()
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    maven("https://oss.sonatype.org/content/repositories/snapshots")
    maven("https://repo.skriptlang.org/releases")
    maven("https://repo.songoda.com/repository/public/")

    maven {
        url = uri("https://maven.pkg.github.com/tanoKun/ReactiveSk-Common")
        credentials {
            username = System.getenv("GITHUB_ACTOR")
            password = System.getenv("GITHUB_TOKEN")
        }

        content {
            includeGroup("com.github.tanokun.reactivesk")
        }
    }
}

kotlin {
    jvmToolchain(8)
}

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

tasks {
    val copyJarToPlugins by registering(Copy::class) {
        dependsOn(shadowJar)
        from(shadowJar.get().archiveFile)
        into("C:/Users/owner/Desktop/1.12.2 paper/plugins")
    }

    shadowJar {
        dependsOn(subprojects.map { it.tasks.named("test") })

        duplicatesStrategy = DuplicatesStrategy.EXCLUDE

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

dependencies {
    compileOnly(libs.spigot)
    compileOnly(libs.skript.api.v263)

    implementation(libs.bundles.mccoroutine)
    implementation(libs.kotlinx.coroutines)

    implementation("net.bytebuddy:byte-buddy-agent:1.17.7")
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