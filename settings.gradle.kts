plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0" // Used for Java toolchain
}

rootProject.name = "raptor"
include("raptor-core", "integration-tests")
