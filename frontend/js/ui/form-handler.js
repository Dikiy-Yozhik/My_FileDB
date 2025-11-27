class FormHandler {
    constructor() {
        this.form = document.getElementById('employee-form');
        this.fields = this.cacheFields();
    }

    cacheFields() {
        return {
            id: document.getElementById('employee-id'),
            name: document.getElementById('employee-name'),
            department: document.getElementById('employee-department'),
            position: document.getElementById('employee-position'),
            salary: document.getElementById('employee-salary'),
            hireDate: document.getElementById('employee-hireDate')
        };
    }

    getFormData() {
        return {
            id: parseInt(this.fields.id.value),
            name: this.fields.name.value.trim(),
            department: this.fields.department.value.trim(),
            position: this.fields.position.value.trim(),
            salary: parseFloat(this.fields.salary.value),
            hireDate: this.fields.hireDate.value
        };
    }

    validate() {
        this.clearErrors();
        const data = this.getFormData();
        const errors = [];

        if (!data.id || data.id <= 0) {
            errors.push({ field: 'id', message: 'ID должен быть положительным числом' });
        }

        if (!data.name) {
            errors.push({ field: 'name', message: 'ФИО обязательно для заполнения' });
        }

        if (!data.department) {
            errors.push({ field: 'department', message: 'Отдел обязателен для заполнения' });
        }

        if (!data.position) {
            errors.push({ field: 'position', message: 'Должность обязательна для заполнения' });
        }

        if (!data.salary || data.salary < 0) {
            errors.push({ field: 'salary', message: 'Зарплата должна быть неотрицательным числом' });
        }

        if (!data.hireDate) {
            errors.push({ field: 'hireDate', message: 'Дата приема обязательна для заполнения' });
        }

        errors.forEach(error => this.showFieldError(error.field, error.message));
        
        return errors.length === 0;
    }

    showFieldError(fieldName, message) {
        const field = this.fields[fieldName];
        if (field) {
            field.classList.add('error');
        }
        return message;
    }

    clearErrors() {
        Object.values(this.fields).forEach(field => {
            field.classList.remove('error');
        });
    }

    populate(employee) {
        this.fields.id.value = employee.id;
        this.fields.name.value = employee.name;
        this.fields.department.value = employee.department;
        this.fields.position.value = employee.position;
        this.fields.salary.value = employee.salary;
        this.fields.hireDate.value = employee.hireDate;
    }

    reset() {
        this.form.reset();
        this.clearErrors();
    }

    setEditMode(editing) {
        this.fields.id.disabled = editing;
    }
}
