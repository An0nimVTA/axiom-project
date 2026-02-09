#!/bin/bash

# Настройки сервера (берем из update-vps.sh)
VPS_HOST="root@193.23.201.6"
VPS_PATH="/var/www/axiom/updates"
VPS_PASS="artur907665A"

echo "=== Создание и загрузка client_core.zip ==="

# Откуда брать файлы?
# Предполагаем, что файлы лежат в build_portable/AxiomClient/minecraft/
# Если их там нет, скрипт попытается найти их в ~/.minecraft (как резерв)
SOURCE_DIR="build_portable/AxiomClient/minecraft"

if [ ! -d "$SOURCE_DIR/versions" ]; then
    echo "⚠️  В $SOURCE_DIR нет файлов игры."
    SOURCE_DIR="$HOME/.minecraft"
    echo "🔄 Пробуем взять из: $SOURCE_DIR"
fi

if [ ! -d "$SOURCE_DIR/versions" ]; then
    echo "❌ ОШИБКА: Не найдена папка с игрой (versions, libraries) ни в проекте, ни в ~/.minecraft"
    echo "Пожалуйста, скопируйте рабочий клиент (без модов) в build_portable/AxiomClient/minecraft/"
    exit 1
fi

echo "📦 Упаковка файлов из $SOURCE_DIR..."
# Создаем временный архив
# Исключаем mods, logs, saves, screenshots, options.txt, servers.dat
cd "$SOURCE_DIR" || exit
zip -r ../client_core.zip \
    libraries \
    versions \
    config \
    defaultconfigs \
    -x "mods/*" \
    -x "logs/*" \
    -x "saves/*" \
    -x "screenshots/*" \
    -x "web_cache/*" \
    -x "assets/*" \
    -x "*.log"

cd - > /dev/null
ZIP_FILE="$SOURCE_DIR/../client_core.zip"

echo "📤 Загрузка на сервер ($VPS_HOST)..."
if command -v sshpass &> /dev/null; then
    sshpass -p "$VPS_PASS" scp -o StrictHostKeyChecking=no "$ZIP_FILE" "$VPS_HOST:$VPS_PATH/client_core.zip"
else
    echo "⚠️  sshpass не установлен. Пробуем обычный scp (введите пароль: $VPS_PASS)"
    scp -o StrictHostKeyChecking=no "$ZIP_FILE" "$VPS_HOST:$VPS_PATH/client_core.zip"
fi

if [ $? -eq 0 ]; then
    echo "✅ client_core.zip успешно обновлен на сервере!"
    echo "🔗 Ссылка: http://193.23.201.6:8080/updates/client_core.zip"
    rm "$ZIP_FILE" # Удалить локальный архив после загрузки
else
    echo "❌ Ошибка загрузки."
fi
