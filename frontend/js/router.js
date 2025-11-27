class Router {
    constructor() {
        this.routes = {};
        this.currentPage = null;
        this.init();
    }

    init() {
        // Обработка изменения hash в URL
        window.addEventListener('hashchange', () => this.handleRouteChange());
        
        // Обработка загрузки страницы
        window.addEventListener('load', () => this.handleRouteChange());
    }

    addRoute(path, pageComponent) {
        this.routes[path] = pageComponent;
    }

    handleRouteChange() {
        const hash = window.location.hash.slice(1) || 'dashboard';
        
        if (this.currentPage && this.currentPage.onLeave) {
            this.currentPage.onLeave();
        }

        const pageComponent = this.routes[hash];
        if (pageComponent) {
            this.currentPage = pageComponent;
            if (pageComponent.onEnter) {
                pageComponent.onEnter();
            }
            this.renderPage(pageComponent);
        } else {
            this.showError('Страница не найдена');
        }

        this.updateNavigation(hash);
    }

    renderPage(pageComponent) {
        const appContent = document.getElementById('app-content');
        if (appContent && pageComponent.render) {
            appContent.innerHTML = pageComponent.render();
            
            // Инициализация компонента после рендеринга
            if (pageComponent.init) {
                setTimeout(() => pageComponent.init(), 0);
            }
        }
    }

    updateNavigation(activePage) {
        document.querySelectorAll('.nav-link').forEach(link => {
            link.classList.remove('active');
            if (link.getAttribute('data-page') === activePage) {
                link.classList.add('active');
            }
        });
    }

    navigateTo(page) {
        window.location.hash = page;
    }

    showError(message) {
        const appContent = document.getElementById('app-content');
        appContent.innerHTML = `
            <div class="error-page">
                <h2>Ошибка</h2>
                <p>${message}</p>
                <button onclick="router.navigateTo('dashboard')" class="btn btn-primary">
                    На главную
                </button>
            </div>
        `;
    }
}

const router = new Router();
