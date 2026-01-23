// website/src/services/api.js
import axios from 'axios';

const API_BASE_URL = process.env.REACT_APP_API_URL || 'http://localhost:3001/api';

const api = axios.create({
  baseURL: API_BASE_URL,
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${localStorage.getItem('axiom_token') || ''}`
  }
});

// Request interceptor
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('axiom_token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Response interceptor
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('axiom_token');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

export default api;

// API endpoints
export const apiEndpoints = {
  // Server info
  getStatus: () => api.get('/status'),
  getNations: () => api.get('/nations'),
  getStats: () => api.get('/stats'),
  getMods: () => api.get('/mods'),
  getTerritory: () => api.get('/territory'),
  
  // Nation management
  createNation: (data) => api.post('/nations', data),
  getNation: (id) => api.get(`/nations/${id}`),
  updateNation: (id, data) => api.put(`/nations/${id}`, data),
  deleteNation: (id) => api.delete(`/nations/${id}`),
  
  // Economy
  getEconomicData: () => api.get('/economy/global'),
  getNationEconomicData: (id) => api.get(`/economy/nations/${id}`),
  
  // Diplomacy
  getDiplomacyData: (id) => api.get(`/diplomacy/nations/${id}`),
  
  // Technology
  getTechData: (id) => api.get(`/technology/nations/${id}`),
  
  // Execute commands
  executeCommand: (data) => api.post('/command', data)
};