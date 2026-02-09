#!/bin/bash

VPS_HOST="root@193.23.201.6"
VPS_PATH="/var/www/axiom/updates"
VPS_PASS="artur907665A"

echo "=== Создание разделённых архивов ==="

SOURCE_DIR="build_portable/AxiomClient/minecraft"

if [ ! -d "$SOURCE_DIR/versions" ]; then
    echo "❌ Не найдена папка $SOURCE_DIR"
    exit 1
fi

cd "$SOURCE_DIR" || exit

# 1. Основной архив (без assets)
echo "📦 Создание client_core.zip (libraries, versions, config)..."
zip -r ../client_core.zip \
    libraries \
    versions \
    config \
    defaultconfigs \
    -x "*.log"

# 2. Assets отдельно
echo "📦 Создание assets.zip..."
zip -r ../assets.zip assets

cd - > /dev/null

echo ""
echo "📊 Размеры архивов:"
ls -lh build_portable/AxiomClient/client_core.zip
ls -lh build_portable/AxiomClient/assets.zip

echo ""
echo "📤 Загрузка client_core.zip на сервер..."
if command -v sshpass &> /dev/null; then
    sshpass -p "$VPS_PASS" scp -o StrictHostKeyChecking=no \
        build_portable/AxiomClient/client_core.zip \
        "$VPS_HOST:$VPS_PATH/client_core.zip"
else
    scp -o StrictHostKeyChecking=no \
        build_portable/AxiomClient/client_core.zip \
        "$VPS_HOST:$VPS_PATH/client_core.zip"
fi

if [ $? -eq 0 ]; then
    echo "✅ client_core.zip загружен"
else
    echo "❌ Ошибка загрузки client_core.zip"
    exit 1
fi

echo ""
echo "📤 Загрузка assets.zip на сервер..."
if command -v sshpass &> /dev/null; then
    sshpass -p "$VPS_PASS" scp -o StrictHostKeyChecking=no \
        build_portable/AxiomClient/assets.zip \
        "$VPS_HOST:$VPS_PATH/assets.zip"
else
    scp -o StrictHostKeyChecking=no \
        build_portable/AxiomClient/assets.zip \
        "$VPS_HOST:$VPS_PATH/assets.zip"
fi

if [ $? -eq 0 ]; then
    echo "✅ assets.zip загружен"
    echo ""
    echo "🔗 Ссылки:"
    echo "   http://193.23.201.6:8080/updates/client_core.zip"
    echo "   http://193.23.201.6:8080/updates/assets.zip"
    
    # Удаляем локальные архивы
    rm build_portable/AxiomClient/client_core.zip
    rm build_portable/AxiomClient/assets.zip
else
    echo "❌ Ошибка загрузки assets.zip"
    exit 1
fi
