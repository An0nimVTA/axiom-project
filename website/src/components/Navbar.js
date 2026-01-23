// website/src/components/Navbar.js
import React from 'react';
import { Link } from 'react-router-dom';
import './Navbar.scss';

function Navbar({ status }) {
  const getStatusColor = (status) => {
    return status && status.status === 'online' ? 'online' : 'offline';
  };

  return (
    <nav className="navbar">
      <div className="nav-brand">
        <Link to="/">AXIOM Dashboard</Link>
      </div>
      <div className="nav-menu">
        <Link to="/">Home</Link>
        <Link to="/nations">Nations</Link>
        <Link to="/territory">Territory</Link>
        <Link to="/mods">Mods</Link>
        <Link to="/technology">Technology</Link>
        <Link to="/statistics">Statistics</Link>
      </div>
      <div className="nav-status">
        <div className={`status-indicator ${getStatusColor(status)}`}>
          {status ? status.status : 'connecting'}
        </div>
        <div className="player-count">
          {status ? `${status.playerCount} players online` : 'N/A'}
        </div>
      </div>
    </nav>
  );
}

export default Navbar;