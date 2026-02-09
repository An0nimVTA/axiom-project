#!/bin/bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
CONFIG_FILE="$ROOT_DIR/launcher_config.json"

GAME_DIR_NAME="${AXIOM_GAME_DIR:-}"
if [ -z "$GAME_DIR_NAME" ] && [ -f "$CONFIG_FILE" ]; then
  GAME_DIR_NAME="$(CONFIG_FILE="$CONFIG_FILE" python3 - <<'PY'
import json
import os
path = os.environ.get("CONFIG_FILE", "")
try:
    with open(path, "r", encoding="utf-8") as f:
        data = json.load(f)
    print(data.get("gameDir", "") or "")
except Exception:
    print("")
PY
)"
fi

GAME_DIR_NAME="${GAME_DIR_NAME:-minecraft}"
if [[ "$GAME_DIR_NAME" = /* ]]; then
  GAME_DIR="$GAME_DIR_NAME"
else
  GAME_DIR="$ROOT_DIR/$GAME_DIR_NAME"
fi

PACK_DIR="${AXIOM_PACKAGES_DIR:-$ROOT_DIR/build_portable/AxiomClient}"
mkdir -p "$PACK_DIR"

if ! command -v zip >/dev/null 2>&1; then
  echo "❌ Утилита zip не найдена. Установи zip и повтори."
  exit 1
fi

echo "📦 Создание пакетов клиента"
echo "Game dir: $GAME_DIR"
echo "Output:   $PACK_DIR"
echo ""

if [ ! -d "$GAME_DIR/versions" ] || [ ! -d "$GAME_DIR/libraries" ]; then
  echo "❌ Не найдены versions/ или libraries/ в $GAME_DIR"
  echo "   Запусти клиент хотя бы один раз и повтори."
  exit 1
fi

if ! find "$GAME_DIR/libraries/net/minecraft/client" -name "*.jar" -print -quit >/dev/null 2>&1; then
  if find "$GAME_DIR/versions" -maxdepth 2 -name "*.jar" -print -quit >/dev/null 2>&1; then
    echo "⚠️  Не найдены client-*.jar в libraries/net/minecraft/client."
    echo "    Продолжаю упаковку на основе versions/*.jar."
  else
    echo "❌ Нет vanilla client библиотек в $GAME_DIR/libraries и отсутствуют версии в $GAME_DIR/versions"
    echo "   Запусти клиент хотя бы один раз и повтори."
    exit 1
  fi
fi

CORE_OUT="$PACK_DIR/client_core.zip"
ASSETS_OUT="$PACK_DIR/assets.zip"

echo "1️⃣ client_core.zip..."
(
  cd "$GAME_DIR"
  zip -r "$CORE_OUT" libraries versions config defaultconfigs -x "*.log"
)

if [ -d "$GAME_DIR/assets" ]; then
  echo "2️⃣ assets.zip..."
  (cd "$GAME_DIR" && zip -r "$ASSETS_OUT" assets)
else
  echo "⚠️  assets/ не найдена, assets.zip не создан."
fi

echo ""
echo "✅ Готово:"
ls -lh "$CORE_OUT" 2>/dev/null || true
ls -lh "$ASSETS_OUT" 2>/dev/null || true
