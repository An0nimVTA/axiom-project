# AXIOM: автотесты и запуск (RU)

Коротко: этот документ описывает, как запускать автотесты (быстрые и полные), где смотреть отчеты, и что делать при типовых проблемах.

## 1) Быстрые проверки (локально, без игры)

Проверяет рецепты, UI мод, и unit-тесты лаунчера.

```bash
./tools/run_autotests.sh
```

Отдельно по частям:

```bash
python3 tools/validate_recipes.py balance_config/recipes.json
python3 tools/validate_ui.py
cd axiom-launcher-kotlin && ./gradlew --no-daemon test
./run-tests.sh
```

## 2) Полный прогон (сервер + клиент + автотесты)

Полный запуск собирает отчеты и прогоняет UI/командные проверки в игре.

```bash
python3 tools/axiom_test_runner.py --profile full
```

Если пересборка не нужна:

```bash
python3 tools/axiom_test_runner.py --profile full --skip-build
```

Только серверные тесты (без клиента):

```bash
python3 tools/axiom_test_runner.py --profile full --server-only --skip-build
```

## 3) Headless и Xvfb (когда нет GUI)

Лаунчер умеет headless режим. Для него полезно принудительно включить виртуальный дисплей:

```bash
AXIOM_USE_XVFB=1 python3 tools/axiom_test_runner.py --profile full --skip-build
```

Если нужно руками запускать headless клиента:

```bash
AXIOM_HEADLESS=1 ./build_portable/AxiomClient/start.sh --headless
```

## 4) Где смотреть логи и отчеты

- Отчеты прогона: `reports/autotest-YYYYMMDD-HHMMSS/`
  - `logs/` — логи шагов
  - `junit/` — JUnit-отчеты
  - `summary.txt` — краткое резюме
- Серверные отчеты тестбота/автотеста:
  - `server/plugins/AXIOM/test-reports/`

## 5) Важные настройки

### server.properties

Онлайн/офлайн режим сервера:

- `server/server.properties`:
  - `online-mode=true` — продакшен/реальный сервер
  - `online-mode=false` — удобно для автотестов без аккаунта

### launcher_config.json

Главное место для автотестовых команд и параметров:

- `autoUiTests` — включить автотесты UI
- `autoUiTestCommands` — список команд, выполняемых ботом
- `autoUiTestAutoStartDelayTicks` — задержка старта в тиках
- `autoUiTestStepDelayTicks` — задержка между шагами
- `autoUiTestCommandTimeoutTicks` — таймаут команд
- `autoStartServer` — автостарт сервера

## 6) Типовые проблемы и решения

### 6.1 Gradle падает с "Operation not permitted"

Это происходит в средах с ограничением сокетов. Решения:
- запускать тесты в полноценной системе без ограничений
- запускать через Codex с разрешением на сокеты

### 6.2 Сервер падает на старте (JNA/Netty)

В `server/start.sh` добавлены флаги:

- `-Djava.io.tmpdir=...` и `-Djna.tmpdir=...` — фикс JNA temp
- `-Dio.netty.transport.noNative=true` — отключение native netty

Это нужно для стабильного старта в ограниченных окружениях.

### 6.3 Клиент не может подключиться

Частые причины:
- `online-mode=true`, но клиент в offline/legacy (тогда не пустит)
- не прогружены моды или modpack
- клиент не получил OP (нет прав на /testbot)

### 6.4 Шумные ошибки от модов (MTSERROR, missing textures)

Это предупреждения/ошибки контента модов. Они шумные, но не всегда критичны. 
Если нужно чисто — править ресурсы модпака.

## 7) Пример рабочего полного прогона (коротко)

```bash
# 1) Переводим сервер в offline (для автотестов)
#    server/server.properties -> online-mode=false

# 2) Полный прогон
AXIOM_USE_XVFB=1 python3 tools/axiom_test_runner.py --profile full --skip-build

# 3) Смотрим отчет
ls -t reports | head -n 1
```

## 8) Примечания по безопасности

- Offline режим нужен только для автотестов.
- После завершения — обязательно вернуть `online-mode=true`.

