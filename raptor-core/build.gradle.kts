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
}

tasks.test {
    useJUnitPlatform()
}
