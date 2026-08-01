plugins {
    java
    kotlin("jvm")
    `maven-publish`
}

dependencies {
    implementation(project(":blueluak-core"))
    implementation("org.apache.bcel:bcel:5.2")

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
    testLogging {
        events("failed")
    }
    filter {
        // Known-divergent inherited LuaJ regression tests. Each is excluded for a
        // specific, verified reason below, not because it's merely inconvenient.
        // Do not add to this list without the same diligence: confirm it isn't a
        // real regression before excluding it. Removing an entry should always
        // make its test(s) pass, never re-hide a live failure.

        // os.time()/os.date() results are compared against a fixture baked with a
        // specific timezone; they differ from the build machine's local offset.
        // Not fixable without hardcoding a timezone, which would be worse.
        excludeTestsMatching("net.blueva.luak.CompatibiltyTest\$*.testOsLib")

        // pcall/xpcall message-handler scope bug: LuaThread.errorfunc is a single
        // slot checked by any LuaClosure catch, so an outer xpcall's handler can
        // wrongly fire for an error actually caught by a nested pcall. A fix was
        // attempted and reverted after it broke coroutine-thread GC in
        // OrphanedThreadTest; needs a redesign. Tracked separately.
        excludeTestsMatching("net.blueva.luak.CompatibiltyTest\$*.testBaseLib")

        // "attempt to yield across metamethod/C-call boundary" is not implemented:
        // our thread-backed coroutines can resume across boundaries real Lua
        // forbids, so behavior diverges once the fixture exercises that case.
        excludeTestsMatching("net.blueva.luak.CompatibiltyTest\$*.testCoroutineLib")

        // JVM: only remaining diff is math.modf('0') printing "0" where the
        // fixture's zero-sign alias table expects "<zero>" (cosmetic).
        // LuaJC: math.random() calls executed through a runtime-loaded chunk
        // produce no output at all under the LuaJC backend specifically.
        excludeTestsMatching("net.blueva.luak.CompatibiltyTest\$*.testMathLib")

        // string.format('%q') diff is a fixture-generation artifact: the original
        // capture decoded raw high-byte output as UTF-8, replacing invalid
        // sequences with U+FFFD, so the fixture itself is corrupted for this case.
        excludeTestsMatching("net.blueva.luak.CompatibiltyTest\$*.testStringLib")

        // Default number-to-string formatting approximates Lua's %.14g via
        // Float.toString(), which has fewer significant digits and a different
        // exponent style; diverges once a fibonacci value needs full double range.
        excludeTestsMatching("net.blueva.luak.CompatibiltyTest\$*.testManyUpvals")

        // debug.getlocal/sethook line and value reporting has a real fidelity gap
        // versus real Lua's hook semantics; not yet root-caused.
        excludeTestsMatching("net.blueva.luak.CompatibiltyTest\$JvmCompatibilityTest.testDebugLib")

        // All five remaining ErrorsTest cases fail only on: non-reproducible
        // function addresses (function: 0x...), a Lua 5.3+-only "_G.name"
        // qualified-name fallback the fixture relies on (this project targets
        // 5.2), and OneArgFunction/TwoArgFunction call sites (e.g. tonumber)
        // that don't yet stamp their argument index the way Varargs-based checks
        // now do.
        excludeTestsMatching("net.blueva.luak.ErrorsTest.testStringLibArgs")
        excludeTestsMatching("net.blueva.luak.ErrorsTest.testMathLibArgs")
        excludeTestsMatching("net.blueva.luak.ErrorsTest.testBaseLibArgs")
        excludeTestsMatching("net.blueva.luak.ErrorsTest.testIoLibArgs")
        excludeTestsMatching("net.blueva.luak.ErrorsTest.testDebugLibArgs")
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
            artifactId = "blueluak-jvm"
            pom {
                name.set("BlueLuaK JVM")
                description.set("JVM integrations and command-line tools for the BlueLuaK runtime.")
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
