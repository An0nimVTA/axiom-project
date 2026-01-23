// website/src/components/StatusCard.js
import React from 'react';
import './StatusCard.scss';

function StatusCard({ status }) {
  if (!status) return <div className="status-card">Loading...</div>;

  const getStatusColor = (status) => {
    return status === 'online' ? 'status-online' : 'status-offline';
  };

  return (
    <div className="status-card">
      <h2>Server Status</h2>
      <div className={`status-indicator ${getStatusColor(status.status)}`}>
        {status.status}
      </div>
      <div className="status-details">
        <div className="detail-item">
          <span className="label">Version:</span>
          <span className="value">{status.version}</span>
        </div>
        <div className="detail-item">
          <span className="label">Players Online:</span>
          <span className="value">{status.playerCount}</span>
        </div>
        <div className="detail-item">
          <span className="label">Nations:</span>
          <span className="value">{status.nationCount}</span>
        </div>
        <div className="detail-item">
          <span className="label">TPS:</span>
          <span className="value">{status.tps}</span>
        </div>
        <div className="detail-item">
          <span className="label">Mod Integration:</span>
          <span className="value">{status.modIntegrationEnabled ? 'Enabled' : 'Disabled'}</span>
        </div>
      </div>
    </div>
  );
}

export default StatusCard;