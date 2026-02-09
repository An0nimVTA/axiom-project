# Руководство разработчика

## Сборка Лаунчера

Лаунчер написан на Kotlin с использованием JavaFX и Launch4j для создания EXE.

1.  Перейдите в папку проекта:
    ```bash
    cd axiom-launcher-kotlin
    ```
2.  Запустите сборку:
    ```bash
    ./gradlew build
    ```
3.  Результат сборки (EXE и JAR) появится в `axiom-launcher-kotlin/build/launch4j/`.

## Сборка всего релиза (Portable)

Чтобы собрать готовую папку `AxiomClient` со всеми скриптами и зависимостями:

```bash
bash assemble_release.sh
```

Результат будет в `build_portable/AxiomClient/`.

## Структура Лаунчера

*   **Main.kt**: Точка входа. Инициализирует конфиг и запускает `AutoInstaller`.
*   **AutoInstaller.kt**: Отвечает за скачивание `client_core.zip` с VPS и распаковку.
*   **GameLauncher.kt**: Формирует команду запуска Java, собирает classpath и запускает Minecraft с аргументами `--server` (авто-вход).
*   **ConfigManager.kt**: Управляет `launcher_config.json`.

## Настройка Launch4j

Конфигурация `.exe` находится в `axiom-launcher-kotlin/build.gradle.kts`.
Параметр `bundledJrePath = "runtime"` указывает лаунчеру искать Java в папке `runtime` рядом с `.exe`.

## Автотесты

Единый раннер для всех проверок и сборов:

```bash
python3 tools/axiom_test_runner.py --profile full
```

Быстрый прогон без запуска клиента/сервера:

```bash
python3 tools/axiom_test_runner.py --profile fast
```

Отчеты сохраняются в `reports/autotest-<timestamp>/` (логи + JUnit XML).
