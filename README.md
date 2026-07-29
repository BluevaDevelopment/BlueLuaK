<p align="center">
  <img src="docs/media/logo.png" alt="BlueLuaK" width="760">
</p>

<p align="center">
  <strong>A Kotlin-first fork of LuaJ, modernized for embedding Lua in JVM applications.</strong>
</p>

<p align="center">
  <img alt="Version" src="https://img.shields.io/badge/version-3.0.2-blue">
  <img alt="Kotlin" src="https://img.shields.io/badge/Kotlin-2.1.0-7F52FF?logo=kotlin&logoColor=white">
  <img alt="Java" src="https://img.shields.io/badge/Java-17-ED8B00?logo=openjdk&logoColor=white">
  <img alt="Lua" src="https://img.shields.io/badge/Lua-5.2-000080?logo=lua&logoColor=white">
  <img alt="License" src="https://img.shields.io/badge/license-MIT-green">
</p>

## Overview

BlueLuaK is a fresh fork of [LuaJ](https://github.com/luaj/luaj) rebuilt around Kotlin and modern JVM tooling. The upstream LuaJ site is no longer active, so this fork is maintained from its GitHub sources.

The main goal is twofold:

1. **Kotlin migration** — replace the Java-centric build and API surface with idiomatic Kotlin and Gradle.
2. **Bring Lua up to date** — the current code targets Lua 5.2 (LuaJ 3.0.2), while the latest official release is [Lua 5.5.0](https://www.lua.org/download.html). Long term, BlueLuaK should move closer to modern Lua.

Right now it preserves the lightweight, embeddable Lua 5.2 runtime and removes legacy J2ME and Eclipse-specific baggage so it can live comfortably in contemporary projects.

The project provides:

- A clean Kotlin API over the LuaJ VM and value model.
- Lua 5.2 language support with bytecode compilation.
- Coroutines, metatables, and the standard library set.
- A path toward idiomatic Kotlin bindings and Gradle-based builds.

This is a first iteration: the README and repository metadata are being modernized before deeper structural changes land.

## Project Structure

| Component | Path | Purpose |
|---|---|---|
| Core VM | [`blueluak-core/src/main/java/net/blueva/luak/`](blueluak-core/src/main/java/net/blueva/luak/) | Platform-neutral Lua VM, compiler, and standard libraries |
| JSE Runtime | [`blueluak-jse/src/main/java/net/blueva/luak/`](blueluak-jse/src/main/java/net/blueva/luak/) | Java SE platform bindings, parser, and `luajava` integration |
| Grammar | [`grammar/`](grammar/) | JavaCC grammar for the Lua parser |
| Examples | [`examples/`](examples/) | Sample scripts and Java integrations |
| Tests | [`blueluak-jse/src/test/java/net/blueva/luak/`](blueluak-jse/src/test/java/net/blueva/luak/) | JUnit suites and Lua test suites |

Gradle modules:

| Module | Path | Output |
|---|---|---|
| `blueluak-core` | [`blueluak-core/`](blueluak-core/) | Core VM jar |
| `blueluak-jse` | [`blueluak-jse/`](blueluak-jse/) | JSE runtime jar |

All Java packages have been moved from `org.luaj.vm2` to `net.blueva.luak`.

## Building

```bash
./gradlew build
```

This compiles both Gradle modules and runs the existing JUnit test suite. Some legacy tests still fail under Java 17 (C-based Lua compatibility tests, deprecated APIs); the build succeeds but reports those failures so they can be addressed in follow-up iterations.

Build just one module:

```bash
./gradlew :blueluak-core:build
./gradlew :blueluak-jse:build
```

Run tests:

```bash
./gradlew test
```

## Requirements

| Component | Requirement |
|---|---|
| Java | 17 or later |
| Kotlin | 2.1.0 |
| Build | Gradle (Kotlin DSL) |

The project builds with Gradle.

## Development Status

BlueLuaK is in its very first iteration.

| Area | Status |
|---|---|
| README & metadata | Modernized |
| Build system | Gradle (Kotlin DSL) |
| Kotlin API | Scaffold planned |
| JSE runtime | Functional, based on LuaJ 3.0.2 |
| Lua version | 5.2 (targeting migration toward current Lua 5.5) |

