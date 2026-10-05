# TurtleLib – NeoForge

Thin NeoForge packaging module for TurtleLib.

All library code lives in the shared `:turtlelib-core` module (pure Java + Gson,
with no Minecraft/loader classes). This module only carries the NeoForge mod
metadata (`META-INF/neoforge.mods.toml`) and bundles the compiled core classes
into the distributable NeoForge jar.

- Helper API and usage: see the [GitHub wiki](https://github.com/hrobasti/turtle-lib-mod/wiki)
- Shared source of truth for the code: [`../core`](../core)
