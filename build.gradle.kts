plugins {
    kotlin("jvm") version "2.4.10" apply false
    id("com.strumenta.antlr-kotlin") version "1.0.10" apply false
}

allprojects {
    group = "net.blueva"
    version = "3.0.2"

    repositories {
        mavenCentral()
    }
}
