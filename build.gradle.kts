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
    version = "developing"

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

fun TaskContainer.registerRunEnvTask(
    name: String,
    paperVersion: String,
    skriptVersionWithPath: String,
    moduleName: String,
    javaVersion: Int,
) {

    val copyPluginTask = register<Copy>("copyPluginFor_$name") {
        group = "Run Environments"
        description = "Copies the built plugin JAR for the '$name' environment."

        dependsOn(":$moduleName:shadowJar")
        from(project.projectDir.resolve("$moduleName/build/libs/ReactiveSk-developing-all.jar"))
        into(project.projectDir.resolve("docker/plugins"))
    }

    register<Exec>(name) {
        group = "Run Environments"
        description = "Builds '$moduleName', then runs Paper $paperVersion with Skript $skriptVersionWithPath via Docker."

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
    name = "runEnvConsole1_12_2_v2_6_3",
    paperVersion = "1.12.2",
    skriptVersionWithPath = "2.6.3/Skript.jar",
    moduleName = "ReactiveSk-skript-v2_6_3",
    javaVersion = 8
)

tasks.registerRunEnvTask(
    name = "runEnvConsole1_17_1_v2_7_0",
    paperVersion = "1.17.1",
    skriptVersionWithPath = "2.7.0/Skript.jar",
    moduleName = "ReactiveSk-skript-v2_7_0",
    javaVersion = 17
)