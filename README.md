<p align="center">
  <img src="docs/media/logo.png" alt="BlueLuaK" width="760">
</p>

<p align="center">
  <strong>A Kotlin Multiplatform implementation of an embeddable Lua 5.2 runtime.</strong>
</p>

<p align="center">
  <img alt="Version" src="https://img.shields.io/badge/version-26.4-blue">
  <img alt="Kotlin Multiplatform" src="https://img.shields.io/badge/Kotlin_Multiplatform-JVM_%7C_JS_%7C_Wasm_%7C_Native-7F52FF?logo=kotlin&logoColor=white">
  <img alt="Kotlin" src="https://img.shields.io/badge/Kotlin-2.4.10-7F52FF?logo=kotlin&logoColor=white">
  <img alt="Gradle" src="https://img.shields.io/badge/Gradle-9.6.1-02303A?logo=gradle&logoColor=white">
  <img alt="JVM" src="https://img.shields.io/badge/JVM-17+-ED8B00?logo=openjdk&logoColor=white">
  <img alt="Lua" src="https://img.shields.io/badge/Lua-5.2-000080?logo=lua&logoColor=white">
  <img alt="License" src="https://img.shields.io/badge/license-MIT-green">
</p>

## Overview

BlueLuaK is a Kotlin-first fork of [LuaJ 3.0.2](https://github.com/luaj/luaj), rebuilt as a **Kotlin Multiplatform** library. Its shared module currently targets:

- **JVM 17+**
- **JavaScript IR**, tested on Node.js
- **WebAssembly**, tested on Node.js
- **Kotlin/Native** for Linux x64, Windows x64, macOS x64, and macOS ARM64

The Lua runtime, value model, bytecode compiler, AST, standard libraries, and ANTLR Kotlin parser live in `commonMain`. JVM-specific integration is isolated from the shared runtime.

BlueLuaK currently implements Lua 5.2 and provides:

- An embeddable Lua VM written entirely in Kotlin.
- Lua source parsing through ANTLR Kotlin, without JavaCC or generated Java.
- Lua bytecode compilation and execution across the configured KMP targets.
- Tables, metatables, functions, coroutines, and Lua 5.2 standard libraries.
- `LuaPlatform.standardGlobals()`, one entry point that builds a fully loaded `Globals` on every target.
- A shared `io` library — `io.open`, `io.lines`, `io.tmpfile`, file handles, `os.remove`/`rename`/`tmpname` — on every target, not just the JVM.
- Shared tests for the runtime, compiler, and parser across KMP targets.
- JVM integrations for processes, Java reflection, script engines, and `luajava`.

BlueLuaK is no longer source-compatible with LuaJ: modules, packages, platform classes, and APIs use BlueLuaK naming under `net.blueva.luak`.

## Multiplatform Architecture

| Source set or module | Purpose |
|---|---|
| [`blueluak-core/src/commonMain/kotlin/`](blueluak-core/src/commonMain/kotlin/) | Shared Lua runtime, compiler, AST, parser, and libraries |
| [`blueluak-core/src/jvmMain/kotlin/`](blueluak-core/src/jvmMain/kotlin/) | JVM implementations of platform abstractions |
| [`blueluak-core/src/nonJvmMain/kotlin/`](blueluak-core/src/nonJvmMain/kotlin/) | Portable implementations shared by JavaScript and Wasm |
| [`blueluak-core/src/jsHostMain/kotlin/`](blueluak-core/src/jsHostMain/kotlin/) | JavaScript-host implementations (`node:fs`, `process`) for the JS and Wasm-JS targets |
| [`blueluak-core/src/wasmWasiMain/kotlin/`](blueluak-core/src/wasmWasiMain/kotlin/) | WASI implementations over raw `wasi_snapshot_preview1` syscalls |
| [`blueluak-core/src/nativeMain/kotlin/`](blueluak-core/src/nativeMain/kotlin/) | Kotlin/Native implementations of platform abstractions |
| [`blueluak-core/src/nativePosixMain/kotlin/`](blueluak-core/src/nativePosixMain/kotlin/) | 64-bit file offsets for Linux and macOS |
| [`blueluak-core/src/nativeWindowsMain/kotlin/`](blueluak-core/src/nativeWindowsMain/kotlin/) | 64-bit file offsets for Windows |
| [`blueluak-core/src/commonTest/kotlin/`](blueluak-core/src/commonTest/kotlin/) | Tests shared by all core targets |
| [`blueluak-jvm/src/main/kotlin/`](blueluak-jvm/src/main/kotlin/) | JVM-only integrations and command-line tooling |
| [`grammar/`](grammar/) | ANTLR Kotlin lexer and parser grammars for Lua 5.2 |
| [`examples/`](examples/) | Kotlin and Lua usage examples |

Gradle modules:

| Module | Targets | Purpose |
|---|---|---|
| `blueluak-core` | JVM, JavaScript IR, Wasm, Kotlin/Native | Multiplatform Lua runtime, compiler, and parser |
| `blueluak-jvm` | JVM | JVM platform adapters, `luajava`, scripting, CLI, and JIT support |

Platform-dependent functionality is exposed through `expect`/`actual` implementations. Code intended to run on every target belongs in `commonMain`; Java and JVM APIs remain confined to JVM source sets and `blueluak-jvm`. No type in the public `commonMain` API is platform-specific.

The host surface every shared library is built on is deliberately small: console streams, resource lookup, a random-access file handle, delete/rename/temp-name, environment variables, exit, GC, and weak references. Everything else — the value model, the compiler, the parser, and all nine standard libraries — is shared code.

## Installation

Releases publish to [repo.blueva.net](https://repo.blueva.net/releases), a public Maven repository — no authentication needed to depend on BlueLuaK.

### JVM projects

Two artifacts are available. Pick one:

| Artifact | Contains | Use it when |
|---|---|---|
| `blueluak-jvm` | The multiplatform core (as a compile dependency) plus `JvmPlatform.standardGlobals()`, `luajava`, `io.popen`/`os.execute`, the `luajc` JIT compiler, CLI tooling, and `javax.script` integration | You want a ready-to-use Lua runtime — the common case |
| `blueluak-core-jvm` | Just the shared runtime, compiler, AST, parser, and standard libraries on the JVM target, including `LuaPlatform.standardGlobals()`, but without `luajava`, `io.popen`, `os.execute`, or the JIT | You don't need the JVM-only integrations, or want the smallest possible footprint |

`blueluak-jvm` pulls in `blueluak-core-jvm` transitively, so depending on it alone is enough for most projects.

**Gradle (Kotlin DSL)**

```kotlin
repositories {
    maven("https://repo.blueva.net/releases")
}

dependencies {
    implementation("net.blueva:blueluak-jvm:26.4")
}
```

**Maven**

```xml
<repositories>
  <repository>
    <id>blueva</id>
    <url>https://repo.blueva.net/releases</url>
  </repository>
</repositories>

<dependency>
  <groupId>net.blueva</groupId>
  <artifactId>blueluak-jvm</artifactId>
  <version>26.4</version>
</dependency>
```

### Other Kotlin Multiplatform targets

`blueluak-core` is only distributed as a Kotlin Multiplatform library: every non-JVM target is a Kotlin `.klib`, consumable from another Kotlin Multiplatform Gradle project — not a raw JS/npm package, and not a C-callable Native library.

`LuaPlatform.standardGlobals()` works on every target, so no target needs a hand-assembled `Globals`:

```kotlin
import net.blueva.luak.lib.LuaPlatform

val globals = LuaPlatform.standardGlobals()
globals.load("print('hello, world')")!!.call()
```

`LuaPlatform.debugGlobals()` adds the `debug` library. Loading the individual classes in `net.blueva.luak.lib` (`BaseLib`, `PackageLib`, `StringLib`, `TableLib`, `MathLib`, `CoroutineLib`, `OsLib`, `IoLib`, `Bit32Lib`) by hand remains available when you want a smaller footprint.

Add the `repo.blueva.net/releases` repository shown above at the project level, then depend on the shared `net.blueva:blueluak-core:26.4

| Target | Gradle target function | Source set | Tested on |
|---|---|---|---|
| JavaScript IR | `js { nodejs() }` | `jsMain` | Node.js |
| WebAssembly | `wasmJs { nodejs() }` | `wasmJsMain` | Node.js |
| WebAssembly (WASI) | `wasmWasi { nodejs() }` | `wasmWasiMain` | Node.js's experimental `node:wasi` (raw `wasi_snapshot_preview1` syscalls — no host-specific APIs, so wasmtime/wasmer should work too, though only Node has been verified so far) |
| Kotlin/Native | `linuxX64()`, `mingwX64()`, `macosX64()`, `macosArm64()` | `linuxX64Main`, `mingwX64Main`, `macosX64Main`, `macosArm64Main` | Matching GitHub Actions runners in CI |

```kotlin
repositories {
    maven("https://repo.blueva.net/releases")
}

kotlin {
    js { nodejs() }
    wasmJs { nodejs() }
    wasmWasi { nodejs() }
    linuxX64()
    macosArm64()

    sourceSets {
        commonMain {
            dependencies {
                // Resolves to blueluak-core-js, -wasm-js, -wasm-wasi, -linuxx64,
                // -macosarm64, etc. automatically for each target above.
                implementation("net.blueva:blueluak-core:26.4")
            }
        }
    }
}
```

## Building

Build every target and module from a clean checkout:

```bash
./gradlew clean build
```

Build only the multiplatform core:

```bash
./gradlew :blueluak-core:build
```

Compile an individual target:

```bash
./gradlew :blueluak-core:compileKotlinJvm
./gradlew :blueluak-core:compileKotlinJs
./gradlew :blueluak-core:compileKotlinWasmJs
./gradlew :blueluak-core:compileKotlinMacosArm64
```

## Testing

Run every test suite available on the current host:

```bash
./gradlew :blueluak-core:allTests
```

Run an individual target suite:

```bash
./gradlew :blueluak-core:jvmTest
./gradlew :blueluak-core:jsNodeTest
./gradlew :blueluak-core:wasmJsNodeTest
./gradlew :blueluak-core:wasmWasiNodeTest
./gradlew :blueluak-core:macosArm64Test
```

Native tests can only run on their matching host. Cross-platform Native compilation remains available from supported hosts. The full build also runs the inherited JVM regression suite; it is green with no `ignoreFailures` exemptions, so any real regression fails the build.

## Requirements

| Component | Requirement |
|---|---|
| JDK | 17 or later, for Gradle and JVM targets |
| Kotlin | 2.4.10 |
| Gradle | 9.6.1 through the included wrapper |
| Node.js | Used for JavaScript and Wasm tests; managed by the Kotlin Gradle plugin |
| Native toolchain | Required only to link or run Kotlin/Native binaries on the host |

Use the included wrapper rather than a system Gradle installation.

## Development Status

BlueLuaK has completed its initial Kotlin and KMP restructuring. It is functional, but remains under active development.

| Area | Status |
|---|---|
| Kotlin migration | Complete; no Java source files |
| Multiplatform core | Compiles for JVM, JavaScript IR, Wasm, Linux x64, Windows x64, macOS x64, and macOS ARM64 |
| Lua parser | ANTLR Kotlin in `commonMain` |
| Lua runtime and compiler | Shared in `commonMain`; tested on JVM, JavaScript, and Wasm |
| Standard libraries | All nine (`base`, `package`, `string`, `table`, `math`, `io`, `os`, `coroutine`, `bit32`) shared in `commonMain` and available on every target |
| JVM integrations | `luajava`, `io.popen`, `os.execute`, the `luajc` JIT, CLI tooling, and `javax.script`, through `blueluak-jvm` |
| Lua language version | Lua 5.2 |
| Legacy regression suite | Green; no `ignoreFailures` exemptions |
| Native platform services | Coroutines, file/resource I/O, environment variables, GC control, and weak references are implemented for real and tested |
| Public API | `commonMain` exposes no JVM- or Node-specific type; binary compatibility is not yet guaranteed between releases |
| Future Lua work | Migration path from Lua 5.2 toward modern Lua releases |

## Platform Support and Limitations

The shared runtime, compiler, parser, and standard libraries behave identically on every target. What differs is what the *host* can provide, and BlueLuaK reports those gaps the way Lua does — `nil` plus a message, or an ordinary Lua error — rather than omitting functions:

| Capability | JVM | Kotlin/Native | JavaScript / Wasm-JS | Wasm-WASI |
|---|---|---|---|---|
| Files (`io.open`, `io.lines`, `os.remove`, `os.rename`) | Yes | Yes, POSIX `stdio` | Yes under Node (`node:fs`); unavailable in a browser | Yes, limited to the directories the host pre-opens |
| Script lookup (`require`, `dofile`) | Filesystem, then classpath | Filesystem | Filesystem under Node | Pre-opened directories |
| `os.getenv` | Environment, then system properties | `getenv` | `process.env` under Node | WASI `environ_get` |
| `io.popen`, `os.execute` | Yes | No — no portable process API | No | No |
| `package.loadlib` | Yes | No | No | No |
| Weak tables (`__mode`) | Yes | Yes | No — no weak references in the host | No |
| `os.date` / `os.time` | UTC | UTC | UTC | UTC |

Where a host grants no filesystem at all, `io.open` returns `nil` and a message and the rest of the library keeps working. `io.popen` behaves the same way outside `blueluak-jvm`.

Versioning is a single incrementing release number, published on every push to `master`. BlueLuaK is pre-1.0: the public API is still being refined and **binary compatibility between releases is not guaranteed**. Renamed public types keep a deprecated alias for at least one release. BlueLuaK is not source- or binary-compatible with LuaJ, and reintroducing `org.luaj.vm2` naming is out of scope.

## Roadmap

Current priorities are:

1. Modernize the Lua implementation beyond 5.2.

## License

BlueLuaK is distributed under the [MIT License](LICENSE).
