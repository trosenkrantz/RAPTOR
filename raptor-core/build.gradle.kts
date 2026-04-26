plugins {
    java
}

dependencies {
    implementation(libs.jackson.databind) // For JSON mapping
    implementation(libs.snmp4j) // For SNMP
    implementation(libs.jserialcomm) // For serial port
    implementation(libs.javawebsocket) // For WebSocket

    // For testing
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.jqwik)
    testRuntimeOnly(libs.junit.launcher) // Declaring test framework explicitly as recommended by Gradle: https://docs.gradle.org/8.5/userguide/upgrading_version_8.html#test_framework_implementation_dependencies
}

tasks.test {
    useJUnitPlatform()
    systemProperty("jqwik.database.file", layout.buildDirectory.file(".jqwik-database").get().asFile.absolutePath)
}
