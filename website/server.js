// server.js - Main web server for AXIOM dashboard
const express = require('express');
const http = require('http');
const socketIo = require('socket.io');
const mongoose = require('mongoose');
const cors = require('cors');
const helmet = require('helmet');
const rateLimit = require('express-rate-limit');
const path = require('path');
require('dotenv').config();

const app = express();
const server = http.createServer(app);
const io = socketIo(server, {
  cors: {
    origin: process.env.ALLOWED_ORIGINS?.split(',') || ["http://localhost:3000"],
    methods: ["GET", "POST"]
  }
});

// Middleware
app.use(helmet());
app.use(express.json({ limit: '10mb' }));
app.use(express.urlencoded({ extended: true }));

// Rate limiting
const limiter = rateLimit({
  windowMs: 15 * 60 * 1000, // 15 minutes
  max: 100 // limit each IP to 100 requests per windowMs
});
app.use(limiter);

// CORS
app.use(cors({
  origin: process.env.ALLOWED_ORIGINS?.split(',') || '*',
  credentials: true
}));

// Serve static files from the root directory
app.use(express.static(__dirname));

// Connect to MongoDB
mongoose.connect(process.env.DATABASE_URL || 'mongodb://localhost:27017/axiom_dashboard', {
  useNewUrlParser: true,
  useUnifiedTopology: true
}).then(() => {
  console.log('Connected to MongoDB');
}).catch(err => {
  console.error('MongoDB connection error:', err);
});

// Route for the dashboard
app.get('/dashboard', (req, res) => {
  res.sendFile(path.join(__dirname, 'dashboard.html'));
});


// Mock data for testing - in real implementation would connect to AXIOM plugin via API
const mockData = {
  status: {
    status: 'online',
    version: '1.0.0',
    uptime: Math.floor(Date.now() / 1000),
    playerCount: 42,
    nationCount: 8,
    tps: 19.7,
    modIntegrationEnabled: true
  },
  nations: [
    {
      id: 'n1',
      name: 'Test Nation',
      treasury: 1250000.50,
      citizens: 150,
      territory: 45,
      leader: 'Player123',
      techLevel: 2.3,
      militaryPower: 2800,
      industrialPower: 3200,
      economyType: 'industrial',
      governmentType: 'democracy'
    }
  ],
  statistics: {
    totalNations: 8,
    totalPlayers: 256,
    totalTerritory: 5420,
    totalEconomy: 85000000.00,
    modUsage: {
      tacz: 0.75,
      pointblank: 0.68,
      ballistix: 0.45,
      immersiveengineering: 0.82,
      industrialupgrade: 0.78,
      appliedenergistics2: 0.65
    }
  }
};

// API Routes
app.get('/api/status', (req, res) => {
  res.json(mockData.status);
});

app.get('/api/nations', (req, res) => {
  res.json(mockData.nations);
});

app.get('/api/stats', (req, res) => {
  res.json(mockData.statistics);
});

app.get('/api/mods', (req, res) => {
  res.json({
    compatibilityMatrix: {
      'tacz_pointblank': { score: 0.95, status: 'fully_compatible' },
      'ie_iu': { score: 0.88, status: 'highly_compatible' },
      'ae2_ie': { score: 0.92, status: 'fully_compatible' }
    },
    usage: mockData.statistics.modUsage
  });
});

app.get('/api/territory', (req, res) => {
  res.json({
    totalChunks: 5420,
    wilderness: 1230,
    nationChunks: mockData.nations.reduce((acc, nation) => {
      acc[nation.id] = nation.territory;
      return acc;
    }, {})
  });
});

// WebSocket connections
io.on('connection', (socket) => {
  console.log('User connected:', socket.id);
  
  // Send initial data
  socket.emit('status_update', mockData.status);
  socket.emit('nations_update', mockData.nations);
  socket.emit('stats_update', mockData.statistics);
  
  // Send updates every 30 seconds
  const interval = setInterval(() => {
    socket.emit('status_update', {
      ...mockData.status,
      playerCount: Math.max(0, mockData.status.playerCount + Math.floor(Math.random() * 3) - 1),
      tps: 19.5 + Math.random() * 0.4
    });
  }, 30000);
  
  socket.on('disconnect', () => {
    console.log('User disconnected:', socket.id);
    clearInterval(interval);
  });
});

// Serve index.html for all other routes
app.get('*', (req, res) => {
  res.sendFile(path.join(__dirname, 'index.html'));
});

const PORT = process.env.PORT || 3001;
server.listen(PORT, () => {
  console.log(`AXIOM Dashboard running on port ${PORT}`);
});

module.exports = app;