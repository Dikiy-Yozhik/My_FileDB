class ApiClient {
    constructor(baseURL = 'http://localhost:8080') {
        this.BASE_URL = baseURL;
    }

    async request(endpoint, options = {}) {
        const url = `${this.BASE_URL}${endpoint}`;
        
        const config = {
            credentials: 'include', // Важно для cookies
            headers: {
                'Content-Type': 'application/json',
                ...options.headers
            },
            ...options
        };

        try {
            const response = await fetch(url, config);
            
            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }
            
            return await response.json();
        } catch (error) {
            console.error('API Request failed:', error);
            throw error;
        }
    }

    // Аутентификация
    async login(username, password) {
        return this.request('/auth/login', {
            method: 'POST',
            body: JSON.stringify({ username, password })
        });
    }

    async logout() {
        return this.request('/auth/logout', {
            method: 'POST'
        });
    }

    async getAuthStatus() {
        return this.request('/auth/status');
    }

    // Database methods
    async createDatabase(databasePath) {
        return this.request('/database/create', {
            method: 'POST',
            body: JSON.stringify({ databasePath, overwrite: true })
        });
    }

    async loadDatabase(databasePath) {
        return this.request('/database/load', {
            method: 'POST',
            body: JSON.stringify({ databasePath })
        });
    }

    async getDatabaseInfo() {
        return this.request('/database/info');
    }

    async clearDatabase() {
        return this.request('/database/clear', {
            method: 'DELETE'
        });
    }

    // Employee methods
    async getEmployees() {
        return this.request('/employees');
    }

    async searchEmployees(filters = {}) {
        const params = new URLSearchParams();
        Object.entries(filters).forEach(([key, value]) => {
            if (value) params.append(key, value);
        });
        
        const queryString = params.toString();
        const endpoint = queryString ? `/employees/search?${queryString}` : '/employees/search';
        
        return this.request(endpoint);
    }

    async createEmployee(employeeData) {
        return this.request('/employees', {
            method: 'POST',
            body: JSON.stringify(employeeData)
        });
    }

    async updateEmployee(id, employeeData) {
        return this.request(`/employees/${id}`, {
            method: 'PUT',
            body: JSON.stringify(employeeData)
        });
    }

    async deleteEmployee(id) {
        return this.request(`/employees/${id}`, {
            method: 'DELETE'
        });
    }

    // Backup methods
    async createBackup() {
        return this.request('/backup/create', {
            method: 'POST'
        });
    }

    async restoreBackup(backupPath, targetPath) {
        return this.request('/backup/restore', {
            method: 'POST',
            body: JSON.stringify({ backupPath, targetPath })
        });
    }

    async listBackups() {
        return this.request('/backup/list');
    }

    async deleteBackup(backupPath) {
        return this.request('/backup/delete', {
            method: 'DELETE',
            body: JSON.stringify({ backupPath })
        });
    }

    // Export methods
    async exportToCSV() {
        return this.request('/export/csv');
    }

    async listExportedFiles() {
        return this.request('/export/list');
    }
}
