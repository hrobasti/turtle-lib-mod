# TurtleLib 🐢

TurtleLib is a small helper library for Minecraft mods by kroet.net on Fabric and NeoForge. It adds no gameplay, items or settings of its own; mods such as PermaClicker and TradeShuffler use it for shared technical parts like their update check.

## Quick start for players 🎮

1. Download the TurtleLib file for your loader (Fabric or NeoForge) and Minecraft version.
2. Put it into the `mods` folder, next to the mods that require it.
3. Start the game.

If TurtleLib is missing or too old, the game names the required version when it starts. Launchers such as the Modrinth App and CurseForge install it automatically as a dependency.

## Update together ⚠️

TurtleLib 2.0.0 is not compatible with mods built for TurtleLib 1.x, and those mods do not run with it. Update TurtleLib together with the mods that require it (e.g. PermaClicker 2.0.0).

## What TurtleLib provides ✨

- **Update check** (`UpdateChecker`): finds the newest version of a mod on Modrinth and CurseForge, filtered by release channel (Stable / Beta / Alpha), loader and Minecraft version
- **Version comparison** (`VersionComparator`): compares version numbers such as `2.0.0`, `2.1.0-beta.2` or `1.4.0+26.3` reliably
- the same behavior on Fabric and NeoForge, from one shared code base

## Update check & privacy 🔔

TurtleLib does not connect to anything on its own. Only when a mod uses its update check does that mod query `api.modrinth.com` (Modrinth, official API) and `api.cfwidget.com` (CFWidget, an unofficial third-party service that mirrors CurseForge data). Each mod documents on its own page what it sends and how to turn the check off.

With Mod Menu installed (Fabric), Mod Menu may also check TurtleLib itself for updates on Modrinth; that check is configured in Mod Menu's own settings.

## Quick start for devs 🛠️

This repository is a standalone Gradle build. Clone it and use its own wrapper; no other repository is needed. A JDK 17+ must be installed to start Gradle. The Java toolchain required for compiling is detected automatically or downloaded.

- Build and test: `./gradlew build`, `./gradlew testTurtleLib`
- Validate the build matrix: `./gradlew verifyMatrixTargets`
- Full release build (all matrix targets, Fabric + NeoForge, plus the core jar) into `dist/`: `./gradlew releaseTurtleLib`

Minecraft, Java and loader versions come from [`config/matrix-targets.json`](config/matrix-targets.json). Without parameters, builds use the first target in that file.

**Using TurtleLib in your mod:** every GitHub release includes `TurtleLib_<version>_core.jar`, the loader-neutral library to compile against. Your mod declares TurtleLib as a required dependency and never bundles it; the core jar is a developer artifact and does not belong in the `mods` folder. Gradle setup, API reference and the migration from 1.x are in the [wiki](https://github.com/hrobasti/turtle-lib-mod/wiki).

## AI support & privacy transparency 🤖

Parts of the source code and documentation were created with AI assistance.

- **No personal player data is used** in this process.
- Ideas, design decisions, and quality standards come from humans.
- Quality assurance is reviewed and validated by humans.

## Developer notes 🧩

All library code lives in the loader-neutral `core` module; the Fabric and NeoForge modules only add the mod metadata. TurtleLib has no translation system: consumer mods translate with Minecraft's own lang files. Which file owns which setting is defined in [`config/ssot-sources.md`](config/ssot-sources.md).

## License 📄

MIT

Full license text:

- [`LICENSE`](LICENSE)

Third-party license texts/references:

- [`THIRD_PARTY_NOTICES.md`](THIRD_PARTY_NOTICES.md)
