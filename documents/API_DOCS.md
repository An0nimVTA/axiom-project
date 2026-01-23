# AXIOM API Documentation

## Overview
The AXIOM plugin provides a RESTful API for external applications to interact with the geopolitical engine. This API enables real-time data access, command execution, and integration with web interfaces.

## Base URL
```
http://[SERVER_IP]:[API_PORT]/api/v1
```

Default port is 3000, configurable in `config.yml`.

## Authentication
All API requests require an API key in the Authorization header:

```
Authorization: Bearer YOUR_API_KEY
```

The API key is generated automatically during plugin setup and can be found in the plugin configuration.

## Rate Limiting
- Default: 100 requests per minute per IP
- Configurable in plugin configuration
- Excessive requests will return HTTP 429 (Too Many Requests)

## Endpoints

### Server Status
```
GET /status
```

**Response:**
```json
{
  "status": "online",
  "version": "1.20.1-AXIOM-1.0.0",
  "uptime": 3600,
  "playerCount": 25,
  "nationCount": 8,
  "tps": 19.8,
  "pluginEnabled": true,
  "modIntegrationEnabled": true,
  "recipeIntegrationEnabled": true
}
```

### Nations Management

#### Get All Nations
```
GET /nations
```

**Response:**
```json
{
  "totalNations": 8,
  "nations": [
    {
      "id": "n1",
      "name": "Test Nation",
      "leader": "PlayerUUID",
      "treasury": 1500000.50,
      "citizens": 150,
      "territories": 45,
      "economyType": "industrial",
      "governmentType": "democracy",
      "createdDate": "2023-01-15T12:00:00Z",
      "allies": ["n2", "n3"],
      "enemies": ["n4"],
      "techLevel": 2.5,
      "militaryPower": 3200,
      "industrialPower": 4100
    }
  ]
}
```

#### Get Single Nation
```
GET /nations/{nationId}
```

**Response:**
```json
{
  "id": "n1",
  "name": "Test Nation",
  "leader": "PlayerUUID",
  "description": "A test nation for development",
  "capital": "chunk_world_100_100",
  "treasury": 1500000.50,
  "currencyCode": "AXC",
  "taxRate": 15,
  "citizens": [
    {
      "uuid": "PlayerUUID",
      "name": "PlayerName",
      "role": "LEADER",
      "joinDate": "2023-01-15T12:00:00Z"
    }
  ],
  "territories": [
    {
      "chunkKey": "world_x_z",
      "protection": true,
      "claimedDate": "2023-01-15T12:00:00Z"
    }
  ],
  "allies": ["n2", "n3"],
  "enemies": ["n4"],
  "treaties": [
    {
      "id": "t1",
      "type": "non_aggression",
      "with": "n2",
      "expires": "2024-01-15T12:00:00Z",
      "active": true
    }
  ],
  "technologies": {
    "totalUnlocked": 15,
    "progress": {
      "military": 45.2,
      "industry": 62.1,
      "economy": 38.7,
      "infrastructure": 55.0,
      "science": 28.9
    }
  },
  "economy": {
    "gdp": 2500000.00,
    "growthRate": 3.2,
    "population": 150,
    "inflation": 1.2
  },
  "military": {
    "strength": 3200,
    "equipment": {
      "tacz": 120,
      "pointblank": 85,
      "ballistix": 12
    }
  }
}
```

### Economy Data

#### Get Global Economy
```
GET /economy/global
```

**Response:**
```json
{
  "globalEconomy": {
    "totalCurrencyInCirculation": 50000000.00,
    "inflationRate": 2.1,
    "averageNationTreasury": 6250000.00,
    "largestNationTreasury": 15000000.50,
    "totalTransactions": 1250,
    "activeMarkets": 3,
    "currencies": [
      {
        "code": "AXC",
        "name": "AXIOM Credit",
        "value": 1.0
      }
    ]
  }
}
```

#### Get Nation Economy
```
GET /economy/nations/{nationId}
```

**Response:**
```json
{
  "nationId": "n1",
  "treasury": 1500000.50,
  "gdp": 2500000.00,
  "monthlyIncome": 185000.75,
  "taxRevenue": 45000.25,
  "tradeRevenue": 65000.50,
  "resourceRevenue": 75000.00,
  "expenses": {
    "maintenance": 25000.00,
    "military": 45000.00,
    "infrastructure": 35000.50,
    "research": 20000.00
  },
  "tradePartners": [
    {
      "nationId": "n2",
      "tradeVolume": 45000.25,
      "relationship": "ally"
    }
  ],
  "exchangeRates": {
    "USD": 0.05,
    "EUR": 0.045,
    "AXC": 1.0
  }
}
```

### Territory Data

#### Get Territory Information
```
GET /territory/chunks
```

**Response:**
```json
{
  "totalClaimedChunks": 542,
  "territoryDistribution": {
    "n1": 85,
    "n2": 120,
    "n3": 67,
    "n4": 200,
    "wilderness": 70
  },
  "territoryMap": {
    "minX": -500,
    "maxX": 500,
    "minZ": -500,
    "maxZ": 500,
    "regions": [
      {
        "world": "world",
        "nationId": "n1",
        "chunks": [{"x": 100, "z": 100}, {"x": 100, "z": 101}]
      }
    ]
  }
}
```

### Mod Integration Data

#### Get Mod Usage Statistics
```
GET /mods/statistics
```

**Response:**
```json
{
  "modUsage": {
    "totalModUsers": 180,
    "activeMods": [
      {
        "modId": "tacz",
        "usageCount": 150,
        "activeNations": 7,
        "integrationLevel": 0.85,
        "compatibilityScore": 0.92,
        "popularity": 85.7
      },
      {
        "modId": "pointblank",
        "usageCount": 120,
        "activeNations": 6,
        "integrationLevel": 0.78,
        "compatibilityScore": 0.88,
        "popularity": 72.4
      }
    ],
    "overallModIntegration": 0.86
  }
}
```

#### Get Mod Compatibility
```
GET /mods/compatibility
```

**Response:**
```json
{
  "compatibilityMatrix": {
    "tacz_pointblank": {
      "score": 0.95,
      "type": "ammo_compatibility",
      "status": "fully_compatible"
    },
    "immersiveengineering_industrialupgrade": {
      "score": 0.88,
      "type": "energy_compatibility",
      "status": "highly_compatible"
    },
    "appliedenergistics2_immersiveengineering": {
      "score": 0.92,
      "type": "automation_synergy",
      "status": "fully_compatible"
    }
  },
  "suggestions": [
    {
      "sourceMod": "tacz",
      "targetMod": "pointblank",
      "suggestion": "Enable cross-ammo compatibility",
      "benefit": "15% crafting efficiency bonus"
    }
  ]
}
```

### Technology Tree

#### Get Technology Progress
```
GET /technology/nations/{nationId}
```

**Response:**
```json
{
  "nationId": "n1",
  "technologyTree": {
    "military": {
      "totalTechnologies": 25,
      "unlockedTechnologies": 18,
      "progress": 72.0,
      "latestUnlocked": ["firearms_tech", "fortifications", "tactical_warfare"]
    },
    "industry": {
      "totalTechnologies": 30,
      "unlockedTechnologies": 15,
      "progress": 50.0,
      "latestUnlocked": ["basic_industry", "improved_mining", "energy_generation"]
    },
    "economy": {
      "totalTechnologies": 20,
      "unlockedTechnologies": 12,
      "progress": 60.0,
      "latestUnlocked": ["basic_trade", "trade_networks", "banking"]
    },
    "infrastructure": {
      "totalTechnologies": 22,
      "unlockedTechnologies": 10,
      "progress": 45.5,
      "latestUnlocked": ["roads", "transportation", "energy_networks"]
    },
    "science": {
      "totalTechnologies": 18,
      "unlockedTechnologies": 8,
      "progress": 44.4,
      "latestUnlocked": ["basic_education", "advanced_education", "research_labs"]
    }
  },
  "researchBonuses": {
    "military": 1.15,
    "industrial": 1.10,
    "economic": 1.08,
    "scientific": 1.12
  }
}
```

### Diplomacy Data

#### Get Diplomatic Relations
```
GET /diplomacy/nations/{nationId}
```

**Response:**
```json
{
  "nationId": "n1",
  "relations": {
    "n2": {
      "type": "ally",
      "trust": 92.5,
      "lastContact": "2023-10-15T14:30:00Z",
      "treatyIds": ["t1", "t3"]
    },
    "n4": {
      "type": "enemy",
      "trust": -75.0,
      "lastContact": "2023-10-10T08:15:00Z",
      "activeWars": ["w1"]
    }
  },
  "treaties": [
    {
      "id": "t1",
      "type": "non_aggression",
      "participants": ["n1", "n2"],
      "created": "2023-10-01T10:00:00Z",
      "expires": "2024-10-01T10:00:00Z",
      "terms": ["no_wars", "free_trade"],
      "active": true
    }
  ],
  "wars": [
    {
      "id": "w1",
      "attackers": ["n1", "n3"],
      "defenders": ["n4"],
      "started": "2023-10-10T08:00:00Z",
      "status": "active",
      "warGoals": ["territory_capture", "capitulation"],
      "battleResults": {
        "victories": 3,
        "defeats": 1,
        "territoriesCaptured": 12
      }
    }
  ]
}
```

### Execute Commands

#### Execute Server Command
```
POST /command
```

**Request Body:**
```json
{
  "command": "axiom nation info",
  "arguments": ["n1"],
  "executeAsConsole": false
}
```

**Response:**
```json
{
  "success": true,
  "output": [
    "Nation: Test Nation",
    "Leader: PlayerName",
    "Territory: 45 chunks",
    "Treasury: 1,500,000.50 AXC"
  ],
  "executionTime": 125
}
```

### Real-time Data Streaming

The API also supports WebSocket connections for real-time updates:

```
ws://[SERVER_IP]:[API_PORT]/ws
```

**Messages:**
- `"status_update"` - Server status changes
- `"nation_created"` - New nation created
- `"territory_claimed"` - Territory claimed
- `"war_declared"` - War declared
- `"technology_unlocked"` - Technology unlocked

## Error Handling

The API returns standard HTTP error codes:
- `200` - Success
- `400` - Bad Request (malformed request)
- `401` - Unauthorized (invalid API key)
- `403` - Forbidden (insufficient permissions)
- `404` - Not Found (resource doesn't exist)
- `429` - Too Many Requests (rate limit exceeded)
- `500` - Internal Server Error

**Error Response Format:**
```json
{
  "error": {
    "code": "INVALID_API_KEY",
    "message": "Invalid or missing API authorization key",
    "details": "Authorization header must contain Bearer token"
  }
}
```

## Configuration

### Enable API in config.yml
```yaml
api:
  enabled: true
  port: 3000
  host: "0.0.0.0"  # Bind to all interfaces
  apiKey: "generated-or-manual-api-key"
  rateLimiting:
    enabled: true
    requestsPerMinute: 100
    burstLimit: 200
  cors:
    enabled: true
    allowedOrigins: ["*"]  # Configure for production
  logging:
    enabled: true
    logFile: "api.log"
```

## Examples

### 1. Get Nation Data with JavaScript
```javascript
const API_KEY = 'your-api-key';
const BASE_URL = 'http://your-server:3000/api/v1';

async function getNationData(nationId) {
  try {
    const response = await fetch(`${BASE_URL}/nations/${nationId}`, {
      headers: {
        'Authorization': `Bearer ${API_KEY}`
      }
    });
    
    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }
    
    const data = await response.json();
    return data;
  } catch (error) {
    console.error('Error fetching nation data:', error);
    throw error;
  }
}

// Usage
getNationData('test_nation').then(nation => {
  console.log('Nation info:', nation.name);
  console.log('Treasury:', nation.treasury);
});
```

### 2. Execute Command Example
```javascript
async function executeCommand(cmd, args = []) {
  const response = await fetch(`${BASE_URL}/command`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${API_KEY}`
    },
    body: JSON.stringify({
      command: cmd,
      arguments: args
    })
  });
  
  return await response.json();
}

// Change nation description
executeCommand('axiom nation setdesc', ['test_nation', 'New awesome description']);
```

---

For WebSocket examples and advanced usage, see [WebSocket API Guide](WEBSOCKET_API.md).