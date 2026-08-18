import com.strumenta.antlrkotlin.gradle.AntlrKotlinTask
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import java.time.Duration

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
    wasmWasi {
        nodejs()
    }
    linuxX64()
    mingwX64()
    macosX64()
    macosArm64()

    jvmToolchain(17)
    withSourcesJar(publish = true)

    sourceSets {
        // I/O stream classes with zero platform dependency (no JS interop, no
        // WASI syscalls) - shared by every non-JVM, non-Native target,
        // including WASI.
        jsMain {
            kotlin.srcDir("src/portableIoMain/kotlin")
        }
        wasmJsMain {
            kotlin.srcDir("src/portableIoMain/kotlin")
        }
        wasmWasiMain {
            kotlin.srcDir("src/portableIoMain/kotlin")
        }
        // Platform-neutral actuals specific to a JS-hosted engine's process
        // model (still no JS interop themselves, but only ever used by the
        // two JS-hosted targets) - not shared with wasmWasiMain, which has
        // its own Platform.wasmWasi.kt/WeakReference.wasmWasi.kt.
        jsMain {
            kotlin.srcDir("src/nonJvmMain/kotlin")
        }
        wasmJsMain {
            kotlin.srcDir("src/nonJvmMain/kotlin")
        }
        // JS-engine-specific actuals (process/console/node:fs) - only for the
        // two targets that actually run inside a JS host. Never wired into
        // wasmWasiMain: a WASI host has none of that.
        jsMain {
            kotlin.srcDir("src/jsHostMain/kotlin")
        }
        wasmJsMain {
            kotlin.srcDir("src/jsHostMain/kotlin")
        }
        // 64-bit file offsets: fseek/ftell use C `long`, which is 32 bits on
        // Windows and 64 elsewhere, so the two families get their own actual.
        linuxX64Main {
            kotlin.srcDir("src/nativePosixMain/kotlin")
        }
        macosX64Main {
            kotlin.srcDir("src/nativePosixMain/kotlin")
        }
        macosArm64Main {
            kotlin.srcDir("src/nativePosixMain/kotlin")
        }
        mingwX64Main {
            kotlin.srcDir("src/nativeWindowsMain/kotlin")
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
    }
}

tasks.matching { it.name.startsWith("compile") && it.name.contains("Kotlin") }.configureEach {
    dependsOn(generateBuildInfo, generateKotlinGrammarSource)
}

// Bounds every test task so an unresumed coroutine continuation fails
// deterministically instead of hanging CI; kotlin.test has no portable
// per-test timeout, but Task.timeout works on every target uniformly.
tasks.matching { it.name.endsWith("Test") }.configureEach {
    timeout.set(Duration.ofMinutes(10))
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
            name = "BluevaRepo"
            url = uri("https://repo.blueva.net/releases")
            credentials {
                username = providers.environmentVariable("BLUEVA_REPO_USERNAME").orNull
                password = providers.environmentVariable("BLUEVA_REPO_SECRET").orNull
            }
        }
    }
}
