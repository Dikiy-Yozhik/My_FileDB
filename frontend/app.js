class FileDBApp {
    constructor() {
        this.api = new ApiClient();
        this.ui = new UIManager();
        this.table = new TableRenderer(
            document.getElementById('employees-tbody'),
            document.getElementById('record-count')
        );
        this.form = new FormHandler();
        
        this.currentDatabase = null;
        this.employees = [];
        this.editingEmployee = null;
        
        this.initialize();
    }

    initialize() {
        this.bindEvents();
        this.table.bindRowEvents(
            (id) => this.editEmployee(id),
            (id) => this.deleteEmployee(id)
        );
    }

    bindEvents() {
        // Database events
        this.ui.on('createDatabaseClick', () => this.showDatabaseModal('create'));
        this.ui.on('loadDatabaseClick', () => this.showDatabaseModal('load'));
        this.ui.on('backupClick', () => this.createBackup());
        this.ui.on('exportExcelClick', () => this.exportToExcel());
        
        // Data events
        this.ui.on('addEmployeeClick', () => this.showEmployeeForm());
        this.ui.on('refreshClick', () => this.loadEmployees());
        this.ui.on('searchClick', () => this.searchEmployees());
        this.ui.on('clearSearchClick', () => this.clearSearch());
        this.ui.on('searchInput', Helpers.debounce(() => this.searchEmployees(), 300));
        
        // Form events
        this.ui.on('formSubmit', (formData) => this.handleFormSubmit(formData));
        this.ui.on('formCancel', () => this.hideEmployeeForm());
        
        // Modal events
        this.ui.on('modalConfirm', () => this.handleDatabaseAction());
        this.ui.on('modalCancel', () => this.ui.hideDatabaseModal());
    }

    // Database methods
    async createDatabase(filePath) {
        try {
            this.ui.showLoading();
            await this.api.createDatabase(filePath);
            await this.loadDatabase(filePath);
            this.ui.showStatus('База данных создана успешно', 'success');
        } catch (error) {
            this.ui.showStatus(`Ошибка создания БД: ${error.message}`, 'error');
            throw error;
        } finally {
            this.ui.hideLoading();
        }
    }

    async loadDatabase(filePath) {
        try {
            this.ui.showLoading();
            await this.api.loadDatabase(filePath);
            this.currentDatabase = filePath;
            this.ui.updateDatabaseStatus(filePath);
            await this.loadEmployees();
            this.ui.showStatus('База данных загружена', 'success');
        } catch (error) {
            this.ui.showStatus(`Ошибка загрузки БД: ${error.message}`, 'error');
            throw error;
        } finally {
            this.ui.hideLoading();
        }
    }

    async createBackup() {
        if (!this.currentDatabase) {
            this.ui.showStatus('Сначала загрузите БД', 'error');
            return;
        }

        try {
            await this.api.backupDatabase();
            this.ui.showStatus('Backup создан успешно', 'success');
        } catch (error) {
            this.ui.showStatus(`Ошибка создания backup: ${error.message}`, 'error');
        }
    }

    exportToExcel() {
        if (!this.currentDatabase) {
            this.ui.showStatus('Сначала загрузите БД', 'error');
            return;
        }
        this.api.exportToExcel();
    }

    // Employee methods
    async loadEmployees() {
        if (!this.currentDatabase) return;

        try {
            this.ui.showLoading();
            const response = await this.api.getEmployees(); // Используем this.api
            
            this.employees = response.data || [];
            this.table.render(this.employees, true);
        } catch (error) {
            this.ui.showStatus(`Ошибка загрузки сотрудников: ${error.message}`, 'error');
            this.employees = [];
            this.table.render([], true);
        } finally {
            this.ui.hideLoading();
        }
    }

    async searchEmployees() {
        if (!this.currentDatabase) return;

        try {
            this.ui.showLoading();
            const filters = this.ui.getSearchFilters();
            const response = await this.api.searchEmployees(filters); // Используем this.api
            
            this.employees = response.data || [];
            this.table.render(this.employees, true);
        } catch (error) {
            this.ui.showStatus(`Ошибка поиска: ${error.message}`, 'error');
            this.employees = [];
            this.table.render([], true);
        } finally {
            this.ui.hideLoading();
        }
    }

    clearSearch() {
        this.ui.clearSearch();
        this.loadEmployees();
    }

    async addEmployee(employeeData) {
        await this.api.createEmployee(employeeData);
        await this.loadEmployees();
        this.ui.showStatus('Сотрудник добавлен', 'success');
    }

    async updateEmployee(id, employeeData) {
        await this.api.updateEmployee(id, employeeData);
        await this.loadEmployees();
        this.ui.showStatus('Данные сотрудника обновлены', 'success');
    }

    async deleteEmployee(id) {
        if (!Helpers.confirmAction('Вы уверены, что хотите удалить этого сотрудника?')) {
            return;
        }

        try {
            await this.api.deleteEmployee(id);
            await this.loadEmployees();
            this.ui.showStatus('Сотрудник удален', 'success');
        } catch (error) {
            this.ui.showStatus(`Ошибка удаления: ${error.message}`, 'error');
        }
    }

    // UI methods
    showDatabaseModal(action) {
        this.ui.showDatabaseModal(action, this.currentDatabase);
    }

    async handleDatabaseAction() {
        const filePath = this.ui.getDatabasePath();
        if (!filePath) {
            this.ui.showStatus('Введите путь к БД', 'error');
            return;
        }

        try {
            if (this.ui.currentModalAction === 'create') {
                await this.createDatabase(filePath);
            } else {
                await this.loadDatabase(filePath);
            }
            this.ui.hideDatabaseModal();
        } catch (error) {
            // Error already handled in methods
        }
    }

    showEmployeeForm(employee = null) {
        if (!this.currentDatabase) {
            this.ui.showStatus('Сначала загрузите БД', 'error');
            return;
        }

        this.editingEmployee = employee;
        if (employee) {
            this.form.populate(employee);
            this.form.setEditMode(true);
        } else {
            this.form.reset();
            this.form.setEditMode(false);
        }
        this.ui.showEmployeeForm(employee);
    }

    hideEmployeeForm() {
        this.ui.hideEmployeeForm();
        this.editingEmployee = null;
    }

    async handleFormSubmit(formData) {
        const employeeData = {
            id: parseInt(formData.get('id')),
            name: formData.get('name'),
            department: formData.get('department'),
            position: formData.get('position'),
            salary: parseFloat(formData.get('salary')),
            hireDate: formData.get('hireDate')
        };

        if (!this.form.validate()) {
            return;
        }

        try {
            if (this.editingEmployee) {
                await this.updateEmployee(this.editingEmployee.id, employeeData);
            } else {
                await this.addEmployee(employeeData);
            }
            this.hideEmployeeForm();
        } catch (error) {
            // Error already handled in methods
        }
    }

    editEmployee(id) {
        const employee = this.employees.find(emp => emp.id === id);
        if (employee) {
            this.showEmployeeForm(employee);
        }
    }
}

// Инициализация приложения
const app = new FileDBApp();
