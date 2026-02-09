#!/bin/bash

echo "🧹 Начинаю глобальную очистку проекта..."

# 1. Очистка кэшей и артефактов сборки (Java/Kotlin)
echo "🗑️ Удаление build/ и target/..."
find . -type d -name "build" -not -path "./build_portable/*" -exec rm -rf {} +
find . -type d -name "target" -exec rm -rf {} +
find . -type d -name ".gradle" -exec rm -rf {} +
rm -rf .mvn/repository

# 2. Удаление временных папок проекта
echo "🗑️ Удаление временных папок (builds, backups, temp)..."
rm -rf builds/
rm -rf modpacks_temp/
rm -rf backups/
rm -rf test_instances/
rm -rf testbot/
rm -rf web_files/mods.zip
rm -rf web_files/server-core.jar

# 3. Очистка логов и временных файлов
echo "🗑️ Удаление логов..."
rm -rf *.log
rm -rf server/logs/
rm -rf server/cache/
rm -rf server/tacz_backup/
rm -f server.pid

# 4. Очистка build_portable (СОХРАНЯЯ AxiomClient)
echo "✨ Очистка build_portable (сохраняем только AxiomClient)..."
if [ -d "build_portable/AxiomClient" ]; then
    # Временно перемещаем готовый клиент
    mv build_portable/AxiomClient /tmp/AxiomClient_Safe
    # Удаляем всё в папке
    rm -rf build_portable/*
    # Возвращаем клиент обратно
    mv /tmp/AxiomClient_Safe build_portable/AxiomClient
else
    # Если клиента нет, просто чистим всё
    rm -rf build_portable/*
fi

# 5. Удаление лишних zip архивов в корне
rm -f *.zip

echo "✅ Очистка завершена! Проект чист."
echo "📂 Ваш готовый клиент лежит в: build_portable/AxiomClient/"
