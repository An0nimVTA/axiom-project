import React from 'react';
export default function Statistics({ stats }) {
  return <div style={{color: 'white'}}><h2>Статистика</h2><pre>{JSON.stringify(stats, null, 2)}</pre></div>;
}
