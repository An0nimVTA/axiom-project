#!/bin/bash
set -e

echo "🔄 Быстрое обновление dev-среды..."

# 1. Сборка
./build.sh

# Получаем путь к последней сборке
LATEST_BUILD=$(ls -td builds/*/ | head -1)
echo "📂 Исходная сборка: $LATEST_BUILD"

# 2. Обновление сервера
echo "➡️  Копирование мода на сервер..."
cp "${LATEST_BUILD}axiomui-mod.jar" "server/mods/axiomui-0.1.0.jar"

echo "➡️  Копирование плагина на сервер..."
rm -f server/plugins/axiom-plugin-*.jar
cp "${LATEST_BUILD}axiom-plugin.jar" "server/plugins/axiom-plugin-1.0.0.jar"

# 3. Обновление релизных файлов (для GitHub)
echo "➡️  Обновление релизных файлов..."
mkdir -p release_assets
cp "${LATEST_BUILD}axiom-launcher.jar" release_assets/
cp "${LATEST_BUILD}axiomui-mod.jar" release_assets/
# Пересобираем архив модов с обновленным jar
cd server/mods
zip -u ../../release_assets/mods.zip axiomui-0.1.0.jar
cd ../..

echo "✅ Готово! Можно запускать сервер."
