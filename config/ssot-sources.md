# TurtleLib – Single Source of Truth

## Authoritative sources

- `config/matrix-targets.json`
  - Authoritative per target values:
    - `minecraft`
    - `java`
    - `neoforge`
    - `fabricLoader`
    - `fabricApi`
    - `modmenu`
- `version.properties`
  - Authoritative version source (`version`).

## Module layout

- `core/` (`:turtlelib-core`) is the single source of truth for all loader-neutral
  Java code and tests (pure Java + Gson, no Minecraft/loader classes).
- `fabric/` (`:turtlelib-fabric`) and `neoforge/` (`:turtlelib-neoforge`) are thin
  packaging modules: they only own loader metadata (`fabric.mod.json` /
  `neoforge.mods.toml`) and bundle the compiled `core` classes/sources into the
  distributable jar. Do not duplicate library code into the loader modules.

## Derived locations

- `gradle/matrix-defaults.gradle` reads this file for every module build. Without `-Pturtlelib_minecraft_version`, the first target is the default. Explicit `-Pturtlelib_*` values override single versions.
- The repository root `build.gradle` derives the matrix release tasks (`releaseTurtleLib`) from this file.
- No copies of these versions exist in `gradle.properties`, neither in this repository nor in the hrobasti-mods workspace root.

## Change workflow

1. Change `config/matrix-targets.json` first.
2. Keep docs/build metadata in sync after the matrix update.
3. Run `./gradlew verifyMatrixTargets` before merge. Inside the hrobasti-mods workspace, the root task additionally checks that all mods agree on the first target's Minecraft/Java versions.

## TurtleLib-specific policy

- NeoForge selection per MC branch is stable-first; if no stable exists, use latest beta.
