#!/bin/bash
# Быстрая сборка UI мода

set -e
cd "$(dirname "$0")/axiom-mod-integration"

echo "🎨 СБОРКА UI МОДА"
echo "================="
echo ""

echo "📦 Сборка (это займет 3-5 минут)..."
./gradlew build --no-daemon

echo ""
echo "📋 Копирование..."

# Найти собранный JAR
MOD_JAR=$(find build/libs -name "axiomui*.jar" ! -name "*sources*" 2>/dev/null | head -1)

if [ -z "$MOD_JAR" ]; then
    echo "❌ Ошибка: JAR не найден!"
    exit 1
fi

# Копировать в клиент
cp "$MOD_JAR" ~/.minecraft/mods/
echo "✅ Скопирован в ~/.minecraft/mods/"

# Копировать на сервер (опционально)
if [ -d "../server/mods" ]; then
    cp "$MOD_JAR" ../server/mods/
    echo "✅ Скопирован в server/mods/"
fi

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "✅ UI МОД СОБРАН И УСТАНОВЛЕН!"
echo ""
echo "📝 Файл: $(basename $MOD_JAR)"
echo "📊 Размер: $(du -h $MOD_JAR | cut -f1)"
echo ""
echo "🎮 Теперь можно запускать Minecraft и тестировать!"
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
