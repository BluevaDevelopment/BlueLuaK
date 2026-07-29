plugins {
    java
    kotlin("jvm")
}

val generatedSrc = layout.buildDirectory.dir("generated-src/main/java")

val processSources = tasks.register<Copy>("processSources") {
    group = "build"
    description = "Copies and filters core sources to inject the project version."

    from("src/main/java")
    into(generatedSrc)
    filter { line ->
        line.replace("\"Luaj 0.0\"", "\"BlueLuaK ${project.version}\"")
    }
}

sourceSets {
    main {
        java {
            setSrcDirs(listOf(generatedSrc))
        }
        kotlin {
            setSrcDirs(listOf("src/main/kotlin"))
        }
    }
}

tasks.compileJava {
    dependsOn(processSources)
    options.release = 17
}
repositories {
    mavenCentral()
}
dependencies {
    testImplementation(kotlin("test"))
}
kotlin {
    jvmToolchain(17)
}