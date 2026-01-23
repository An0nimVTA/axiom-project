// website/src/components/NationsPanel.js
import React from 'react';
import './NationsPanel.scss';

function NationsPanel({ nations }) {
  return (
    <div className="nations-panel">
      <h2>Nations</h2>
      <div className="nations-grid">
        {nations && nations.map(nation => (
          <div key={nation.id} className="nation-card">
            <h3>{nation.name}</h3>
            <div className="nation-details">
              <div className="detail-row">
                <span className="label">Leader:</span>
                <span className="value">{nation.leader}</span>
              </div>
              <div className="detail-row">
                <span className="label">Treasury:</span>
                <span className="value">{nation.treasury.toLocaleString()}</span>
              </div>
              <div className="detail-row">
                <span className="label">Citizens:</span>
                <span className="value">{nation.citizens}</span>
              </div>
              <div className="detail-row">
                <span className="label">Territory:</span>
                <span className="value">{nation.territory} chunks</span>
              </div>
              <div className="detail-row">
                <span className="label">Tech Level:</span>
                <span className="value">{nation.techLevel}</span>
              </div>
              <div className="detail-row">
                <span className="label">Military Power:</span>
                <span className="value">{nation.militaryPower}</span>
              </div>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}

export default NationsPanel;