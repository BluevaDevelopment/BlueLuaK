<p align="center">
  <img src="docs/media/logo.png" alt="BlueLuaK" width="760">
</p>

<p align="center">
  <strong>A Kotlin Multiplatform implementation of an embeddable Lua 5.2 runtime.</strong>
</p>

<p align="center">
  <img alt="Version" src="https://img.shields.io/badge/version-26.2-blue">
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
- Shared tests for the runtime, compiler, and parser across KMP targets.
- JVM integrations for filesystem access, processes, Java reflection, script engines, and `luajava`.

BlueLuaK is no longer source-compatible with LuaJ: modules, packages, platform classes, and APIs use BlueLuaK naming under `net.blueva.luak`.

## Multiplatform Architecture

| Source set or module | Purpose |
|---|---|
| [`blueluak-core/src/commonMain/kotlin/`](blueluak-core/src/commonMain/kotlin/) | Shared Lua runtime, compiler, AST, parser, and libraries |
| [`blueluak-core/src/jvmMain/kotlin/`](blueluak-core/src/jvmMain/kotlin/) | JVM implementations of platform abstractions |
| [`blueluak-core/src/nonJvmMain/kotlin/`](blueluak-core/src/nonJvmMain/kotlin/) | Portable implementations shared by JavaScript and Wasm |
| [`blueluak-core/src/nativeMain/kotlin/`](blueluak-core/src/nativeMain/kotlin/) | Kotlin/Native implementations of platform abstractions |
| [`blueluak-core/src/commonTest/kotlin/`](blueluak-core/src/commonTest/kotlin/) | Tests shared by all core targets |
| [`blueluak-jvm/src/main/kotlin/`](blueluak-jvm/src/main/kotlin/) | JVM-only integrations and command-line tooling |
| [`grammar/`](grammar/) | ANTLR Kotlin lexer and parser grammars for Lua 5.2 |
| [`examples/`](examples/) | Kotlin and Lua usage examples |

Gradle modules:

| Module | Targets | Purpose |
|---|---|---|
| `blueluak-core` | JVM, JavaScript IR, Wasm, Kotlin/Native | Multiplatform Lua runtime, compiler, and parser |
| `blueluak-jvm` | JVM | JVM platform adapters, `luajava`, scripting, CLI, and JIT support |

Platform-dependent functionality is exposed through `expect`/`actual` implementations. Code intended to run on every target belongs in `commonMain`; Java and JVM APIs remain confined to JVM source sets and `blueluak-jvm`.

## Installation

Releases publish to [repo.blueva.net](https://repo.blueva.net/releases), a public Maven repository — no authentication needed to depend on BlueLuaK.

### JVM projects

Two artifacts are available. Pick one:

| Artifact | Contains | Use it when |
|---|---|---|
| `blueluak-jvm` | The multiplatform core (as a compile dependency) plus `JvmPlatform.standardGlobals()`, `luajava`, the `luajc` JIT compiler, CLI tooling, and `javax.script` integration | You want a ready-to-use Lua runtime — the common case |
| `blueluak-core-jvm` | Just the shared runtime, compiler, AST, parser, and standard libraries on the JVM target, with no JVM-specific globals helper | You're assembling your own `Globals` from individual library classes, or want the smallest possible footprint |

`blueluak-jvm` pulls in `blueluak-core-jvm` transitively, so depending on it alone is enough for most projects.

**Gradle (Kotlin DSL)**

```kotlin
repositories {
    maven("https://repo.blueva.net/releases")
}

dependencies {
    implementation("net.blueva:blueluak-jvm:26.2")
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
  <version>26.2</version>
</dependency>
```

### Other Kotlin Multiplatform targets

`blueluak-core` is only distributed as a Kotlin Multiplatform library: every non-JVM target is a Kotlin `.klib`, consumable from another Kotlin Multiplatform Gradle project — not a raw JS/npm package, and not a C-callable Native library. There's no `JvmPlatform`-equivalent convenience on these targets yet; build your own `Globals` from the individual library classes in `net.blueva.luak.lib` (`BaseLib`, `PackageLib`, `StringLib`, `TableLib`, `MathLib`, `CoroutineLib`, `OsLib`, `IoLib`, `Bit32Lib`).

Add the `repo.blueva.net/releases` repository shown above at the project level, then depend on the shared `net.blueva:blueluak-core:26.2` coordinates from each target's own source set — Gradle resolves the matching platform artifact automatically, so you never reference the suffixed artifacts (`blueluak-core-js`, `-wasm-js`, `-wasm-wasi`, `-linuxx64`, ...) directly:

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
                implementation("net.blueva:blueluak-core:26.2")
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
./gradlew :blueluak-core:macosArm64Test
```

Native tests can only run on their matching host. Cross-platform Native compilation remains available from supported hosts. The full build also runs the inherited JVM regression suite. Some legacy LuaJ tests remain configured with `ignoreFailures` while their Kotlin migration issues are resolved; failures are reported without hiding compilation or KMP test failures.

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
| JVM integrations | Available through `blueluak-jvm` |
| Lua language version | Lua 5.2 |
| Legacy regression suite | Compiles; remaining migrated-test failures are being addressed |
| Native platform services | Portable core available; filesystem resources and full coroutine semantics still need target-specific implementations |
| Future Lua work | Migration path from Lua 5.2 toward modern Lua releases |

## Roadmap

Current priorities are:

1. Resolve the remaining inherited JVM regression failures.
2. Complete target-specific Native I/O, resources, and coroutine behavior.
3. Modernize the Lua implementation beyond 5.2.
4. Continue refining multiplatform APIs without restoring LuaJ compatibility constraints.

## License

BlueLuaK is distributed under the [MIT License](LICENSE).
