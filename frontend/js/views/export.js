const ExportPage = {
    exportedFiles: [],

    render() {
        return `
            <div class="page-container">
                <div class="page-header">
                    <h2>Экспорт данных</h2>
                </div>

                <div class="export-info">
                    <p>Экспортируйте данные сотрудников в различные форматы для внешнего использования.</p>
                </div>

                <!-- Форматы экспорта -->
                <div class="export-formats">
                    <div class="format-card">
                        <h3>📊 CSV формат</h3>
                        <p>Экспорт в CSV файл, совместимый с Excel и другими табличными редакторами.</p>
                        <button id="export-csv" class="btn btn-success">Экспорт в CSV</button>
                    </div>
                </div>

                <!-- История экспорта -->
                <div class="table-section">
                    <div class="table-header">
                        <h3>История экспорта</h3>
                        <button id="refresh-exports" class="btn btn-info btn-sm">🔄 Обновить</button>
                    </div>
                    
                    <div id="exports-list" class="exports-list">
                        <div class="loading-placeholder">Загрузка истории экспорта...</div>
                    </div>
                </div>
            </div>
        `;
    },

    init() {
        this.bindEvents();
        this.loadExportedFiles();
        this.checkPermissions();
    },

    bindEvents() {
        document.getElementById('export-csv')?.addEventListener('click', () => this.exportToCSV());
        document.getElementById('refresh-exports')?.addEventListener('click', () => this.loadExportedFiles());
    },

    checkPermissions() {
        const canExport = app.currentUser && 
                         (app.currentUser.role === 'ADMIN' || app.currentUser.role === 'OPERATOR');
        
        const exportButton = document.getElementById('export-csv');
        if (exportButton) {
            exportButton.disabled = !canExport;
            if (!canExport) {
                exportButton.title = 'Недостаточно прав для экспорта данных';
            }
        }
    },

    async exportToCSV() {
        if (!app.currentUser || (app.currentUser.role !== 'ADMIN' && app.currentUser.role !== 'OPERATOR')) {
            app.ui.showStatus('Недостаточно прав для экспорта данных', 'error');
            return;
        }

        try {
            app.ui.showLoading();
            const response = await app.api.exportToCSV();
            
            if (response.success) {
                app.ui.showStatus('Данные успешно экспортированы в CSV', 'success');
                await this.loadExportedFiles();
                
                // Предлагаем скачать файл
                if (response.data && response.data.filePath) {
                    this.downloadFile(response.data.filePath, response.data.fileName);
                }
            }
        } catch (error) {
            console.error('Error exporting to CSV:', error);
        } finally {
            app.ui.hideLoading();
        }
    },

    async loadExportedFiles() {
        if (!app.api) return;

        try {
            const response = await app.api.listExportedFiles();
            const exportsList = document.getElementById('exports-list');
            
            if (!exportsList) return;

            if (response.success) {
                this.exportedFiles = response.data || [];
                this.renderExportsList();
            } else {
                exportsList.innerHTML = '<div class="empty-state">Ошибка загрузки истории экспорта</div>';
            }
        } catch (error) {
            console.error('Error loading exported files:', error);
            const exportsList = document.getElementById('exports-list');
            if (exportsList) {
                exportsList.innerHTML = '<div class="empty-state">Ошибка загрузки истории экспорта</div>';
            }
        }
    },

    renderExportsList() {
        const exportsList = document.getElementById('exports-list');
        if (!exportsList) return;

        if (this.exportedFiles.length === 0) {
            exportsList.innerHTML = `
                <div class="empty-state">
                    <h4>Нет экспортированных файлов</h4>
                    <p>Экспортируйте данные впервые</p>
                </div>
            `;
            return;
        }

        exportsList.innerHTML = this.exportedFiles.map(file => `
            <div class="export-item">
                <div class="export-info">
                    <h4>${file.name}</h4>
                    <div class="export-details">
                        <span class="export-date">📅 ${new Date(file.lastModified).toLocaleString()}</span>
                        <span class="export-size">💾 ${this.formatFileSize(file.size)}</span>
                        <span class="export-format">${file.name.endsWith('.csv') ? 'CSV' : 'Unknown'}</span>
                    </div>
                </div>
                <div class="export-actions">
                    <button class="btn btn-primary btn-sm" onclick="ExportPage.downloadFile('${file.path}', '${file.name}')">
                        📥 Скачать
                    </button>
                </div>
            </div>
        `).join('');
    },

    downloadFile(filePath, fileName) {
        // Создаем временную ссылку для скачивания
        const downloadUrl = `${app.api.BASE_URL}/export/download?file=${encodeURIComponent(filePath)}`;
        const link = document.createElement('a');
        link.href = downloadUrl;
        link.download = fileName;
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
    },

    formatFileSize(bytes) {
        if (bytes < 1024) return bytes + ' B';
        else if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
        else return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
    },

    onEnter() {
        console.log('Export page entered');
    },

    onLeave() {
        console.log('Export page left');
    }
};