plugins {
    kotlin("jvm") version "2.2.0"
}

kotlin {
    jvmToolchain(8)
}

repositories {
    mavenCentral()
}

allprojects {
    group = "com.github.tanokun"

    repositories {
        mavenCentral()

        maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
        maven("https://oss.sonatype.org/content/repositories/snapshots")
        maven("https://repo.skriptlang.org/releases")

        maven("https://jitpack.io")
    }
}

val cleanTask = tasks.register<Delete>("cleanRunEnv") {
    group = "Run Environments"
    description = "Cleans the plugin directory for the environment."
    delete(project.projectDir.resolve("docker/plugins"))
}

val stopServerTask = tasks.register<Exec>("stopServer") {
    group = "Run Environments"
    description = "Stops any running Docker containers for the environment."
    workingDir = project.projectDir.resolve("docker")

    isIgnoreExitValue = true
    commandLine("docker-compose", "exec", "paper", "rcon-cli", "stop")
}

val dockerDownTask = tasks.register<Exec>("dockerDown") {
    group = "Run Environments"
    description = "Stops and removes any running Docker containers for the environment."
    workingDir = project.projectDir.resolve("docker")

    commandLine("docker-compose", "down")
}

val copyPluginTask = tasks.register<Copy>("copyPlugin") {
    group = "Run Environments"
    description = "Copies the built plugin JAR for the '$name' environment."

    dependsOn(":ReactiveSk:shadowJar")
    from(project.projectDir.resolve("ReactiveSk/build/libs").listFiles().first { it.name.startsWith("ReactiveSk") })
    into(project.projectDir.resolve("docker/plugins"))
}

fun TaskContainer.registerRunEnvTask(
    paperVersion: String,
    skriptVersionWithPath: String,
    javaVersion: Int,
) {
    val name = "runEnv_Paper${paperVersion.replace('.', '_')}_v${skriptVersionWithPath.replace('.', '_').split("/")[0]}_Java$javaVersion"

    register<Exec>(name) {
        group = "Run Environments"
        description = "Builds ReactiveSk runs Paper $paperVersion with Skript $skriptVersionWithPath via Docker."

        finalizedBy(stopServerTask)
        dependsOn(dockerDownTask, cleanTask, copyPluginTask)
        copyPluginTask.get().mustRunAfter(cleanTask.get())

        val projectRoot = project.projectDir.absolutePath.replace('\\', '/')
        val skriptUrl = "https://github.com/SkriptLang/Skript/releases/download/$skriptVersionWithPath"

        workingDir = project.projectDir.resolve("docker")
        environment("PAPER_VERSION", paperVersion)
        environment("SKRIPT_URL", skriptUrl)
        environment("JAVA_VERSION", javaVersion.toString())
        environment("PROJECT_ROOT", projectRoot)

        commandLine("docker-compose", "up")

        standardInput = System.`in`
        standardOutput = System.out
        errorOutput = System.err
    }
}

tasks.registerRunEnvTask(
    paperVersion = "1.12.2",
    skriptVersionWithPath = "2.6.3/Skript.jar",
    javaVersion = 8
)

tasks.registerRunEnvTask(
    paperVersion = "1.17.1",
    skriptVersionWithPath = "2.7.0/Skript.jar",
    javaVersion = 17
)

tasks.registerRunEnvTask(
    paperVersion = "1.17.1",
    skriptVersionWithPath = "2.9.5/Skript-2.9.5.jar",
    javaVersion = 17
)

tasks.registerRunEnvTask(
    paperVersion = "1.21.4",
    skriptVersionWithPath = "2.10.0/Skript-2.10.0.jar",
    javaVersion = 21
)

tasks.registerRunEnvTask(
    paperVersion = "1.21.8",
    skriptVersionWithPath = "2.12.2/Skript-2.12.2.jar",
    javaVersion = 21
)