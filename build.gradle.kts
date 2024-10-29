plugins {
    java
}

val distributionsDir = layout.buildDirectory.dir("distributions")

group = "com.github.trosenkrantz"
version = "1.1.0"

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

    // For testing
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
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

tasks.register<Exec>("run") {
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