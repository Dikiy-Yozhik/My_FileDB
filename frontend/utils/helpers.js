const Helpers = {
    debounce(func, wait) {
        let timeout;
        return function executedFunction(...args) {
            const later = () => {
                clearTimeout(timeout);
                func(...args);
            };
            clearTimeout(timeout);
            timeout = setTimeout(later, wait);
        };
    },

    confirmAction(message) {
        return confirm(message);
    },

    formatDate(dateString) {
        if (!dateString) return '';
        return new Date(dateString).toLocaleDateString('ru-RU');
    },

    formatCurrency(amount) {
        return new Intl.NumberFormat('ru-RU', {
            style: 'currency',
            currency: 'RUB'
        }).format(amount);
    }
};
