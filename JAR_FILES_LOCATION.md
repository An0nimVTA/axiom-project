# 📦 Расположение JAR файлов для релиза

## Для загрузки в GitHub Release v2.0.0

### 1. AXIOM Plugin (Main)
```
Путь: axiom-plugin/target/axiom-plugin-2.1.0-SNAPSHOT.jar
Размер: 1.5 MB
Переименовать в: axiom-plugin.jar
```

**Описание:** Основной плагин с 170+ сервисами геополитической системы

### 2. AXIOM UI Mod
```
Путь: axiom-mod-integration/build/libs/axiomui-0.1.0.jar
Размер: 124 KB
Переименовать в: axiomui-mod.jar
```

**Описание:** Клиентский мод с 12 экранами UI (Education, Culture, Ecology, Espionage, Analytics, Tech Tree, Nation Map, Stats)

### 3. AXIOM Launcher
```
Путь: axiom-launcher-kotlin/build/libs/axiom-launcher-1.0.0.jar
Размер: 55 MB
Переименовать в: axiom-launcher.jar
```

**Описание:** Автоматический лаунчер с JavaFX (копирует 33 мода, запускает сервер и клиент)

---

## Дополнительные модули (опционально)

Если хотите загрузить модули отдельно:

### AXIOM Core Module
```
Путь: axiom-core/target/axiom-core-2.0.0.jar
Размер: 21 KB
```

### AXIOM Nations Module
```
Путь: axiom-nations/target/axiom-nations-2.0.0.jar
Размер: 12 KB
```

### AXIOM Economy Module
```
Путь: axiom-economy/target/axiom-economy-2.0.0.jar
Размер: 10 KB
```

### AXIOM Military Module
```
Путь: axiom-military/target/axiom-military-2.0.0.jar
Размер: 11 KB
```

### AXIOM Diplomacy Module
```
Путь: axiom-diplomacy/target/axiom-diplomacy-2.0.0.jar
Размер: 4.8 KB
```

### AXIOM Technology Module
```
Путь: axiom-technology/target/axiom-technology-2.0.0.jar
Размер: 6.6 KB
```

---

## Команды для копирования

### Создать директорию для релиза
```bash
mkdir -p ~/axiom-release-v2.0.0
```

### Скопировать основные файлы
```bash
cd "/home/an0nimvta/axiom plugin"

# Plugin
cp axiom-plugin/target/axiom-plugin-2.1.0-SNAPSHOT.jar ~/axiom-release-v2.0.0/axiom-plugin.jar

# UI Mod
cp axiom-mod-integration/build/libs/axiomui-0.1.0.jar ~/axiom-release-v2.0.0/axiomui-mod.jar

# Launcher
cp axiom-launcher-kotlin/build/libs/axiom-launcher-1.0.0.jar ~/axiom-release-v2.0.0/axiom-launcher.jar
```

### Проверить
```bash
ls -lh ~/axiom-release-v2.0.0/
```

Должно быть:
```
axiom-plugin.jar    1.5 MB
axiomui-mod.jar     124 KB
axiom-launcher.jar   55 MB
```

---

## Описание для GitHub Release

```markdown
# AXIOM v2.0.0 - Complete Geopolitical Server

## 🎮 Что включено

### 1. AXIOM Plugin (1.5 MB)
- 170+ сервисов геополитической системы
- Нации, города, территории, экономика
- Войны, дипломатия, технологии, религии
- Интеграция с модами оружия

### 2. AXIOM UI Mod (124 KB)
- 12 экранов с полным функционалом
- Дерево технологий, карта наций
- Образование, культура, экология
- Шпионаж, аналитика

### 3. AXIOM Launcher (55 MB)
- Автоматическая установка всех компонентов
- Копирование 33 модов (275 MB)
- Запуск сервера и клиента одной кнопкой

## 📖 Установка

1. Скачать все 3 файла
2. Запустить `axiom-launcher.jar`
3. Нажать "PLAY"

## 🔧 Требования

- Java 17+
- Minecraft 1.20.1
- Forge 47.4.4

## 📚 Документация

См. README.md в репозитории
```

---

**Готово к загрузке!** 🚀
