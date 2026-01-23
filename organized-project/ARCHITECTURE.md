# AXIOM Project Architecture Overview

## High-Level Architecture

The AXIOM project follows a modular, layered architecture designed for scalability and maintainability:

```
┌─────────────────────────────────────────┐
│            Client Interface Layer        │
├─────────────────────────────────────────┤
│           Service Integration Layer      │
├─────────────────────────────────────────┤
│           Business Logic Layer           │
├─────────────────────────────────────────┤
│            Data Access Layer             │
├─────────────────────────────────────────┤
│         External Mod Integration         │
└─────────────────────────────────────────┘
```

## Component Architecture

### Core System (axiom-core)
```
AXIOM Core
├── Model Layer (Data Structures)
│   ├── Nation.java
│   ├── PlayerData.java
│   ├── Cities.java
│   └── Territory.java
├── Service Layer (Business Logic)
│   ├── NationManager.java
│   ├── EconomyService.java
│   ├── DiplomacySystem.java
│   └── ResourceService.java
├── Command Layer
│   ├── AxiomCommand.java
│   ├── NationCommand.java
│   └── ...
├── Listener Layer
│   ├── TerritoryProtectionListener.java
│   ├── WarMobilizationListener.java
│   └── ...
└── GUI Layer
    ├── NationMainMenu.java
    ├── EconomyMenu.java
    └── ...
```

### Mod Integration Addon (axiom-addons/mod-integration)
```
Mod Integration Addon
├── API Layer
│   └── ModIntegrationAPI.java
├── Service Layer
│   ├── economic/
│   │   └── EconomicIndicatorsService.java
│   ├── diplomatic/
│   │   └── DiplomaticIntegrationService.java
│   ├── propaganda/
│   │   └── PropagandaService.java
│   ├── migration/
│   │   └── MigrationService.java
│   ├── disaster/
│   │   └── DisasterService.java
│   ├── cybersecurity/
│   │   └── CyberSecurityService.java
│   ├── espionage/
│   │   └── SpyService.java
│   └── funding/
│       └── CrowdFundingService.java
├── Model Layer
│   ├── MigrationModels.java
│   ├── DisasterModels.java
│   └── ...
└── Util Layer
    └── ModCompatibilityChecker.java
```

### Module Architecture (axiom-modules)
```
Modules (Optional Extensions)
├── AdvancedWarfareModule
├── EconomicsExpansionModule
├── DiplomaticProtocolModule
└── ...
```

## Integration Patterns

### Mod Integration Architecture
```
External Mod <-API-> Mod Integration API <-Bridge-> AXIOM Services
     ↓                    ↓                      ↓
Mod Resources ←→ Mod Resource Service ←→ Core Resource Service
     ↓                    ↓                      ↓
Mod Events ←→ Event Processing ←→ AXIOM Logic → Mod Effects
```

### Service Registration Pattern
```
ModIntegrationAPI
    ↓ (registers)
ModDetectionService → ModResourceTracker → EconomicIndicatorService
    ↓ (monitors)        ↓ (tracks)           ↓ (analyzes)
ModAvailability ←---- ModResourceFlow ←--- EconomicTrends
```

## Event Flow Architecture

1. **Mod Event Occurs**
2. **Mod Integration API Detects**
3. **Service Layer Processes**
4. **Core AXIOM Systems Respond**
5. **Feedback Sent to Mods** (when applicable)

## Configuration Hierarchy

```
Global Configuration (config.yml)
    ↓ Overrides
Module Configuration (module-specific)
    ↓ Overrides  
Addon Configuration (mod-integration/config.yml)
    ↓ Influences
Runtime Behavior
```

## Dependency Management

- **Core AXIOM** provides base services
- **Modules** extend core functionality
- **Addons** provide additional features and mod integration
- **External Mods** are accessed through API layer

## Scalability Considerations

- Service interfaces allow for easy extension
- Event-driven architecture minimizes coupling
- Configurable integration points
- Asynchronous processing for heavy operations
- Caching layers for repetitive computations

## Security Architecture

- Mod access validation
- Resource permission controls
- Economy transaction monitoring
- Anti-cheat integration points

This architecture ensures that the system remains maintainable while providing extensive mod integration capabilities.