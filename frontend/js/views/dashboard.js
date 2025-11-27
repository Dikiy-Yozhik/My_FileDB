const DashboardPage = {
    render() {
        return `
            <div class="page-container">
                <div class="page-header">
                    <h2>Главная панель</h2>
                    <p id="db-status" class="db-status">БД не загружена</p>
                </div>

                <div class="dashboard-cards">
                    <div class="card">
                        <h3>📊 Статистика</h3>
                        <div id="stats-content" class="card-content">
                            <p>Загрузите базу данных для просмотра статистики</p>
                        </div>
                    </div>

                    <div class="card">
                        <h3>🗃️ Управление БД</h3>
                        <div class="card-content">
                            <div class="db-controls">
                                <button id="create-db" class="btn btn-primary">Создать БД</button>
                                <button id="load-db" class="btn btn-secondary">Загрузить БД</button>
                                <button id="clear-db" class="btn btn-danger" disabled>Очистить БД</button>
                            </div>
                            <div class="current-db">
                                <strong>Текущая БД:</strong> 
                                <span id="current-db-path">не загружена</span>
                            </div>
                        </div>
                    </div>

                    <div class="card">
                        <h3>👥 Быстрый доступ</h3>
                        <div class="card-content">
                            <div class="quick-actions">
                                <button onclick="router.navigateTo('employees')" class="btn btn-info">
                                    📋 Список сотрудников
                                </button>
                                <button onclick="router.navigateTo('backup')" class="btn btn-warning">
                                    💾 Управление бэкапами
                                </button>
                                <button onclick="router.navigateTo('export')" class="btn btn-success">
                                    📤 Экспорт данных
                                </button>
                            </div>
                        </div>
                    </div>
                </div>

                <!-- Модальное окно для управления БД -->
                <div id="db-modal" class="modal hidden">
                    <div class="modal-content">
                        <h3 id="modal-title">Создание базы данных</h3>
                        <input type="text" id="db-path" placeholder="Введите путь к БД (например: my_database)">
                        <div class="modal-hint">
                            💡 БД создается как папка. Пример: <code>company_db</code>
                        </div>
                        <div class="modal-actions">
                            <button id="modal-confirm" class="btn btn-primary">Подтвердить</button>
                            <button id="modal-cancel" class="btn btn-secondary">Отмена</button>
                        </div>
                    </div>
                </div>
            </div>
        `;
    },

    init() {
        this.bindEvents();
        this.loadDatabaseInfo();
    },

    bindEvents() {
        document.getElementById('create-db')?.addEventListener('click', () => this.showDbModal('create'));
        document.getElementById('load-db')?.addEventListener('click', () => this.showDbModal('load'));
        document.getElementById('clear-db')?.addEventListener('click', () => this.clearDatabase());
        
        document.getElementById('modal-confirm')?.addEventListener('click', () => this.handleDbAction());
        document.getElementById('modal-cancel')?.addEventListener('click', () => this.hideDbModal());
    },

    async loadDatabaseInfo() {
        try {
            const response = await app.api.getDatabaseInfo();
            if (response.success) {
                this.updateDatabaseInfo(response.data);
            }
        } catch (error) {
            // БД не загружена - это нормально
        }
    },

    updateDatabaseInfo(dbInfo) {
        const statusElement = document.getElementById('db-status');
        const pathElement = document.getElementById('current-db-path');
        const clearButton = document.getElementById('clear-db');

        if (dbInfo && dbInfo.databasePath) {
            statusElement.textContent = 'БД загружена';
            statusElement.className = 'db-status connected';
            pathElement.textContent = dbInfo.databasePath;
            clearButton.disabled = false;
            
            // Загружаем статистику
            this.loadStatistics();
        } else {
            statusElement.textContent = 'БД не загружена';
            statusElement.className = 'db-status disconnected';
            pathElement.textContent = 'не загружена';
            clearButton.disabled = true;
        }
    },

    async loadStatistics() {
        try {
            const employees = await app.api.getEmployees();
            const statsContent = document.getElementById('stats-content');
            
            if (employees.data && employees.data.length > 0) {
                const departments = [...new Set(employees.data.map(emp => emp.department))];
                const totalSalary = employees.data.reduce((sum, emp) => sum + emp.salary, 0);
                
                statsContent.innerHTML = `
                    <div class="stats-grid">
                        <div class="stat-item">
                            <strong>${employees.data.length}</strong>
                            <span>сотрудников</span>
                        </div>
                        <div class="stat-item">
                            <strong>${departments.length}</strong>
                            <span>отделов</span>
                        </div>
                        <div class="stat-item">
                            <strong>${totalSalary.toFixed(2)}</strong>
                            <span>общая зарплата</span>
                        </div>
                    </div>
                `;
            }
        } catch (error) {
            // ignore
        }
    },

    showDbModal(action) {
        this.currentAction = action;
        const modal = document.getElementById('db-modal');
        const title = document.getElementById('modal-title');
        
        title.textContent = action === 'create' 
            ? 'Создание новой базы данных' 
            : 'Загрузка базы данных';
            
        modal.classList.remove('hidden');
    },

    hideDbModal() {
        document.getElementById('db-modal').classList.add('hidden');
    },

    async handleDbAction() {
        const path = document.getElementById('db-path').value.trim();
        if (!path) {
            app.ui.showStatus('Введите путь к БД', 'error');
            return;
        }

        try {
            if (this.currentAction === 'create') {
                await app.api.createDatabase(path);
            } else {
                await app.api.loadDatabase(path);
            }
            this.hideDbModal();
            this.loadDatabaseInfo();
        } catch (error) {
            // Ошибка обрабатывается в api
        }
    },

    async clearDatabase() {
        if (!confirm('Вы уверены, что хотите очистить базу данных? Все данные будут удалены.')) {
            return;
        }

        try {
            await app.api.clearDatabase();
            this.loadDatabaseInfo();
        } catch (error) {
            // Ошибка обрабатывается в api
        }
    },

    onEnter() {
        console.log('Dashboard page entered');
    },

    onLeave() {
        console.log('Dashboard page left');
    }
};
