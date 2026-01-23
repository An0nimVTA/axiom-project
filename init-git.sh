#!/bin/bash

echo "=== AXIOM - Инициализация Git репозитория ==="
echo ""

# Проверка Git
if ! command -v git &> /dev/null; then
    echo "❌ Git не установлен!"
    echo "Установите: sudo apt install git"
    exit 1
fi

echo "✅ Git найден"
echo ""

# Инициализация репозитория
if [ ! -d ".git" ]; then
    echo "📦 Инициализация Git..."
    git init
    echo "✅ Репозиторий инициализирован"
else
    echo "✅ Репозиторий уже существует"
fi

echo ""

# Добавление файлов
echo "📝 Добавление файлов..."
git add .gitignore
git add README_FULL.md
git add axiom-plugin/
git add axiom-mod-integration/
git add axiom-launcher-kotlin/
git add balance_config/
git add docs/

echo "✅ Файлы добавлены"
echo ""

# Первый коммит
echo "💾 Создание первого коммита..."
git commit -m "Initial commit: AXIOM Server v2.0.0

- Плагин: 170+ сервисов
- UI Мод: 115 KB, 47 классов, 17 систем
- Лаунчер: Автоматическая установка
- Балансировка: Все моды взаимосвязаны
- Дерево технологий: 9 уровней прогрессии
- 33 мода: оружие, техника, автоматизация"

echo "✅ Коммит создан"
echo ""

# Инструкции для GitHub
echo "=== Следующие шаги ==="
echo ""
echo "1. Создайте приватный репозиторий на GitHub:"
echo "   https://github.com/new"
echo "   Название: axiom-server"
echo "   Видимость: Private ✅"
echo ""
echo "2. Добавьте remote:"
echo "   git remote add origin https://github.com/YOUR_USERNAME/axiom-server.git"
echo ""
echo "3. Запушьте код:"
echo "   git branch -M main"
echo "   git push -u origin main"
echo ""
echo "4. Добавьте описание репозитория:"
echo "   🎮 AXIOM - Геополитический сервер Minecraft 1.20.1"
echo ""
echo "5. Добавьте топики:"
echo "   minecraft, forge, geopolitics, server, plugin, mod"
echo ""
echo "✅ Готово!"
