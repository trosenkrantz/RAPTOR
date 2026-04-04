plugins {
    java // To be able to reference the build task
    alias(libs.plugins.errorprone) // To be able to apply Error Prone for lint checking
}

val distributionsDir: Provider<Directory> = layout.buildDirectory.dir("distributions")
val runtimesDir: Provider<Directory> = layout.buildDirectory.dir("runtimes")

allprojects {
    version = "2.2.0"

    repositories {
        mavenCentral()
    }

    plugins.withType<JavaBasePlugin> {
        extensions.configure<JavaPluginExtension> {
            toolchain {
                languageVersion.set(JavaLanguageVersion.of(21))
                vendor.set(JvmVendorSpec.AZUL)
            }
        }

        tasks.withType<Jar> {
            manifest {
                attributes(
                    "Implementation-Title" to project.name,
                    "Implementation-Version" to project.version,
                    "Implementation-Vendor" to "RAPTOR"
                )
            }
        }

        // Apply the Error Prone plugin
        pluginManager.apply(libs.plugins.errorprone.get().pluginId)
        dependencies {
            errorprone(libs.errorprone.core)
        }

        tasks.withType<JavaCompile>().configureEach {
            options.compilerArgs.add("-Werror") // Converts all Java compiler warnings into errors, to accept no warnings
        }
    }
}

val runtimeConfig by configurations.registering {
    isTransitive = false
}

val distribution by configurations.registering {
    isCanBeResolved = false
    isCanBeConsumed = true
}

dependencies {
    add(runtimeConfig.name, project(":raptor-core"))

    // For JSON mapping
    add(runtimeConfig.name, libs.jackson.databind)
    add(runtimeConfig.name, libs.jackson.core)
    add(runtimeConfig.name, libs.jackson.annotations)

    // For SNMP
    add(runtimeConfig.name, libs.snmp4j)

    // For serial port
    add(runtimeConfig.name, libs.jserialcomm)

    // For WebSocket
    add(runtimeConfig.name, libs.javawebsocket)
    add(runtimeConfig.name, libs.slf4j.api) // Java-WebSocket use SLF4J
    add(runtimeConfig.name, libs.slf4j.jdk14) // Route SLF4J to java.util.logging
}

val distributeFiles = tasks.register<Copy>("distributeFiles") {
    from(layout.projectDirectory.dir("src/main/distributions"))
    into(distributionsDir)
}

val distributeRuntime = tasks.register<Copy>("distributeRuntime") {
    from(runtimeConfig)
    into(layout.buildDirectory.dir("distributions/libs"))
}

val distributeDocumentation = tasks.register<Copy>("distributeDocumentation") {
    from(file("readme.md"), file("licence"))
    into(distributionsDir)
}

val distribute = tasks.register("distribute") {
    dependsOn(distributeFiles, distributeRuntime, distributeDocumentation)
}

artifacts {
    add(distribution.name, distributionsDir) {
        builtBy(distribute)
    }
}

tasks.jar {
    enabled = false // Do not produce a JAR file for this project
}

val zip = tasks.register<Zip>("zip") {
    dependsOn(distribute)

    from(distributionsDir)
    destinationDirectory.set(layout.buildDirectory.dir("libs"))
    archiveFileName.set("RAPTOR $version.zip")
}

tasks.build {
    dependsOn(zip)
}

val size: String? by project
val runTasks = mutableListOf<Task>()
repeat(size?.toIntOrNull() ?: 1) { index ->
    val runtimeDir = runtimesDir.get().dir("${index + 1}")

    val copyTask = tasks.register<Copy>("createRuntime${index + 1}") {
        dependsOn(distribute)
        from(distributionsDir)
        into(runtimeDir)
    }

    runTasks.add(tasks.register<Exec>("run${index + 1}") {
        dependsOn(copyTask)
        workingDir(runtimeDir)
        commandLine(
            "cmd",
            "/C",
            "start",
            "cmd",
            "/C",
            "raptor.cmd"
        ) // Starts RAPTOR in new console window, but in a way so the console windows closes when RAPTOR exists
    }.get())
}

tasks.register("run") {
    dependsOn(runTasks)
}

tasks.register<Exec>("debug") {
    dependsOn(distribute)
    workingDir(distributionsDir)
    commandLine(
        "cmd",
        "/C",
        "start",
        "java",
        "-cp",
        "libs\\*",
        "-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*:5005",
        "com.github.trosenkrantz.raptor.Main"
    )
}
