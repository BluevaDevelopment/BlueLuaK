import com.strumenta.antlrkotlin.gradle.AntlrKotlinTask

plugins {
    java
    kotlin("jvm")
    id("com.strumenta.antlr-kotlin")
}

dependencies {
    implementation(project(":blueluak-core"))
    implementation("org.apache.bcel:bcel:5.2")
    implementation("com.strumenta:antlr-kotlin-runtime:1.0.10")

    testImplementation("junit:junit:4.13.2")
    testImplementation(kotlin("test"))
}

val generateKotlinGrammarSource = tasks.register<AntlrKotlinTask>("generateKotlinGrammarSource") {
    source = fileTree(rootProject.layout.projectDirectory.dir("grammar")) {
        include("LuaLexer.g4", "LuaParser.g4")
    }
    packageName = "net.blueva.luak.parser.antlr"
    arguments = listOf("-visitor", "-no-listener")
    outputDirectory = layout.buildDirectory
        .dir("generated-src/main/kotlin/net/blueva/luak/parser/antlr")
        .get()
        .asFile
}

kotlin.sourceSets.main {
    kotlin.srcDir(generateKotlinGrammarSource)
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
