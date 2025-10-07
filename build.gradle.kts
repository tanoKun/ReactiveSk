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

fun Exec.runEnvConsole(
    serverJarName: String,
    skriptJarName: String,
    reactiveSkName: String
) {
    group = "verification"
    description = "Prepare files then run server in foreground with console attached (appears in Gradle console)."

    val scriptSh = project.projectDir.resolve("runEnv.sh").absolutePath
    val scriptPs = project.projectDir.resolve("runEnv.ps1").absolutePath
    val isWindows = System.getProperty("os.name").lowercase().contains("windows")

    val reactiveProjects = project.rootProject.subprojects.filter { it.name.startsWith(reactiveSkName) }
    if (reactiveProjects.isNotEmpty()) {
        dependsOn(reactiveProjects.map { it.tasks.named("shadowJar") })
    }

    doFirst {
        if (isWindows) {
            project.exec {
                commandLine = listOf("powershell", "-ExecutionPolicy", "Bypass", "-File", scriptPs, "-server", serverJarName, "-NoStart", "-skript", skriptJarName, "-reactive", reactiveSkName)
                workingDir = project.projectDir
            }
        } else {
            project.exec {
                commandLine = listOf("bash", scriptSh, "--server", serverJarName, "--no-start", "--skript", skriptJarName, "--reactive", reactiveSkName)
                workingDir = project.projectDir
            }
        }

        standardInput = System.`in`
        standardOutput = System.out
        errorOutput = System.err
    }

    val serverDir = project.projectDir.resolve("test/servers").resolve(serverJarName.removeSuffix(".jar"))
    val serverJar = serverDir.resolve("server.jar").absolutePath

    if (isWindows) {
        commandLine = listOf("java", "-jar", serverJar, "nogui")
        workingDir = serverDir
        isIgnoreExitValue = false
    } else {
        commandLine = listOf("java", "-jar", serverJar, "nogui")
        workingDir = serverDir
        isIgnoreExitValue = false
    }
}

tasks.register<Exec>("runEnvConsole1_12_2_v2_6_3") {
    runEnvConsole("1.12.2.jar", "Skript-v2.6.3.jar", "ReactiveSk-skript-v2_6_3")
}
