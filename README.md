# TurtleLib ⚙️

A lightweight helper library for Minecraft mods on Fabric and NeoForge. TurtleLib does not add gameplay on its own — it provides shared technical systems used by other mods.

TurtleLib is a dependency library designed to keep mod code cleaner and more reusable across loaders. If a mod requires TurtleLib, it must be installed in the same `mods/` folder.

## Features ✨

- Locale loading and synchronization (`LangLoader`)
- Localized message formatting (`MessageService`)
- Provider-based update checks (`UpdateChecker`)
- Deterministic version comparison (`VersionComparator`)
- Shared behavior across Fabric and NeoForge modules

## Quick Start (Players) 🚀

1. Download the TurtleLib version that matches your Minecraft/loader version.
2. Put the TurtleLib JAR into your Minecraft `mods/` folder.
3. Add the dependent mod(s) that require TurtleLib.
4. Start the game normally.

If TurtleLib is missing, dependent mods may fail to load.

## Build quickstart (developers) 🛠️

This repository is a standalone Gradle build. Clone it and use its own wrapper; no other repository is needed. A JDK 17+ must be installed to start Gradle. The Java toolchain required for compiling is detected automatically or downloaded.

- Build and test: `./gradlew build`, `./gradlew testTurtleLib`
- Validate the build matrix: `./gradlew verifyMatrixTargets`
- Full release build (all matrix targets) into `dist/`: `./gradlew releaseTurtleLib`

Minecraft, Java and loader versions come from [`config/matrix-targets.json`](config/matrix-targets.json). Without parameters, builds use the first target in that file.

Every release also produces `TurtleLib_<version>_core.jar`. This is the loader-neutral library that other mods compile against, published as a GitHub release asset. It is a developer artifact and must not be installed into the `mods/` folder.

Optional: for the nested matrix builds, a specific JDK per Java version can be set via the Gradle property `jdk_<n>_home` or the environment variable `JDK_<n>_HOME`, e.g. `JDK_25_HOME`. Without it, they run on the current JVM.

## AI support & privacy transparency 🤖

Parts of the source code and documentation were created with AI assistance.

- **No personal player data is used** in this process.
- Ideas, design decisions, and quality standards come from humans.
- Quality assurance is reviewed and validated by humans.

## Developer notes (short) 🧩

- SSOT ownership rules for TurtleLib are defined in [`config/ssot-sources.md`](config/ssot-sources.md).

## License 📄

Apache-2.0

Full license text:

- [`LICENSE`](LICENSE)

Third-party license texts/references:

- [`THIRD_PARTY_NOTICES.md`](THIRD_PARTY_NOTICES.md)
