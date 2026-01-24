#!/bin/bash
set -e

echo "🔨 AXIOM - Быстрая сборка"
echo "=========================="

cd "$(dirname "$0")"

# 1. Плагин
echo ""
echo "📦 1/3 Сборка плагина..."
cd axiom-plugin
if [ -f "gradlew" ]; then
    ./gradlew clean shadowJar --no-daemon
else
    mvn clean package
fi
cd ..

# 2. UI Мод
echo ""
echo "🎨 2/3 Сборка UI мода..."
cd axiom-mod-integration
if [ -f "gradlew" ]; then
    ./gradlew clean build --no-daemon
else
    echo "⚠️  Нет gradlew, пропускаем"
fi
cd ..

# 3. Копирование
echo ""
echo "📋 3/3 Копирование файлов..."

PLUGIN_JAR=$(find axiom-plugin/target axiom-plugin/build/libs -name "axiom-plugin*.jar" 2>/dev/null | grep -v "original" | head -1)
MOD_JAR=$(find axiom-mod-integration/build/libs -name "axiomui*.jar" 2>/dev/null | grep -v "sources" | head -1)

if [ -n "$PLUGIN_JAR" ]; then
    cp "$PLUGIN_JAR" server/plugins/
    echo "✅ Плагин: $PLUGIN_JAR"
else
    echo "❌ Плагин не найден"
fi

if [ -n "$MOD_JAR" ]; then
    cp "$MOD_JAR" server/mods/
    echo "✅ UI мод: $MOD_JAR"
else
    echo "❌ UI мод не найден"
fi

echo ""
echo "✅ Сборка завершена!"
echo ""
echo "🚀 Запуск сервера:"
echo "   cd server && ./start.sh"
