// website/src/components/ModIntegrationPanel.js
import React, { useState, useEffect } from 'react';
import './ModIntegrationPanel.scss';

function ModIntegrationPanel() {
  const [modData, setModData] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    // Simulate API call
    setTimeout(() => {
      setModData({
        compatibilityMatrix: {
          'tacz_pointblank': { score: 0.95, status: 'fully_compatible', description: 'Full ammo compatibility' },
          'ie_iu': { score: 0.88, status: 'highly_compatible', description: 'Energy system integration' },
          'ae2_ie': { score: 0.92, status: 'fully_compatible', description: 'Network synergy' },
          'tacz_ballistix': { score: 0.75, status: 'partially_compatible', description: 'Limited integration' }
        },
        usage: {
          tacz: 75,
          pointblank: 68,
          ballistix: 45,
          immersiveengineering: 82,
          industrialupgrade: 78,
          appliedenergistics2: 65
        },
        integrationRules: [
          { id: 1, source: 'TACZ', target: 'PointBlank', type: 'ammo_compatibility', active: true },
          { id: 2, source: 'Industrial Upgrade', target: 'IE', type: 'energy_exchange', active: true },
          { id: 3, source: 'AE2', target: 'TACZ', type: 'storage_network_access', active: true }
        ]
      });
      setLoading(false);
    }, 1000);
  }, []);

  if (loading) {
    return <div className="loading">Loading mod integration data...</div>;
  }

  return (
    <div className="mod-integration-panel">
      <h2>Mod Integration Manager</h2>
      
      <div className="compatibility-section">
        <h3>Compatibility Matrix</h3>
        <table className="compatibility-table">
          <thead>
            <tr>
              <th>Mod Combination</th>
              <th>Score</th>
              <th>Status</th>
              <th>Description</th>
              <th>Action</th>
            </tr>
          </thead>
          <tbody>
            {Object.entries(modData.compatibilityMatrix).map(([key, value]) => {
              const mods = key.split('_');
              return (
                <tr key={key}>
                  <td>{mods[0]} ↔ {mods[1]}</td>
                  <td>{Math.round(value.score * 100)}%</td>
                  <td className={`status ${value.status}`}>
                    {value.status.replace('_', ' ')}
                  </td>
                  <td>{value.description}</td>
                  <td>
                    <button className="btn btn-primary">Configure</button>
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
      
      <div className="mod-usage-section">
        <h3>Mod Usage Statistics</h3>
        <div className="usage-bars">
          {Object.entries(modData.usage).map(([mod, percentage]) => (
            <div key={mod} className="usage-bar">
              <div className="mod-name">{mod}</div>
              <div className="usage-progress">
                <div 
                  className="progress-fill" 
                  style={{ width: `${percentage}%` }}
                ></div>
                <div className="percentage">{percentage}%</div>
              </div>
            </div>
          ))}
        </div>
      </div>
      
      <div className="integration-rules-section">
        <h3>Active Integration Rules</h3>
        <div className="rules-grid">
          {modData.integrationRules.map(rule => (
            <div key={rule.id} className={`rule-card ${rule.active ? 'active' : 'inactive'}`}>
              <h4>{rule.source} → {rule.target}</h4>
              <p>Type: {rule.type}</p>
              <div className="rule-status">
                Status: <span className={rule.active ? 'active-status' : 'inactive-status'}>
                  {rule.active ? 'ACTIVE' : 'INACTIVE'}
                </span>
              </div>
              <button className="btn btn-secondary">
                {rule.active ? 'Deactivate' : 'Activate'}
              </button>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}

export default ModIntegrationPanel;