// website/src/components/TerritoryMap.js
import React from 'react';
import { MapContainer, TileLayer, Marker, Popup } from 'react-leaflet';
import 'leaflet/dist/leaflet.css';
import L from 'leaflet';
import './TerritoryMap.scss';

// Fix for Leaflet marker icons
delete L.Icon.Default.prototype._getIconUrl;
L.Icon.Default.mergeOptions({
  iconRetinaUrl: require('leaflet/dist/images/marker-icon-2x.png'),
  iconUrl: require('leaflet/dist/images/marker-icon.png'),
  shadowUrl: require('leaflet/dist/images/marker-shadow.png'),
});

function TerritoryMap() {
  return (
    <div className="territory-map">
      <h2>Territory Visualization</h2>
      <div className="map-container">
        <MapContainer 
          center={[51.505, -0.09]} 
          zoom={13} 
          style={{ height: '500px', width: '100%' }}
          className="leaflet-map"
        >
          <TileLayer
            url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
            attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
          />
          <Marker position={[51.505, -0.09]}>
            <Popup>
              Capital of Test Nation<br />Nation: Test Nation
            </Popup>
          </Marker>
        </MapContainer>
      </div>
      <div className="territory-controls">
        <h3>Controls</h3>
        <button>View All Territories</button>
        <button>Filter by Nation</button>
        <button>Export Data</button>
      </div>
    </div>
  );
}

export default TerritoryMap;