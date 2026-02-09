#!/bin/bash
# Скрипт быстрого обновления файлов на VPS

VPS_HOST="root@193.23.201.6"
VPS_PATH="/var/www/axiom/updates"
VPS_PASS="artur907665A"

echo "🔄 AXIOM - Обновление файлов на VPS"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

# Функция загрузки файла
upload_file() {
    local file=$1
    local name=$2
    
    if [ ! -f "$file" ]; then
        echo "❌ Файл не найден: $file"
        return 1
    fi
    
    echo "📤 Загрузка $name..."
    sshpass -p "$VPS_PASS" scp -o StrictHostKeyChecking=no "$file" "$VPS_HOST:$VPS_PATH/$name"
    
    if [ $? -eq 0 ]; then
        echo "✅ $name загружен"
        return 0
    else
        echo "❌ Ошибка загрузки $name"
        return 1
    fi
}

# Меню
echo ""
echo "Выберите действие:"
echo "1) Обновить UI мод (axiomui-mod.jar)"
echo "2) Обновить все моды (mods.zip)"
echo "3) Обновить ядро сервера (server-core.jar)"
echo "4) Обновить всё"
echo "5) Проверить статус сервера"
echo "0) Выход"
echo ""
read -p "Ваш выбор: " choice

case $choice in
    1)
        # Найти последнюю версию мода
        MOD_FILE=$(ls -t builds/*/axiomui-mod.jar 2>/dev/null | head -1)
        if [ -z "$MOD_FILE" ]; then
            MOD_FILE="server/mods/axiomui-0.1.0.jar"
        fi
        upload_file "$MOD_FILE" "axiomui-mod.jar"
        ;;
    2)
        if [ ! -f "web_files/mods.zip" ]; then
            echo "📦 Создание архива модов..."
            cd server/mods
            zip -r ../../web_files/mods.zip . -x "axiomui-0.1.0.jar"
            cd ../..
        fi
        upload_file "web_files/mods.zip" "mods.zip"
        ;;
    3)
        if [ ! -f "web_files/server-core.jar" ]; then
            echo "📦 Копирование ядра сервера..."
            cp server/mohist.jar web_files/server-core.jar
        fi
        upload_file "web_files/server-core.jar" "server-core.jar"
        ;;
    4)
        echo "📦 Подготовка всех файлов..."
        
        # Мод
        MOD_FILE=$(ls -t builds/*/axiomui-mod.jar 2>/dev/null | head -1)
        if [ -z "$MOD_FILE" ]; then
            MOD_FILE="server/mods/axiomui-0.1.0.jar"
        fi
        
        # Моды
        if [ ! -f "web_files/mods.zip" ]; then
            cd server/mods
            zip -r ../../web_files/mods.zip . -x "axiomui-0.1.0.jar"
            cd ../..
        fi
        
        # Ядро
        if [ ! -f "web_files/server-core.jar" ]; then
            cp server/mohist.jar web_files/server-core.jar
        fi
        
        # Загрузка
        upload_file "$MOD_FILE" "axiomui-mod.jar"
        upload_file "web_files/mods.zip" "mods.zip"
        upload_file "web_files/server-core.jar" "server-core.jar"
        ;;
    5)
        echo "🔍 Проверка сервера..."
        echo ""
        echo "Статус nginx:"
        sshpass -p "$VPS_PASS" ssh "$VPS_HOST" 'systemctl status nginx | head -5'
        echo ""
        echo "Файлы на сервере:"
        sshpass -p "$VPS_PASS" ssh "$VPS_HOST" "ls -lh $VPS_PATH"
        echo ""
        echo "Доступность через HTTP:"
        curl -s http://193.23.201.6:8080/updates/ | grep -o 'href="[^"]*"'
        ;;
    0)
        echo "👋 Выход"
        exit 0
        ;;
    *)
        echo "❌ Неверный выбор"
        exit 1
        ;;
esac

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "✅ Готово!"
echo ""
echo "Проверить: http://193.23.201.6:8080/updates/"
