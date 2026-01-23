# Примеры использования новых компонентов

## Введение

Этот документ содержит примеры использования новых интерфейсов и репозиториев.

## 1. Использование MilitaryService через новый интерфейс

### Старый способ (прямое использование):
```java
MilitaryService service = new MilitaryService(plugin);
double strength = service.getMilitaryStrength(nationId);
```

### Новый способ (через интерфейс):
```java
// Через адаптер (совместимость со старым кодом)
MilitaryServiceInterface service = new MilitaryServiceAdapter(plugin);
double strength = service.getMilitaryStrength(nationId);
```

### Будущий способ (новая реализация):
```java
// Создание репозиториев
NationRepository nationRepo = new JsonNationRepository(plugin);
MilitaryRepository militaryRepo = new JsonMilitaryRepository(plugin);

// Создание нового сервиса
MilitaryServiceInterface service = new NewMilitaryServiceExample(nationRepo, militaryRepo);

// Использование
String result = service.recruitUnits(nationId, "infantry", 100, 50.0);
double strength = service.getMilitaryStrength(nationId);
```

## 2. Использование репозиториев

### Работа с NationRepository:
```java
// Создание репозитория
NationRepository nationRepo = new JsonNationRepository(plugin);

// Поиск нации
Optional<Nation> nationOpt = nationRepo.findById("nation123");
if (nationOpt.isPresent()) {
    Nation nation = nationOpt.get();
    System.out.println("Found nation: " + nation.getName());
}

// Сохранение нации
Nation newNation = new Nation("nation123", "Great Empire");
nationRepo.save(newNation);

// Поиск всех наций
List<Nation> allNations = nationRepo.findAll();

// Поиск по имени
Optional<Nation> byName = nationRepo.findByName("Great Empire");
```

### Работа с MilitaryRepository:
```java
// Создание репозитория
MilitaryRepository militaryRepo = new JsonMilitaryRepository(plugin);

// Поиск военных данных
Optional<MilitaryData> dataOpt = militaryRepo.findByNationId("nation123");
if (dataOpt.isPresent()) {
    MilitaryData data = dataOpt.get();
    System.out.println("Infantry: " + data.getInfantry());
}

// Создание и сохранение новых данных
MilitaryData newData = new MilitaryData("nation123");
newData.setInfantry(1000);
newData.setCavalry(500);
militaryRepo.save(newData);

// Поиск всех военных данных
List<MilitaryData> allData = militaryRepo.findAll();
```

## 3. Использование адаптеров

### MilitaryServiceAdapter:
```java
// Создание адаптера
MilitaryServiceAdapter adapter = new MilitaryServiceAdapter(plugin);

// Использование через интерфейс
MilitaryServiceInterface service = adapter;
double strength = service.getMilitaryStrength(nationId);

// Доступ к оригинальному сервису при необходимости
MilitaryService legacyService = adapter.getLegacyService();
```

### SiegeServiceAdapter:
```java
// Создание адаптера
SiegeServiceAdapter adapter = new SiegeServiceAdapter(plugin);

// Использование через интерфейс
SiegeServiceInterface service = adapter;
String result = service.startSiege("city1", "attacker1", "defender1");

// Проверка состояния
boolean isAtWar = service.isNationAtWar("attacker1");
```

## 4. Пример интеграции с существующим кодом

### Постепенная замена в основном классе:
```java
public class AXIOM extends JavaPlugin {
    private MilitaryServiceInterface militaryService;
    
    @Override
    public void onEnable() {
        // Старый способ (пока что)
        // militaryService = new MilitaryService(this);
        
        // Новый способ через адаптер
        militaryService = new MilitaryServiceAdapter(this);
        
        // В будущем:
        // NationRepository nationRepo = new JsonNationRepository(this);
        // MilitaryRepository militaryRepo = new JsonMilitaryRepository(this);
        // militaryService = new NewMilitaryServiceExample(nationRepo, militaryRepo);
    }
    
    public MilitaryServiceInterface getMilitaryService() {
        return militaryService;
    }
}
```

## 5. Пример тестирования

### Тестирование репозитория:
```java
@Test
public void testNationRepository() {
    // Создание тестового плагина (мок)
    AXIOM plugin = mock(AXIOM.class);
    when(plugin.getDataFolder()).thenReturn(new File("test/data"));
    
    // Создание репозитория
    NationRepository repo = new JsonNationRepository(plugin);
    
    // Тестирование сохранения и загрузки
    Nation testNation = new Nation("test1", "Test Nation");
    repo.save(testNation);
    
    Optional<Nation> loaded = repo.findById("test1");
    assertTrue(loaded.isPresent());
    assertEquals("Test Nation", loaded.get().getName());
    
    // Очистка
    repo.delete("test1");
}
```

### Тестирование сервиса:
```java
@Test
public void testMilitaryService() {
    // Создание мок-репозиториев
    NationRepository nationRepo = mock(NationRepository.class);
    MilitaryRepository militaryRepo = mock(MilitaryRepository.class);
    
    // Создание сервиса
    MilitaryServiceInterface service = new NewMilitaryServiceExample(nationRepo, militaryRepo);
    
    // Настройка моков
    Nation testNation = new Nation("test1", "Test Nation");
    testNation.setTreasury(10000.0);
    when(nationRepo.findById("test1")).thenReturn(Optional.of(testNation));
    
    // Тестирование
    String result = service.recruitUnits("test1", "infantry", 100, 50.0);
    assertNotNull(result);
    assertTrue(result.contains("Recruited"));
    
    // Проверка вызовов
    verify(nationRepo).save(any(Nation.class));
    verify(militaryRepo).save(any(MilitaryData.class));
}
```

## Заключение

Эти примеры показывают, как использовать новые компоненты и как постепенно мигрировать с старой архитектуры на новую. Ключевые преимущества:

1. **Совместимость**: Адаптеры позволяют использовать новый интерфейс с существующей реализацией
2. **Тестируемость**: Новые компоненты легко тестировать с помощью моков
3. **Гибкость**: Можно постепенно заменять компоненты без поломки существующего функционала
4. **Чистота**: Новая архитектура более организована и следует лучшим практикам