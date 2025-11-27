const BackupPage = {
    backups: [],

    render() {
        return `
            <div class="page-container">
                <div class="page-header">
                    <h2>Управление бэкапами</h2>
                    <button id="create-backup" class="btn btn-primary">💾 Создать бэкап</button>
                </div>

                <div class="backup-info">
                    <p>Бэкапы позволяют сохранить резервную копию базы данных для восстановления в случае необходимости.</p>
                </div>

                <!-- Список бэкапов -->
                <div class="table-section">
                    <div class="table-header">
                        <h3>Доступные бэкапы</h3>
                        <button id="refresh-backups" class="btn btn-info btn-sm">🔄 Обновить</button>
                    </div>
                    
                    <div id="backups-list" class="backups-list">
                        <div class="loading-placeholder">Загрузка бэкапов...</div>
                    </div>
                </div>

                <!-- Модальное окно восстановления -->
                <div id="restore-modal" class="modal hidden">
                    <div class="modal-content">
                        <h3>Восстановление из бэкапа</h3>
                        <div class="form-group">
                            <label for="restore-path">Путь для восстановления:</label>
                            <input type="text" id="restore-path" placeholder="Введите путь для новой БД">
                        </div>
                        <div class="modal-hint">
                            💡 Будет создана новая база данных из бэкапа
                        </div>
                        <div class="modal-actions">
                            <button id="confirm-restore" class="btn btn-warning">Восстановить</button>
                            <button id="cancel-restore" class="btn btn-secondary">Отмена</button>
                        </div>
                    </div>
                </div>
            </div>
        `;
    },

    init() {
        this.bindEvents();
        this.loadBackups();
        this.checkPermissions();
    },

    bindEvents() {
        document.getElementById('create-backup')?.addEventListener('click', () => this.createBackup());
        document.getElementById('refresh-backups')?.addEventListener('click', () => this.loadBackups());
        document.getElementById('confirm-restore')?.addEventListener('click', () => this.confirmRestore());
        document.getElementById('cancel-restore')?.addEventListener('click', () => this.hideRestoreModal());
    },

    checkPermissions() {
        const isAdmin = app.currentUser && app.currentUser.role === 'ADMIN';
        const createButton = document.getElementById('create-backup');
        
        if (createButton) {
            createButton.disabled = !isAdmin;
            if (!isAdmin) {
                createButton.title = 'Недостаточно прав для управления бэкапами';
            }
        }
    },

    async loadBackups() {
        if (!app.api) return;

        try {
            const response = await app.api.listBackups();
            const backupsList = document.getElementById('backups-list');
            
            if (!backupsList) return;

            if (response.success) {
                this.backups = response.data || [];
                this.renderBackupsList();
            } else {
                backupsList.innerHTML = '<div class="empty-state">Ошибка загрузки бэкапов</div>';
            }
        } catch (error) {
            console.error('Error loading backups:', error);
            const backupsList = document.getElementById('backups-list');
            if (backupsList) {
                backupsList.innerHTML = '<div class="empty-state">Ошибка загрузки бэкапов</div>';
            }
        }
    },

    renderBackupsList() {
        const backupsList = document.getElementById('backups-list');
        if (!backupsList) return;

        if (this.backups.length === 0) {
            backupsList.innerHTML = `
                <div class="empty-state">
                    <h4>Бэкапы не найдены</h4>
                    <p>Создайте первый бэкап вашей базы данных</p>
                </div>
            `;
            return;
        }

        const isAdmin = app.currentUser && app.currentUser.role === 'ADMIN';

        backupsList.innerHTML = this.backups.map(backup => `
            <div class="backup-item">
                <div class="backup-info">
                    <h4>${backup.name}</h4>
                    <div class="backup-details">
                        <span class="backup-date">📅 ${backup.createdAt}</span>
                        <span class="backup-size">💾 ${backup.formattedSize}</span>
                        <span class="backup-files">📁 ${backup.fileCount} файлов</span>
                    </div>
                </div>
                <div class="backup-actions">
                    <button class="btn btn-warning btn-sm" onclick="BackupPage.showRestoreModal('${backup.path}')">
                        🔄 Восстановить
                    </button>
                    ${isAdmin ? `
                        <button class="btn btn-danger btn-sm" onclick="BackupPage.deleteBackup('${backup.path}')">
                            🗑️ Удалить
                        </button>
                    ` : ''}
                </div>
            </div>
        `).join('');
    },

    async createBackup() {
        if (!app.currentUser || app.currentUser.role !== 'ADMIN') {
            app.ui.showStatus('Недостаточно прав для создания бэкапов', 'error');
            return;
        }

        try {
            app.ui.showLoading();
            const response = await app.api.createBackup();
            
            if (response.success) {
                app.ui.showStatus('Бэкап успешно создан', 'success');
                await this.loadBackups();
            }
        } catch (error) {
            console.error('Error creating backup:', error);
        } finally {
            app.ui.hideLoading();
        }
    },

    showRestoreModal(backupPath) {
        this.currentBackupPath = backupPath;
        const modal = document.getElementById('restore-modal');
        const pathInput = document.getElementById('restore-path');
        
        if (modal && pathInput) {
            // Генерируем имя для восстановления
            const backupName = backupPath.split('/').pop().replace('_backup_', '_restored_');
            pathInput.value = backupName;
            modal.classList.remove('hidden');
        }
    },

    hideRestoreModal() {
        const modal = document.getElementById('restore-modal');
        if (modal) {
            modal.classList.add('hidden');
        }
        this.currentBackupPath = null;
    },

    async confirmRestore() {
        if (!this.currentBackupPath) return;

        const targetPath = document.getElementById('restore-path')?.value.trim();
        if (!targetPath) {
            app.ui.showStatus('Введите путь для восстановления', 'error');
            return;
        }

        try {
            app.ui.showLoading();
            const response = await app.api.restoreBackup(this.currentBackupPath, targetPath);
            
            if (response.success) {
                app.ui.showStatus('Бэкап успешно восстановлен', 'success');
                this.hideRestoreModal();
                await this.loadBackups();
            }
        } catch (error) {
            console.error('Error restoring backup:', error);
        } finally {
            app.ui.hideLoading();
        }
    },

    async deleteBackup(backupPath) {
        if (!app.currentUser || app.currentUser.role !== 'ADMIN') {
            app.ui.showStatus('Недостаточно прав для удаления бэкапов', 'error');
            return;
        }

        if (!confirm('Вы уверены, что хотите удалить этот бэкап?')) {
            return;
        }

        try {
            app.ui.showLoading();
            const response = await app.api.deleteBackup(backupPath);
            
            if (response.success) {
                app.ui.showStatus('Бэкап удален', 'success');
                await this.loadBackups();
            }
        } catch (error) {
            console.error('Error deleting backup:', error);
        } finally {
            app.ui.hideLoading();
        }
    },

    onEnter() {
        console.log('Backup page entered');
    },

    onLeave() {
        console.log('Backup page left');
    }
};