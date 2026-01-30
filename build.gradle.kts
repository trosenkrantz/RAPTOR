plugins {
    java
    id("net.ltgt.errorprone") version "4.1.0" // For lint checking
}

val distributionsDir = layout.buildDirectory.dir("distributions")
val runtimesDir = layout.buildDirectory.dir("runtimes")

group = "com.github.trosenkrantz"
version = "2.0.0"

repositories {
    mavenCentral()
}

configurations {
    create("runtime") {
        isTransitive = false // Force explicit runtime dependencies to minimise runtime
    }
}

dependencies {
    // For JSON mapping
    implementation("com.fasterxml.jackson.core:jackson-databind:2.18.0")
    add("runtime", "com.fasterxml.jackson.core:jackson-databind:2.18.0")
    add("runtime", "com.fasterxml.jackson.core:jackson-core:2.18.0")
    add("runtime", "com.fasterxml.jackson.core:jackson-annotations:2.18.0")

    // For SNMP
    implementation("org.snmp4j:snmp4j:3.8.2")
    add("runtime", "org.snmp4j:snmp4j:3.8.2")

    // For serial port
    implementation("com.fazecast:jSerialComm:2.11.0")
    add("runtime", "com.fazecast:jSerialComm:2.11.0")

    // For WebSocket
    implementation("org.java-websocket:Java-WebSocket:1.5.7")
    add("runtime", "org.java-websocket:Java-WebSocket:1.5.7")
    add("runtime", "org.slf4j:slf4j-api:2.0.6") // Java-WebSocket use SLF4J
    add("runtime", "org.slf4j:slf4j-jdk14:2.0.6") // Route SLF4J to java.util.logging

    // For testing
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
    testImplementation("org.testcontainers:testcontainers:2.0.1")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testRuntimeOnly("org.slf4j:slf4j-nop:2.0.7") // testcontainers use SLF4J, route to NOP to ignore

    // For lint checking
    errorprone("com.google.errorprone:error_prone_core:2.36.0")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
        vendor.set(JvmVendorSpec.AZUL)
    }
}

tasks.jar {
    manifest {
        attributes(
            "Implementation-Title" to project.name,
            "Implementation-Version" to project.version,
            "Implementation-Vendor" to project.group
        )
    }
    destinationDirectory.set(distributionsDir)
}

val distributeScripts = tasks.register<Copy>("distributeScripts") {
    from(layout.projectDirectory.dir("src/main/distributions"))
    into(distributionsDir)
}

val distributeRuntime = tasks.register<Copy>("distributeRuntime") {
    from(configurations["runtime"])
    into(layout.buildDirectory.dir("distributions/libs"))
}

val distributeDocumentation = tasks.register<Copy>("distributeDocumentation") {
    from(file("README.md"), file("LICENCE"))
    into(distributionsDir)

    filter { line ->
        line.replace("src/main/distributions/", "") // Adapt links to other structure
    }
}

tasks.assemble {
    dependsOn(tasks.jar, distributeScripts, distributeRuntime, distributeDocumentation)
}

val zip = tasks.register<Zip>("zip") {
    dependsOn(tasks.assemble)

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
        dependsOn(tasks.assemble)
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
    dependsOn(tasks.assemble)
    workingDir(distributionsDir)
    commandLine(
        "cmd",
        "/C",
        "start",
        "java",
        "-cp",
        ".\\*;libs\\*",
        "-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*:5005",
        "com.github.trosenkrantz.raptor.Main"
    )
}

val buildDockerImage = tasks.register<Exec>("buildDockerImage") {
    dependsOn(tasks.assemble)
    commandLine("docker", "build", "-q", "-f", "./src/main/docker/Dockerfile", "-t", "raptor:latest", ".")
}

tasks.test {
    dependsOn(buildDockerImage) // For integration tests
    useJUnitPlatform()

    val concurrentIntegrationTestCases = maxOf(1, Runtime.getRuntime().availableProcessors() / 2)
    systemProperty("concurrent.integration.test.cases", concurrentIntegrationTestCases)

    doFirst {
        println("Running with $concurrentIntegrationTestCases concurrent test-cases for integration testing.")
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.add("-Werror") // Converts all Java compiler warnings into errors, to accept no warnings
}
