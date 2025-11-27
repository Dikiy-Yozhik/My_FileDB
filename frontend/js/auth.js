class AuthManager {
    constructor() {
        this.apiBase = 'http://localhost:8080';
        this.init();
    }

    init() {
        this.bindEvents();
        this.checkExistingAuth();
    }

    bindEvents() {
        const loginForm = document.getElementById('login-form');
        if (loginForm) {
            loginForm.addEventListener('submit', (e) => this.handleLogin(e));
        }

        // Демо-логин по выбору роли
        const roleSelect = document.getElementById('role');
        if (roleSelect) {
            roleSelect.addEventListener('change', (e) => this.fillDemoCredentials(e.target.value));
        }
    }

    fillDemoCredentials(role) {
        const credentials = {
            'guest': { username: 'guest', password: 'guest123' },
            'operator': { username: 'operator', password: 'operator123' },
            'admin': { username: 'admin', password: 'admin123' }
        };

        const creds = credentials[role];
        if (creds) {
            document.getElementById('username').value = creds.username;
            document.getElementById('password').value = creds.password;
        }
    }

    async handleLogin(e) {
        e.preventDefault();
        
        const username = document.getElementById('username').value;
        const password = document.getElementById('password').value;
        const statusElement = document.getElementById('auth-status');

        if (!username || !password) {
            this.showStatus('Заполните все поля', 'error', statusElement);
            return;
        }

        try {
            this.showStatus('Вход...', 'info', statusElement);
            
            const response = await fetch(`${this.apiBase}/auth/login`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({ username, password }),
                credentials: 'include'
            });

            const data = await response.json();

            if (data.success) {
                this.showStatus('Успешный вход! Перенаправление...', 'success', statusElement);
                setTimeout(() => {
                    window.location.href = 'index.html';
                }, 1000);
            } else {
                this.showStatus(data.message || 'Ошибка входа', 'error', statusElement);
            }
        } catch (error) {
            console.error('Login error:', error);
            this.showStatus('Ошибка соединения с сервером', 'error', statusElement);
        }
    }

    showStatus(message, type, element) {
        if (!element) return;
        
        element.textContent = message;
        element.className = `status-message ${type}`;
        element.classList.remove('hidden');
    }

    async checkExistingAuth() {
        try {
            const response = await fetch(`${this.apiBase}/auth/status`, {
                credentials: 'include'
            });
            
            const data = await response.json();
            if (data.success && data.data.authenticated) {
                // Если уже авторизован, перенаправляем на главную
                window.location.href = 'index.html';
            }
        } catch (error) {
            // Игнорируем ошибки при проверке статуса
        }
    }
}

// Инициализация при загрузке страницы
document.addEventListener('DOMContentLoaded', () => {
    new AuthManager();
});