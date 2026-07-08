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

- Root workspace orchestration (`../..` via root `build.gradle`) derives matrix tasks from this file.
- Loader-specific module builds derive their effective versions from matrix target selection.

## Change workflow

1. Change `config/matrix-targets.json` first.
2. Keep docs/build metadata in sync after the matrix update.
3. Run root `verifyMatrixTargets` before merge.

## TurtleLib-specific policy

- NeoForge selection per MC branch is stable-first; if no stable exists, use latest beta.
