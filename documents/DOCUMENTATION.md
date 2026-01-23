# AXIOM Geopolitical Engine Documentation

## Table of Contents
1. [Overview](#overview)
2. [Installation & Setup](#installation--setup)
3. [Core Systems](#core-systems)
4. [Mod Integration](#mod-integration)
5. [Commands Reference](#commands-reference)
6. [Configuration](#configuration)
7. [Development](#development)
8. [Troubleshooting](#troubleshooting)
9. [Website Development](#website-development)

## Overview

AXIOM is a comprehensive geopolitical and economic engine for Minecraft servers with over 165 interconnected systems designed to transform a regular Minecraft server into a deep geopolitical simulator. The plugin integrates territorial control, economics, diplomacy, military systems, and mod support to create a complex political environment.

### Key Features
- **Nation System**: Create and manage nations with complex governmental structures
- **Economic Engine**: Realistic market dynamics with taxation, trade, and resource management
- **Diplomacy System**: Treaties, alliances, embargoes, and diplomatic relations
- **Military Systems**: Wars, sieges, conquest, and military logistics
- **Mod Integration**: Deep support for over 30+ modpacks (TACZ, PointBlank, Industrial Upgrade, etc.)
- **Technology Tree**: 5-stage progression (Stone Age → Industrial → Accumulation → High Tech → Military-Industrial Complex)
- **Political Systems**: Elections, governance, and political parties

### Philosophy
AXIOM follows a 5-stage technological progression philosophy:
1. **Stone Age**: Basic survival, simple tools
2. **Industrial Revolution**: Energy, automation, basic industrialization
3. **Accumulation Era**: Storage, logistics, infrastructure, mass production
4. **High Tech**: Optimization, miniaturization, efficiency
5. **Military-Industrial Complex**: Weapons, warfare, defense industry

## Installation & Setup

### Prerequisites
- Minecraft 1.20.1 server
- Minecraft Forge/Mohist 1.20.1 server (for mod support)
- Java 17+
- At least 8GB RAM (16GB+ recommended for modpacks)

### Server Setup
1. Download AXIOM plugin JAR file
2. Install on your Minecraft server plugins folder
3. If using mods, ensure all required mods are installed on server/clients
4. Restart the server
5. Configure `config.yml` as needed
6. Start the server again

### Required Dependencies
- WorldEdit (for territory claims)
- (Optional) PlaceholderAPI for placeholder integration
- (Optional) Vault for economy integration

### Initial Configuration
1. Set up nation creation permissions
2. Configure economic parameters
3. Set up territory protection settings
4. Configure mod integration settings if using mods

## Core Systems

### Nation System
The central component of AXIOM. Nations control territories, manage citizens, and participate in geopolitics.

#### Key Functions:
- **Territory Claims**: Nations can claim chunks for protection and resource extraction
- **Citizen Management**: Add/remove citizens, assign roles
- **Treasury Management**: Economic resource management
- **Government Roles**: Leader, Minister, General, Governor, Citizen

#### Roles and Permissions:
- **LEADER**: Full control over nation
- **MINISTER**: Administrative functions
- **GENERAL**: Military operations
- **GOVERNOR**: Territory management
- **CITIZEN**: Voting and basic participation

### Economic System
Comprehensive economic system with resource management and trade.

#### Components:
- **Currency System**: Each nation can have its own currency
- **Taxation**: Income tax, sales tax, export/import duties
- **Trading**: Nation-to-nation trade agreements
- **Banking**: Loans, investments, financial services
- **Stock Market**: Corporation creation and trading

### Diplomacy System
Complex system for international relations.

#### Components:
- **Treaties**: Trade agreements, non-aggression pacts, peace treaties
- **Alliances**: Military and economic partnerships
- **Embargos**: Economic sanctions and trade restrictions
- **Diplomatic Recognition**: International legitimacy system
- **Summits**: International conferences and negotiations

### Military Systems
Advanced warfare mechanics.

#### Components:
- **War Declarations**: Formal declarations with costs and consequences
- **Siege Mechanics**: Territory conquest and defense
- **Mobilization**: Military organization and command
- **Weapons**: Integration with TACZ, PointBlank, and other militarization mods
- **Conscription**: Draft and military service

### Technology Tree
5-stage technology progression system that encourages the use of all available mods.

## Mod Integration

### Supported Mods
AXIOM provides deep integration for over 30+ military and industrial mods:

#### Warfare Mods
- **TACZ**: Modern firearms system
- **PointBlank**: Alternative firearms system  
- **Ballistix**: Heavy artillery and explosives
- **Superb Warfare**: Military vehicles and equipment
- **Warium**: Advanced weaponry and equipment
- **CAPS AWIMS**: Tactical equipment and armor

#### Industrial Mods
- **Industrial Upgrade**: Advanced industrial machines
- **Immersive Engineering**: Multiblock industrial structures
- **Applied Energistics 2**: Networked storage and automation
- **Mekanism**: Resource processing and energy
- **Thermal**: Resource processing and energy
- **Simply Quarries**: Automated mining systems
- **Quantum Generators**: Advanced energy production

#### Utility Mods
- **Xaero's Minimap/World Map**: Enhanced navigation
- **Voice Chat**: In-game voice communication
- **Embeddium**: Client-side optimization
- **Entity Culling**: Performance optimization
- **FerriteCore**: Memory optimization

### Modpack Integration
The system supports pre-configured modpacks:
- **Warfare Expansion**: Military-focused modpack
- **Industrial Revolution**: Industrial and logistics-focused
- **Technology Revolution**: High-tech and automation-focused
- **Balanced Experience**: Well-rounded combination
- **Hardcore Military**: Maximum military content

### Cross-Mod Compatibility
- Shared resource pools between compatible mods
- Interchangeable ammunition systems
- Energy network compatibility
- Recipe integration and substitution
- Economic value synchronization

## Commands Reference

### Base Commands
- `/axiom` or `/ax` - Main AXIOM menu
- `/nation` or `/n` - Nation management (aliases: `/country`)
- `/claim` - Claim territory for your nation
- `/unclaim` - Unclaim territory from your nation
- `/testbot` or `/tb` - Automated testing system
- `/technology` or `/tech` - Technology tree menu
- `/modpack` or `/mp` - Mod pack management
- `/capture` or `/siege` - Territory conquest system

### Axiom Command Subcommands
- `/axiom nation create <name>` - Create a new nation
- `/axiom claim` - Claim current chunk
- `/axiom unclaim` - Unclaim current chunk
- `/axiom economy print <amount>` - Print money for your nation
- `/axiom diplomacy` - Open diplomacy menu
- `/axiom war` - Warfare and conflict management
- `/axiom tech` - Technology tree access
- `/axiom stats` - Nation statistics
- `/axiom mapvis` - Map boundary visualization
- `/axiom religion` - Religion management
- `/axiom city` - City management
- `/axiom resource` - Resource management
- `/axiom trade` - Trade and market access

### Mod Integration Commands
- `/modbal [options]` - Mod balancing and integration management
- `/recipeint [options]` - Recipe integration management
- `/modenhance [options]` - Mod integration enhancement
- `/moddedecon [options]` - Modded economic balance
- `/modpackm [options]` - Mod pack management
- `/modbalancer [options]` - Global mod balance management
- `/dmb [options]` - Dynamic mod balancing

## Configuration

### Main Configuration (config.yml)
See `config.yml` for all configuration options including:
- Branding settings
- Economy parameters
- Territory protection
- Event frequencies
- Auto-save intervals
- PVP settings
- Mod integration balancing

### Mod Balancing Configuration
- `modpacks.yml` - Defines available modpacks and integration rules
- `recipe-integration.yml` - Defines cross-mod recipe compatibility
- `dynamic-balancing.yml` - Dynamic balancing parameters
- `mod-integration-rules.yml` - Mod compatibility rules

## Development

### Project Structure
```
axiom-plugin/
├── src/
│   ├── main/
│   │   ├── java/com/axiom/
│   │   │   ├── command/      # Command implementations
│   │   │   ├── gui/          # GUI interfaces
│   │   │   ├── listener/     # Event listeners
│   │   │   ├── model/        # Data models
│   │   │   ├── service/      # Core business logic
│   │   │   └── util/         # Utilities
│   │   └── resources/
│   │       ├── plugin.yml    # Plugin metadata
│   │       ├── config.yml    # Main configuration
│   │       └── lang/         # Language files
├── pom.xml                   # Maven build configuration
├── README.md                 # Project documentation
└── docs/                     # Detailed documentation
```

### Building the Project
```bash
mvn clean install
```

### Testing
```bash
# Run unit tests
mvn test

# Run integration tests
./run_tests.sh

# Build and test
./build_and_test.sh
```

## Troubleshooting

### Common Issues
1. **Missing Dependencies**: Ensure WorldEdit is installed
2. **Mod Compatibility**: Make sure client and server mods match
3. **Permission Errors**: Check player permissions match expected roles
4. **Performance Issues**: Adjust mod integration settings in config

### Debugging
- Enable debug mode with `debug: true` in config
- Check console for error messages
- Verify all required mods are installed and compatible

## Website Development

The AXIOM website provides web-based administration and visualization tools. See [Website Development](#website-development) section.

---

Continue to [Installation Guide](INSTALLATION.md) for detailed setup instructions.