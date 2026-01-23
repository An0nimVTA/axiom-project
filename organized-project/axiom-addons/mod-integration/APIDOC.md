# AXIOM Mod Integration API Documentation

## Overview
The AXIOM Mod Integration API provides a standardized interface for interacting with mod resources and systems within the AXIOM Geopolitical Engine ecosystem.

## Core Components

### ModIntegrationAPI
The main API class that provides access to all mod integration functionality:

```java
public class ModIntegrationAPI {
    // Mod detection and identification
    public boolean isModAvailable(String modId);
    public Set<String> getDetectedMods();
    public String detectModFromItem(ItemStack item);
    public String detectModFromBlock(Block block);
    
    // Category-based queries
    public boolean hasWarfareMods();
    public boolean hasIndustrialMods();
    public boolean hasLogisticsMods();
    public boolean hasEnergyMods();
    
    // Mod resource management
    public void recordModItemExtraction(String nationId, ItemStack item);
    public Map<String, Object> getResourceStatistics(String nationId);
    
    // Warfare integration
    public boolean isPlayerArmed(Player player);
    public double getModMilitaryBonus(Player player);
    
    // Energy integration
    public double getEnergyProduction(String nationId);
    public boolean isEnergyGenerator(Block block);
    
    // Advanced metrics
    public double calculateCompositePowerIndex(String nationId);
    public Map<String, Object> getCompositeStatistics(String nationId);
}
```

## Service Categories

### Economic Services
- **EconomicIndicatorsService**: Tracks mod-based resources and calculates economic indicators
- Integrates with modded resources, industrial systems, and marketplaces

### Diplomatic Services
- **DiplomaticIntegrationService**: Manages diplomatic relations enhanced by mod capabilities
- Includes treaty negotiations involving mod resources and capabilities

### Propaganda Services
- **PropagandaService**: Utilizes mod items and systems for influence operations
- Leverages mod-specific features and capabilities for messaging

### Migration Services
- **MigrationService**: Manages population movements including mod entities
- Handles migration policies with mod-based considerations

### Disaster Services
- **DisasterService**: Simulates disasters with mod-specific causes and effects
- Includes mod-related disaster prevention and recovery

### Cyber Security Services
- **CyberSecurityService**: Integrates with computer mods for digital security
- Supports ComputerCraft and OpenComputers for network protection

### Espionage Services
- **SpyService**: Implements spy operations with mod-enhanced capabilities
- Uses mod equipment for stealth and reconnaissance

### Funding Services
- **CrowdFundingService**: Enables funding campaigns using mod resources
- Supports resource-based and mod-item contributions

## Integration Points

The API provides several hooks for other systems to integrate with mod functionality:

1. **Resource Tracking**: Systems can register and query mod resources
2. **Event Handling**: Receive notifications about mod-related events
3. **Capability Queries**: Check for mod capabilities before executing actions
4. **Effect Application**: Apply mod-specific bonuses and penalties

## Best Practices

1. Always check if mods are available before using mod-specific features
2. Implement graceful degradation when mods aren't available
3. Use the API for all mod interactions rather than direct mod access
4. Respect mod compatibility requirements and limitations
5. Maintain consistent user experience regardless of mod presence

## Error Handling

The API includes comprehensive error handling for various failure scenarios:
- Missing mod dependencies
- Incompatible mod versions
- Resource unavailability
- Cross-mod conflicts

## Testing

The API includes test utilities for verifying mod integration functionality and compatibility.