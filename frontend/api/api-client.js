class ApiClient {
    constructor(baseURL = 'http://localhost:8080') {
        this.BASE_URL = baseURL;
    }

    async request(endpoint, options = {}) {
        const url = `${this.BASE_URL}${endpoint}`;
        
        const config = {
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

    // Database methods
    async createDatabase(databasePath, overwrite = true) {
        return this.request('/database/create', {
            method: 'POST',
            body: JSON.stringify({ databasePath, overwrite })
        });
    }

    async loadDatabase(databasePath) {
        return this.request('/database/load', {
            method: 'POST',
            body: JSON.stringify({ databasePath })
        });
    }

    async backupDatabase() {
        return this.request('/database/backup', {
            method: 'POST'
        });
    }

    async getDatabaseInfo() {
        return this.request('/database/info');
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

    // Export
    exportToExcel() {
        window.open(`${this.BASE_URL}/export/excel`, '_blank');
    }
}