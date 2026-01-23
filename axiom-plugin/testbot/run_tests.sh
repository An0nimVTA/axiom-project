#!/bin/bash

# Скрипт для автоматического запуска тестов AXIOM плагина
# Использование: ./run_tests.sh [путь_к_серверу]

SERVER_PATH=${1:-"../server"}

echo "=== Запуск автоматических тестов AXIOM плагина ==="

# Проверка наличия сервера
if [ ! -d "$SERVER_PATH" ]; then
    echo "Ошибка: Директория сервера не найдена: $SERVER_PATH"
    exit 1
fi

# Проверка наличия JAR файла плагина
PLUGIN_JAR=$(find ../target -name "axiom-geopolitical-engine-*.jar" | head -n 1)
if [ -z "$PLUGIN_JAR" ]; then
    echo "Ошибка: JAR файл плагина не найден. Сначала выполните: mvn clean package"
    exit 1
fi

echo "Найден JAR файл: $PLUGIN_JAR"

# Проверка наличия плагина в директории сервера
if [ ! -f "$SERVER_PATH/plugins/$(basename $PLUGIN_JAR)" ]; then
    echo "Копирование плагина в директорию сервера..."
    cp "$PLUGIN_JAR" "$SERVER_PATH/plugins/"
fi

# Запуск сервера с включенным автотестом
echo "Запуск тестового сервера..."
cd "$SERVER_PATH"

# Создание скрипта для выполнения команд автотеста
TEST_SCRIPT="test_commands.txt"
cat > $TEST_SCRIPT << EOF
op test_player
give test_player minecraft:stone 64
execute as test_player run say Начало автоматического тестирования
execute as test_player run axiom test run all
execute as test_player run testbot run
execute as test_player run say Завершение автоматического тестирования
deop test_player
EOF

# Важно: для настоящего запуска нужно использовать консоль сервера
echo "Для запуска тестов введите на серверной консоли:"
echo "  /testbot run"
echo "Или для специфического теста:"
echo "  /testbot run 'Название теста'"

echo "=== Автотесты инициализированы ==="