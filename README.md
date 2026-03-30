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

TurtleLib ships loader-local Gradle wrappers, so no global Gradle install is required.

- Fabric build entrypoints: `fabric/gradlew` (Linux/macOS), `fabric/gradlew.bat` (Windows)
- NeoForge build entrypoints: `neoforge/gradlew` (Linux/macOS), `neoforge/gradlew.bat` (Windows)

Typical tasks: `build`, `test`, `tasks`.

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
