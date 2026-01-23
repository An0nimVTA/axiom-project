# 🚀 Загрузка на GitHub (Приватный репозиторий)

## Шаг 1: Инициализация Git

```bash
cd "/home/an0nimvta/axiom plugin"
./init-git.sh
```

Скрипт автоматически:
- Инициализирует Git репозиторий
- Добавит все файлы
- Создаст первый коммит

---

## Шаг 2: Создание репозитория на GitHub

1. Открыть: https://github.com/new
2. Заполнить:
   - **Repository name:** `axiom-server`
   - **Description:** `🎮 AXIOM - Геополитический сервер Minecraft 1.20.1 с 170+ сервисами, UI модом и автоматическим лаунчером`
   - **Visibility:** ✅ **Private** (ВАЖНО!)
3. **НЕ** добавлять README, .gitignore, license (уже есть)
4. Нажать **"Create repository"**

---

## Шаг 3: Подключение remote

```bash
# Замените YOUR_USERNAME на ваш GitHub username
git remote add origin https://github.com/YOUR_USERNAME/axiom-server.git

# Проверить
git remote -v
```

---

## Шаг 4: Загрузка кода

```bash
# Переименовать ветку в main
git branch -M main

# Запушить
git push -u origin main
```

При запросе логина:
- **Username:** ваш GitHub username
- **Password:** Personal Access Token (не пароль!)

### Создание Personal Access Token:
1. GitHub → Settings → Developer settings → Personal access tokens → Tokens (classic)
2. Generate new token (classic)
3. Название: `AXIOM Server`
4. Права: ✅ `repo` (полный доступ к приватным репозиториям)
5. Generate token
6. **Скопировать токен** (показывается один раз!)
7. Использовать вместо пароля

---

## Шаг 5: Настройка репозитория

### 5.1. Добавить описание
```
🎮 AXIOM - Геополитический сервер Minecraft 1.20.1

✨ Возможности:
• 170+ сервисов в плагине
• UI мод (115 KB, 17 систем)
• Автоматический лаунчер
• Дерево технологий (9 уровней)
• 33 мода (оружие, техника, автоматизация)
• Балансировка (все моды взаимосвязаны)
```

### 5.2. Добавить топики
```
minecraft
forge
geopolitics
server
plugin
mod
launcher
ui
technology-tree
```

### 5.3. Настроить About
- Website: (если есть)
- Topics: добавить топики выше

---

## Шаг 6: Создать Release

```bash
# Создать тег
git tag -a v2.0.0 -m "Release v2.0.0

Первый релиз AXIOM Server

Возможности:
- Плагин: 170+ сервисов
- UI Мод: 115 KB, 47 классов
- Лаунчер: Автоматическая установка
- Балансировка: Все моды взаимосвязаны
- Дерево технологий: 9 уровней
- 33 мода: TACZ, Point Blank, IE, AE2, и др.

Файлы:
- axiom-launcher-1.0.0.jar (55 MB)
- axiomui-0.1.0.jar (115 KB)
- AXIOM-1.0.0.jar (плагин)
"

# Запушить тег
git push origin v2.0.0
```

На GitHub:
1. Releases → Create a new release
2. Choose a tag: `v2.0.0`
3. Release title: `AXIOM Server v2.0.0`
4. Description: (скопировать из тега)
5. Прикрепить файлы:
   - `axiom-launcher-1.0.0.jar`
   - `axiomui-0.1.0.jar`
   - `AXIOM-1.0.0.jar`
6. ✅ Set as the latest release
7. Publish release

---

## Шаг 7: Защита репозитория

### 7.1. Настройки безопасности
Settings → Security:
- ✅ Private vulnerability reporting
- ✅ Dependency graph
- ✅ Dependabot alerts

### 7.2. Управление доступом
Settings → Collaborators:
- Добавить разработчиков
- Установить права доступа

### 7.3. Branch protection
Settings → Branches → Add rule:
- Branch name pattern: `main`
- ✅ Require pull request reviews before merging
- ✅ Require status checks to pass

---

## Шаг 8: Документация

### 8.1. Wiki (опционально)
1. Settings → Features → ✅ Wikis
2. Wiki → Create the first page
3. Добавить страницы:
   - Home
   - Installation Guide
   - Technology Tree
   - Commands Reference
   - FAQ

### 8.2. Projects (опционально)
1. Projects → New project
2. Template: Board
3. Название: `AXIOM Development`
4. Добавить колонки:
   - 📋 Backlog
   - 🔄 In Progress
   - ✅ Done

---

## ✅ Проверка

После загрузки проверьте:

```bash
# Клонировать в другую директорию
cd /tmp
git clone https://github.com/YOUR_USERNAME/axiom-server.git
cd axiom-server

# Проверить файлы
ls -la

# Проверить коммиты
git log --oneline

# Проверить теги
git tag
```

Должно быть:
- ✅ Все файлы на месте
- ✅ Коммит "Initial commit"
- ✅ Тег v2.0.0
- ✅ README_FULL.md читается
- ✅ .gitignore работает

---

## 🎉 Готово!

Репозиторий создан и защищён!

**URL:** https://github.com/YOUR_USERNAME/axiom-server

**Клонирование:**
```bash
git clone https://github.com/YOUR_USERNAME/axiom-server.git
```

**Обновление:**
```bash
git pull origin main
```

---

## 📝 Дальнейшая работа

### Добавление изменений
```bash
# Проверить статус
git status

# Добавить файлы
git add .

# Коммит
git commit -m "Описание изменений"

# Запушить
git push origin main
```

### Создание веток
```bash
# Создать ветку
git checkout -b feature/new-feature

# Работать в ветке
git add .
git commit -m "Add new feature"

# Запушить ветку
git push origin feature/new-feature

# На GitHub создать Pull Request
```

---

**Репозиторий готов!** 🚀
