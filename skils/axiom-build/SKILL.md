---
name: axiom-build
description: Build and package AXIOM components (plugin, UI mod, launcher, portable client). Use when asked to compile, assemble releases, or produce build artifacts using build.sh, assemble_release.sh, or module-specific Gradle/Maven builds.
---

# AXIOM Build

## Overview

Use this skill to choose the correct build path and produce artifacts quickly and repeatably.

## Quick Start

1. Identify the target: plugin, UI mod, launcher, portable client, or full build.
2. Run the matching script/command.
3. Confirm output artifacts exist and report paths.

## Workflow

### 1) Full build (all components)

Run the repo-level build script:

```bash
./build.sh
```

Artifacts are copied to `builds/<timestamp>/`.

### 2) Portable client (AxiomClient)

Build the launcher first, then assemble:

```bash
cd axiom-launcher-kotlin
./gradlew clean build createExe -x test
cd ..
./assemble_release.sh
```

Output: `build_portable/AxiomClient/`.

### 3) Plugin only

```bash
cd axiom-plugin
mvn clean package -DskipTests
```

Output: `axiom-plugin/target/axiom-plugin-*.jar` (or `axiom-geopolitical-engine-*.jar` depending on module).

### 4) UI mod only

```bash
cd axiom-mod-integration
./gradlew clean build -x test
```

Output: `axiom-mod-integration/build/libs/axiomui-*.jar`.

### 5) Launcher only

```bash
cd axiom-launcher-kotlin
./gradlew clean build createExe -x test
```

Outputs:
- `axiom-launcher-kotlin/build/libs/axiom-launcher-*.jar`
- `axiom-launcher-kotlin/build/launch4j/AxiomLauncher.exe`

## Verification

- List artifacts with `ls -lh` and include sizes.
- For portable builds, confirm `AxiomLauncher.exe` and `lib/` exist in `build_portable/AxiomClient/`.

## Notes

- See `docs/DEV_GUIDE.md` for launcher details and packaging notes.
- `assemble_release.sh` expects launcher outputs in `axiom-launcher-kotlin/build/launch4j/`.
- The portable structure must preserve `minecraft/`, `lib/`, and `runtime/` layout.
