# 🚀 Быстрая загрузка на GitHub

## Шаг 1: Создать репозиторий (1 минута)

1. Открыть: https://github.com/new
2. Заполнить:
   - **Repository name:** `axiom-server`
   - **Visibility:** ✅ **Private**
3. Нажать **Create repository**

## Шаг 2: Загрузить код (2 минуты)

```bash
cd "/home/an0nimvta/axiom plugin"

# Замените USERNAME на ваш GitHub username
git remote add origin https://github.com/USERNAME/axiom-server.git

# Загрузить
git push -u origin main
```

## Шаг 3: Создать релиз (5 минут)

1. Перейти в репозиторий на GitHub
2. **Releases** → **Create a new release**
3. Заполнить:
   - **Tag:** `v2.0.0`
   - **Title:** `AXIOM v2.0.0 - Complete Geopolitical Server`
4. Прикрепить файлы:
   - `axiom-plugin/target/axiom-plugin-2.1.0-SNAPSHOT.jar` → переименовать в `axiom-plugin.jar`
   - `axiom-mod-integration/build/libs/axiomui-0.1.0.jar` → переименовать в `axiomui-mod.jar`
   - `axiom-launcher-kotlin/build/libs/axiom-launcher-1.0.0.jar` → переименовать в `axiom-launcher.jar`
5. **Publish release**

## ✅ Готово!

Репозиторий готов к использованию:
- Код: `https://github.com/USERNAME/axiom-server`
- Релиз: `https://github.com/USERNAME/axiom-server/releases/tag/v2.0.0`

---

**Полная инструкция:** UPLOAD_TO_GITHUB.md
