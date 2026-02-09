---
name: axiom-autotests
description: Run and extend AXIOM automated tests for launcher, recipes, UI mod, and plugin testbot. Use when validating changes, adding new tests, or checking recipe balance and UI behavior.
---

# AXIOM Autotests

## Overview

Use this skill to run fast validation, smoke tests, and to extend test coverage for launcher logic, recipes, UI mod flows, and server-side testbot suites.

## Quick Start

Run fast checks (launcher unit tests + recipe JSON validation):

```bash
./tools/run_autotests.sh
```

## Test Types

### 1) Recipe validation (static)

Validate `balance_config/recipes.json` structure and item IDs:

```bash
python3 tools/validate_recipes.py balance_config/recipes.json
```

This checks:
- JSON structure and required fields
- item id format (`namespace:item`)
- positive integer counts

### 2) Launcher unit tests (JUnit)

```bash
cd axiom-launcher-kotlin
./gradlew --no-daemon test
```

Current coverage: ConfigManager persistence, GameLauncher classpath and Java detection.

### 3) UI mod build smoke tests

Use the repo-level UI mod test script:

```bash
./run-tests.sh
```

This compiles the UI mod and validates the built JAR content.

### 4) UI menu smoke tests (in-game)

From inside the client, run:

```
/axiomui test run
```

This opens the main menu, all UI screens, and all command detail/input screens without executing commands.

### 5) Plugin testbot (server integration)

Use the AXIOM plugin testbot on a running server:

```bash
cd axiom-plugin/testbot
./run_tests.sh ../server
```

Then execute in server console (or as OP):

```
/testbot run
```

See `axiom-plugin/docs/TESTING_GUIDE.md` for available suites.

## Extending UI Menu Tests

When you need to validate UI menu flows (not just commands):

1. Add a dev-only test mode toggle (config or hidden command) in `axiom-mod-integration`.
2. On client tick, open the target screen (e.g., `CommandMenuScreen`) and programmatically click each button.
3. Verify each click leads to a new screen or sends a packet without exceptions.
4. Log failures and return to the previous screen to continue.

Keep this behind a dev flag so it never runs in production builds.

## Extending Recipe Tests (integration)

For deeper validation beyond static JSON:

- Add a testbot case that inspects `RecipeManager` and confirms expected recipes are present.
- Check that each ingredient exists in the registry and no recipe resolves to empty ingredients.

## Notes

- Use static validation for fast feedback on balance changes.
- Use testbot integration for full server-side verification after mod/plugin updates.
