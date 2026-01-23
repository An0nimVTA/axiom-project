# 🚀 Быстрый запуск AXIOM MULTIVERSE v2.0

## 📋 Пошаговая инструкция

### ✅ Шаг 1: База данных PostgreSQL (уже создана)

Если БД и пользователь уже существуют, **ВАЖНО:** нужно дать права на схему `public`.

**Способ 1 (самый простой):** Используй автоматический скрипт:

```bash
cd backend
chmod +x grant_permissions.sh
./grant_permissions.sh
```

**Способ 2:** Выполни команды напрямую через psql:

```bash
sudo -u postgres psql -d axiom_launcher -c "GRANT ALL ON SCHEMA public TO axiom_user;"
sudo -u postgres psql -d axiom_launcher -c "ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO axiom_user;"
sudo -u postgres psql -d axiom_launcher -c "ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO axiom_user;"
```

**Способ 3:** Или выполни команды вручную в psql (по одной):

```bash
sudo -u postgres psql

# В консоли PostgreSQL:
# 1. Сначала подключись к БД (команда psql, выполни отдельно и нажми Enter):
\c axiom_launcher

# 2. После этого должна появиться строка "axiom_launcher=#" (не "postgres=#")
#    Теперь выполни SQL команды по одной:
GRANT ALL ON SCHEMA public TO axiom_user;

ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO axiom_user;

ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO axiom_user;

# 3. Выйди:
\q
```

**ВАЖНО:** Если видишь ошибку "invalid integer value", значит команда `\c` не выполнилась. Используй Способ 1!

Если БД и пользователь ещё не созданы:
```bash
sudo -u postgres psql

# В консоли PostgreSQL:
CREATE DATABASE axiom_launcher;
CREATE USER axiom_user WITH PASSWORD 'axiom_password';
GRANT ALL PRIVILEGES ON DATABASE axiom_launcher TO axiom_user;
\c axiom_launcher
GRANT ALL ON SCHEMA public TO axiom_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO axiom_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO axiom_user;
\q
```

---

### ✅ Шаг 2: Установка зависимостей Backend

**ВАЖНО:** Сначала установи зависимости!

```bash
cd backend

# Создать venv (если ещё не создан)
python3 -m venv venv

# Активировать venv (ОБЯЗАТЕЛЬНО!)
source venv/bin/activate

# Теперь установить зависимости
pip install -r requirements.txt
```

**⚠️ ВАЖНО:** После активации venv в начале строки должно появиться `(venv)`.
Если видишь `(venv)` - значит venv активирован правильно!

### ✅ Шаг 3: Создание таблиц в БД

**ВАЖНО:** Таблицы нужно создать обязательно!

```bash
# Если venv активирован, просто выполни:
python3 setup_db.py

# Если venv НЕ активирован, сначала активируй:
source venv/bin/activate
python3 setup_db.py
```

**⚠️ ВАЖНО:** Убедись что venv активирован (должно быть `(venv)` в начале строки)!

Это создаст все необходимые таблицы:
- `users` - пользователи
- `servers` - серверы
- `server_stats` - статистика серверов
- `news` - новости
- `modpacks` - модпаки
- `launcher_versions` - версии лаунчера

**Проверка:**
```bash
python3 check_db.py
```

Должно показать:
- ✅ Подключение успешно
- ✅ Таблица users существует
- ✅ Таблица servers существует
- и т.д.

---

### ✅ Шаг 4: Тестовые данные (ОБЯЗАТЕЛЬНО!)

**ВАЖНО:** Без тестовых данных лаунчер не будет показывать серверы и новости!

```bash
# Если venv активирован, просто выполни:
python3 init_db.py

# Если venv НЕ активирован, сначала активируй:
source venv/bin/activate
python3 init_db.py
```

**⚠️ ВАЖНО:** Убедись что venv активирован (должно быть `(venv)` в начале строки)!

Это создаст:
- Тестового пользователя: `test` / `test1234`
- 4 тестовых сервера (МОДЕРН, СРЕДНЕВЕКОВЬЕ, МАГИЯ, МИНИ-ИГРЫ)
- 3 тестовые новости

**Проверка данных:**
```bash
python3 check_data.py  # Проверить данные в БД
python3 test_api.py    # Проверить работу API
```

---

### ✅ Шаг 5: Запустить Backend API (ТЕРМИНАЛ 1)

```bash
# Если venv активирован, просто выполни:
python3 app.py

# Если venv НЕ активирован, сначала активируй:
source venv/bin/activate
python3 app.py
```

Или используй автоматический скрипт (он сам создаст и активирует venv):
```bash
chmod +x start.sh
./start.sh
```

**⚠️ ВАЖНО:** Убедись что venv активирован (должно быть `(venv)` в начале строки)!

**API должен быть доступен на:** `http://localhost:5000`

**Проверка:**
```bash
curl http://localhost:5000/health
```

Должен вернуть:
```json
{
  "status": "ok",
  "service": "AXIOM Launcher API",
  "version": "2.0.0"
}
```

---

### ✅ Шаг 6: Запустить лаунчер (ТЕРМИНАЛ 2)

```bash
cd launcher

# Создать venv (если ещё не создан)
python3 -m venv venv
source venv/bin/activate

# Установить зависимости (если ещё не установлены)
pip install -r requirements.txt

# Запустить лаунчер
python3 main.py
```

**⚠️ ВАЖНО:** Лаунчер использует динамические темы, поддержку RU/EN локализации, регистрацию по логину/паролю и автоматическое обновление каждые 3-5 минут!

---

### ✅ Шаг 7: Запустить веб-сайт (ТЕРМИНАЛ 3, опционально)

```bash
cd website
python3 -m http.server 8000
```

**Сайт будет доступен на:** `http://localhost:8000`

---

## 🐛 Решение проблем

### Ошибка: "ModuleNotFoundError: No module named 'psycopg2'"
**Решение:** Установи зависимости backend:
```bash
cd backend
pip3 install -r requirements.txt
```

### Ошибка: "permission denied for schema public"
**Решение:** Пользователь не имеет прав на схему. 

**Самый простой способ - используй скрипт:**
```bash
cd backend
chmod +x grant_permissions.sh
./grant_permissions.sh
```

**Или выполни команды вручную через psql:**
```bash
sudo -u postgres psql -d axiom_launcher -c "GRANT ALL ON SCHEMA public TO axiom_user;"
sudo -u postgres psql -d axiom_launcher -c "ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO axiom_user;"
sudo -u postgres psql -d axiom_launcher -c "ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO axiom_user;"
```

**Или вручную в интерактивном psql (если другие способы не работают):**
```bash
sudo -u postgres psql
# Затем выполни по одной:
\c axiom_launcher
GRANT ALL ON SCHEMA public TO axiom_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO axiom_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO axiom_user;
\q
```

Затем снова выполни:
```bash
cd backend
python3 setup_db.py
```

### Ошибка: "relation users does not exist"
**Решение:** Таблицы не созданы. Выполни:
```bash
cd backend
pip3 install -r requirements.txt  # Если ещё не установлены
python3 setup_db.py
```

### Проблема: "Серверы и новости не отображаются в лаунчере"
**Решение:** Выполни проверки по порядку:

1. **Проверь, что данные есть в БД:**
```bash
cd backend
source venv/bin/activate  # Если используешь venv
python3 check_data.py
```

2. **Проверь, что API возвращает данные:**
```bash
python3 test_api.py
```

3. **Если данных нет, загрузи тестовые данные:**
```bash
python3 init_db.py
```

4. **Убедись, что Backend API запущен и перезапусти его:**
```bash
# Останови текущий процесс (Ctrl+C)
python3 app.py
```

5. **Перезапусти лаунчер:**
```bash
cd ../launcher
source venv/bin/activate  # Если используешь venv
python3 main.py
```

### Ошибка: "Connection refused" на localhost:5000
**Решение:** Backend API не запущен. Запусти его:
```bash
cd backend
source venv/bin/activate  # Если используешь venv
python3 app.py
```

### Ошибка: "Database connection error"
**Решение:** Проверь что PostgreSQL запущен:
```bash
sudo systemctl status postgresql
sudo systemctl start postgresql
```

### Ошибка: "customtkinter module not found"
**Решение:** Установи зависимости в venv:
```bash
cd launcher
source venv/bin/activate
pip install -r requirements.txt
```

### Ошибка: "tkinter module not found"
**Решение:** Установи системный пакет:
```bash
sudo apt-get install python3-tk
```

---

## 📝 Тестовые данные

После запуска `init_db.py`:
- **Пользователь**: `test` / `test1234`
- **4 сервера**: МОДЕРН, СРЕДНЕВЕКОВЬЕ, МАГИЯ, МИНИ-ИГРЫ
- **3 новости**: С тегами [ОБНОВА], [НОВЫЙ СЕРВЕР]

---

## 🎯 Готово!

Теперь у тебя работает:
- ✅ Backend API на `http://localhost:5000`
- ✅ Лаунчер AXIOM MULTIVERSE v2.0 с динамическими темами
- ✅ Поддержка RU/EN локализации
- ✅ Регистрация по логину/паролю (без Discord OAuth)
- ✅ Автоматическое обновление лаунчера
- ✅ Веб-сайт на `http://localhost:8000` (если запущен)

**Важно:** Backend API должен быть запущен **ПЕРЕД** запуском лаунчера!

---

## 📊 Структура команд

```
1. Установка зависимостей: cd backend && pip3 install -r requirements.txt
2. БД и таблицы:           backend/setup_db.py
3. Тестовые данные:        backend/init_db.py
4. Запуск API:             backend/app.py
5. Запуск лаунчера:        launcher/ (python3 main.py)
6. Запуск сайта:           website/ (python3 -m http.server 8000)
```
