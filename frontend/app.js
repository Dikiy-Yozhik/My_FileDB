class SinglePageApp {
    constructor() {
        this.apiBase = 'http://localhost:8080';
        this.currentUser = null;
        this.currentToken = null;
        this.currentDatabase = null;
        this.employees = [];
        
        this.init();
    }

    init() {
        this.setupEventListeners();
        this.checkExistingAuth();
    }

    setupEventListeners() {
        // Логин
        document.getElementById('login-form').addEventListener('submit', (e) => this.handleLogin(e));
        
        // Выход
        document.getElementById('logout-btn').addEventListener('click', () => this.logout());
        
        // Управление БД
        document.getElementById('create-db').addEventListener('click', () => this.createDatabase());
        document.getElementById('load-db').addEventListener('click', () => this.loadDatabase());
        document.getElementById('backup-db').addEventListener('click', () => this.createBackup());
        document.getElementById('export-excel').addEventListener('click', () => this.exportToExcel());
        document.getElementById('clear-db').addEventListener('click', () => this.clearDatabase());
        
        // Сотрудники
        document.getElementById('add-employee').addEventListener('click', () => this.showEmployeeForm());
        document.getElementById('search-btn').addEventListener('click', () => this.searchEmployees());
        document.getElementById('clear-search').addEventListener('click', () => this.clearSearch());
        
        // Форма
        document.getElementById('employee-data-form').addEventListener('submit', (e) => this.saveEmployee(e));
        document.getElementById('cancel-form').addEventListener('click', () => this.hideEmployeeForm());
    }

    async checkExistingAuth() {
        const savedToken = localStorage.getItem('authToken');
        const savedUser = localStorage.getItem('userData');
        
        if (savedToken && savedUser) {
            try {
                this.currentToken = savedToken;
                this.currentUser = JSON.parse(savedUser);
                console.log('✅ Restored auth from localStorage');
                this.showApp();
                await this.validateToken();
            } catch (e) {
                console.error('❌ Error restoring auth:', e);
                this.clearAuth();
                this.showLogin();
            }
        } else {
            this.showLogin();
        }
    }

    async validateToken() {
        try {
            const response = await this.fetchAPI('/auth/status');
            if (response.success && response.data.authenticated) {
                console.log('✅ Token is valid');
                return true;
            } else {
                console.log('❌ Token expired');
                this.clearAuth();
                this.showLogin();
                return false;
            }
        } catch (error) {
            console.error('💥 Token validation error:', error);
            return true; // В случае ошибки сети оставляем локальную авторизацию
        }
    }

    async handleLogin(e) {
        e.preventDefault();
        
        const username = document.getElementById('username').value;
        const password = document.getElementById('password').value;

        if (!username || !password) {
            this.showLoginStatus('Заполните все поля', 'error');
            return;
        }

        try {
            this.showLoginStatus('Вход...', 'info');
            
            const response = await this.fetchAPI('/auth/login', 'POST', { 
                username, 
                password 
            }, false);

            console.log('📊 Login response:', response);

            if (response.success) {
                if (response.token) {
                    this.currentToken = response.token;
                    this.currentUser = response.data;
                    
                    localStorage.setItem('authToken', this.currentToken);
                    localStorage.setItem('userData', JSON.stringify(this.currentUser));
                    
                    console.log('🔑 Token saved:', this.currentToken);
                    this.showLoginStatus('Успешный вход!', 'success');
                } else {
                    console.error('❌ NO TOKEN IN RESPONSE!');
                    this.showLoginStatus('Ошибка: токен не получен', 'error');
                    return;
                }
                
                setTimeout(() => this.showApp(), 500);
            } else {
                this.showLoginStatus(response.message || 'Ошибка входа', 'error');
            }
        } catch (error) {
            this.showLoginStatus('Ошибка соединения с сервером', 'error');
            console.error('Login error:', error);
        }
    }

    showLogin() {
        document.getElementById('login-section').classList.remove('hidden');
        document.getElementById('app-section').classList.add('hidden');
    }

    showApp() {
        document.getElementById('login-section').classList.add('hidden');
        document.getElementById('app-section').classList.remove('hidden');
        this.updateUserInfo();
        this.enableControls(true);
        this.showStatus(`Добро пожаловать, ${this.currentUser.username}!`, 'success');
        this.loadEmployees();
    }

    showLoginStatus(message, type) {
        const element = document.getElementById('login-status');
        element.textContent = message;
        element.className = `status ${type} fade-in`;
    }

    showStatus(message, type = 'info') {
        const element = document.getElementById('status-message');
        element.textContent = message;
        element.className = `status ${type} fade-in`;
        
        setTimeout(() => {
            if (element.textContent === message) {
                element.textContent = '';
                element.className = 'status';
            }
        }, 5000);
    }

    updateUserInfo() {
        const element = document.getElementById('user-info');
        if (element && this.currentUser) {
            element.textContent = `${this.currentUser.username} (${this.currentUser.role})`;
        }
    }

    enableControls(enabled) {
        const controls = ['add-employee', 'backup-db', 'export-excel', 'clear-db'];
        controls.forEach(id => {
            const element = document.getElementById(id);
            if (element) element.disabled = !enabled;
        });
        
        const logoutBtn = document.getElementById('logout-btn');
        if (logoutBtn) logoutBtn.style.display = enabled ? 'inline-block' : 'none';
    }

    async logout() {
        try {
            await this.fetchAPI('/auth/logout', 'POST');
        } catch (error) {
            // Игнорируем ошибки
        }
        
        this.clearAuth();
        this.showLogin();
        this.showLoginStatus('Вы вышли из системы', 'info');
    }

    clearAuth() {
        this.currentUser = null;
        this.currentToken = null;
        localStorage.removeItem('authToken');
        localStorage.removeItem('userData');
    }

    async createDatabase() {
        const path = document.getElementById('db-path').value || 'data/mydatabase';
        
        this.showStatus('Создание базы данных...', 'info');
        try {
            const result = await this.fetchAPI('/database/create', 'POST', { 
                databasePath: path
            });
            
            console.log('📊 Create DB result:', result);
            
            if (result.success) {
                this.currentDatabase = path;
                this.showStatus('База данных создана успешно!', 'success');
                await this.loadEmployees();
            } else {
                this.showStatus(`Ошибка: ${result.message}`, 'error');
            }
        } catch (error) {
            this.showStatus('Ошибка при создании БД', 'error');
            console.error('💥 Create DB error:', error);
        }
    }

    async loadDatabase() {
        const path = document.getElementById('db-path').value || 'data/mydatabase';
        
        this.showStatus('Загрузка базы данных...', 'info');
        try {
            const result = await this.fetchAPI('/database/load', 'POST', { 
                databasePath: path
            });
            
            console.log('📊 Load DB result:', result);
            
            if (result.success) {
                this.currentDatabase = path;
                this.showStatus('База данных загружена успешно!', 'success');
                await this.loadEmployees();
            } else {
                this.showStatus(`Ошибка: ${result.message}`, 'error');
            }
        } catch (error) {
            this.showStatus('Ошибка при загрузке БД', 'error');
            console.error('💥 Load DB error:', error);
        }
    }

    async createBackup() {
        this.showStatus('Создание бэкапа...', 'info');
        try {
            const result = await this.fetchAPI('/backup/create', 'POST');
            
            if (result.success) {
                this.showStatus('Бэкап создан успешно!', 'success');
            } else {
                this.showStatus(`Ошибка: ${result.message}`, 'error');
            }
        } catch (error) {
            this.showStatus('Ошибка при создании бэкапа', 'error');
            console.error('💥 Backup error:', error);
        }
    }

    async exportToExcel() {
        this.showStatus('Экспорт в Excel...', 'info');
        try {
            const result = await this.fetchAPI('/export/excel', 'GET');
            
            if (result.success) {
                this.showStatus('Экспорт завершен успешно!', 'success');
            } else {
                this.showStatus(`Ошибка: ${result.message}`, 'error');
            }
        } catch (error) {
            this.showStatus('Ошибка при экспорте', 'error');
            console.error('💥 Export error:', error);
        }
    }

    async clearDatabase() {
        if (!confirm('Вы уверены, что хотите очистить базу данных? Все данные будут удалены.')) {
            return;
        }

        this.showStatus('Очистка базы данных...', 'info');
        try {
            const result = await this.fetchAPI('/database/clear', 'DELETE');
            
            if (result.success) {
                this.showStatus('База данных очищена!', 'success');
                await this.loadEmployees();
            } else {
                this.showStatus(`Ошибка: ${result.message}`, 'error');
            }
        } catch (error) {
            this.showStatus('Ошибка при очистке БД', 'error');
            console.error('💥 Clear DB error:', error);
        }
    }

    async loadEmployees() {
        try {
            const result = await this.fetchAPI('/employees');
            
            if (result.success) {
                this.employees = result.data || [];
                this.renderEmployees();
            } else {
                console.error('❌ Failed to load employees:', result.message);
            }
        } catch (error) {
            console.error('💥 Load employees error:', error);
        }
    }

    renderEmployees() {
        const tbody = document.getElementById('employees-table');
        const countElement = document.getElementById('record-count');
        
        if (!tbody) return;

        if (this.employees.length === 0) {
            tbody.innerHTML = `
                <tr>
                    <td colspan="7" class="no-data">
                        Нет данных для отображения
                    </td>
                </tr>
            `;
            if (countElement) countElement.textContent = 'Записей: 0';
            return;
        }

        tbody.innerHTML = this.employees.map(emp => `
            <tr class="fade-in">
                <td>${emp.id}</td>
                <td>${emp.name}</td>
                <td>${emp.department}</td>
                <td>${emp.position}</td>
                <td>${emp.salary}</td>
                <td>${emp.hireDate}</td>
                <td>
                    <button class="btn" onclick="app.editEmployee(${emp.id})">✏️</button>
                    <button class="btn btn-danger" onclick="app.deleteEmployee(${emp.id})">🗑️</button>
                </td>
            </tr>
        `).join('');

        if (countElement) countElement.textContent = `Записей: ${this.employees.length}`;
    }

    showEmployeeForm(employee = null) {
        const form = document.getElementById('employee-form');
        const title = document.getElementById('form-title');
        const formElement = document.getElementById('employee-data-form');
        
        if (!form || !title || !formElement) return;

        if (employee) {
            title.textContent = 'Редактирование сотрудника';
            this.fillEmployeeForm(employee);
        } else {
            title.textContent = 'Добавление сотрудника';
            formElement.reset();
        }
        
        form.classList.remove('hidden');
    }

    hideEmployeeForm() {
        const form = document.getElementById('employee-form');
        if (form) {
            form.classList.add('hidden');
        }
    }

    fillEmployeeForm(employee) {
        document.getElementById('employee-id').value = employee.id;
        document.getElementById('employee-name').value = employee.name;
        document.getElementById('employee-department').value = employee.department;
        document.getElementById('employee-position').value = employee.position;
        document.getElementById('employee-salary').value = employee.salary;
        document.getElementById('employee-hireDate').value = employee.hireDate;
    }

    async saveEmployee(event) {
        event.preventDefault();
        
        const employee = {
            id: document.getElementById('employee-id').value || null,
            name: document.getElementById('employee-name').value,
            department: document.getElementById('employee-department').value,
            position: document.getElementById('employee-position').value,
            salary: parseFloat(document.getElementById('employee-salary').value),
            hireDate: document.getElementById('employee-hireDate').value
        };

        try {
            let result;
            if (employee.id) {
                result = await this.fetchAPI(`/employees/${employee.id}`, 'PUT', employee);
            } else {
                result = await this.fetchAPI('/employees', 'POST', employee);
            }

            if (result.success) {
                this.showStatus('Сотрудник сохранен успешно!', 'success');
                this.hideEmployeeForm();
                await this.loadEmployees();
            } else {
                this.showStatus(`Ошибка: ${result.message}`, 'error');
            }
        } catch (error) {
            this.showStatus('Ошибка при сохранении', 'error');
            console.error('💥 Save employee error:', error);
        }
    }

    editEmployee(id) {
        const employee = this.employees.find(emp => emp.id == id);
        if (employee) {
            this.showEmployeeForm(employee);
        }
    }

    async deleteEmployee(id) {
        if (!confirm('Вы уверены, что хотите удалить этого сотрудника?')) {
            return;
        }

        try {
            const result = await this.fetchAPI(`/employees/${id}`, 'DELETE');
            if (result.success) {
                this.showStatus('Сотрудник удален!', 'success');
                await this.loadEmployees();
            } else {
                this.showStatus(`Ошибка: ${result.message}`, 'error');
            }
        } catch (error) {
            this.showStatus('Ошибка при удалении', 'error');
            console.error('💥 Delete error:', error);
        }
    }

    async searchEmployees() {
        const name = document.getElementById('search-input').value;
        const department = document.getElementById('department-filter').value;
        
        try {
            let url = '/employees/search?';
            if (name) url += `name=${encodeURIComponent(name)}&`;
            if (department) url += `department=${encodeURIComponent(department)}`;
            
            const result = await this.fetchAPI(url);
            if (result.success) {
                this.employees = result.data || [];
                this.renderEmployees();
                this.showStatus(`Найдено записей: ${this.employees.length}`, 'info');
            }
        } catch (error) {
            console.error('💥 Search error:', error);
        }
    }

    clearSearch() {
        document.getElementById('search-input').value = '';
        document.getElementById('department-filter').value = '';
        this.loadEmployees();
    }

    async fetchAPI(endpoint, method = 'GET', data = null, useToken = true) {
        const options = {
            method,
            headers: { 
                'Content-Type': 'application/json',
            }
        };

        if (useToken && this.currentToken) {
            options.headers['Authorization'] = `Bearer ${this.currentToken}`;
        }

        if (data && (method === 'POST' || method === 'PUT')) {
            options.body = JSON.stringify(data);
        }

        console.log(`🌐 API Call: ${method} ${endpoint}`, options);

        try {
            const response = await fetch(`${this.apiBase}${endpoint}`, options);
            const result = await response.json();
            console.log(`📨 API Response:`, result);
            return result;
        } catch (error) {
            console.error(`💥 API Error (${endpoint}):`, error);
            throw error;
        }
    }
}

// Инициализация приложения
let app;
document.addEventListener('DOMContentLoaded', () => {
    console.log('📄 DOM loaded, initializing app...');
    app = new SinglePageApp();
    window.app = app;
});