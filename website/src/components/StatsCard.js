// website/src/components/StatsCard.js
import React from 'react';
import './StatsCard.scss';

function StatsCard({ stats }) {
  if (!stats) return <div className="stats-card">Loading...</div>;

  return (
    <div className="stats-card">
      <h2>Server Statistics</h2>
      <div className="stats-grid">
        <div className="stat-item">
          <div className="stat-value">{stats.totalNations}</div>
          <div className="stat-label">Nations</div>
        </div>
        <div className="stat-item">
          <div className="stat-value">{stats.totalPlayers}</div>
          <div className="stat-label">Players</div>
        </div>
        <div className="stat-item">
          <div className="stat-value">{stats.totalTerritory}</div>
          <div className="stat-label">Territory (chunks)</div>
        </div>
        <div className="stat-item">
          <div className="stat-value">{stats.totalEconomy.toLocaleString()}</div>
          <div className="stat-label">Total Economy</div>
        </div>
      </div>
    </div>
  );
}

export default StatsCard;