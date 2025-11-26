class UIManager {
    constructor() {
        this.elements = this.cacheDOM();
        this.bindEvents();
    }

    cacheDOM() {
        return {
            // Header
            dbStatus: document.getElementById('db-status'),
            
            // Controls
            searchInput: document.getElementById('search-input'),
            departmentFilter: document.getElementById('department-filter'),
            searchBtn: document.getElementById('search-btn'),
            clearSearchBtn: document.getElementById('clear-search'),
            addEmployeeBtn: document.getElementById('add-employee'),
            refreshBtn: document.getElementById('refresh-data'),
            
            // Database controls
            createDbBtn: document.getElementById('create-db'),
            loadDbBtn: document.getElementById('load-db'),
            backupDbBtn: document.getElementById('backup-db'),
            exportExcelBtn: document.getElementById('export-excel'),
            
            // Form
            employeeFormSection: document.getElementById('employee-form-section'),
            employeeForm: document.getElementById('employee-form'),
            formTitle: document.getElementById('form-title'),
            cancelFormBtn: document.getElementById('cancel-form'),
            
            // Table
            employeesTbody: document.getElementById('employees-tbody'),
            recordCount: document.getElementById('record-count'),
            
            // Modal
            dbModal: document.getElementById('db-modal'),
            modalTitle: document.getElementById('modal-title'),
            dbPathInput: document.getElementById('db-path'),
            modalConfirmBtn: document.getElementById('modal-confirm'),
            modalCancelBtn: document.getElementById('modal-cancel'),
            
            // Status
            statusMessage: document.getElementById('status-message'),
            loadingIndicator: document.getElementById('loading-indicator')
        };
    }

    bindEvents() {
        // Database events
        this.elements.createDbBtn.addEventListener('click', () => this.emit('createDatabaseClick'));
        this.elements.loadDbBtn.addEventListener('click', () => this.emit('loadDatabaseClick'));
        this.elements.backupDbBtn.addEventListener('click', () => this.emit('backupClick'));
        this.elements.exportExcelBtn.addEventListener('click', () => this.emit('exportExcelClick'));
        
        // Data events
        this.elements.addEmployeeBtn.addEventListener('click', () => this.emit('addEmployeeClick'));
        this.elements.refreshBtn.addEventListener('click', () => this.emit('refreshClick'));
        this.elements.searchBtn.addEventListener('click', () => this.emit('searchClick'));
        this.elements.clearSearchBtn.addEventListener('click', () => this.emit('clearSearchClick'));
        
        // Form events
        this.elements.employeeForm.addEventListener('submit', (e) => {
            e.preventDefault();
            this.emit('formSubmit', new FormData(e.target));
        });
        this.elements.cancelFormBtn.addEventListener('click', () => this.emit('formCancel'));
        
        // Modal events
        this.elements.modalConfirmBtn.addEventListener('click', () => this.emit('modalConfirm'));
        this.elements.modalCancelBtn.addEventListener('click', () => this.emit('modalCancel'));
        
        // Search input
        this.elements.searchInput.addEventListener('input', (e) => {
            this.emit('searchInput', e.target.value);
        });
    }

    // Event system
    on(event, callback) {
        if (!this.handlers) this.handlers = {};
        if (!this.handlers[event]) this.handlers[event] = [];
        this.handlers[event].push(callback);
    }

    emit(event, data) {
        if (this.handlers && this.handlers[event]) {
            this.handlers[event].forEach(callback => callback(data));
        }
    }

    // UI state methods
    updateDatabaseStatus(databasePath) {
        if (databasePath) {
            this.elements.dbStatus.textContent = `БД: ${databasePath}`;
            this.elements.dbStatus.className = 'db-status connected';
            this.setControlsEnabled(true);
        } else {
            this.elements.dbStatus.textContent = 'БД не загружена';
            this.elements.dbStatus.className = 'db-status disconnected';
            this.setControlsEnabled(false);
        }
    }

    setControlsEnabled(enabled) {
        const controls = [
            this.elements.addEmployeeBtn,
            this.elements.refreshBtn,
            this.elements.backupDbBtn,
            this.elements.exportExcelBtn
        ];
        
        controls.forEach(control => {
            control.disabled = !enabled;
        });
    }

    showLoading() {
        this.elements.loadingIndicator.classList.remove('hidden');
    }

    hideLoading() {
        this.elements.loadingIndicator.classList.add('hidden');
    }

    showStatus(message, type = 'info') {
        this.elements.statusMessage.textContent = message;
        this.elements.statusMessage.className = `status-message ${type}`;
        
        setTimeout(() => {
            if (this.elements.statusMessage.textContent === message) {
                this.elements.statusMessage.textContent = 'Готов к работе';
                this.elements.statusMessage.className = 'status-message';
            }
        }, 3000);
    }

    // Modal methods
    showDatabaseModal(action, currentPath = '') {
        this.elements.dbPathInput.value = currentPath;
        this.elements.modalTitle.textContent = action === 'create' 
            ? 'Создание новой базы данных' 
            : 'Загрузка базы данных';
        this.elements.dbModal.classList.remove('hidden');
    }

    hideDatabaseModal() {
        this.elements.dbModal.classList.add('hidden');
    }

    getDatabasePath() {
        return this.elements.dbPathInput.value.trim();
    }

    // Form methods
    showEmployeeForm(employee = null) {
        if (employee) {
            this.elements.formTitle.textContent = 'Редактирование сотрудника';
            this.populateForm(employee);
            document.getElementById('employee-id').disabled = true;
        } else {
            this.elements.formTitle.textContent = 'Добавление сотрудника';
            this.elements.employeeForm.reset();
            document.getElementById('employee-id').disabled = false;
        }
        this.elements.employeeFormSection.classList.remove('hidden');
    }

    hideEmployeeForm() {
        this.elements.employeeFormSection.classList.add('hidden');
    }

    populateForm(employee) {
        document.getElementById('employee-id').value = employee.id;
        document.getElementById('employee-name').value = employee.name;
        document.getElementById('employee-department').value = employee.department;
        document.getElementById('employee-position').value = employee.position;
        document.getElementById('employee-salary').value = employee.salary;
        document.getElementById('employee-hireDate').value = employee.hireDate;
    }

    // Search methods
    getSearchFilters() {
        return {
            name: this.elements.searchInput.value,
            department: this.elements.departmentFilter.value
        };
    }

    clearSearch() {
        this.elements.searchInput.value = '';
        this.elements.departmentFilter.value = '';
    }
}
