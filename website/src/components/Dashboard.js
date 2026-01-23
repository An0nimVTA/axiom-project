// website/src/components/Dashboard.js
import React from 'react';
import StatusCard from './StatusCard';
import StatsCard from './StatsCard';
import NationsSummary from './NationsSummary';
import ModUsageChart from './ModUsageChart';
import './Dashboard.scss';

function Dashboard({ status, stats }) {
  return (
    <div className="dashboard">
      <h1>AXIOM Dashboard</h1>
      <div className="dashboard-grid">
        <StatusCard status={status} />
        <StatsCard stats={stats} />
        <NationsSummary />
        <ModUsageChart modUsage={stats?.modUsage} />
      </div>
    </div>
  );
}

export default Dashboard;