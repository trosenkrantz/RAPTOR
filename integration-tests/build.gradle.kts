plugins {
    java
}

val distribution by configurations.registering {
    isCanBeResolved = true
    isCanBeConsumed = false
}

dependencies {
    add(distribution.name, project(path = ":", configuration = "distribution"))

    testImplementation(libs.junit.jupiter)
    testImplementation(libs.testcontainers)

//    testRuntimeOnly(libs.junit.launcher) // TODO Needed? Maybe in CI
    testRuntimeOnly(libs.slf4j.nop) // testcontainers use SLF4J, route to NOP to ignore
}

val syncDistributions = tasks.register<Sync>("syncDistributions") {
    from(distribution)
    into(layout.buildDirectory.dir("distributions"))
}

val buildDockerImage = tasks.register<Exec>("buildDockerImage") {
    dependsOn(syncDistributions)
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
