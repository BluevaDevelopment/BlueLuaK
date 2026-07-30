plugins {
    java
    kotlin("jvm")
}

dependencies {
    implementation(project(":blueluak-core"))
    implementation("org.apache.bcel:bcel:5.2")

    testImplementation("junit:junit:4.13.2")
    testImplementation(kotlin("test"))
}

tasks.compileJava {
    options.release = 17
}

tasks.compileTestJava {
    options.release = 17
}

tasks.test {
    useJUnit()
    ignoreFailures = true
    testLogging {
        events("failed")
    }
}
repositories {
    mavenCentral()
}
kotlin {
    jvmToolchain(17)
}