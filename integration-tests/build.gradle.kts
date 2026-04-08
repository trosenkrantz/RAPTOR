plugins {
    java
}

val buildDockerImageMarkerFile = layout.buildDirectory.file("buildDockerImage-marker.txt")

val distribution by configurations.registering {
    isCanBeResolved = true
    isCanBeConsumed = false
}

dependencies {
    add(distribution.name, project(path = ":", configuration = "distribution"))

    testImplementation(libs.junit.jupiter)
    testImplementation(libs.testcontainers)

    testRuntimeOnly(libs.slf4j.nop) // testcontainers use SLF4J, route to NOP to ignore
    testRuntimeOnly(libs.junit.launcher) // Declaring test framework explicitly as recommended by Gradle: https://docs.gradle.org/8.5/userguide/upgrading_version_8.html#test_framework_implementation_dependencies
}

val syncDistributions = tasks.register<Sync>("syncDistributions") {
    from(distribution)
    into(layout.buildDirectory.dir("distributions"))
}

val buildDockerImage = tasks.register<Exec>("buildDockerImage") {
    dependsOn(syncDistributions)
    inputs.file("./src/main/docker/Dockerfile")
    inputs.dir(layout.buildDirectory.dir("distributions"))

    commandLine("docker", "build", "-q", "-f", "./src/main/docker/Dockerfile", "-t", "raptor:latest", ".")

    // Use a marker file for Configuration Cache
    outputs.file(buildDockerImageMarkerFile)
    doLast {
        val file = outputs.files.singleFile // Access the file through the task's outputs to avoid serialization issues
        file.writeText("Built at: ${System.currentTimeMillis()}")
    }
}

tasks.test {
    dependsOn(buildDockerImage)
    inputs.file(buildDockerImageMarkerFile)

    useJUnitPlatform()

    val concurrentIntegrationTestCases = maxOf(1, Runtime.getRuntime().availableProcessors()) // TODO ?
    systemProperty("concurrent.integration.test.cases", concurrentIntegrationTestCases)

    doFirst {
        println("Running with $concurrentIntegrationTestCases concurrent test-cases for integration testing.")
    }

    testLogging.showStandardStreams = true
}
