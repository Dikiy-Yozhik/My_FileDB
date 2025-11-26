class TableRenderer {
    constructor(tbodyElement, recordCountElement) {
        this.tbody = tbodyElement;
        this.recordCount = recordCountElement;
    }

    render(employees, databaseLoaded = false) {
        if (!employees || employees.length === 0) {
            this.renderEmptyState(databaseLoaded);
            return;
        }

        this.tbody.innerHTML = employees.map(employee => this.renderEmployeeRow(employee)).join('');
        this.recordCount.textContent = `Записей: ${employees.length}`;
    }

    renderEmployeeRow(employee) {
        return `
            <tr>
                <td>${employee.id}</td>
                <td>${this.escapeHtml(employee.name)}</td>
                <td>${this.escapeHtml(employee.department)}</td>
                <td>${this.escapeHtml(employee.position)}</td>
                <td>${employee.salary?.toFixed(2) || '0.00'}</td>
                <td>${employee.hireDate}</td>
                <td class="actions-cell">
                    <button class="action-btn edit-btn" data-id="${employee.id}">✏️</button>
                    <button class="action-btn delete-btn" data-id="${employee.id}">🗑️</button>
                </td>
            </tr>
        `;
    }

    renderEmptyState(databaseLoaded) {
        this.tbody.innerHTML = `
            <tr>
                <td colspan="7" style="text-align: center; color: #666;">
                    ${databaseLoaded ? 'Нет данных для отображения' : 'БД не загружена'}
                </td>
            </tr>
        `;
        this.recordCount.textContent = 'Записей: 0';
    }

    bindRowEvents(onEdit, onDelete) {
        this.tbody.addEventListener('click', (e) => {
            const button = e.target.closest('button');
            if (!button) return;

            const id = button.dataset.id;
            if (button.classList.contains('edit-btn')) {
                onEdit(parseInt(id));
            } else if (button.classList.contains('delete-btn')) {
                onDelete(parseInt(id));
            }
        });
    }

    escapeHtml(unsafe) {
        if (!unsafe) return '';
        return unsafe
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;")
            .replace(/'/g, "&#039;");
    }
}
