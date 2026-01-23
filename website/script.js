// API Base URL
const API_BASE = 'http://localhost:5000';

// Load servers
async function loadServers() {
    try {
        const response = await fetch(`${API_BASE}/servers/list`);
        const data = await response.json();
        
        const grid = document.getElementById('serversGrid');
        if (data.success && data.servers.length > 0) {
            grid.innerHTML = data.servers.map(server => `
                <div class="server-card fade-in">
                    <div class="server-name">${server.name}</div>
                    <div class="server-type">${server.type || 'modern'}</div>
                    <div class="server-description">${server.description || ''}</div>
                    <div class="server-status">
                        <span class="${server.online ? 'status-online' : 'status-offline'}">
                            ${server.online ? '🟢 Онлайн' : '🔴 Оффлайн'}
                        </span>
                        <span>${server.players_online || 0}/${server.max_players || 0}</span>
                    </div>
                </div>
            `).join('');
            document.querySelectorAll('.server-card.fade-in').forEach(element => observer.observe(element));
        } else {
            grid.innerHTML = '<div class="loading">Серверы не найдены</div>';
        }
    } catch (error) {
        console.error('Error loading servers:', error);
        document.getElementById('serversGrid').innerHTML = '<div class="loading">Ошибка загрузки серверов</div>';
    }
}

// Load news
async function loadNews() {
    try {
        const response = await fetch(`${API_BASE}/news`);
        const data = await response.json();
        
        const grid = document.getElementById('newsGrid');
        if (data.success && data.news.length > 0) {
            grid.innerHTML = data.news.map(item => `
                <div class="news-card fade-in">
                    <div class="news-title">${item.title}</div>
                    <div class="news-date">${new Date(item.created_at).toLocaleDateString('ru-RU')}</div>
                    <div class="news-content">${item.content || ''}</div>
                    ${item.tags ? `<div class="news-tags">${item.tags.split(',').map(tag => `<span class="news-tag">${tag.trim()}</span>`).join('')}</div>` : ''}
                </div>
            `).join('');
            document.querySelectorAll('.news-card.fade-in').forEach(element => observer.observe(element));
        } else {
            grid.innerHTML = '<div class="loading">Новостей пока нет</div>';
        }
    } catch (error) {
        console.error('Error loading news:', error);
        document.getElementById('newsGrid').innerHTML = '<div class="loading">Ошибка загрузки новостей</div>';
    }
}

// Modal functions
function openModal(modalId) {
    document.getElementById(modalId).style.display = 'block';
}

function closeModal(modalId) {
    document.getElementById(modalId).style.display = 'none';
}

// Notification function
function showNotification(message, type = 'success') {
    const container = document.getElementById('notification-container');
    const notification = document.createElement('div');
    notification.className = `notification ${type}`;
    notification.textContent = message;
    
    container.appendChild(notification);
    
    setTimeout(() => {
        notification.remove();
    }, 3000);
}

// Update UI after login
function updateUIAfterLogin(user) {
    const headerActions = document.querySelector('.header-actions');
    headerActions.innerHTML = ''; // Clear login/register buttons

    const welcomeMessage = document.createElement('div');
    welcomeMessage.className = 'welcome-message';
    welcomeMessage.textContent = `Привет, ${user.login}`;

    const dashboardBtn = document.createElement('a');
    dashboardBtn.href = '/dashboard';
    dashboardBtn.className = 'btn btn-outline';
    dashboardBtn.textContent = 'Панель управления';

    const logoutBtn = document.createElement('button');
    logoutBtn.className = 'btn btn-primary';
    logoutBtn.textContent = 'Выйти';
    logoutBtn.addEventListener('click', () => {
        localStorage.removeItem('auth_token');
        localStorage.removeItem('user');
        window.location.reload();
    });

    headerActions.appendChild(welcomeMessage);
    headerActions.appendChild(dashboardBtn);
    headerActions.appendChild(logoutBtn);
}

const openLoginModal = () => openModal('loginModal');

// Login
document.getElementById('loginForm').addEventListener('submit', async (e) => {
    e.preventDefault();
    const login = document.getElementById('loginInput').value;
    const password = document.getElementById('passwordInput').value;
    const errorMsg = document.getElementById('loginError');
    errorMsg.classList.remove('show');
    
    try {
        const response = await fetch(`${API_BASE}/auth/login`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ login, password })
        });
        
        const data = await response.json();
        
        if (data.success) {
            localStorage.setItem('auth_token', data.token);
            localStorage.setItem('user', JSON.stringify(data.user));
            closeModal('loginModal');
            showNotification('Вход выполнен успешно!');
            updateUIAfterLogin(data.user);
        } else {
            errorMsg.textContent = data.message || 'Ошибка входа';
            errorMsg.classList.add('show');
        }
    } catch (error) {
        errorMsg.textContent = 'Ошибка сети';
        errorMsg.classList.add('show');
    }
});

// Register
document.getElementById('registerForm').addEventListener('submit', async (e) => {
    e.preventDefault();
    const login = document.getElementById('registerLoginInput').value;
    const password = document.getElementById('registerPasswordInput').value;
    const errorMsg = document.getElementById('registerError');
    errorMsg.classList.remove('show');
    
    try {
        const response = await fetch(`${API_BASE}/auth/register`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ login, password })
        });
        
        const data = await response.json();
        
        if (data.success) {
            localStorage.setItem('auth_token', data.token);
            localStorage.setItem('user', JSON.stringify(data.user));
            closeModal('registerModal');
            showNotification('Регистрация успешна!');
            updateUIAfterLogin(data.user);
        } else {
            errorMsg.textContent = data.message || 'Ошибка регистрации';
            errorMsg.classList.add('show');
        }
    } catch (error) {
        errorMsg.textContent = 'Ошибка сети';
        errorMsg.classList.add('show');
    }
});

// Download launcher
function downloadLauncher(platform) {
    showNotification(`Скачивание для ${platform} скоро будет доступно`, 'error');
}

// Event listeners
document.getElementById('loginBtn').addEventListener('click', openLoginModal);
document.getElementById('registerBtn').addEventListener('click', () => openModal('registerModal'));

document.querySelectorAll('.close').forEach(closeBtn => {
    closeBtn.addEventListener('click', (e) => {
        const modal = e.target.closest('.modal');
        if (modal) {
            modal.style.display = 'none';
        }
    });
});

window.addEventListener('click', (e) => {
    if (e.target.classList.contains('modal')) {
        e.target.style.display = 'none';
    }
});

// Smooth scroll
document.querySelectorAll('a[href^="#"]').forEach(anchor => {
    anchor.addEventListener('click', function (e) {
        e.preventDefault();
        const target = document.querySelector(this.getAttribute('href'));
        if (target) {
            target.scrollIntoView({ behavior: 'smooth' });
        }
    });
});

// Load data on page load
document.addEventListener('DOMContentLoaded', () => {
    loadServers();
    loadNews();
    
    // Check if user is logged in
    const token = localStorage.getItem('auth_token');
    if (token) {
        const user = JSON.parse(localStorage.getItem('user') || '{}');
        updateUIAfterLogin(user);
    }
});

// Mobile Menu
const navToggle = document.getElementById('navToggle');
const navMenu = document.getElementById('navMenu');
const navLinks = document.querySelectorAll('.nav-link');

navToggle.addEventListener('click', () => {
    navMenu.classList.toggle('active');
});

navLinks.forEach(link => {
    link.addEventListener('click', () => {
    });
});

// Scroll Animations
const fadeInElements = document.querySelectorAll('.fade-in');

const observer = new IntersectionObserver((entries) => {
    entries.forEach(entry => {
        if (entry.isIntersecting) {
            entry.target.classList.add('visible');
            observer.unobserve(entry.target);
        }
    });
}, {
    threshold: 0.1
});

fadeInElements.forEach(element => {
    observer.observe(element);
});
