package com.tanokun.buildsrc

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.testing.Test
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

class ReactiveConventionPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val toolchain = project.findProperty("reactive.jvmToolchain")?.toString()?.toIntOrNull() ?: 8

        project.plugins.withId("org.jetbrains.kotlin.jvm") {
            project.extensions.configure(KotlinJvmProjectExtension::class.java) {
                jvmToolchain(toolchain)
            }
        }

        project.tasks.withType(KotlinCompile::class.java).configureEach {
            dependsOn("generateGrammarSource")
        }

        project.tasks.findByName("generateTestGrammarSource")?.let { task ->
            try { task.enabled = false } catch (_: Exception) { }
        }

        try {
            val sourceSets = project.extensions.getByName("sourceSets") as SourceSetContainer
            sourceSets.getByName("main").allSource.srcDir("build/generated-src/antlr/main")
            sourceSets.getByName("test").allSource.srcDir("build/generated-src/antlr/main")
        } catch (_: Exception) { }

        project.tasks.withType(Test::class.java).configureEach {
            useJUnitPlatform()
        }

        project.logger.lifecycle("ReactiveConventionPlugin applied (toolchain=$toolchain) to ${project.path}")
    }
}
