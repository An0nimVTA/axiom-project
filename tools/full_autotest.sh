#!/bin/bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT_DIR"

SKIP_FAST_TESTS="${SKIP_FAST_TESTS:-0}"
SKIP_BUILD="${SKIP_BUILD:-0}"
SKIP_LAUNCH="${SKIP_LAUNCH:-0}"
AUTO_USER="${AUTO_USER:-Autotest}"
AUTO_SERVER_DELAY_MS="${AUTO_SERVER_DELAY_MS:-60000}"
AUTO_MAX_RUNTIME_MS="${AUTO_MAX_RUNTIME_MS:-720000}"
AUTO_UI_TEST_AUTO_START_DELAY_TICKS="${AUTO_UI_TEST_AUTO_START_DELAY_TICKS:-200}"
AUTO_UI_TEST_STEP_DELAY_TICKS="${AUTO_UI_TEST_STEP_DELAY_TICKS:-10}"
AUTO_UI_TEST_COMMAND_TIMEOUT_TICKS="${AUTO_UI_TEST_COMMAND_TIMEOUT_TICKS:-600}"
AXIOM_AUTOTEST_SERVER_ONLY="${AXIOM_AUTOTEST_SERVER_ONLY:-0}"
AXIOM_AUTOTEST_FORCE_OFFLINE="${AXIOM_AUTOTEST_FORCE_OFFLINE:-0}"

echo "🧪 AXIOM FULL AUTOTEST"
echo "======================"
echo ""

CONFIG_FILE="$ROOT_DIR/launcher_config.json"
CONFIG_GAME_DIR=""

CONFIG_SERVER_START=""
if [ -f "$CONFIG_FILE" ]; then
  CONFIG_SERVER_START="$(CONFIG_FILE="$CONFIG_FILE" python3 - <<'PY'
import json
import os
path = os.environ.get("CONFIG_FILE", "")
try:
    with open(path, "r", encoding="utf-8") as f:
        data = json.load(f)
    print(data.get("serverStartPath", "") or "")
except Exception:
    print("")
PY
)"
  CONFIG_GAME_DIR="$(CONFIG_FILE="$CONFIG_FILE" python3 - <<'PY'
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

SERVER_START="${AXIOM_SERVER_START:-}"
if [ -z "$SERVER_START" ] && [ -n "$CONFIG_SERVER_START" ]; then
  SERVER_START="$CONFIG_SERVER_START"
fi
if [ -z "$SERVER_START" ]; then
  for candidate in \
    "$ROOT_DIR/server/start.sh" \
    "$ROOT_DIR/../server/start.sh" \
    "$HOME/axiom plugin/server/start.sh"
  do
    if [ -f "$candidate" ]; then
      SERVER_START="$candidate"
      break
    fi
  done
fi

GAME_DIR_NAME="${CONFIG_GAME_DIR:-minecraft}"
if [[ "$GAME_DIR_NAME" = /* ]]; then
  GAME_DIR="$GAME_DIR_NAME"
else
  GAME_DIR="$ROOT_DIR/$GAME_DIR_NAME"
fi

if [ -z "$SERVER_START" ]; then
  echo "❌ Скрипт запуска сервера не найден."
  echo "   Укажи путь через AXIOM_SERVER_START или launcher_config.json (serverStartPath)."
  exit 1
fi

SERVER_DIR="$(cd "$(dirname "$SERVER_START")" && pwd)"
SERVER_PROPERTIES="$SERVER_DIR/server.properties"
OFFLINE_BACKUP=""

restore_offline_mode() {
  if [ -n "${OFFLINE_BACKUP:-}" ] && [ -f "$OFFLINE_BACKUP" ]; then
    mv "$OFFLINE_BACKUP" "$SERVER_PROPERTIES"
    echo "   ✅ online-mode восстановлен из backup"
  fi
}

if [ "$AXIOM_AUTOTEST_FORCE_OFFLINE" = "1" ] && [ -f "$SERVER_PROPERTIES" ]; then
  OFFLINE_BACKUP="$SERVER_PROPERTIES.autotest.bak"
  cp "$SERVER_PROPERTIES" "$OFFLINE_BACKUP"
  SERVER_PROPERTIES="$SERVER_PROPERTIES" python3 - <<'PY'
import os
from pathlib import Path

path = Path(os.environ["SERVER_PROPERTIES"])
lines = []
found = False
for line in path.read_text(encoding="utf-8").splitlines():
    if line.strip().startswith("online-mode="):
        lines.append("online-mode=false")
        found = True
    else:
        lines.append(line)
if not found:
    lines.append("online-mode=false")
path.write_text("\n".join(lines) + "\n", encoding="utf-8")
PY
  echo "   ⚠️  online-mode=false для автотестов"
  trap restore_offline_mode EXIT
fi

if [ "$SKIP_FAST_TESTS" != "1" ]; then
  echo "1️⃣ Быстрые проверки..."
  ./tools/run_autotests.sh
  echo ""
fi

if [ "$SKIP_BUILD" != "1" ]; then
  echo "2️⃣ Сборка плагина..."
  cd "$ROOT_DIR/axiom-plugin"
  mvn clean package -DskipTests
  cd "$ROOT_DIR"

  echo "3️⃣ Сборка UI мода..."
  cd "$ROOT_DIR/axiom-mod-integration"
  GRADLE_USER_HOME="$ROOT_DIR/.gradle" ./gradlew clean build -x test
  cd "$ROOT_DIR"

  echo "4️⃣ Сборка лаунчера..."
  cd "$ROOT_DIR/axiom-launcher-kotlin"
  ./gradlew clean build createExe -x test
  cd "$ROOT_DIR"

  echo "5️⃣ Сборка portable клиента..."
  TEMP_PACK_DIR="$(mktemp -d)"
  for f in client_core.zip assets.zip modpack.zip; do
    if [ -f "$ROOT_DIR/build_portable/AxiomClient/$f" ]; then
      cp "$ROOT_DIR/build_portable/AxiomClient/$f" "$TEMP_PACK_DIR/"
    fi
  done
  if [ -x "$ROOT_DIR/assemble_release.sh" ]; then
    ./assemble_release.sh
  else
    bash ./assemble_release.sh
  fi
  for f in "$TEMP_PACK_DIR"/*; do
    if [ -f "$f" ]; then
      cp "$f" "$ROOT_DIR/build_portable/AxiomClient/"
    fi
  done
  rm -rf "$TEMP_PACK_DIR"
  echo ""
fi

echo "6️⃣ Деплой артефактов..."
mkdir -p "$ROOT_DIR/server/plugins" "$ROOT_DIR/server/mods"

AUTOTEST_BACKUP_DIR="$ROOT_DIR/server/plugins/_autotest_disabled/$(date +%Y%m%d-%H%M%S)"
moved_plugins=0
shopt -s nullglob
for f in "$ROOT_DIR/server/plugins"/axiom-*.jar \
         "$ROOT_DIR/server/plugins"/axiom-plugin-*.jar \
         "$ROOT_DIR/server/plugins"/AXIOM.jar; do
  if [ -f "$f" ]; then
    mkdir -p "$AUTOTEST_BACKUP_DIR"
    mv "$f" "$AUTOTEST_BACKUP_DIR/"
    moved_plugins=1
  fi
done
shopt -u nullglob
if [ "$moved_plugins" = "1" ]; then
  echo "   ⚠️  Старые AXIOM плагины перемещены в $AUTOTEST_BACKUP_DIR"
fi

PLUGIN_JAR="$(ls -t "$ROOT_DIR/axiom-plugin/target/axiom-plugin-"*.jar 2>/dev/null | grep -vE '(-sources|-javadoc)\.jar$' | head -1 || true)"
if [ -z "$PLUGIN_JAR" ]; then
  echo "❌ Не найден JAR плагина в axiom-plugin/target"
  exit 1
fi
cp "$PLUGIN_JAR" "$ROOT_DIR/server/plugins/AXIOM.jar"
echo "   ✅ Плагин -> server/plugins/AXIOM.jar"

UI_MOD_JAR="$(ls -t "$ROOT_DIR/axiom-mod-integration/build/libs/axiomui-"*.jar 2>/dev/null | grep -vE '(-sources|-javadoc)\.jar$' | head -1 || true)"
if [ -z "$UI_MOD_JAR" ]; then
  echo "❌ Не найден JAR UI мода в axiom-mod-integration/build/libs"
  exit 1
fi
cp "$UI_MOD_JAR" "$ROOT_DIR/server/mods/"
echo "   ✅ UI мод -> server/mods/"
echo ""

if [ "$AXIOM_AUTOTEST_SERVER_ONLY" = "1" ]; then
  echo "7️⃣ Запуск сервера (server-only autotest)..."
  SERVER_DIR="$SERVER_DIR"
  (
    cd "$SERVER_DIR"
    AXIOM_AUTOTEST=1 \
    AXIOM_AUTOTEST_SHUTDOWN=1 \
    AXIOM_AUTOTEST_DELAY_TICKS="${AXIOM_AUTOTEST_DELAY_TICKS:-200}" \
    bash "$SERVER_START"
  )
  exit 0
fi

echo "7️⃣ Настройка автозапуска/автотестов..."
ROOT_DIR="$ROOT_DIR" SERVER_START="$SERVER_START" AUTO_USER="$AUTO_USER" AUTO_SERVER_DELAY_MS="$AUTO_SERVER_DELAY_MS" python3 - <<'PY'
import json
import os
from pathlib import Path

root = Path(os.environ["ROOT_DIR"])
config_path = root / "launcher_config.json"
server_start = os.environ["SERVER_START"]
auto_user = os.environ["AUTO_USER"]
delay_ms = int(os.environ["AUTO_SERVER_DELAY_MS"])

data = {}
if config_path.exists():
    try:
        data = json.loads(config_path.read_text(encoding="utf-8"))
    except Exception:
        data = {}

data.setdefault("javaPath", "java")
data.setdefault("minRam", 2048)
data.setdefault("maxRam", 4096)
data.setdefault("gameDir", "minecraft")
data.setdefault("serverAddress", "localhost")
data.setdefault("serverPort", 25565)

if not data.get("lastUser"):
    data["lastUser"] = auto_user

data["serverStartPath"] = server_start
data["autoLaunch"] = True
data["autoStartServer"] = True
data["autoStartServerDelayMs"] = delay_ms
data["autoUiTests"] = True
data["autoUiTestIncludeScreens"] = True
data["autoUiTestAutoStartDelayTicks"] = int(os.environ.get("AUTO_UI_TEST_AUTO_START_DELAY_TICKS", "200"))
data["autoUiTestStepDelayTicks"] = int(os.environ.get("AUTO_UI_TEST_STEP_DELAY_TICKS", "10"))
data["autoUiTestCommandTimeoutTicks"] = int(os.environ.get("AUTO_UI_TEST_COMMAND_TIMEOUT_TICKS", "600"))
commands_raw = os.environ.get("AUTO_UI_TEST_COMMANDS", "").strip()
commands = []
if commands_raw:
    try:
        loaded = json.loads(commands_raw)
        if isinstance(loaded, list):
            commands = [str(v).strip() for v in loaded if str(v).strip()]
    except Exception:
        commands = [v.strip() for v in commands_raw.split(",") if v.strip()]
if not commands:
    commands = ["/test", "/testbot run", "/stop"]
data["autoUiTestCommands"] = commands
data["autoUiTestCommandBlacklist"] = []

config_path.write_text(json.dumps(data, ensure_ascii=False, indent=2), encoding="utf-8")
PY
echo "   ✅ launcher_config.json обновлен"
echo ""

if [ "$SKIP_LAUNCH" != "1" ]; then
  if [ ! -f "$ROOT_DIR/build_portable/AxiomClient/client_core.zip" ]; then
    echo "7.5️⃣ Создание локальных client_core.zip/assets.zip..."
    AXIOM_GAME_DIR="$GAME_DIR" AXIOM_PACKAGES_DIR="$ROOT_DIR/build_portable/AxiomClient" \
      "$ROOT_DIR/tools/make_client_packages.sh" || true
  fi

  if ! find "$GAME_DIR/libraries/net/minecraft/client" -name "*.jar" -print -quit >/dev/null 2>&1; then
    if [ -f "$ROOT_DIR/build_portable/AxiomClient/client_core.zip" ]; then
      echo "⚠️  Нет vanilla client библиотек в $GAME_DIR/libraries."
      echo "    Использую client_core.zip из build_portable/AxiomClient."
    else
      echo "❌ Нет vanilla client библиотек в $GAME_DIR/libraries."
      echo "   Положи client_core.zip + assets.zip в build_portable/AxiomClient или packages/"
      echo "   Либо укажи AXIOM_PACKAGES_DIR и запусти снова."
      exit 1
    fi
  fi
fi

if [ "$SKIP_LAUNCH" != "1" ]; then
  echo "8️⃣ Запуск лаунчера (автотест)..."
  export AXIOM_SERVER_START="$SERVER_START"
  LAUNCH_CMD=("$ROOT_DIR/build_portable/AxiomClient/start.sh" --headless)
  if [ "${AXIOM_USE_XVFB:-0}" = "1" ] || [ -z "${DISPLAY:-}" ]; then
    if command -v xvfb-run >/dev/null 2>&1; then
      LAUNCH_CMD=(xvfb-run -a "${LAUNCH_CMD[@]}")
      echo "   🖥️  Использую xvfb-run для виртуального дисплея."
    else
      echo "   ⚠️  xvfb-run не найден; запуск без виртуального дисплея."
    fi
  fi
  AXIOM_HEADLESS=1 AXIOM_AUTOTEST_MAX_RUNTIME_MS="$AUTO_MAX_RUNTIME_MS" \
    "${LAUNCH_CMD[@]}"
fi
