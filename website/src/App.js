// website/src/App.js
import React, { useState, useEffect } from 'react';
import { BrowserRouter as Router, Routes, Route, Link } from 'react-router-dom';
import io from 'socket.io-client';
import Dashboard from './components/Dashboard';
import NationsPanel from './components/NationsPanel';
import TerritoryMap from './components/TerritoryMap';
import ModIntegrationPanel from './components/ModIntegrationPanel';
import TechnologyTree from './components/TechnologyTree';
import Statistics from './components/Statistics';
import Navbar from './components/Navbar';
import Sidebar from './components/Sidebar';
import './styles/App.scss';

const socket = io(process.env.REACT_APP_API_URL || 'http://localhost:3001');

function App() {
  const [serverStatus, setServerStatus] = useState(null);
  const [nations, setNations] = useState([]);
  const [stats, setStats] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    // Initial data fetch
    fetch('/api/status')
      .then(res => res.json())
      .then(data => setServerStatus(data))
      .catch(console.error);

    fetch('/api/nations')
      .then(res => res.json())
      .then(data => setNations(data))
      .catch(console.error);

    fetch('/api/stats')
      .then(res => res.json())
      .then(data => setStats(data))
      .catch(console.error);

    // WebSocket listeners
    socket.on('status_update', (data) => {
      setServerStatus(data);
    });

    socket.on('nations_update', (data) => {
      setNations(data);
    });

    socket.on('stats_update', (data) => {
      setStats(data);
    });

    setLoading(false);

    return () => {
      socket.disconnect();
    };
  }, []);

  if (loading) {
    return (
      <div className="loading-container">
        <div className="loading-spinner">Loading AXIOM Dashboard...</div>
      </div>
    );
  }

  return (
    <div className="app">
      <Navbar status={serverStatus} />
      <div className="app-content">
        <Sidebar />
        <main className="main-content">
          <Routes>
            <Route path="/" element={<Dashboard status={serverStatus} stats={stats} />} />
            <Route path="/nations" element={<NationsPanel nations={nations} />} />
            <Route path="/territory" element={<TerritoryMap />} />
            <Route path="/mods" element={<ModIntegrationPanel />} />
            <Route path="/technology" element={<TechnologyTree />} />
            <Route path="/statistics" element={<Statistics stats={stats} />} />
          </Routes>
        </main>
      </div>
    </div>
  );
}

export default App;