# AXIOM Web Interface Setup Guide

## Overview
The AXIOM web interface provides administrative tools, data visualization, and real-time server statistics. This guide covers setting up the web dashboard for server administrators and nation leaders.

## Prerequisites
- Node.js 18+ installed
- Web server (Apache/Nginx) or direct Node.js hosting
- SSL certificate (recommended but not required)
- MongoDB or PostgreSQL database
- AXIOM plugin running on Minecraft server

## Architecture

### Frontend Components
- **React Dashboard**: Main administrative interface
- **Interactive Maps**: Territory visualization and management
- **Real-time Analytics**: Live server and nation statistics
- **Mod Integration Panel**: Mod usage and balance management
- **Diplomacy Interface**: Treaty and alliance management
- **Economic Charts**: Treasury and resource visualization

### Backend Components
- **REST API**: Data exchange between plugin and web interface
- **WebSocket Server**: Real-time updates
- **Authentication System**: User management and permissions
- **Data Export Service**: Generate reports and exports

## Frontend Setup

### 1. Install Node.js Dependencies
```bash
# Navigate to website directory
cd website/

# Install dependencies
npm install

# Verify installation
npm audit
```

### 2. Configure Environment Variables
Create/edit `website/.env`:

```env
# Application Configuration
NODE_ENV=production
PORT=3001
HOST=0.0.0.0

# Database Connection
DATABASE_URL=mongodb://localhost:27017/axiom-website
DB_CONNECTION_POOL_SIZE=10

# API Configuration
API_BASE_URL=http://your-server-ip:3000
API_TIMEOUT=30000
API_RETRY_ATTEMPTS=3

# Authentication & Security
JWT_SECRET=your_very_secure_jwt_secret_here_remember_to_change_me
SESSION_SECRET=another_unique_secret_for_sessions
BCRYPT_ROUNDS=12
CSRF_PROTECTION=true

# Admin Access
ADMIN_USERNAME=admin
ADMIN_PASSWORD_HASH=hashed_admin_password_here
DEFAULT_ADMIN_EMAIL=your-email@example.com

# SSL Configuration (optional but recommended)
SSL_ENABLED=false
SSL_CERT_PATH=/path/to/certificate.crt
SSL_KEY_PATH=/path/to/private.key

# CORS Configuration
ALLOWED_ORIGINS=http://your-server.com,http://localhost:3000,https://your-domain.com
CORS_MAX_AGE=86400  # 24 hours

# Rate Limiting
RATE_LIMIT_WINDOW_MS=15000
RATE_LIMIT_MAX_REQUESTS=100

# File Upload Limits
MAX_FILE_UPLOAD_SIZE=10MB
ALLOWED_FILE_TYPES=image/*,application/pdf,text/*
UPLOAD_DIRECTORY=uploads/

# Game Server Integration
MINECRAFT_SERVER_IP=your-server-ip
MINECRAFT_SERVER_PORT=25565
PLUGIN_API_PORT=3000
PLUGIN_API_KEY=your_secure_api_key_here

# Feature Flags
ENABLE_REALTIME_DASHBOARD=true
ENABLE_MOD_INTEGRATION_UI=true
ENABLE_ECONOMIC_CHARTS=true
ENABLE_TERRITORY_VISUALIZATION=true
ENABLE_DIPLOMACY_TOOLS=true
ENABLE_STATISTICS_EXPORT=true
```

### 3. Build Frontend Assets
```bash
# Build for production
npm run build

# Or build for development with hot reloading
npm run dev

# Create production build with optimizations
npm run build:prod
```

### 4. Frontend Configuration Files

#### Webpack Configuration (`website/config/webpack.config.js`)
```javascript
const path = require('path');
const HtmlWebpackPlugin = require('html-webpack-plugin');
const MiniCssExtractPlugin = require('mini-css-extract-plugin');
const { CleanWebpackPlugin } = require('clean-webpack-plugin');

module.exports = {
  entry: './src/index.js',
  output: {
    path: path.resolve(__dirname, '../dist'),
    filename: '[name].[contenthash].js',
    publicPath: '/',
    assetModuleFilename: 'assets/[hash][ext][query]'
  },
  resolve: {
    extensions: ['.js', '.jsx', '.ts', '.tsx'],
    alias: {
      '@': path.resolve(__dirname, '../src'),
      '@components': path.resolve(__dirname, '../src/components'),
      '@services': path.resolve(__dirname, '../src/services'),
      '@utils': path.resolve(__dirname, '../src/utils')
    }
  },
  module: {
    rules: [
      {
        test: /\.(js|jsx)$/,
        exclude: /node_modules/,
        use: {
          loader: 'babel-loader'
        }
      },
      {
        test: /\.css$/,
        use: [
          process.env.NODE_ENV === 'production' ? MiniCssExtractPlugin.loader : 'style-loader',
          'css-loader',
          'postcss-loader'
        ]
      },
      {
        test: /\.scss$/,
        use: [
          process.env.NODE_ENV === 'production' ? MiniCssExtractPlugin.loader : 'style-loader',
          'css-loader',
          'sass-loader'
        ]
      },
      {
        test: /\.(png|jpe?g|gif|svg)$/,
        type: 'asset/resource'
      },
      {
        test: /\.(woff|woff2|eot|ttf|otf)$/,
        type: 'asset/resource'
      }
    ]
  },
  plugins: [
    new CleanWebpackPlugin(),
    new HtmlWebpackPlugin({
      template: './public/index.html',
      minify: process.env.NODE_ENV === 'production'
    }),
    ...(process.env.NODE_ENV === 'production' ? [
      new MiniCssExtractPlugin({
        filename: '[name].[contenthash].css'
      })
    ] : [])
  ],
  optimization: {
    splitChunks: {
      chunks: 'all',
      cacheGroups: {
        vendor: {
          test: /[\\/]node_modules[\\/]/,
          name: 'vendors',
          chunks: 'all'
        }
      }
    }
  },
  devServer: {
    contentBase: path.join(__dirname, '../dist'),
    port: 3000,
    host: '0.0.0.0',
    hot: true,
    historyApiFallback: true,
    proxy: {
      '/api': {
        target: 'http://localhost:3000',
        changeOrigin: true
      }
    }
  }
};
```

#### Babel Configuration (`website/config/babel.config.js`)
```javascript
module.exports = {
  presets: [
    ['@babel/preset-env', {
      targets: {
        browsers: ['> 1%', 'last 2 versions']
      }
    }],
    ['@babel/preset-react', {
      runtime: 'automatic'
    }]
  ],
  plugins: [
    '@babel/plugin-proposal-class-properties',
    '@babel/plugin-transform-runtime'
  ]
};
```

## Backend Setup

### 1. Create Backend Configuration

#### Main Server Configuration (`website/server.js`)
```javascript
const express = require('express');
const mongoose = require('mongoose');
const cors = require('cors');
const helmet = require('helmet');
const rateLimit = require('express-rate-limit');
const jwt = require('jsonwebtoken');
const WebSocket = require('ws');
const http = require('http');
const path = require('path');

// Import routes
const authRoutes = require('./routes/auth');
const adminRoutes = require('./routes/admin');
const statsRoutes = require('./routes/stats');
const nationsRoutes = require('./routes/nations');
const modRoutes = require('./routes/mods');
const apiRoutes = require('./routes/api');

const app = express();
const server = http.createServer(app);
const wss = new WebSocket.Server({ server });

// Security middleware
app.use(helmet());
app.use(cors({
  origin: process.env.ALLOWED_ORIGINS ? process.env.ALLOWED_ORIGINS.split(',') : '*',
  credentials: true
}));

// Rate limiting
const limiter = rateLimit({
  windowMs: parseInt(process.env.RATE_LIMIT_WINDOW_MS) || 15000,
  max: parseInt(process.env.RATE_LIMIT_MAX_REQUESTS) || 100
});
app.use(limiter);

// Body parsing middleware
app.use(express.json({ limit: process.env.MAX_FILE_UPLOAD_SIZE || '10mb' }));
app.use(express.urlencoded({ extended: true }));

// Static files
app.use(express.static(path.join(__dirname, 'dist')));

// API Routes
app.use('/api/auth', authRoutes);
app.use('/api/admin', adminRoutes);
app.use('/api/stats', statsRoutes);
app.use('/api/nations', nationsRoutes);
app.use('/api/mods', modRoutes);
app.use('/api/data', apiRoutes);

// WebSocket handling
wss.on('connection', (ws, req) => {
  console.log('Client connected to WebSocket');
  
  ws.on('message', (message) => {
    // Handle incoming messages from clients
    try {
      const data = JSON.parse(message);
      // Broadcast to all connected clients
      wss.clients.forEach(client => {
        if (client !== ws && client.readyState === WebSocket.OPEN) {
          client.send(JSON.stringify(data));
        }
      });
    } catch (error) {
      console.error('WebSocket message error:', error);
    }
  });
  
  ws.on('close', () => {
    console.log('Client disconnected from WebSocket');
  });
});

// Connect to database
mongoose.connect(process.env.DATABASE_URL, {
  useNewUrlParser: true,
  useUnifiedTopology: true,
  poolSize: parseInt(process.env.DB_CONNECTION_POOL_SIZE) || 5
})
.then(() => console.log('Connected to MongoDB'))
.catch(err => console.error('MongoDB connection error:', err));

// Start server
const PORT = process.env.PORT || 3001;
server.listen(PORT, () => {
  console.log(`AXIOM Web Interface running on port ${PORT}`);
  console.log(`Environment: ${process.env.NODE_ENV}`);
});
```

### 2. API Integration

#### Plugin API Connector (`website/services/api-connector.js`)
```javascript
const axios = require('axios');

class AXIOMAPIConnector {
  constructor() {
    this.baseURL = process.env.API_BASE_URL || 'http://localhost:3000';
    this.apiKey = process.env.PLUGIN_API_KEY;
    this.timeout = parseInt(process.env.API_TIMEOUT) || 30000;
    
    this.client = axios.create({
      baseURL: this.baseURL,
      timeout: this.timeout,
      headers: {
        'Authorization': `Bearer ${this.apiKey}`,
        'Content-Type': 'application/json'
      }
    });
  }

  // Test connection to Minecraft server
  async testConnection() {
    try {
      const response = await this.client.get('/status');
      return response.data.status === 'online';
    } catch (error) {
      console.error('API connection test failed:', error.message);
      return false;
    }
  }

  // Get nation data
  async getNationsData() {
    try {
      const response = await this.client.get('/api/nations');
      return response.data;
    } catch (error) {
      console.error('Error fetching nations:', error.message);
      throw error;
    }
  }

  // Get server statistics
  async getServerStats() {
    try {
      const response = await this.client.get('/api/stats');
      return response.data;
    } catch (error) {
      console.error('Error fetching server stats:', error.message);
      throw error;
    }
  }

  // Get mod integration data
  async getModIntegrationData() {
    try {
      const response = await this.client.get('/api/mods');
      return response.data;
    } catch (error) {
      console.error('Error fetching mod data:', error.message);
      throw error;
    }
  }

  // Get economic data
  async getEconomicData() {
    try {
      const response = await this.client.get('/api/economy');
      return response.data;
    } catch (error) {
      console.error('Error fetching economy data:', error.message);
      throw error;
    }
  }

  // Get territory data
  async getTerritoryData() {
    try {
      const response = await this.client.get('/api/territory');
      return response.data;
    } catch (error) {
      console.error('Error fetching territory data:', error.message);
      throw error;
    }
  }

  // Execute command on server
  async executeServerCommand(command, parameters) {
    try {
      const response = await this.client.post('/api/command', {
        command: command,
        parameters: parameters
      });
      return response.data;
    } catch (error) {
      console.error('Error executing server command:', error.message);
      throw error;
    }
  }
}

module.exports = new AXIOMAPIConnector();
```

## Database Schema

### MongoDB Collections
```javascript
// User Schema (users)
const UserSchema = new mongoose.Schema({
  username: { type: String, required: true, unique: true },
  email: { type: String, required: true, unique: true },
  passwordHash: { type: String, required: true },
  role: { type: String, enum: ['admin', 'moderator', 'nation_leader', 'viewer'], default: 'viewer' },
  nationId: { type: String }, // For nation leaders
  permissions: [String],
  createdAt: { type: Date, default: Date.now },
  lastLogin: Date,
  isActive: { type: Boolean, default: true }
});

// Server Stats Schema (server_stats)
const ServerStatsSchema = new mongoose.Schema({
  timestamp: { type: Date, default: Date.now },
  playerCount: Number,
  nationCount: Number,
  totalTerritory: Number,
  serverUptime: Number, // in seconds
  tps: Number,
  memoryUsage: Number,
  pluginVersions: Map
});

// Mod Usage Schema (mod_usage)
const ModUsageSchema = new mongoose.Schema({
  modId: String,
  usageCount: Number,
  activeUsers: Number,
  nationUsage: Map, // Map<nationId, usageCount>
  timestamp: { type: Date, default: Date.now }
});

// Nation Stats Schema (nation_stats)
const NationStatsSchema = new mongoose.Schema({
  nationId: String,
  name: String,
  treasury: Number,
  territoryCount: Number,
  citizenCount: Number,
  militaryPower: Number,
  industrialPower: Number,
  technologyLevel: Number,
  collectedAt: { type: Date, default: Date.now }
});
```

## Deployment Options

### 1. Direct Node.js Hosting
```bash
# Install PM2 for process management
npm install -g pm2

# Start with PM2
pm2 start ecosystem.config.js

# Create ecosystem file
```

#### Ecosystem Configuration (`ecosystem.config.js`)
```javascript
module.exports = {
  apps: [{
    name: 'axiom-web',
    script: './server.js',
    instances: 1,
    autorestart: true,
    watch: false,
    max_memory_restart: '1G',
    env: {
      NODE_ENV: 'production',
      PORT: 3001,
      DATABASE_URL: 'mongodb://localhost:27017/axiom-website'
    }
  }]
};
```

### 2. Docker Deployment
```dockerfile
# Dockerfile
FROM node:18-alpine

WORKDIR /app

COPY package*.json ./
RUN npm ci --only=production

COPY . .

EXPOSE 3001

CMD ["npm", "start"]
```

```yaml
# docker-compose.yml
version: '3.8'
services:
  axiom-web:
    build: .
    ports:
      - "3001:3001"
    environment:
      - NODE_ENV=production
      - DATABASE_URL=mongodb://mongo:27017/axiom-website
      - API_BASE_URL=http://minecraft-server:3000
    depends_on:
      - mongo
    restart: unless-stopped

  mongo:
    image: mongo:latest
    volumes:
      - mongodb_data:/data/db
    ports:
      - "27017:27017"
    restart: unless-stopped

volumes:
  mongodb_data:
```

### 3. Reverse Proxy Configuration
For Nginx:
```nginx
server {
    listen 80;
    server_name your-domain.com;

    location / {
        proxy_pass http://localhost:3001;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection 'upgrade';
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_cache_bypass $http_upgrade;
    }
    
    # HTTPS redirect (after SSL is configured)
    listen 443 ssl;
    ssl_certificate /path/to/cert.pem;
    ssl_certificate_key /path/to/key.pem;
}
```

## Plugin API Setup

The website communicates with the Minecraft plugin through a REST API. Ensure the AXIOM plugin has the API component enabled:

### 1. Enable Plugin API
In AXIOM's main config.yml:
```yaml
api:
  enabled: true
  port: 3000
  host: "0.0.0.0"
  apiKey: "your_generated_api_key_here"
  rateLimiting:
    enabled: true
    requestsPerMinute: 100
  endpoints:
    status: true
    nations: true
    economy: true
    territory: true
    diplomacy: true
    technology: true
    mods: true
    stats: true
    chat: false  # Disabled for security
```

### 2. API Endpoints
The following endpoints are available:
- `GET /api/status` - Server status
- `GET /api/nations` - All nations data
- `GET /api/nations/:id` - Specific nation data
- `GET /api/economy` - Economy statistics
- `GET /api/territory` - Territory information
- `GET /api/mods` - Mod integration data
- `GET /api/stats` - Server statistics
- `POST /api/command` - Execute server commands
- `GET /api/chat` - Chat logs (if enabled)

## Testing the Web Interface

1. **Frontend Build Test**:
   ```bash
   cd website/
   npm run build
   # Check for build errors
   ```

2. **Backend Connection Test**:
   ```bash
   curl -H "Authorization: Bearer YOUR_API_KEY" \
        http://localhost:3001/api/status
   ```

3. **Database Connection Test**:
   ```bash
   # Check if MongoDB is accessible
   mongo --eval "db.runCommand({serverStatus: 1})"
   ```

4. **Minecraft Server Connection Test**:
   ```bash
   # From the website directory
   node -e "
   const api = require('./services/api-connector');
   api.testConnection().then(result => console.log('Connection test:', result));
   "
   ```

After successful installation, access the web interface at `http://your-server-ip:3001` and log in with the admin credentials configured in the environment variables.

---

Continue to [API Documentation](API_DOCS.md) for detailed endpoint specifications.