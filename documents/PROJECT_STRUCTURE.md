# Структура Проекта AXIOM

```
axiom-plugin-full/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/axiom/
│   │   │       ├── command/                 # Команды плагина
│   │   │       │   ├── AxiomCommand.java   # Главная команда
│   │   │       │   ├── ClaimCommand.java   # Захват территории
│   │   │       │   ├── ModpackCommand.java # Управление модпаками
│   │   │       │   ├── RecipeIntegrationCommand.java # Интеграция рецептов
│   │   │       │   ├── ModBalanceCommand.java # Баланс модов
│   │   │       │   ├── ModIntegrationEnhancementCommand.java # Улучшенная интеграция
│   │   │       │   └── ModPackManagerCommand.java # Управление пакетами модов
│   │   │       │   └── DynamicModBalancerCommand.java # Динамический баланс
│   │   │       │   └── ModdedEconomicBalanceCommand.java # Экономический баланс модов
│   │   │       │
│   │   │       ├── gui/                     # Графические интерфейсы
│   │   │       │   ├── AdvancedTechnologyTreeMenu.java  # Продвинутое дерево технологий
│   │   │       │   ├── ModPackBuilderMenu.java # Строитель модпаков
│   │   │       │   └── NationMainMenu.java   # Главное меню нации
│   │   │       │
│   │   │       ├── listener/                # Слушатели событий
│   │   │       │   ├── TerritoryProtectionListener.java # Защита территории
│   │   │       │   ├── ModIntegrationListener.java # Интеграция модов
│   │   │       │   └── DiplomacySystemListener.java # Система дипломатии
│   │   │       │
│   │   │       ├── model/                   # Модельные классы
│   │   │       │   ├── Nation.java         # Модель нации
│   │   │       │   ├── Mod.java            # Модель мода
│   │   │       │   ├── City.java           # Город
│   │   │       │   └── TechNode.java       # Узел технологии
│   │   │       │
│   │   │       └── service/                # Сервисы
│   │   │           ├── NationManager.java     # Управление нациями
│   │   │           ├── EconomyService.java    # Экономическая система
│   │   │           ├── DiplomacySystem.java   # Дипломатическая система
│   │   │           ├── ModIntegrationService.java # Интеграция модов
│   │   │           ├── ModPackManagerService.java # Управление пакетами модов
│   │   │           ├── RecipeIntegrationService.java # Интеграция рецептов
│   │   │           ├── ModIntegrationEnhancementService.java # Улучшенная интеграция
│   │   │           ├── ModdedEconomicBalanceService.java # Экономический баланс модов
│   │   │           ├── ModBalancerService.java # Основной баланс модов
│   │   │           ├── ModEnergyService.java # Энергетические системы модов
│   │   │           └── ModResourceService.java # Ресурсы модов
│   │   │           └── ModWarfareService.java # Военные системы модов
│   │   │           └── DynamicModBalancerService.java # Динамический баланс
│   │   │
│   │   └── resources/
│   │       ├── plugin.yml          # Основной файл плагина
│   │       ├── config.yml          # Основная конфигурация
│   │       ├── modpacks.yml        # Конфигурация пакетов модов
│   │       ├── recipe-integration.yml # Интеграция рецептов
│   │       └── dynamic-balancing.yml # Динамический баланс
│   │
├── website/                        # Веб-интерфейс
│   ├── public/                     # Публичные файлы
│   │   └── index.html              # Основной HTML
│   ├── src/                        # Исходный код веб-приложения
│   │   ├── components/             # React компоненты
│   │   │   ├── Dashboard.js        # Панель управления
│   │   │   ├── ModIntegrationPanel.js # Панель интеграции модов
│   │   │   ├── NationsPanel.js     # Панель наций
│   │   │   ├── TerritoryMap.js     # Карта территорий
│   │   │   └── TechnologyTree.js   # Дерево технологий
│   │   ├── services/               # API сервисы
│   │   │   └── api.js              # API клиент
│   │   ├── styles/                 # Стили
│   │   │   └── main.scss           # Основные стили
│   │   └── index.js                # Точка входа
│   ├── server.js                   # Node.js сервер
│   ├── package.json                # Зависимости
│   └── webpack.config.js           # Конфигурация сборки
│
├── docs/                           # Документация
│   ├── INSTALLATION.md             # Руководство по установке
│   ├── CONFIGURATION.md            # Настройка
│   ├── API_DOCS.md                 # API документация
│   ├── COMMANDS_REFERENCE.md       # Справочник команд
│   └── ADMIN_GUIDE.md              # Руководство администратора
│
├── config-templates/               # Шаблоны конфигурации
│   ├── config.yml                  # Основной шаблон
│   ├── modpacks.yml                # Шаблон модпаков
│   ├── recipe-integration.yml      # Шаблон интеграции рецептов
│   └── dynamic-balancing.yml       # Шаблон динамического баланса
│
├── tools/                          # Утилиты
│   ├── build-modpack.java          # Утилита сборки модпаков
│   ├── export-nations.java         # Экспорт данных наций
│   └── backup-manager.java         # Система бэкапов
│
├── build/                          # Временные файлы сборки
├── target/                         # Скомпилированные артефакты
│
├── README.md                       # Основное описание
├── README_FULL_INSTALLATION.md     # Полное руководство установки
├── CHANGELOG.md                    # История изменений
├── LICENSE                         # Лицензия
├── pom.xml                         # Maven конфигурация
└── install-axiom.bat               # Скрипт установки для Windows
```

## Основные директории и файлы

### `src/main/java/com/axiom/service/` - Сервисы
- **ModPackManagerService.java** - Управление пакетами модов
- **RecipeIntegrationService.java** - Интеграция рецептов между модами
- **ModIntegrationEnhancementService.java** - Расширенная интеграция модов
- **ModdedEconomicBalanceService.java** - Баланс экономики модов
- **DynamicModBalancerService.java** - Динамический баланс модов
- **BalancingService.java** - Общий баланс

### `src/main/resources/` - Конфигурационные файлы
- **config.yml** - Основные настройки
- **modpacks.yml** - Определения пакетов модов
- **recipe-integration.yml** - Правила интеграции рецептов
- **dynamic-balancing.yml** - Параметры динамического баланса
- **plugin.yml** - Метаданные плагина

### `website/` - Веб-интерфейс
- **React SPA** с интерактивными панелями управления
- **WebSocket связи** для реального времени
- **API интеграции** с плагином
- **Интерактивные карты** для визуализации территорий

### `docs/` - Документация
- Подробные руководства по установке, настройке и администрированию
- API документация
- Справочники команд
- Примеры использования

## Интеграция с модами

### Поддерживаемые моды
- **TACZ** - Современное оружие
- **PointBlank** - Альтернативное оружие
- **Industrial Upgrade** - Индустрия
- **Immersive Engineering** - Промышленность
- **Applied Energistics 2** - Автоматизация
- **Ballistix** - Артиллерия
- и более 30 других модов

### Интеграция работает через:
1. **Модульное определение** - моды определяются по ID
2. **Крос-модовые рецепты** - использование материалов из разных модов
3. **Совместимость боеприпасов** - пули одного мода работают в оружии другого
4. **Объединение энергетических систем** - FE как стандартный юнит
5. **Совместимость компонентов** - взаимозаменяемые части
6. **Динамическое балансирование** - автоматическая регулировка

## Прогрессия 5-ступенчатой философии

1. **Каменный Век** → Survival, basic tools, primitive defense
2. **Промышленная Революция** → Energy, automation, basic machines
3. **Эпоха Накопления** → Storage, logistics, mass production
4. **Высокие Технологии** → Optimization, integration, efficiency
5. **Военно-Промышленный Комплекс** → Weapons, warfare, military-industrial

Каждый уровень открывает доступ к новым модам и возможностям, создавая естественную прогрессию.