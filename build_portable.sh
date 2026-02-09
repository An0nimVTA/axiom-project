#!/bin/bash
# Скрипт создания портативного клиента

echo "📦 Создание Portable Client..."

# 1. Создаем рабочую папку
mkdir -p build_portable/AxiomClient
cd build_portable

# 2. Скачивание Java 17 для Windows (Azul Zulu)
if [ ! -f "jre.zip" ]; then
    echo "⬇️ Скачивание Java 17..."
    wget -O jre.zip "https://cdn.azul.com/zulu/bin/zulu17.46.19-ca-jre17.0.9-win_x64.zip"
fi

# 3. Распаковываем Java
if [ ! -d "AxiomClient/runtime" ]; then
    echo "📂 Распаковка Java..."
    unzip -qo jre.zip
    mv zulu* AxiomClient/runtime
fi

# 4. Копируем лаунчер
echo "📋 Копирование лаунчера..."
LATEST_BUILD=$(ls -td ../builds/*/ | head -1)
cp "${LATEST_BUILD}AxiomLauncher.exe" AxiomClient/

# 5. Создаем BAT файл для запуска (на всякий случай)
cat > AxiomClient/Start.bat << 'EOF'
@echo off
title AXIOM Client
echo Zapusk...
start "" AxiomLauncher.exe
EOF

# 6. Архивируем всё вместе
echo "🗜️ Создание архива..."
zip -r AxiomClient_Portable.zip AxiomClient

echo "✅ Готово: $(pwd)/AxiomClient_Portable.zip"
