document.addEventListener('DOMContentLoaded', () => {
    const API_BASE = 'http://localhost:5000';
    const token = localStorage.getItem('auth_token');

    const loadingEl = document.getElementById('dashboard-loading');
    const contentEl = document.getElementById('dashboard-content');

    if (!token) {
        // Если токена нет, перенаправляем на главную страницу
        window.location.href = '/';
        return;
    }

    async function loadDashboardData() {
        try {
            const response = await fetch(`${API_BASE}/auth/dashboard`, {
                method: 'GET',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${token}`
                }
            });

            if (response.status === 401) {
                // Невалидный токен, чистим localStorage и перенаправляем
                localStorage.removeItem('auth_token');
                localStorage.removeItem('user');
                window.location.href = '/';
                return;
            }

            const data = await response.json();

            if (data.success) {
                const dashboard = data.dashboard;
                document.getElementById('db-login').textContent = dashboard.login || 'N/A';
                document.getElementById('db-player-name').textContent = dashboard.player_name || 'N/A';
                document.getElementById('db-nation-name').textContent = dashboard.nation_name || 'Нет нации';
                document.getElementById('db-nation-rank').textContent = dashboard.nation_rank || 'Нет ранга';
                
                // Форматируем баланс
                const balance = parseFloat(dashboard.balance || 0);
                document.getElementById('db-balance').textContent = balance.toLocaleString('ru-RU', { minimumFractionDigits: 2, maximumFractionDigits: 2 });

                // Форматируем дату
                const lastUpdated = new Date(dashboard.last_updated);
                document.getElementById('db-last-updated').textContent = lastUpdated.toLocaleString('ru-RU');
                
                loadingEl.style.display = 'none';
                contentEl.style.display = 'grid';
            } else {
                loadingEl.textContent = `Ошибка: ${data.message || 'Не удалось загрузить данные.'}`;
            }

        } catch (error) {
            console.error('Error loading dashboard data:', error);
            loadingEl.textContent = 'Ошибка сети при загрузке данных.';
        }
    }

    loadDashboardData();
});
