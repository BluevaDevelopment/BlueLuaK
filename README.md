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
| Core VM | [`src/core/`](src/core/) | Platform-neutral Lua VM, compiler, and standard libraries |
| JSE Runtime | [`src/jse/`](src/jse/) | Java SE platform bindings and `luajava` integration |
| JME Runtime | [`src/jme/`](src/jme/) | Java ME platform bindings (legacy, under review) |
| Grammar | [`grammar/`](grammar/) | JavaCC grammars for Lua 5.1 and 5.2 |
| Examples | [`examples/`](examples/) | Sample scripts and Java integrations |
| Tests | [`test/`](test/) | JUnit suites and Lua test suites |

## Requirements

| Component | Requirement |
|---|---|
| Java | 17 or later |
| Kotlin | 2.1.0 |
| Build | Ant (Gradle migration planned) |

The project currently builds with Ant (`build.xml`). A Gradle-based build with Kotlin DSL is part of the planned Kotlin migration.

## Development Status

BlueLuaK is in its very first iteration.

| Area | Status |
|---|---|
| README & metadata | Modernized |
| Build system | Ant (legacy) → Gradle migration planned |
| Kotlin API | Scaffold planned |
| JSE runtime | Functional, based on LuaJ 3.0.2 |
| JME runtime | Legacy, under review for removal |
| Lua version | 5.2 (targeting migration toward current Lua 5.5) |

