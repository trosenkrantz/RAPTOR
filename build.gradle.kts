plugins {
    java
}

val distributionsDir = layout.buildDirectory.dir("distributions")
val extraRuntimeDir = layout.buildDirectory.dir("extra-runtime")

group = "com.github.trosenkrantz"
version = "1.3.0"

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

    // For WebSocket
    implementation("org.java-websocket:Java-WebSocket:1.5.7")
    add("runtime", "org.java-websocket:Java-WebSocket:1.5.7")
    add("runtime", "org.slf4j:slf4j-api:2.0.6") // Java-WebSocket use SLF4J
    add("runtime", "org.slf4j:slf4j-jdk14:2.0.6") // Route SLF4J to java.util.logging

    // For testing
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
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
    from(file("README.md"), file("LICENSE"))
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

val run = tasks.register<Exec>("run") {
    dependsOn(tasks.assemble)
    workingDir(distributionsDir)
    commandLine(
        "cmd",
        "/C",
        "start",
        "cmd",
        "/C",
        "raptor.cmd"
    ) // Starts RAPTOR in new console window, but in a way so the console windows closes when RAPTOR exists
}

val assembleExtraRuntime = tasks.register<Copy>("assembleExtraRuntime") {
    dependsOn(tasks.assemble)
    from(distributionsDir)
    into(extraRuntimeDir)
}

val runExtra = tasks.register<Exec>("runExtra") {
    dependsOn(assembleExtraRuntime)
    workingDir(extraRuntimeDir)
    commandLine(
        "cmd",
        "/C",
        "start",
        "cmd",
        "/C",
        "raptor.cmd"
    ) // Starts RAPTOR in new console window, but in a way so the console windows closes when RAPTOR exists
}

tasks.register("run2") {
    dependsOn(run, runExtra)
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

tasks.test {
    useJUnitPlatform()
}