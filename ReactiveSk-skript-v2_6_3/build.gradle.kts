plugins {
    kotlin("jvm")
    `java-library`
    alias(libs.plugins.bukkit.yaml)
    alias(libs.plugins.shadow)
    alias(libs.plugins.antlr)
}

group = "com.github.tanokun.reactivesk.v263"
version = "Developing-5-v2.6.3"

repositories {
    mavenCentral()
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    maven("https://oss.sonatype.org/content/repositories/snapshots")
    maven("https://repo.skriptlang.org/releases")
    maven("https://repo.songoda.com/repository/public/")
    maven("https://jitpack.io")
}

extra["reactive.jvmToolchain"] = 8
apply(plugin = "com.tanokun.reactive-convention")

tasks {
    val copyJarToPlugins by registering(Copy::class) {
        dependsOn(shadowJar)
        from(shadowJar.get().archiveFile)
        into("C:/Users/owner/Desktop/1.17.1 paper/plugins")
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