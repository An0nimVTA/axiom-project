#!/bin/bash
# AXIOM UI Mod - Скрипт сборки и установки

set -e

echo "╔═══════════════════════════════════════════════════════════════╗"
echo "║         AXIOM UI Mod - Сборка и установка                    ║"
echo "╚═══════════════════════════════════════════════════════════════╝"
echo ""

# Цвета
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Проверка Java
echo -e "${YELLOW}[1/5]${NC} Проверка Java..."
if ! command -v java &> /dev/null; then
    echo -e "${RED}✗ Java не найдена!${NC}"
    echo "Установите Java 17+: sudo apt install openjdk-17-jdk"
    exit 1
fi
JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt 17 ]; then
    echo -e "${RED}✗ Требуется Java 17+, найдена: $JAVA_VERSION${NC}"
    exit 1
fi
echo -e "${GREEN}✓ Java $JAVA_VERSION${NC}"

# Переход в директорию мода
cd "$(dirname "$0")/axiom-mod-integration"

# Очистка предыдущей сборки
echo -e "${YELLOW}[2/5]${NC} Очистка предыдущей сборки..."
if [ -d "build" ]; then
    rm -rf build
    echo -e "${GREEN}✓ Очищено${NC}"
else
    echo -e "${GREEN}✓ Нет предыдущей сборки${NC}"
fi

# Сборка мода
echo -e "${YELLOW}[3/5]${NC} Сборка мода..."
GRADLE_USER_HOME=$PWD/.gradle ./gradlew --no-daemon clean build

if [ $? -ne 0 ]; then
    echo -e "${RED}✗ Ошибка сборки!${NC}"
    exit 1
fi
echo -e "${GREEN}✓ Мод собран${NC}"

# Проверка JAR файла
JAR_FILE=$(find build/libs -name "axiomui-*.jar" -type f | head -n 1)
if [ -z "$JAR_FILE" ]; then
    echo -e "${RED}✗ JAR файл не найден!${NC}"
    exit 1
fi
echo -e "${GREEN}✓ Найден: $JAR_FILE${NC}"

# Установка в Minecraft
echo -e "${YELLOW}[4/5]${NC} Установка в Minecraft..."
MINECRAFT_DIR="$HOME/.minecraft"
MODS_DIR="$MINECRAFT_DIR/mods"

if [ ! -d "$MINECRAFT_DIR" ]; then
    echo -e "${RED}✗ Директория Minecraft не найдена: $MINECRAFT_DIR${NC}"
    echo "Создайте профиль Forge 1.20.1 и запустите игру хотя бы раз"
    exit 1
fi

if [ ! -d "$MODS_DIR" ]; then
    mkdir -p "$MODS_DIR"
    echo -e "${GREEN}✓ Создана директория модов${NC}"
fi

# Удаление старой версии
OLD_MOD=$(find "$MODS_DIR" -name "axiomui-*.jar" -type f | head -n 1)
if [ -n "$OLD_MOD" ]; then
    rm "$OLD_MOD"
    echo -e "${GREEN}✓ Удалена старая версия${NC}"
fi

# Копирование нового мода
cp "$JAR_FILE" "$MODS_DIR/"
echo -e "${GREEN}✓ Мод установлен в: $MODS_DIR${NC}"

# Проверка установки
echo -e "${YELLOW}[5/5]${NC} Проверка установки..."
INSTALLED_MOD=$(find "$MODS_DIR" -name "axiomui-*.jar" -type f | head -n 1)
if [ -n "$INSTALLED_MOD" ]; then
    MOD_SIZE=$(du -h "$INSTALLED_MOD" | cut -f1)
    echo -e "${GREEN}✓ Мод установлен: $(basename "$INSTALLED_MOD") ($MOD_SIZE)${NC}"
else
    echo -e "${RED}✗ Мод не найден в директории модов!${NC}"
    exit 1
fi

echo ""
echo "╔═══════════════════════════════════════════════════════════════╗"
echo "║                    Установка завершена!                      ║"
echo "╠═══════════════════════════════════════════════════════════════╣"
echo "║  Следующие шаги:                                             ║"
echo "║  1. Запустите Minecraft с профилем Forge 1.20.1              ║"
echo "║  2. Подключитесь к серверу с AXIOM плагином                  ║"
echo "║  3. Откройте меню: /axiomui                                  ║"
echo "║                                                               ║"
echo "║  Команды:                                                     ║"
echo "║    /axiomui           - Главное меню команд                  ║"
echo "║    /axiomui commands  - Меню команд                          ║"
echo "║    /axiomui religions - Меню религий                         ║"
echo "╚═══════════════════════════════════════════════════════════════╝"
echo ""
echo -e "${GREEN}Документация:${NC}"
echo "  - AXIOM_UI_MOD_GUIDE.md      - Полное руководство"
echo "  - AXIOM_UI_VISUAL_SCHEME.md  - Визуальная схема"
echo "  - AXIOM_UI_CHEATSHEET.md     - Быстрая шпаргалка"
echo "  - MODS_AND_TECH_TREE.md      - Моды и технологии"
echo ""
