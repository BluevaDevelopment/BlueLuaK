plugins {
    java
}

dependencies {
    implementation(project(":blueluak-core"))
    implementation("org.apache.bcel:bcel:5.2")

    testImplementation("junit:junit:4.13.2")
}

sourceSets {
    main {
        java {
            srcDir("../src/jse")
        }
        resources {
            srcDirs("../src/jse")
            include("META-INF/services/**")
        }
    }
    test {
        java {
            srcDir("../test/junit")
        }
        resources {
            srcDir("../test/lua")
        }
    }
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
