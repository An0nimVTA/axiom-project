# AXIOM API для разработчиков

## Обзор

AXIOM предоставляет расширенный API для интеграции с другими плагинами и разработки дополнительных функций.

## Основные классы API

### AXIOM (главный класс)
```java
AXIOM plugin = AXIOM.getInstance();
```

Доступ к основным сервисам:
- `getNationManager()` - управление нациями
- `getEconomyService()` - экономическая система
- `getDiplomacySystem()` - дипломатия
- и другие 100+ методов доступа к сервисам

### NationManager
```java
NationManager nationManager = plugin.getNationManager();

// Получить нацию игрока
Nation nation = nationManager.getPlayerNation(player.getUniqueId());

// Создать нацию
boolean success = nationManager.createNation("MyNation", player.getUniqueId());

// Проверить территорию
boolean isClaimed = nationManager.isClaimed(world, chunkX, chunkZ);
```

### EconomyService
```java
EconomyService economy = plugin.getEconomyService();

// Получить баланс
double balance = economy.getBalance(player.getUniqueId());

// Перевод
boolean success = economy.transfer(senderId, receiverId, amount);

// Добавить/снять деньги
economy.addBalance(playerId, amount);
economy.removeBalance(playerId, amount);
```

## Events API

### Национальные события
- `NationCreateEvent` - создание нации
- `NationDisbandEvent` - удаление нации
- `TerritoryClaimEvent` - захват территории
- `TerritoryUnclaimEvent` - освобождение территории

### Экономические события
- `BalanceChangeEvent` - изменение баланса
- `TransferEvent` - перевод между игроками
- `BankTransactionEvent` - банковская операция

### Пример слушателя события
```java
@EventHandler
public void onNationCreate(NationCreateEvent event) {
    Nation nation = event.getNation();
    Player creator = event.getCreator();
    
    // Дополнительная логика
    Bukkit.broadcastMessage(creator.getName() + " создал нацию " + nation.getName());
}
```

## PlaceholderAPI интеграция

AXIOM предоставляет множество placeholders:
- `%axiom_nation_name%` - имя нации игрока
- `%axiom_nation_balance%` - баланс нации  
- `%axiom_player_balance%` - личный баланс
- `%axiom_nation_size%` - размер территории
- и другие

## Vault интеграция

Плагин полностью совместим с Vault:
- Экономическая система
- Система разрешений
- Таблица очков (Scoreboard API)

## Пример интеграции

### Maven зависимость
```xml
<dependency>
    <groupId>com.axiom</groupId>
    <artifactId>axiom-geopolitical-engine</artifactId>
    <version>1.0.0</version>
    <scope>provided</scope>
</dependency>
```

### Пример плагина, использующего AXIOM API
```java
public class MyPlugin extends JavaPlugin {
    private AXIOM axiom;
    
    @Override
    public void onEnable() {
        // Проверка наличия AXIOM
        if (!getServer().getPluginManager().isPluginEnabled("AXIOM")) {
            getLogger().severe("AXIOM не найден!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        axiom = AXIOM.getInstance();
        getLogger().info("Интеграция с AXIOM установлена");
    }
    
    public void giveBonusToNation(Player player) {
        Nation nation = axiom.getNationManager().getPlayerNation(player.getUniqueId());
        if (nation != null) {
            // Добавляем бонус нации
            axiom.getEconomyService().addBalance(nation.getLeaderId(), 100.0);
            player.sendMessage("Вашей нации начислен бонус!");
        }
    }
}
```

## Best Practices

### Проверка на null
Всегда проверяйте возвращаемые значения:
```java
Nation nation = axiom.getNationManager().getPlayerNation(player.getUniqueId());
if (nation != null) {
    // Работа с нацией
}
```

### Асинхронные операции
Для тяжелых операций используйте async:
```java
Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
    // Асинхронная работа с данными AXIOM
    double balance = axiom.getEconomyService().getBalance(playerId);
    // Возврат в основной поток для обновления UI
    Bukkit.getScheduler().runTask(this, () -> {
        player.sendMessage("Ваш баланс: " + balance);
    });
});
```

## Ошибки и исключения

AXIOM API может выбрасывать:
- `NationNotFoundException` - нация не найдена
- `EconomyException` - ошибка экономической системы
- `InsufficientFundsException` - недостаточно средств

## Поддержка и документация

- JavaDoc: включён в JAR файл
- Примеры: см. в `src/test/java/examples/`
- Поддержка: issue tracker проекта