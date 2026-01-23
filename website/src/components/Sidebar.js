// website/src/components/Sidebar.js
import React from 'react';
import { Link } from 'react-router-dom';
import './Sidebar.scss';

function Sidebar() {
  return (
    <aside className="sidebar">
      <div className="sidebar-section">
        <h3>Navigation</h3>
        <ul className="nav-links">
          <li><Link to="/">Dashboard</Link></li>
          <li><Link to="/nations">Nations</Link></li>
          <li><Link to="/territory">Territory Map</Link></li>
          <li><Link to="/mods">Mod Integration</Link></li>
          <li><Link to="/technology">Technology Tree</Link></li>
          <li><Link to="/statistics">Statistics</Link></li>
        </ul>
      </div>
      
      <div className="sidebar-section">
        <h3>Quick Actions</h3>
        <ul className="quick-actions">
          <li><button>Create Nation</button></li>
          <li><button>Claim Territory</button></li>
          <li><button>Start War</button></li>
          <li><button>Sign Treaty</button></li>
        </ul>
      </div>
      
      <div className="sidebar-section">
        <h3>Server Info</h3>
        <div className="server-info">
          <div>Minecraft 1.20.1</div>
          <div>AXIOM v1.0.0</div>
          <div>Modpack: Balanced</div>
        </div>
      </div>
    </aside>
  );
}

export default Sidebar;