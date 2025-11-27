class FileDBApp {
    constructor() {
        this.api = new ApiClient();
        this.ui = new UIManager();
        this.currentUser = null;
        this.init();
    }

    async init() {
        // Проверяем аутентификацию
        await this.checkAuth();
        
        // Настраиваем роутинг
        this.setupRouting();
        
        // Загружаем начальные данные
        this.loadInitialData();
    }

    async checkAuth() {
        try {
            const response = await this.api.getAuthStatus();
            if (response.success) {
                this.currentUser = response.data;
                this.updateUserInterface();
            } else {
                this.redirectToLogin();
            }
        } catch (error) {
            this.redirectToLogin();
        }
    }

    setupRouting() {
        // Регистрируем страницы
        router.addRoute('dashboard', DashboardPage);
        router.addRoute('employees', EmployeesPage);
        router.addRoute('backup', BackupPage);
        router.addRoute('export', ExportPage);

        // Обработка выхода
        document.getElementById('logout-btn')?.addEventListener('click', () => this.logout());
    }

    updateUserInterface() {
        const userInfoElement = document.getElementById('user-info');
        if (userInfoElement && this.currentUser) {
            userInfoElement.innerHTML = `
                <span class="user-name">${this.currentUser.username}</span>
                <span class="user-role">${this.currentUser.displayName}</span>
            `;
        }

        // Обновляем доступность функций в зависимости от роли
        this.updatePermissions();
    }

    updatePermissions() {
        // Эта функция будет обновлять UI в зависимости от прав пользователя
        // Например, скрывать кнопки для гостей
        if (this.currentUser) {
            const isAdmin = this.currentUser.role === 'ADMIN';
            const isOperator = this.currentUser.role === 'OPERATOR' || isAdmin;
            const isGuest = this.currentUser.role === 'GUEST';

            // Можно добавить логику скрытия/показа элементов
        }
    }

    async logout() {
        try {
            await this.api.logout();
            this.redirectToLogin();
        } catch (error) {
            console.error('Logout error:', error);
            this.redirectToLogin();
        }
    }

    redirectToLogin() {
        window.location.href = 'login.html';
    }

    loadInitialData() {
        // Загрузка начальных данных приложения
        console.log('App initialized');
    }
}

// Инициализация приложения после загрузки DOM
document.addEventListener('DOMContentLoaded', () => {
    window.app = new FileDBApp();
});
