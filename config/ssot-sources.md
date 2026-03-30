# TurtleLib – Single Source of Truth

## Authoritative sources

- `config/matrix-targets.json`
  - Authoritative per target values:
    - `minecraft`
    - `java`
    - `neoforge`
    - `loader`
- `version.properties`
  - Authoritative version source (`version`).

## Derived locations

- Root workspace orchestration (`../..` via root `build.gradle`) derives matrix tasks from this file.
- Loader-specific module builds derive their effective versions from matrix target selection.

## Change workflow

1. Change `config/matrix-targets.json` first.
2. Keep docs/build metadata in sync after the matrix update.
3. Run root `verifyMatrixTargets` before merge.

## TurtleLib-specific policy

- NeoForge selection per MC branch is stable-first; if no stable exists, use latest beta.
