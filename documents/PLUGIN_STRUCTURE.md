# Структура проекта AXIOM

## Общая архитектура

```
axiom-plugin/
├── pom.xml                          # Maven конфигурация
├── plugin.yml                       # Bukkit плагин конфигурация
├── README.md                        # Основное описание
├── AXIOM_SUPERGUIDE.md              # Полное руководство
├── IDEAS_LIST.md                    # Список идей
├── FUTURE_IDEAS.md                  # Будущие улучшения
├── SERVER_CONCEPT.md                # Концепция сервера
├── API_GUIDE.md                     # API документация
├── COMMANDS_REFERENCE.md            # Справочник команд
├── TESTING_GUIDE.md                 # Руководство по тестированию
├── LAUNCHER_SETUP.md                # Настройка лаунчера
├── PLUGIN_STRUCTURE.md              # Структура плагина
├── QUICK_START.md                   # Быстрый старт
├── COMMANDS.md                      # Команды
├── run_tests.sh                     # Скрипт тестирования
├── build_and_test.sh                # Скрипт сборки и тестирования
├── src/
│   └── main/
│       └── java/
│           └── com/
│               └── axiom/
│                   ├── AXIOM.java                 # Главный класс плагина
│                   ├── command/                   # Команды плагина
│                   │   ├── AxiomCommand.java      # Главная команда
│                   │   ├── ClaimCommand.java      # Команда захвата
│                   │   ├── CreateNationCommand.java # Создание нации
│                   │   ├── UnclaimCommand.java    # Освобождение территории
│                   │   ├── TechnologyCommand.java # Команды технологий
│                   │   ├── ModPackCommand.java    # Команды модпаков
│                   │   ├── EconomicIndicatorsCommand.java # Команды экономических индикаторов
│                   │   ├── TestBotCommand.java    # Команда тестового бота
│                   │   └── ...
│                   ├── gui/                       # GUI интерфейсы
│                   │   ├── CardBasedMenu.java     # Карточная система GUI
│                   │   ├── NationMainMenu.java    # Главное меню нации
│                   │   ├── EconomicIndicatorsMenu.java # Меню экономических показателей
│                   │   ├── TechnologyMenu.java    # Меню технологий
│                   │   ├── ModPackBuilderMenu.java # Меню конструктора модпаков
│                   │   ├── MapBoundaryVisualizationMenu.java # Меню визуализации границ
│                   │   ├── AdvancedTechnologyTreeMenu.java # Расширенное меню технологий
│                   │   └── ...
│                   ├── listener/                  # Слушатели событий
│                   │   ├── ...
│                   │   └── ...
│                   ├── model/                     # Модели данных
│                   │   ├── Nation.java           # Модель нации
│                   │   ├── City.java             # Модель города
│                   │   ├── PlayerProfile.java     # Модель игрока
│                   │   ├── TechNode.java         # Модель технологии
│                   │   └── ...
│                   ├── service/                   # Бизнес-логика
│                   │   ├── EconomyService.java   # Экономическая система
│                   │   ├── NationManager.java    # Управление нациями
│                   │   ├── DiplomacySystem.java  # Дипломатическая система
│                   │   ├── ReligionManager.java  # Религиозная система
│                   │   ├── CityGrowthEngine.java  # Рост городов
│                   │   ├── TechnologyTreeService.java # Система технологий
│                   │   ├── ModPackBuilderService.java # Система модпаков
│                   │   ├── EconomicIndicatorsService.java # Служба экономических индикаторов
│                   │   ├── MapBoundaryVisualizationService.java # Визуализация границ
│                   │   └── 165+ других сервисов...
│                   └── test/                     # Автотесты
│                       ├── SuperTestBot.java     # Главный тестовый бот
│                       └── EconomicIndicatorsTestSuite.java # Тесты экономических индикаторов
├── target/                          # Скомпилированные артефакты
│   └── axiom-geopolitical-engine-1.0.0.jar
└── resources/
    ├── config.yml                   # Основная конфигурация
    └── lang/                        # Файлы локализации
        ├── ru.json
        └── en.json
```

## Архитектурные принципы

### 1. Карточная система GUI
- Все меню построены на карточной системе
- Использует динамические цветовые схемы
- Поддерживает темы для разных типов серверов
- Интерактивные элементы с визуальными эффектами

### 2. Модульность
- Каждый сервис изолирован
- Поддержка плагинов и расширений
- Легко добавлять новые функции

### 3. Интеграции
- Система технологий связывает функции модов
- Моды включаются/выключаются через технологические деревья
- Поддержка внешних API и плагинов

### 4. Производительность
- Асинхронные операции
- Кэширование данных
- Оптимизированное хранение
- Многопоточность

## Системы плагина

### Основные системы (уровень 1)
- Национальная система (NationManager)
- Экономическая система (EconomyService)
- Дипломатическая система (DiplomacySystem)
- Военная система (WarSystem)

### Промежуточные системы (уровень 2)
- Религиозная система (ReligionManager)
- Городская система (CityGrowthEngine)
- Образовательная система (EducationService)
- Система технологий (TechnologyTreeService)

### Специализированные системы (уровень 3)
- Система модов (ModPackBuilderService)
- Система границ (MapBoundaryVisualizationService)
- Система экономических индикаторов (EconomicIndicatorsService)
- Система тестирования (SuperTestBot)

## Интеграции

### Внутренние
- Сервисы обмениваются данными через систему событий
- Согласованная система аутентификации
- Централизованное хранение данных

### Внешние
- PlaceholderAPI
- Vault
- WorldEdit
- Xaeros Maps
- Discord (через webhook)

## Структура данных

### Основные сущности
- Nation (нация)
- City (город)
- Player (игрок)
- Technology (технология)
- ModPack (модпак)
- EconomicData (экономические данные)

### Отношения
- Игрок принадлежит нации
- Нация контролирует территории
- Нация имеет города
- Технологии разблокируют моды
- Моды влияют на экономику