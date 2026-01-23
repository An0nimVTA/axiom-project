# AXIOM Installation and Setup Guide

## Table of Contents
1. [System Requirements](#system-requirements)
2. [Server Setup](#server-setup)
3. [Modpack Installation](#modpack-installation)
4. [AXIOM Plugin Setup](#axiom-plugin-setup)
5. [Backend Setup](#backend-setup)
6. [Website Setup](#website-setup)
7. [Testing and Verification](#testing-and-verification)
8. [Advanced Configuration](#advanced-configuration)
9. [Troubleshooting](#troubleshooting)

## System Requirements

### Server Hardware
- **CPU**: 4+ cores (8+ recommended)
- **RAM**: 8GB minimum, 16GB+ recommended
- **Storage**: SSD recommended, 50GB+ available space
- **OS**: Linux (Ubuntu/Debian) or Windows Server 2019+

### Software Requirements
- **Java**: OpenJDK 17 or higher
- **Minecraft Server**: Paper/Spigot 1.20.1 or Mohist 1.20.1 (for mod support)
- **Database**: MySQL 8.0 or PostgreSQL 13+ (optional but recommended)

### Modpack Requirements
For full mod integration (TACZ, PointBlank, Industrial Upgrade, etc.), use:
- **Mohist Server**: Minecraft 1.20.1 with required mods
- **Client**: Matching modpack installation
- **Memory**: Additional 2-4GB for client-side performance

## Server Setup

### 1. Choose Server Type
For full AXIOM functionality with mod integration, use **Mohist** server (Forge + Bukkit hybrid):

```bash
# Download Mohist server jar
wget https://mohistmc.com/api/download/mohist-1.20.1

# Or use Paper for basic functionality
wget https://papermc.io/api/v2/projects/paper/versions/1.20.1/builds/latest/downloads/paper-1.20.1-latest.jar
```

### 2. Server Configuration
Create server.properties with optimal settings:

```properties
# Server Properties for AXIOM
server-port=25565
max-players=100
view-distance=8
simulation-distance=6
enable-command-block=true
online-mode=false  # Required for modded clients
level-type=world
difficulty=normal
enable-query=true
enable-rcon=true
rcon.password=your_secure_password
white-list=false
```

### 3. JVM Arguments
For optimal performance with mods:

```bash
java -Xms8G -Xmx16G \
  -XX:+UseG1GC \
  -XX:+ParallelRefProcEnabled \
  -XX:MaxGCPauseMillis=200 \
  -XX:+UnlockExperimentalVMOptions \
  -XX:+DisableExplicitGC \
  -XX:G1HeapWastePercent=5 \
  -XX:G1MixedGCCountTarget=4 \
  -XX:G1MixedGCLiveThresholdPercent=90 \
  -XX:G1RSetUpdatingPauseTimePercent=5 \
  -XX:SurvivorRatio=32 \
  -XX:+PerfDisableSharedMem \
  -XX:MaxTenuringThreshold=1 \
  -jar mohist-1.20.1-server.jar nogui
```

## Modpack Installation

### 1. Download Required Mods
Create a `mods` folder and download required mods:

```bash
mkdir mods
cd mods

# Download industrial mods
wget [MOD_URL_FOR] industrialupgrade-[version].jar
wget [MOD_URL_FOR] immersiveengineering-[version].jar
wget [MOD_URL_FOR] appliedenergistics2-[version].jar
wget [MOD_URL_FOR] mekanism-[version].jar
wget [MOD_URL_FOR] simplyquarries-[version].jar
wget [MOD_URL_FOR] quantumgenerators-[version].jar

# Download military mods
wget [MOD_URL_FOR] tacz-[version].jar
wget [MOD_URL_FOR] pointblank-[version].jar
wget [MOD_URL_FOR] ballistix-[version].jar
wget [MOD_URL_FOR] superbwarfare-[version].jar
wget [MOD_URL_FOR] warium-[version].jar
wget [MOD_URL_FOR] capsawims-[version].jar

# Download utility mods
wget [MOD_URL_FOR] xaerominimap-[version].jar
wget [MOD_URL_FOR] xaeroworldmap-[version].jar
wget [MOD_URL_FOR] simple-voice-chat-[version].jar
wget [MOD_URL_FOR] embeddium-[version].jar
wget [MOD_URL_FOR] entityculling-[version].jar
wget [MOD_URL_FOR] ferritecore-[version].jar
```

### 2. Client-Side Installation
Players need matching modpack installation:
1. Install Minecraft 1.20.1
2. Install Forge 47.2.x or Mohist client
3. Place identical mods in `.minecraft/mods` folder

### 3. Mod Configuration
Configure mod compatibility:

```yaml
# config/immersiveengineering.cfg
general {
    # Enable multiblock structures
    B:enableMultiblocks=true
    
    # Interoperability settings
    B:enableWireInterops=true
}

# config/appliedenergistics2/common.json
{
  "general": {
    "supportedDevices": [
      "minecraft",
      "immersiveengineering",
      "industrialupgrade"
    ]
  }
}
```

## AXIOM Plugin Setup

### 1. Plugin Installation
1. Download AXIOM plugin JAR
2. Place in `plugins/` folder
3. Install dependencies:
   - WorldEdit
   - PlaceholderAPI (optional)
   - Vault (optional)

### 2. Initial Configuration
Edit `plugins/AXIOM/config.yml`:

```yaml
# AXIOM Main Configuration

branding:
  pluginName: "AXIOM Geopolitical Engine"
  motto: "В AXIOM вы не играете в мире — вы его строите"

storage:
  baseFolder: "plugins/AXIOM"
  prettyPrintJson: true

economy:
  defaultCurrencyCode: "AXC"
  startingTreasury: 10000.0
  maxPrintAmountPerCommand: 1000000.0
  inflationControl:
    maxDailyMoneyPrint: 0.2  # 20% of treasury per day
    penaltyMultiplier: 0.95  # Reduce print efficiency if overused

territory:
  protectionEnabled: true
  allowNationMembersToBuild: true
  chunkClaimCooldownSeconds: 60
  maxChunksPerPlayer: 100

events:
  newsBroadcastIntervalSeconds: 900

autosave:
  intervalSeconds: 300

pvp:
  requireToggle: true

modIntegration:
  enabled: true
  modDetectionIntervalMinutes: 30
  energySystem:
    # Standardize all mod energy to FE
    standardUnit: "FE"
    conversionRates:
      rf: 1.0  # Redstone Flux
      eu: 0.25  # IndustrialCraft EU (4 FE = 1 EU)
      
  modCompatibility:
    restricted_combinations:
      - ["tacz", "pointblank"]  # Both provide firearms
      - ["ballistix", "superbwarfare", "warium"]  # All provide heavy explosives
    synergy_bonus: 0.1  # 10% bonus for compatible mod combinations
    conflict_penalty: 0.15  # 15% penalty for conflicting combinations

recipeIntegration:
  enabled: true
  balancing:
    maximum_recipe_cost_multiplier: 2.0  # Recipe costs can't exceed vanilla by more than 2x
    minimum_resource_efficiency: 0.5  # Recipes must be at least 50% efficient compared to vanilla
    cross_mod_synergy_bonus: 0.15  # 15% bonus for using compatible mod recipes together
    conflicting_mod_penalty: 0.2  # 20% penalty for using conflicting mod components

debug: false
```

### 3. Modpack Configuration
Configure modpack settings in `plugins/AXIOM/modpacks.yml`:

```yaml
# AXIOM Mod Pack Configuration
modPacks:
  enabled: true
  defaultModPack: "balanced"
  
  definitions:
    warfare:
      id: "warfare"
      name: "Warfare Expansion Pack"
      description: "Military mods: TACZ, PointBlank, Superb Warfare, Ballistix, Warium"
      enabled: true
      requiredMods:
        - "tacz"
        - "pointblank"
      optionalMods:
        - "superbwarfare"
        - "ballistix"
        - "warium"
        - "capsawims"
      compatibilityLevel: "balanced"
      recommendedPlayers: 10
      settings:
        militaryBonuses: 0.2
        productionCostReduction: 0.1
        energyRequirements: 1.1
        resourceConsumption: 1.2
      integrationRules:
        - sourceMod: "tacz"
          targetMod: "pointblank"
          conversionType: "ammo_compatibility"
          conversionRate: 1.0

    industrial:
      id: "industrial"
      name: "Industrial Revolution Pack"
      description: "Industrial mods: Industrial Upgrade, IE, AE2, etc."
      enabled: true
      requiredMods:
        - "industrialupgrade"
        - "immersiveengineering"
      optionalMods:
        - "appliedenergistics2"
        - "mekanism"
        - "thermal"
      compatibilityLevel: "balanced"
      recommendedPlayers: 15
      settings:
        industrialBonuses: 0.3
        energyEfficiency: 1.2
        resourceProcessingSpeed: 1.4
        automationBonus: 1.25

    technology:
      id: "technology"
      name: "Technology Revolution Pack"
      description: "High-tech mods: AE2, Mekanism, Quantum Generators, etc."
      enabled: true
      requiredMods:
        - "appliedenergistics2"
        - "mekanism"
      optionalMods:
        - "quantumgenerators"
        - "immersiveengineering"
      compatibilityLevel: "hardcore"
      recommendedPlayers: 20
      settings:
        techBonuses: 0.4
        researchSpeedBoost: 1.3
        energyEfficiency: 1.5
        processingSpeed: 1.5

    balanced:
      id: "balanced"
      name: "Balanced Experience Pack"
      description: "Well-balanced combination of military, industrial, and technology mods"
      enabled: true
      requiredMods:
        - "industrialupgrade"
        - "immersiveengineering"
        - "appliedenergistics2"
        - "tacz"
      optionalMods:
        - "pointblank"
        - "superbwarfare"
        - "ballistix"
        - "mekanism"
        - "thermal"
        - "capsawims"
      compatibilityLevel: "balanced"
      recommendedPlayers: 25
      settings:
        overallBalance: 1.0
        resourceDiversity: 1.2
        crossModSynergy: 1.15
        progressionRate: 1.0

  # Global modpack settings
  globalSettings:
    allowModPackSwitching: true
    modPackSwitchingCooldownHours: 24
    modPackSwitchingCostPercent: 0.05  # 5% of treasury
    preserveResourcesDuringSwitch: true
    announceModPackChanges: true

  # Modpack balancing settings
  balancing:
    mismatchPenalty:
      enabled: true
      penaltyMultiplier: 0.8  # 20% penalty if modpack doesn't match nation size
      triggerThreshold: 2.0   # If nation is 2x larger/smaller than recommended
    appropriateBonus:
      enabled: true
      bonusMultiplier: 1.15   # 15% bonus for appropriate modpack
      triggerThreshold: 0.8   # If nation size is within 20% of recommendation
```

### 4. Dynamic Balancing Configuration
Set up dynamic balancing in `plugins/AXIOM/dynamic-balancing.yml`:

```yaml
# AXIOM Dynamic Mod Balancing Configuration
dynamicBalancing:
  enabled: true
  balancingIntervalMinutes: 30
  activityTrackingEnabled: true
  activityUpdateIntervalMinutes: 5
  
  popularityThresholds:
    low: 20.0      # Below this % = low popularity
    medium: 60.0   # Between low-medium = medium popularity
    high: 100.0    # Above medium = high popularity
  
  adjustmentSensitivity:
    difficulty: 0.1     # 10% max adjustment per cycle
    resources: 0.15     # 15% max adjustment per cycle
    crafting: 0.2       # 20% max adjustment per cycle
    energy: 0.12        # 12% max adjustment per cycle
    combat: 0.18        # 18% max adjustment per cycle
  
  actionThresholds:
    popularityDrop: -15.0    # If usage drops by 15%, take action
    popularitySurge: 25.0    # If usage surges by 25%, take action
    engagementLow: 10.0      # Low engagement action threshold
    engagementHigh: 80.0     # High engagement action threshold

  modPackBalancing:
    warfare:
      baseDifficulty: 1.0
      resourceMultiplier: 1.0
      craftingTimeMultiplier: 1.0
      combatEffectiveness: 1.0
      integrationRules:
        tacz_pointblank_compatibility: true
        ballistix_supplementary: true
        warfare_vehicle_integration: true
    industrial:
      baseDifficulty: 0.9
      resourceMultiplier: 1.1
      craftingTimeMultiplier: 0.9
      combatEffectiveness: 0.8
      integrationRules:
        ie_iu_energy_compatibility: true
        ae2_ie_automation_synergy: true
        quarry_industrial_efficiency: true

  balancingMechanisms:
    antiDominance:
      enabled: true
      popularityLimit: 75.0  # No mod should exceed 75% usage
      penaltyMultiplier: 0.9  # 10% penalty when exceeding limit
    diversityRewards:
      enabled: true
      bonusPerModUsed: 0.05  # 5% bonus per different mod type used
      maxBonus: 0.25  # 25% max diversity bonus
    specializationBalancing:
      enabled: true
      militarySpecializationPenalty: 0.1  # 10% penalty for pure military focus
      industrialSpecializationBonus: 0.05  # 5% bonus for industrial focus
      technologySpecializationRequirement: 0.7  # Need 70% tech usage for full benefit

  performance:
    maxActiveRules: 1000
    cacheTimeoutMinutes: 30
    updateBatchSize: 10
    enableAsyncProcessing: true
```

## Backend Setup

### 1. Backend Requirements
The AXIOM backend provides REST API and data export functionality:

- **Runtime**: Node.js 18+ or Java 17+
- **Database**: MongoDB 6.0+ or PostgreSQL 13+
- **Memory**: 2GB+ RAM
- **Storage**: SSD recommended

### 2. Backend Installation
```bash
# Navigate to backend directory
cd backend/

# If using Node.js backend
npm install
npm run build
npm start

# If using Java backend
mvn clean install
java -jar target/axiom-backend-[version].jar
```

### 3. Backend Configuration
Edit `backend/config.json`:

```json
{
  "server": {
    "port": 3000,
    "host": "0.0.0.0"
  },
  "database": {
    "type": "mongodb",
    "connection": "mongodb://localhost:27017/axiom",
    "poolSize": 10
  },
  "api": {
    "enabled": true,
    "rateLimit": 100,
    "tokens": {
      "expirationHours": 24
    }
  },
  "export": {
    "enabled": true,
    "formats": ["json", "xml", "csv"],
    "maxExportSize": 1000000,
    "schedule": "0 */6 * * *"  # Every 6 hours
  },
  "sync": {
    "enabled": true,
    "intervalSeconds": 300,
    "plugins": ["AXIOM"]
  }
}
```

## Website Setup

The AXIOM website provides web-based administration and visualization tools.

### 1. Website Requirements
- **Runtime**: Node.js 18+ and NPM
- **Build Tools**: Webpack, Babel
- **Memory**: 1GB+ RAM
- **Storage**: 5GB+ space

### 2. Website Installation
```bash
# Navigate to website directory
cd website/

# Install dependencies
npm install

# Build for production
npm run build

# Start development server
npm run dev

# Start production server
npm start
```

### 3. Website Configuration
Create `website/.env`:

```env
NODE_ENV=production
PORT=3001
API_BASE_URL=http://your-server-ip:3000
MONGODB_URI=mongodb://localhost:27017/axiom-website
JWT_SECRET=your_very_secure_jwt_secret_here
SESSION_SECRET=your_session_secret
ADMIN_PASSWORD=your_admin_password
```

### 4. Website Files Structure
```
website/
├── public/
│   ├── index.html
│   ├── favicon.ico
│   └── manifest.json
├── src/
│   ├── components/     # React components
│   │   ├── admin/      # Admin panels
│   │   ├── maps/       # Interactive maps
│   │   ├── nations/    # Nation displays
│   │   └── stats/      # Statistics dashboards
│   ├── pages/          # Page components
│   ├── services/       # API service layer
│   ├── utils/          # Helper functions
│   ├── styles/         # CSS/SCSS files
│   └── index.js        # Main entry point
├── config/
│   ├── webpack.config.js
│   └── env.js
├── package.json
└── .env                # Environment variables
```

### 5. Website Build and Deployment
```bash
# Build for production
npm run build

# Serve using PM2 (recommended)
npm install -g pm2
pm2 start ecosystem.config.js

# Or serve with node
node server.js
```

## Testing and Verification

### 1. Plugin Startup Verification
Check server console for successful startup messages:
```
[INFO] [AXIOM] AXIOM Geopolitical Engine v1.0.0 enabled successfully!
[INFO] [AXIOM] Mod Integration: Detected 25 installed mods
[INFO] [AXIOM] Recipe Integration: Loaded 150 cross-mod recipes
[INFO] [AXIOM] Technology Tree: Initialized 5 progression stages
```

### 2. Command Testing
Test core commands:
```bash
# Create a nation
/nation create TestNation

# Try claiming territory
/claim

# Test technology tree
/technology

# Test mod integration
/modpack list
/recipeint list
/modbal global
```

### 3. Mod Integration Testing
Verify mod integration:
- Check `/modbal status` - should show mod compatibility
- Test cross-mod crafting recipes
- Verify ammo compatibility between different firearm mods
- Test energy sharing between different energy systems

### 4. Economic System Verification
- Test treasury management
- Verify taxation and income systems
- Check trade functionality
- Validate currency systems

## Advanced Configuration

### 1. Performance Tuning
For large servers, adjust performance settings:

```yaml
# In config.yml
performance:
  chunkClaimLimitPerMinute: 10
  territoryUpdateFrequency: 20  # Ticks between territory updates
  economyUpdateFrequency: 40    # Ticks between economy updates
  modIntegrationUpdateFrequency: 100  # Ticks between mod integration checks
  
asyncProcessing:
  enabled: true
  threadPoolSize: 8
  maxConcurrentTasks: 50
```

### 2. Custom Mod Integration
Add custom mod integration rules in `mod-integration-rules.yml`:

```yaml
# Custom integration rules
modIntegrationRules:
  customMod1_customMod2:
    enabled: true
    itemConversion:
      customMod1:ingot_special: customMod2:ingot_rare
      customMod2:block_advanced: customMod1:block_elite
    energyConversionRate: 1.2
    craftingSynergyBonus: 0.15
    compatibilityLevel: 0.8
```

### 3. Custom Technology Trees
Add custom tech paths by modifying the plugin or creating custom data files:

```yaml
# In custom-tech-trees.yml
technologyTrees:
  customPath:
    branch: "specialization"
    techs:
      - id: "custom_tech_1"
        name: "Custom Technology"
        prerequisites: []
        cost: 10000
        tier: 1
        bonuses:
          customBonus: 1.2
        requiredMod: "custommod"
```

## Troubleshooting

### Common Issues and Solutions

1. **Plugin Won't Load**
   - Check server version compatibility
   - Verify all dependencies are installed
   - Ensure correct plugin folder placement

2. **Mod Integration Not Working**
   - Verify client and server have matching mod versions
   - Check mod names match expected IDs
   - Enable debug mode and check logs

3. **Performance Issues**
   - Increase server memory allocation
   - Adjust update frequencies
   - Enable async processing
   - Check for mod conflicts

4. **Missing Commands**
   - Check permissions
   - Verify plugin is properly loaded
   - Ensure commands are registered in plugin.yml

### Debug Mode
Enable debug mode in config.yml:
```yaml
debug: true
logging:
  level: INFO  # Change to DEBUG for more details
  file: "axiom-debug.log"
```

### Log Analysis
Check these log locations:
- `logs/latest.log` - Main server logs
- `plugins/AXIOM/logs/` - AXIOM-specific logs
- `plugins/AXIOM/debug.log` - Debug logs (if enabled)

---

Next: [Configuration Guide](CONFIGURATION.md) for detailed customization options.