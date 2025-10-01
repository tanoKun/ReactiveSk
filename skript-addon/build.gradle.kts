import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    //  java
    alias(libs.plugins.bukkit.yaml)
    alias(libs.plugins.shadow)
    alias(libs.plugins.antlr)
}

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

dependencies {
    compileOnly(libs.spigot)
    compileOnly(libs.skript.api.v263)

    implementation(libs.bundles.mccoroutine)
    implementation(libs.kotlinx.coroutines)

    implementation("net.bytebuddy:byte-buddy-agent:1.17.7")
    implementation(libs.bytebuddy)

    implementation(project(":lang"))
    implementation(project(":compiler:backend"))
    implementation(project(":compiler:frontend"))

    implementation(project(":skript-adapter:common"))
    implementation(project(":skript-adapter:v2_6_3"))

    testImplementation(kotlin("test"))
}

bukkit {
    main = "com.github.tanokun.reactivesk.addon.ReactiveSkAddon"
    name = "ReactiveSk"

    depend = listOf("Skript")
    version = "1.0.0"
}