#!/bin/bash

# Скрипт быстрой сборки и тестирования AXIOM плагина
# Использование: ./build_and_test.sh [clean|test|install]

ACTION=${1:-"build"}

echo "=== AXIOM Build & Test Script ==="

case $ACTION in
    "clean")
        echo "Очистка проекта..."
        mvn clean
        ;;
    "build")
        echo "Сборка проекта..."
        mvn clean package -DskipTests
        
        if [ $? -eq 0 ]; then
            echo "Сборка завершена успешно!"
            echo "JAR файл: $(find target -name '*.jar' | head -n 1)"
        else
            echo "Ошибка сборки!"
            exit 1
        fi
        ;;
    "test")
        echo "Запуск тестов..."
        mvn test
        
        if [ $? -eq 0 ]; then
            echo "Тесты пройдены успешно!"
        else
            echo "Тесты не пройдены!"
            exit 1
        fi
        ;;
    "install")
        echo "Установка плагина на тестовый сервер..."
        
        # Сборка проекта
        mvn clean package -DskipTests
        if [ $? -ne 0 ]; then
            echo "Ошибка сборки!"
            exit 1
        fi
        
        # Копирование в тестовый сервер
        JAR_FILE=$(find target -name "axiom-geopolitical-engine-*.jar" | head -n 1)
        if [ -z "$JAR_FILE" ]; then
            echo "JAR файл не найден!"
            exit 1
        fi
        
        # Проверка существования тестового сервера
        if [ -d "../server" ]; then
            SERVER_DIR="../server"
        elif [ -d "server" ]; then
            SERVER_DIR="server"
        else
            echo "Тестовый сервер не найден!"
            exit 1
        fi
        
        cp "$JAR_FILE" "$SERVER_DIR/plugins/"
        echo "Плагин установлен в: $SERVER_DIR/plugins/"
        echo "Для запуска сервера выполните: cd $SERVER_DIR && java -jar mohist.jar"
        ;;
    *)
        echo "Использование: $0 [clean|build|test|install]"
        echo "  clean   - очистить проект"
        echo "  build   - собрать проект"
        echo "  test    - запустить тесты"
        echo "  install - собрать и установить на тестовый сервер"
        ;;
esac

echo "=== Завершено ==="