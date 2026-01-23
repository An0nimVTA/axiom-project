# 🚀 Команды для работы с AXIOM MULTIVERSE v2.0

## 📦 Установка зависимостей

### Лаунчер AXIOM MULTIVERSE v2.0 (Python)
```bash
cd launcher
python3 -m venv venv
source venv/bin/activate  # Linux/Mac: или . venv/bin/activate
pip install -r requirements.txt
```

### Backend API (Python/Flask)
```bash
cd backend
python3 -m venv venv
source venv/bin/activate  # Linux/Mac: или . venv/bin/activate
pip install -r requirements.txt
```

### Плагин (Maven)
```bash
# Установка зависимостей Maven (автоматически при сборке)
mvn clean install
```

---

## 🎮 Лаунчер AXIOM MULTIVERSE v2.0

### Запуск лаунчера
```bash
cd launcher
python3 -m venv venv
source venv/bin/activate
pip install -r requirements.txt
python3 main.py
```

### Сборка исполняемого файла
```bash
cd launcher
python3 build_exe.py
```

### Результат сборки
- **Windows**: `dist/AXIOM_MULTIVERSE.exe`
- **Linux/Mac**: `dist/AXIOM_MULTIVERSE`

---

## 🔧 Плагин AXIOM

### Сборка плагина
```bash
mvn clean package
```

### Результат
- JAR файл: `target/AXIOM-*.jar`
- Скопировать в: `server/plugins/`

### Установка плагина
```bash
# После сборки
cp target/AXIOM-*.jar server/plugins/
```

---

## 🖥️ Сервер Minecraft

### Запуск сервера
```bash
cd server
chmod +x start.sh
./start.sh
```

### Или напрямую через Java
```bash
cd server
java -Xmx4G -Xms2G -jar mohist.jar nogui
```

---

## 🗄️ База данных (PostgreSQL)

### Установка PostgreSQL (Linux)
```bash
sudo apt-get update
sudo apt-get install postgresql postgresql-contrib
```

### Создание базы данных
```bash
# Подключение к PostgreSQL
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

**ВАЖНО:** Если БД и пользователь уже существуют, но получаешь ошибку "permission denied", выполни команды напрямую:

```bash
# Самый простой способ - выполни эти команды по одной:
sudo -u postgres psql -d axiom_launcher -c "GRANT ALL ON SCHEMA public TO axiom_user;"
sudo -u postgres psql -d axiom_launcher -c "ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO axiom_user;"
sudo -u postgres psql -d axiom_launcher -c "ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO axiom_user;"
```

Или вручную в psql (если первый способ не работает):
```bash
sudo -u postgres psql
# Затем выполни по одной:
\c axiom_launcher
GRANT ALL ON SCHEMA public TO axiom_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO axiom_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO axiom_user;
\q
```

### Установка зависимостей Backend
```bash
cd backend
python3 -m venv venv
source venv/bin/activate
pip install -r requirements.txt
```

### Создание таблиц в БД
```bash
cd backend
python3 setup_db.py
```

**ВАЖНО:** Сначала установи зависимости (`pip install -r requirements.txt`), затем создай таблицы. Этот скрипт создаст все необходимые таблицы автоматически.

### Проверка подключения
```bash
psql -U axiom_user -d axiom_launcher -c "SELECT version();"
```

---

## 🔍 Проверка и тестирование

### Проверка Python зависимостей
```bash
cd launcher
source venv/bin/activate
pip list | grep -E "(customtkinter|requests|Pillow|pyinstaller)"
```

### Проверка Java версии
```bash
java -version
# Должна быть Java 17+
```

### Проверка Maven
```bash
mvn -version
```

### Проверка PostgreSQL
```bash
psql --version
sudo systemctl status postgresql
```

### Запуск Backend API
```bash
cd backend
source venv/bin/activate
python3 app.py
```

**Проверка API:**
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

## 🧹 Очистка

### Очистка Maven сборки
```bash
mvn clean
```

### Очистка Python кеша
```bash
cd launcher
find . -type d -name __pycache__ -exec rm -r {} +
find . -type f -name "*.pyc" -delete
```

### Очистка PyInstaller сборки
```bash
cd launcher
rm -rf build dist *.spec
```

---

## 📊 Логи и отладка

### Просмотр логов сервера
```bash
cd server/logs
tail -f latest.log
```

### Просмотр логов лаунчера
```bash
# Логи сохраняются в:
# Windows: %APPDATA%/AXIOM/logs/launcher.log
# Linux: ~/.axiom/logs/launcher.log
```

### Запуск лаунчера с отладкой
```bash
cd launcher
source venv/bin/activate
python3 -u main.py 2>&1 | tee debug.log
```

---

## 🔐 Конфигурация

### Настройка лаунчера AXIOM MULTIVERSE v2.0
```bash
# Файл конфигурации:
# Windows: %APPDATA%/AXIOM/config.json
# Linux: ~/.axiom/config.json

# Поддерживаемые настройки:
# - language: "ru" или "en"
# - auto_update: true или false
# - update_check_interval: интервал проверки обновлений (минуты)
```

### Настройка плагина
```bash
# После первого запуска сервера:
nano server/plugins/AXIOM/config.yml
```

### Настройка сервера
```bash
nano server/server.properties
```

---

## 🌐 Сеть

### Проверка IP сервера (Linux)
```bash
hostname -I
# или
ip addr show
```

### Открытие порта (Linux)
```bash
sudo ufw allow 25565/tcp
sudo ufw status
```

### Проверка порта
```bash
netstat -tulpn | grep 25565
# или
ss -tulpn | grep 25565
```

---

## 💾 Backup и восстановление

### Backup базы данных
```bash
pg_dump -U axiom_user axiom_launcher > backup_$(date +%Y%m%d).sql
```

### Восстановление базы данных
```bash
psql -U axiom_user axiom_launcher < backup_YYYYMMDD.sql
```

### Backup плагина
```bash
# Backup данных плагина
tar -czf axiom_backup_$(date +%Y%m%d).tar.gz server/plugins/AXIOM/
```

---

## 🚀 Быстрый старт (полный цикл AXIOM MULTIVERSE v2.0)

### 1. Установка Backend API
```bash
cd backend
python3 -m venv venv
source venv/bin/activate
pip install -r requirements.txt
python3 setup_db.py
python3 init_db.py  # Добавить тестовые данные
python3 app.py      # Запуск API (в отдельном терминале)
```

### 2. Установка и запуск лаунчера
```bash
cd ../launcher
python3 -m venv venv
source venv/bin/activate
pip install -r requirements.txt
python3 main.py     # Запуск лаунчера AXIOM MULTIVERSE v2.0
```

### 3. Сборка и запуск плагина AXIOM
```bash
# В новом терминале
cd ..
mvn clean package
cp target/AXIOM-*.jar server/plugins/
cd server
./start.sh
```

---

## 📝 Особенности AXIOM MULTIVERSE v2.0

- **Динамические темы**: 4 темы для разных типов серверов
- **Авторизация**: Только логин/пароль (без Discord OAuth)
- **Локализация**: Поддержка RU/EN с мгновенным переключением
- **Автообновление**: Проверка каждые 3-5 минут
- **Безопасность**: IP-адреса серверов скрыты, подключение через BungeeCord прокси
- **Хранение данных**: В %APPDATA%/AXIOM/ (Windows) или ~/.axiom/ (Linux/Mac)

---

## 📝 Примечания

- Все команды для Linux/Mac. На Windows используй PowerShell или WSL.
- Замени `axiom_user` и `your_password` на свои значения.
- Убедись, что порты 5000 (API), 25565 (Minecraft) и 5432 (PostgreSQL) открыты.
- Для продакшена используй `screen` или `tmux` для запуска сервера и API.
- Backend API должен быть запущен ПЕРЕД запуском лаунчера!

