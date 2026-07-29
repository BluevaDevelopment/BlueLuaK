plugins {
    java
}

dependencies {
    implementation(project(":blueluak-core"))
    implementation("org.apache.bcel:bcel:5.2")

    testImplementation("junit:junit:4.13.2")
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
