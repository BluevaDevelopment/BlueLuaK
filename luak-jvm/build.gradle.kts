plugins {
    `java-library`
    kotlin("jvm")
    `maven-publish`
}

dependencies {
    api(project(":luak-core"))
    implementation("org.apache.bcel:bcel:6.12.0")

    testImplementation("junit:junit:4.13.2")
    testImplementation(kotlin("test"))
}

val examples = sourceSets.create("examples")
kotlin.sourceSets.named(examples.name) {
    kotlin.srcDir(rootProject.layout.projectDirectory.dir("examples/jvm"))
}
configurations.named(examples.implementationConfigurationName) {
    extendsFrom(configurations.implementation.get())
}
examples.compileClasspath += sourceSets.main.get().output

tasks.check {
    dependsOn(tasks.named("compileExamplesKotlin"))
}

tasks.compileJava {
    options.release = 17
}

tasks.compileTestJava {
    options.release = 17
}

tasks.test {
    useJUnit()
    // LuaConformanceReport reads the Lua reference suite from here. Gradle does
    // not forward its own -D flags to the test JVM, so pass it through; the
    // BLUELUAK_LUA_TESTSUITE environment variable needs no wiring.
    providers.systemProperty("luak.lua.testsuite").orNull
        ?.let { systemProperty("luak.lua.testsuite", it) }
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

java {
    withSourcesJar()
}

publishing {
    publications {
        create<MavenPublication>("jvm") {
            from(components["java"])
            artifactId = "luak-jvm"
            pom {
                name.set("Luak JVM")
                description.set("JVM integrations and command-line tools for the Luak runtime.")
                url.set("https://github.com/BasaltProject/Luak")
                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                        distribution.set("repo")
                    }
                }
                developers {
                    developer {
                        id.set("basalt")
                        name.set("Basalt Project")
                        url.set("https://github.com/BasaltProject")
                    }
                }
                scm {
                    connection.set("scm:git:https://github.com/BasaltProject/Luak.git")
                    developerConnection.set("scm:git:ssh://git@github.com/BasaltProject/Luak.git")
                    url.set("https://github.com/BasaltProject/Luak")
                }
            }
        }
    }
    repositories {
        maven {
            name = "LuakRepo"
            url = uri("https://repo.luaklang.org/releases")
            credentials {
                username = providers.environmentVariable("LUAK_REPO_USERNAME").orNull
                password = providers.environmentVariable("LUAK_REPO_SECRET").orNull
            }
        }
    }
}
