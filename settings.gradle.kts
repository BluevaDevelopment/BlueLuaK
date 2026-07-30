pluginManagement {
    plugins {
        kotlin("jvm") version "2.4.10"
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
rootProject.name = "BlueLuaK"

include(
    "blueluak-core",
    "blueluak-jvm"
)
