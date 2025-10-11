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

extra["reactive.jvmToolchain"] = 17
apply(plugin = "com.tanokun.reactive-convention")

tasks.named<AntlrTask>("generateGrammarSource") {
    outputDirectory = file("build/generated-src/antlr/main")
    arguments = arguments + listOf("-package", "com.github.tanokun.addon", "-visitor")
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