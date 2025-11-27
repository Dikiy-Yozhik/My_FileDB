const EmployeesPage = {
    currentFilters: {},
    employees: [],

    render() {
        return `
            <div class="page-container">
                <div class="page-header">
                    <h2>Управление сотрудниками</h2>
                    <button id="add-employee" class="btn btn-success">➕ Добавить сотрудника</button>
                </div>

                <!-- Панель поиска и фильтров -->
                <div class="controls-panel">
                    <div class="search-controls">
                        <input type="text" id="search-input" placeholder="Поиск по ФИО..." class="search-input">
                        <select id="department-filter" class="department-select">
                            <option value="">Все отделы</option>
                            <option value="IT">IT</option>
                            <option value="HR">HR</option>
                            <option value="Finance">Finance</option>
                            <option value="Marketing">Marketing</option>
                        </select>
                        <button id="search-btn" class="btn btn-search">🔍 Найти</button>
                        <button id="clear-search" class="btn btn-clear">Очистить</button>
                    </div>
                    <div class="action-controls">
                        <button id="refresh-data" class="btn btn-info">🔄 Обновить</button>
                    </div>
                </div>

                <!-- Таблица сотрудников -->
                <div class="table-section">
                    <div class="table-header">
                        <h3>Сотрудники</h3>
                        <span id="record-count" class="record-count">Записей: 0</span>
                    </div>
                    <div class="table-container">
                        <table class="employees-table">
                            <thead>
                                <tr>
                                    <th>ID</th>
                                    <th>ФИО</th>
                                    <th>Отдел</th>
                                    <th>Должность</th>
                                    <th>Зарплата</th>
                                    <th>Дата приема</th>
                                    <th>Действия</th>
                                </tr>
                            </thead>
                            <tbody id="employees-tbody">
                                <!-- Данные будут загружены через JavaScript -->
                            </tbody>
                        </table>
                    </div>
                </div>

                <!-- Форма сотрудника -->
                <div id="employee-form-section" class="form-section hidden">
                    <div class="form-container">
                        <h3 id="form-title">Добавление сотрудника</h3>
                        <form id="employee-form" class="employee-form">
                            <div class="form-row">
                                <div class="form-group">
                                    <label for="employee-id">ID *</label>
                                    <input type="number" id="employee-id" name="id" required min="1">
                                </div>
                                <div class="form-group">
                                    <label for="employee-salary">Зарплата *</label>
                                    <input type="number" id="employee-salary" name="salary" required min="0" step="0.01">
                                </div>
                            </div>
                            <div class="form-group">
                                <label for="employee-name">ФИО *</label>
                                <input type="text" id="employee-name" name="name" required maxlength="100">
                            </div>
                            <div class="form-row">
                                <div class="form-group">
                                    <label for="employee-department">Отдел *</label>
                                    <input type="text" id="employee-department" name="department" required maxlength="50">
                                </div>
                                <div class="form-group">
                                    <label for="employee-position">Должность *</label>
                                    <input type="text" id="employee-position" name="position" required maxlength="50">
                                </div>
                            </div>
                            <div class="form-group">
                                <label for="employee-hireDate">Дата приема *</label>
                                <input type="date" id="employee-hireDate" name="hireDate" required>
                            </div>
                            <div class="form-actions">
                                <button type="submit" class="btn btn-success">Сохранить</button>
                                <button type="button" id="cancel-form" class="btn btn-secondary">Отмена</button>
                            </div>
                        </form>
                    </div>
                </div>
            </div>
        `;
    },

    init() {
        this.bindEvents();
        this.loadEmployees();
        this.checkPermissions();
    },

    bindEvents() {
        // Поиск и фильтрация
        document.getElementById('search-btn')?.addEventListener('click', () => this.searchEmployees());
        document.getElementById('clear-search')?.addEventListener('click', () => this.clearSearch());
        document.getElementById('refresh-data')?.addEventListener('click', () => this.loadEmployees());
        
        // Быстрый поиск
        const searchInput = document.getElementById('search-input');
        if (searchInput) {
            searchInput.addEventListener('input', Helpers.debounce(() => this.searchEmployees(), 300));
        }

        // Форма сотрудника
        document.getElementById('add-employee')?.addEventListener('click', () => this.showEmployeeForm());
        document.getElementById('employee-form')?.addEventListener('submit', (e) => this.handleFormSubmit(e));
        document.getElementById('cancel-form')?.addEventListener('click', () => this.hideEmployeeForm());
    },

    checkPermissions() {
        const canModify = app.currentUser && 
                         (app.currentUser.role === 'ADMIN' || app.currentUser.role === 'OPERATOR');
        
        const addButton = document.getElementById('add-employee');
        if (addButton) {
            addButton.disabled = !canModify;
            if (!canModify) {
                addButton.title = 'Недостаточно прав для добавления сотрудников';
            }
        }
    },

    async loadEmployees() {
        if (!app.api) return;

        try {
            app.ui.showLoading();
            const response = await app.api.getEmployees();
            
            if (response.success) {
                this.employees = response.data || [];
                this.renderEmployeesTable();
                app.ui.showStatus(`Загружено ${this.employees.length} сотрудников`, 'success');
            } else {
                this.employees = [];
                this.renderEmployeesTable();
            }
        } catch (error) {
            this.employees = [];
            this.renderEmployeesTable();
            console.error('Error loading employees:', error);
        } finally {
            app.ui.hideLoading();
        }
    },

    async searchEmployees() {
        if (!app.api) return;

        const searchText = document.getElementById('search-input')?.value || '';
        const department = document.getElementById('department-filter')?.value || '';

        this.currentFilters = {};
        if (searchText) this.currentFilters.name = searchText;
        if (department) this.currentFilters.department = department;

        try {
            app.ui.showLoading();
            const response = await app.api.searchEmployees(this.currentFilters);
            
            if (response.success) {
                this.employees = response.data || [];
                this.renderEmployeesTable();
                app.ui.showStatus(`Найдено ${this.employees.length} сотрудников`, 'success');
            }
        } catch (error) {
            console.error('Error searching employees:', error);
        } finally {
            app.ui.hideLoading();
        }
    },

    clearSearch() {
        const searchInput = document.getElementById('search-input');
        const departmentFilter = document.getElementById('department-filter');
        
        if (searchInput) searchInput.value = '';
        if (departmentFilter) departmentFilter.value = '';
        
        this.currentFilters = {};
        this.loadEmployees();
    },

    renderEmployeesTable() {
        const tbody = document.getElementById('employees-tbody');
        const recordCount = document.getElementById('record-count');
        
        if (!tbody || !recordCount) return;

        if (this.employees.length === 0) {
            tbody.innerHTML = `
                <tr>
                    <td colspan="7" style="text-align: center; color: #666;">
                        ${this.currentFilters.name || this.currentFilters.department ? 'По вашему запросу ничего не найдено' : 'Нет данных для отображения'}
                    </td>
                </tr>
            `;
            recordCount.textContent = 'Записей: 0';
            return;
        }

        const canModify = app.currentUser && 
                         (app.currentUser.role === 'ADMIN' || app.currentUser.role === 'OPERATOR');

        tbody.innerHTML = this.employees.map(employee => `
            <tr>
                <td>${employee.id}</td>
                <td>${Helpers.escapeHtml(employee.name)}</td>
                <td>${Helpers.escapeHtml(employee.department)}</td>
                <td>${Helpers.escapeHtml(employee.position)}</td>
                <td>${employee.salary?.toFixed(2) || '0.00'}</td>
                <td>${employee.hireDate}</td>
                <td class="actions-cell">
                    ${canModify ? `
                        <button class="action-btn edit-btn" onclick="EmployeesPage.editEmployee(${employee.id})">✏️</button>
                        <button class="action-btn delete-btn" onclick="EmployeesPage.deleteEmployee(${employee.id})">🗑️</button>
                    ` : `
                        <span class="no-actions">Только просмотр</span>
                    `}
                </td>
            </tr>
        `).join('');

        recordCount.textContent = `Записей: ${this.employees.length}`;
    },

    showEmployeeForm(employee = null) {
        if (!app.currentUser || (app.currentUser.role !== 'ADMIN' && app.currentUser.role !== 'OPERATOR')) {
            app.ui.showStatus('Недостаточно прав для редактирования сотрудников', 'error');
            return;
        }

        this.editingEmployee = employee;
        const formSection = document.getElementById('employee-form-section');
        const formTitle = document.getElementById('form-title');
        const form = document.getElementById('employee-form');

        if (!formSection || !formTitle || !form) return;

        if (employee) {
            formTitle.textContent = 'Редактирование сотрудника';
            this.populateForm(employee);
            document.getElementById('employee-id').disabled = true;
        } else {
            formTitle.textContent = 'Добавление сотрудника';
            form.reset();
            document.getElementById('employee-id').disabled = false;
        }

        formSection.classList.remove('hidden');
    },

    hideEmployeeForm() {
        const formSection = document.getElementById('employee-form-section');
        if (formSection) {
            formSection.classList.add('hidden');
        }
        this.editingEmployee = null;
    },

    populateForm(employee) {
        const setValue = (id, value) => {
            const element = document.getElementById(id);
            if (element) element.value = value;
        };

        setValue('employee-id', employee.id);
        setValue('employee-name', employee.name);
        setValue('employee-department', employee.department);
        setValue('employee-position', employee.position);
        setValue('employee-salary', employee.salary);
        setValue('employee-hireDate', employee.hireDate);
    },

    async handleFormSubmit(e) {
        e.preventDefault();
        
        const formData = new FormData(e.target);
        const employeeData = {
            id: parseInt(formData.get('id')),
            name: formData.get('name'),
            department: formData.get('department'),
            position: formData.get('position'),
            salary: parseFloat(formData.get('salary')),
            hireDate: formData.get('hireDate')
        };

        // Простая валидация
        if (!this.validateEmployeeData(employeeData)) {
            return;
        }

        try {
            if (this.editingEmployee) {
                await app.api.updateEmployee(this.editingEmployee.id, employeeData);
                app.ui.showStatus('Сотрудник обновлен', 'success');
            } else {
                await app.api.createEmployee(employeeData);
                app.ui.showStatus('Сотрудник добавлен', 'success');
            }
            
            this.hideEmployeeForm();
            await this.loadEmployees();
        } catch (error) {
            // Ошибка обрабатывается в API
        }
    },

    validateEmployeeData(data) {
        if (!data.id || data.id <= 0) {
            app.ui.showStatus('ID должен быть положительным числом', 'error');
            return false;
        }
        if (!data.name || data.name.trim().length === 0) {
            app.ui.showStatus('ФИО обязательно для заполнения', 'error');
            return false;
        }
        if (!data.department || data.department.trim().length === 0) {
            app.ui.showStatus('Отдел обязателен для заполнения', 'error');
            return false;
        }
        if (!data.position || data.position.trim().length === 0) {
            app.ui.showStatus('Должность обязательна для заполнения', 'error');
            return false;
        }
        if (!data.salary || data.salary < 0) {
            app.ui.showStatus('Зарплата должна быть неотрицательным числом', 'error');
            return false;
        }
        if (!data.hireDate) {
            app.ui.showStatus('Дата приема обязательна для заполнения', 'error');
            return false;
        }
        return true;
    },

    editEmployee(id) {
        const employee = this.employees.find(emp => emp.id === id);
        if (employee) {
            this.showEmployeeForm(employee);
        }
    },

    async deleteEmployee(id) {
        if (!app.currentUser || (app.currentUser.role !== 'ADMIN' && app.currentUser.role !== 'OPERATOR')) {
            app.ui.showStatus('Недостаточно прав для удаления сотрудников', 'error');
            return;
        }

        if (!confirm('Вы уверены, что хотите удалить этого сотрудника?')) {
            return;
        }

        try {
            await app.api.deleteEmployee(id);
            app.ui.showStatus('Сотрудник удален', 'success');
            await this.loadEmployees();
        } catch (error) {
            // Ошибка обрабатывается в API
        }
    },

    onEnter() {
        console.log('Employees page entered');
    },

    onLeave() {
        console.log('Employees page left');
    }
};