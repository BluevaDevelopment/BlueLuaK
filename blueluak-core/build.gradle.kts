import com.strumenta.antlrkotlin.gradle.AntlrKotlinTask
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    kotlin("multiplatform")
    id("com.strumenta.antlr-kotlin")
    `maven-publish`
}

val generatedBuildInfo = layout.buildDirectory.dir("generated-src/build-info/commonMain/kotlin")
val generatedGrammar = layout.buildDirectory.dir("generated-src/antlr/commonMain/kotlin")

val generateBuildInfo = tasks.register("generateBuildInfo") {
    group = "build"
    description = "Generates multiplatform build metadata."
    val output = generatedBuildInfo.map { it.file("net/blueva/luak/BuildInfo.kt") }
    outputs.file(output)

    doLast {
        output.get().asFile.apply {
            parentFile.mkdirs()
            writeText(
                """
                package net.blueva.luak

                internal object BuildInfo {
                    const val VERSION: String = "BlueLuaK ${project.version}"
                }
                """.trimIndent() + "\n"
            )
        }
    }
}

val generateKotlinGrammarSource = tasks.register<AntlrKotlinTask>("generateKotlinGrammarSource") {
    source = fileTree(rootProject.layout.projectDirectory.dir("grammar")) {
        include("LuaLexer.g4", "LuaParser.g4")
    }
    packageName = "net.blueva.luak.parser.antlr"
    arguments = listOf("-visitor", "-no-listener")
    outputDirectory = generatedGrammar
        .map { it.dir("net/blueva/luak/parser/antlr") }
        .get()
        .asFile
}

@OptIn(ExperimentalWasmDsl::class)
kotlin {
    jvm()
    js {
        nodejs()
    }
    wasmJs {
        nodejs()
    }
    linuxX64()
    mingwX64()
    macosX64()
    macosArm64()

    jvmToolchain(17)
    withSourcesJar(publish = true)

    sourceSets {
        jsMain {
            kotlin.srcDir("src/nonJvmMain/kotlin")
        }
        wasmJsMain {
            kotlin.srcDir("src/nonJvmMain/kotlin")
        }
        commonMain {
            kotlin.srcDir(generatedBuildInfo)
            kotlin.srcDir(generatedGrammar)
            dependencies {
                implementation("com.strumenta:antlr-kotlin-runtime:1.0.10")
            }
        }
        commonTest {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        jvmTest {
            kotlin.srcDir("src/threadedTest/kotlin")
        }
        nativeTest {
            kotlin.srcDir("src/threadedTest/kotlin")
        }
    }
}

tasks.matching { it.name.startsWith("compile") && it.name.contains("Kotlin") }.configureEach {
    dependsOn(generateBuildInfo, generateKotlinGrammarSource)
}

tasks.matching { it.name.endsWith("SourcesJar", ignoreCase = true) }.configureEach {
    dependsOn(generateBuildInfo, generateKotlinGrammarSource)
}

publishing {
    publications.withType<MavenPublication>().configureEach {
        pom {
            name.set("BlueLuaK Core")
            description.set("Kotlin Multiplatform embeddable Lua 5.2 runtime, compiler, and parser.")
            url.set("https://github.com/BluevaDevelopment/BlueLuaK")
            licenses {
                license {
                    name.set("MIT License")
                    url.set("https://opensource.org/licenses/MIT")
                    distribution.set("repo")
                }
            }
            developers {
                developer {
                    id.set("blueva")
                    name.set("Blueva Development")
                    url.set("https://github.com/BluevaDevelopment")
                }
            }
            scm {
                connection.set("scm:git:https://github.com/BluevaDevelopment/BlueLuaK.git")
                developerConnection.set("scm:git:ssh://git@github.com/BluevaDevelopment/BlueLuaK.git")
                url.set("https://github.com/BluevaDevelopment/BlueLuaK")
            }
        }
    }
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/BluevaDevelopment/BlueLuaK")
            credentials {
                username = providers.environmentVariable("GITHUB_ACTOR").orNull
                password = providers.environmentVariable("GITHUB_TOKEN").orNull
            }
        }
    }
}
