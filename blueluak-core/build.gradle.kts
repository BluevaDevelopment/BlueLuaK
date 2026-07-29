plugins {
    java
}

val generatedSrc = layout.buildDirectory.dir("generated-src/main/java")

val processSources = tasks.register<Copy>("processSources") {
    group = "build"
    description = "Copies and filters core sources to inject the project version."

    from("../src/core")
    into(generatedSrc)
    filter { line ->
        line.replace("\"Luaj 0.0\"", "\"BlueLuaK ${project.version}\"")
    }
}

sourceSets {
    main {
        java {
            srcDir(generatedSrc)
        }
    }
}

tasks.compileJava {
    dependsOn(processSources)
    options.release = 17
}
