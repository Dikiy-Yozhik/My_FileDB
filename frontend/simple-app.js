class SimpleFileDBApp {
    constructor() {
        console.log('🚀 SimpleFileDBApp starting...');
        this.apiBase = 'http://localhost:8080';
        this.currentUser = null;
        this.currentDatabase = null;
        this.employees = [];
        
        this.init();
    }

    async init() {
        console.log('🎨 Setting up event listeners...');
        this.setupEventListeners();
        
        // 🔥 ПРОВЕРЯЕМ LOCALSTORAGE ВМЕСТО СЕРВЕРА
        this.checkLocalStorageAuth();
        
        console.log('✅ App initialized');
    }

    // 🔥 НОВЫЙ МЕТОД: проверка авторизации из localStorage
    checkLocalStorageAuth() {
        const isAuthenticated = localStorage.getItem('isAuthenticated');
        const userSession = localStorage.getItem('userSession');
        
        if (isAuthenticated === 'true' && userSession) {
            try {
                this.currentUser = JSON.parse(userSession);
                console.log('✅ User authenticated from localStorage:', this.currentUser);
                this.updateUserInfo();
                this.enableControls(true);
                this.showStatus(`Добро пожаловать, ${this.currentUser.username}!`, 'success');
                
                // 🔥 СИНХРОНИЗИРУЕМ С СЕРВЕРОМ (проверяем, что сессия жива)
                this.syncWithServer();
                return;
            } catch (e) {
                console.error('❌ Error parsing user session:', e);
            }
        }
        
        // Если нет авторизации в localStorage
        console.log('❌ No auth in localStorage');
        this.currentUser = null;
        this.updateUserInfo();
        this.enableControls(false);
        this.showStatus('Пожалуйста, войдите в систему', 'info');
    }

    // 🔥 НОВЫЙ МЕТОД: синхронизация с сервером
    async syncWithServer() {
        try {
            const response = await this.fetchAPI('/auth/status');
            if (response.success && response.data.authenticated) {
                // Сессия на сервере жива, обновляем данные
                this.currentUser = response.data;
                localStorage.setItem('userSession', JSON.stringify(response.data));
                console.log('✅ Server session synced');
            } else {
                // Сессия на сервере умерла, разлогиниваемся
                console.log('❌ Server session expired');
                this.logout();
            }
        } catch (error) {
            console.error('💥 Sync error:', error);
            // Игнорируем ошибки сети, сохраняем локальную авторизацию
        }
    }

    // 🔥 ОБНОВЛЯЕМ logout для очистки localStorage
    async logout() {
        try {
            await this.fetchAPI('/auth/logout', 'POST');
        } catch (error) {
            // Игнорируем ошибки
        }
        
        // 🔥 ОЧИЩАЕМ LOCALSTORAGE
        localStorage.removeItem('userSession');
        localStorage.removeItem('isAuthenticated');
        
        this.currentUser = null;
        this.updateUserInfo();
        this.enableControls(false);
        this.showStatus('Вы вышли из системы', 'info');
        
        // 🔥 ПЕРЕНАПРАВЛЯЕМ НА СТРАНИЦУ ВХОДА
        setTimeout(() => {
            window.location.href = 'login.html';
        }, 1000);
    }

    // 🔥 ОБНОВЛЯЕМ checkAuth - используем localStorage
    async checkAuth() {
        // Теперь этот метод используется только для проверки серверной сессии
        try {
            const response = await this.fetchAPI('/auth/status');
            return response.success && response.data.authenticated;
        } catch (error) {
            return false;
        }
    }

    // Остальные методы без изменений...
    updateUserInfo() {
        const userInfoElement = document.getElementById('user-info');
        if (userInfoElement) {
            if (this.currentUser) {
                userInfoElement.textContent = `${this.currentUser.username} (${this.currentUser.role})`;
            } else {
                userInfoElement.textContent = 'Не авторизован';
            }
        }
    }

    enableControls(enabled) {
        const controls = ['create-db', 'load-db', 'add-employee', 'backup-db', 'export-excel', 'clear-db'];
        controls.forEach(id => {
            const element = document.getElementById(id);
            if (element) element.disabled = !enabled;
        });
        
        const logoutBtn = document.getElementById('logout-btn');
        if (logoutBtn) logoutBtn.style.display = enabled ? 'inline-block' : 'none';
    }
    

    showStatus(message, type = 'info') {
        const statusElement = document.getElementById('status-message');
        if (statusElement) {
            statusElement.textContent = message;
            statusElement.className = `status ${type}`;
            
            // Автоочистка через 5 секунд
            setTimeout(() => {
                if (statusElement.textContent === message) {
                    statusElement.textContent = '';
                    statusElement.className = 'status';
                }
            }, 5000);
        }
    }

    

    setupEventListeners() {
        console.log('🔗 Setting up event listeners...');
        
        // Кнопка входа
        const loginBtn = document.getElementById('login-btn');
        if (loginBtn) {
            loginBtn.addEventListener('click', () => {
                console.log('🎯 Login button clicked!');
                this.showLoginForm();
            });
        }
        
        // Кнопки управления БД
        const createDbBtn = document.getElementById('create-db');
        const loadDbBtn = document.getElementById('load-db');
        
        if (createDbBtn) {
            createDbBtn.addEventListener('click', () => {
                if (!this.checkAuth()) return;
                console.log('🎯 Create DB button clicked!');
                this.createDatabase();
            });
        }
        
        if (loadDbBtn) {
            loadDbBtn.addEventListener('click', () => {
                if (!this.checkAuth()) return;
                console.log('🎯 Load DB button clicked!');
                this.loadDatabase();
            });
        }
        
        // Кнопка выхода
        const logoutBtn = document.getElementById('logout-btn');
        if (logoutBtn) {
            logoutBtn.addEventListener('click', () => {
                console.log('🎯 Logout button clicked!');
                this.logout();
            });
        }
        
        // Кнопка добавления сотрудника
        const addEmployeeBtn = document.getElementById('add-employee');
        if (addEmployeeBtn) {
            addEmployeeBtn.addEventListener('click', () => {
                console.log('🎯 Add employee button clicked!');
                this.showEmployeeForm();
            });
        }
        
        // Поиск
        const searchBtn = document.getElementById('search-btn');
        if (searchBtn) {
            searchBtn.addEventListener('click', () => {
                console.log('🎯 Search button clicked!');
                this.searchEmployees();
            });
        }
        
        const clearSearchBtn = document.getElementById('clear-search');
        if (clearSearchBtn) {
            clearSearchBtn.addEventListener('click', () => {
                console.log('🎯 Clear search button clicked!');
                this.clearSearch();
            });
        }
        
        // Форма
        const employeeForm = document.getElementById('employee-data-form');
        if (employeeForm) {
            employeeForm.addEventListener('submit', (e) => {
                console.log('🎯 Employee form submitted!');
                this.saveEmployee(e);
            });
        }
        
        const cancelFormBtn = document.getElementById('cancel-form');
        if (cancelFormBtn) {
            cancelFormBtn.addEventListener('click', () => {
                console.log('🎯 Cancel form button clicked!');
                this.hideEmployeeForm();
            });
        }
        
        console.log('✅ All event listeners setup');
    }

    async createDatabase() {
        console.log('📁 Creating database...');
        const path = document.getElementById('db-path').value || 'data/mydatabase';
        
        this.showStatus('Создание базы данных...', 'info');
        try {
            console.log('📤 Sending POST /database/create with path:', path);
            const result = await this.fetchAPI('/database/create', 'POST', { path });
            console.log('📊 Create DB result:', result);
            
            if (result.success) {
                this.currentDatabase = path;
                this.showStatus('База данных создана успешно!', 'success');
                this.enableControls(true);
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
        console.log('📂 Loading database...');
        const path = document.getElementById('db-path').value || 'data/mydatabase';
        
        this.showStatus('Загрузка базы данных...', 'info');
        try {
            console.log('📤 Sending POST /database/load with path:', path);
            const result = await this.fetchAPI('/database/load', 'POST', { path });
            console.log('📊 Load DB result:', result);
            
            if (result.success) {
                this.currentDatabase = path;
                this.showStatus('База данных загружена успешно!', 'success');
                this.enableControls(true);
                await this.loadEmployees();
            } else {
                this.showStatus(`Ошибка: ${result.message}`, 'error');
            }
        } catch (error) {
            this.showStatus('Ошибка при загрузке БД', 'error');
            console.error('💥 Load DB error:', error);
        }
    }

    async loadEmployees() {
        console.log('👥 Loading employees...');
        try {
            const result = await this.fetchAPI('/employees');
            console.log('📊 Employees result:', result);
            
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
        
        if (!tbody) {
            console.error('❌ Employees table not found!');
            return;
        }

        if (this.employees.length === 0) {
            tbody.innerHTML = `
                <tr>
                    <td colspan="7" style="text-align: center; padding: 2rem;">
                        Нет данных для отображения
                    </td>
                </tr>
            `;
            if (countElement) countElement.textContent = 'Записей: 0';
            return;
        }

        tbody.innerHTML = this.employees.map(emp => `
            <tr>
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
        
        if (!form || !title || !formElement) {
            console.error('❌ Employee form elements not found!');
            return;
        }

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
                // Редактирование
                result = await this.fetchAPI(`/employees/${employee.id}`, 'PUT', employee);
            } else {
                // Добавление
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
    

    // 🔥 НОВЫЙ МЕТОД: Показать форму входа
    showLoginForm() {
        const username = prompt('Введите логин:', 'admin');
        if (!username) return;
        
        const password = prompt('Введите пароль:', 'admin123');
        if (!password) return;
        
        this.login(username, password);
    }

    // 🔥 НОВЫЙ МЕТОД: Ручной вход
    async login(username, password) {
        this.showStatus('Вход...', 'info');
        try {
            const result = await this.fetchAPI('/auth/login', 'POST', {
                username: username,
                password: password
            });
            
            console.log('📊 Login result:', result);
            
            if (result.success) {
                this.currentUser = result.data;
                this.updateUserInfo();
                this.showStatus(`Успешный вход! Добро пожаловать, ${result.data.username}`, 'success');
                this.enableControls(true);
            } else {
                this.showStatus(`Ошибка входа: ${result.message}`, 'error');
            }
        } catch (error) {
            this.showStatus('Ошибка при входе', 'error');
            console.error('💥 Login error:', error);
        }
    }


    updateUserInfo() {
        const userInfoElement = document.getElementById('user-info');
        if (userInfoElement) {
            if (this.currentUser) {
                userInfoElement.textContent = `${this.currentUser.username} (${this.currentUser.role})`;
            } else {
                userInfoElement.textContent = 'Не авторизован';
            }
        }
    }

    enableControls(enabled) {
        const controls = ['create-db', 'load-db', 'add-employee', 'backup-db', 'export-excel', 'clear-db'];
        controls.forEach(id => {
            const element = document.getElementById(id);
            if (element) element.disabled = !enabled;
        });
    }


    async fetchAPI(endpoint, method = 'GET', data = null) {
        console.log(`🌐 API Call: ${method} ${endpoint}`, data);
        
        const options = {
            method,
            headers: {
                'Content-Type': 'application/json',
            },
            credentials: 'include'
        };

        if (data && (method === 'POST' || method === 'PUT')) {
            options.body = JSON.stringify(data);
        }

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
    app = new SimpleFileDBApp();
    window.app = app;
});