plugins {
    java
    kotlin("jvm")
}

val generatedSrc = layout.buildDirectory.dir("generated-src/main/kotlin")

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
        kotlin {
            setSrcDirs(listOf(generatedSrc))
        }
    }
}

tasks.compileKotlin {
    dependsOn(processSources)
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