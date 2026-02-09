#!/bin/bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"

python3 "$ROOT_DIR/tools/validate_recipes.py" "$ROOT_DIR/balance_config/recipes.json"
python3 "$ROOT_DIR/tools/validate_ui.py"

cd "$ROOT_DIR/axiom-launcher-kotlin"
GRADLE_USER_HOME="$ROOT_DIR/.gradle" ./gradlew --no-daemon test

cd "$ROOT_DIR"
./run-tests.sh
