plugins {
    kotlin("jvm") version "2.4.10" apply false
    kotlin("multiplatform") version "2.4.10" apply false
    id("com.strumenta.antlr-kotlin") version "1.0.13" apply false
}

val releaseVersion = providers.gradleProperty("version")
    .orElse(providers.environmentVariable("RELEASE_VERSION"))
    .orElse("3.0.2")

allprojects {
    group = "net.blueva"
    version = releaseVersion.get()

    repositories {
        mavenCentral()
    }
}
