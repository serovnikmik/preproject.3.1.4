document.addEventListener('DOMContentLoaded', function() {



    // ==================== ФУНКЦИЯ ОБНОВЛЕНИЯ ТАБЛИЦЫ ====================
    async function loadUsersTable() {
        try {
            const response = await fetch('/api/users');
            if (!response.ok) throw new Error('Ошибка загрузки');

            const users = await response.json();

            const tbody = document.querySelector('#users tbody');
            tbody.innerHTML = users.map(user => {
                // Формируем бейджики ролей
                const rolesBadges = user.roles.map(role => {
                    const badgeClass = role.name === 'ROLE_ADMIN' ? 'bg-danger' : 'bg-primary';
                    return `<span class="badge me-1 ${badgeClass}">${role.name}</span>`;
                }).join('');

                return `
                    <tr>
                        <td>${user.id}</td>
                        <td>${user.name || ''}</td>
                        <td>${user.age || ''}</td>
                        <td>${user.email || ''}</td>
                        <td>${user.username}</td>
                        <td>${rolesBadges}</td>
                        <td>
                            <button class="btn btn-sm btn-warning edit-btn"
                                    data-bs-toggle="modal"
                                    data-bs-target="#editModal"
                                    data-user-id="${user.id}">
                                Edit
                            </button>
                        </td>
                        <td>
                            <button class="btn btn-sm btn-danger delete-btn"
                                    data-bs-toggle="modal"
                                    data-bs-target="#deleteModal"
                                    data-user-id="${user.id}">
                                Delete
                            </button>
                        </td>
                    </tr>
                `;
            }).join('');

        } catch (error) {
            console.error('Ошибка загрузки таблицы:', error);
        }
    }



    // ==================== МОДАЛКА РЕДАКТИРОВАНИЯ ====================
    const editModal = document.getElementById('editModal');

    editModal.addEventListener('show.bs.modal', async function(event) {
        const button = event.relatedTarget;
        const userId = button.getAttribute('data-user-id');

        console.log('Загружаем пользователя с ID:', userId);

        try {
            const response = await fetch(`/api/users/${userId}/simple`);

            if (!response.ok) {
                throw new Error(`Ошибка HTTP: ${response.status}`);
            }

            const user = await response.json();
            console.log('Получены данные:', user);

            // Заполняем поля формы
            document.getElementById('editUserDisplayId').value = user.id;
            document.getElementById('editUserId').value = user.id;
            document.getElementById('editUserName').value = user.name || '';
            document.getElementById('editUserAge').value = user.age || '';
            document.getElementById('editUserEmail').value = user.email || '';
            document.getElementById('editUserUsername').value = user.username || '';
            document.getElementById('editUserPassword').value = '';

            // Сбрасываем чекбоксы
            document.getElementById('editRoleAdmin').checked = false;
            document.getElementById('editRoleUser').checked = false;

            // Устанавливаем роли по ID
            if (user.roles && Array.isArray(user.roles)) {
                user.roles.forEach(role => {
                    if (role.id === 1) {
                        document.getElementById('editRoleAdmin').checked = true;
                    } else if (role.id === 2) {
                        document.getElementById('editRoleUser').checked = true;
                    }
                });
            }

            // Обновляем action формы
            const form = document.getElementById('editForm');
            form.action = `/admin/update/${userId}`;

        } catch (error) {
            console.error('Ошибка загрузки пользователя:', error);
            alert('Не удалось загрузить данные пользователя.');

            // Закрываем модалку при ошибке
            const modal = bootstrap.Modal.getInstance(editModal);
            modal.hide();
        }
    });

    // Очищаем форму при закрытии модалки
    editModal.addEventListener('hidden.bs.modal', function() {
        document.getElementById('editForm').reset();
        document.getElementById('editRoleAdmin').checked = false;
        document.getElementById('editRoleUser').checked = false;
        document.getElementById('editUserPassword').value = '';
    });



    // ==================== ОТПРАВКА ФОРМЫ РЕДАКТИРОВАНИЯ ====================
    const editForm = document.getElementById('editForm');

    editForm.addEventListener('submit', async function(event) {
        event.preventDefault(); // Отменяем обычную отправку формы

        const userId = document.getElementById('editUserId').value;

        // Собираем роли (массив объектов с id)
        const roles = [];
        if (document.getElementById('editRoleAdmin').checked) {
            roles.push({ id: 1 });
        }
        if (document.getElementById('editRoleUser').checked) {
            roles.push({ id: 2 });
        }

        // Собираем данные для отправки
        const userData = {
            name: document.getElementById('editUserName').value,
            age: parseInt(document.getElementById('editUserAge').value) || 0,
            email: document.getElementById('editUserEmail').value,
            username: document.getElementById('editUserUsername').value,
            password: document.getElementById('editUserPassword').value,
            roles: roles
        };

        // Очищаем предыдущие ошибки
        clearEditErrors();

        try {
            const response = await fetch(`/api/users/${userId}`, {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json',
                    // Если нужен CSRF-токен, добавь строку ниже
                    // 'X-CSRF-TOKEN': document.querySelector('input[name="_csrf"]').value
                },
                body: JSON.stringify(userData)
            });

            if (response.ok) {
                // Успех — закрываем модалку и обновляем таблицу
                const modal = bootstrap.Modal.getInstance(editModal);
                modal.hide();
                await loadUsersTable();
            } else {
                // Ошибка — разбираем ответ
                const errorData = await response.json();

                if (errorData.errors) {
                    // Ошибки валидации полей
                    showEditErrors(errorData.errors);
                } else {
                    // Общая ошибка (username already exists и т.д.)
                    alert(errorData.message || 'Ошибка при сохранении');
                }
            }

        } catch (error) {
            console.error('Ошибка:', error);
            alert('Не удалось сохранить пользователя. Проверьте соединение.');
        }
    });



    // ==================== ВСПОМОГАТЕЛЬНЫЕ ФУНКЦИИ ДЛЯ ОШИБОК ====================

    // Показывает ошибки валидации в полях формы
    function showEditErrors(errors) {
        // errors — объект вида { name: "обязательно", age: "должно быть от 1 до 150" }
        for (const [field, message] of Object.entries(errors)) {
            const input = document.getElementById('editUser' + field.charAt(0).toUpperCase() + field.slice(1));
            const errorDiv = document.getElementById('editUser' + field.charAt(0).toUpperCase() + field.slice(1) + 'Error');

            if (input) {
                input.classList.add('is-invalid');
            }
            if (errorDiv) {
                errorDiv.textContent = message;
            }
        }
    }

    // Очищает все ошибки в форме
    function clearEditErrors() {
        const fields = ['Name', 'Age', 'Email', 'Username', 'Password'];
        fields.forEach(field => {
            const input = document.getElementById('editUser' + field);
            const errorDiv = document.getElementById('editUser' + field + 'Error');

            if (input) {
                input.classList.remove('is-invalid');
            }
            if (errorDiv) {
                errorDiv.textContent = '';
            }
        });
    }



    // ==================== МОДАЛКА УДАЛЕНИЯ ====================
    const deleteModal = document.getElementById('deleteModal');

    deleteModal.addEventListener('show.bs.modal', async function(event) {
        const button = event.relatedTarget;
        const userId = button.getAttribute('data-user-id');

        console.log('Загружаем данные для удаления, ID:', userId);

        try {
            const response = await fetch(`/api/users/${userId}/simple`);

            if (!response.ok) {
                throw new Error(`Ошибка HTTP: ${response.status}`);
            }

            const user = await response.json();
            console.log('Данные пользователя:', user);

            // Заполняем readonly-поля
            document.getElementById('deleteUserId').value = user.id || '';
            document.getElementById('deleteUserName').value = user.name || '';
            document.getElementById('deleteUserAge').value = user.age || '';
            document.getElementById('deleteUserEmail').value = user.email || '';
            document.getElementById('deleteUserUsername').value = user.username || '';

            // Заполняем select с ролями
            const rolesSelect = document.getElementById('deleteUserRoles');
            if (rolesSelect) {
                // Сбрасываем все выборы
                Array.from(rolesSelect.options).forEach(option => {
                    option.selected = false;
                });

                // Отмечаем роли пользователя
                if (user.roles && Array.isArray(user.roles)) {
                    user.roles.forEach(role => {
                        Array.from(rolesSelect.options).forEach(option => {
                            // Сравниваем по ID роли
                            if (role.id === 1 && option.value === 'ROLE_ADMIN') {
                                option.selected = true;
                            } else if (role.id === 2 && option.value === 'ROLE_USER') {
                                option.selected = true;
                            }
                        });
                    });
                }
            }

            // Устанавливаем action формы
            const form = document.getElementById('deleteForm');
            if (form) {
                form.action = '/admin/delete/' + userId;
            }

        } catch (error) {
            console.error('Ошибка загрузки данных:', error);
            alert('Не удалось загрузить данные пользователя.');
            const modalInstance = bootstrap.Modal.getInstance(deleteModal);
            modalInstance.hide();
        }
    });



    // ==================== КНОПКА УДАЛЕНИЯ ====================
    const deleteConfirmBtn = document.getElementById('deleteConfirmBtn');

    deleteConfirmBtn.addEventListener('click', async function() {
        const userId = document.getElementById('deleteUserId').value;

        try {
            const response = await fetch(`/api/users/${userId}`, {
                method: 'DELETE',
                headers: {
                    'Content-Type': 'application/json'
                }
            });

            if (response.status === 204 || response.ok) {
                // Успех — закрываем модалку и обновляем таблицу
                const modal = bootstrap.Modal.getInstance(deleteModal);
                modal.hide();
                await loadUsersTable();
            } else {
                const errorData = await response.json();
                alert(errorData.message || 'Ошибка при удалении');
            }

        } catch (error) {
            console.error('Ошибка:', error);
            alert('Не удалось удалить пользователя.');
        }
    });



    // ==================== ОТПРАВКА ФОРМЫ СОЗДАНИЯ ====================
    const createForm = document.querySelector('#newuser form');

    createForm.addEventListener('submit', async function(event) {
        event.preventDefault();

        // Собираем роли (из чекбоксов формы создания)
        const roles = [];
        if (document.getElementById('role_admin').checked) {
            roles.push({ id: 1 });
        }
        if (document.getElementById('role_user').checked) {
            roles.push({ id: 2 });
        }

        // Собираем данные
        const userData = {
            name: document.getElementById('name').value,
            age: parseInt(document.getElementById('age').value) || 0,
            email: document.getElementById('email').value,
            username: document.getElementById('username').value,
            password: document.getElementById('password').value,
            roles: roles
        };

        // Очищаем предыдущие ошибки
        clearCreateErrors();

        try {
            const response = await fetch('/api/users', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(userData)
            });

            if (response.ok) {
                // Успех — сбрасываем форму, переключаемся на таб Users table, обновляем таблицу
                createForm.reset();
                document.getElementById('role_admin').checked = false;
                document.getElementById('role_user').checked = true; // по умолчанию USER

                // Переключаемся на таб с таблицей
                const usersTab = document.getElementById('users-tab');
                const tabInstance = new bootstrap.Tab(usersTab);
                tabInstance.show();

                await loadUsersTable();
            } else {
                const errorData = await response.json();

                if (errorData.errors) {
                    showCreateErrors(errorData.errors);
                } else {
                    alert(errorData.message || 'Ошибка при создании пользователя');
                }
            }

        } catch (error) {
            console.error('Ошибка:', error);
            alert('Не удалось создать пользователя.');
        }
    });



    // ==================== ВСПОМОГАТЕЛЬНЫЕ ФУНКЦИИ СОЗДАНИЯ ====================
    // Показывает ошибки валидации в форме создания
    function showCreateErrors(errors) {
        for (const [field, message] of Object.entries(errors)) {
            const input = document.getElementById(field);
            const errorDiv = document.getElementById(field + 'Error');

            if (input) {
                input.classList.add('is-invalid');
            }
            if (errorDiv) {
                errorDiv.textContent = message;
            }
        }
    }

    // Очищает ошибки в форме создания
    function clearCreateErrors() {
        const fields = ['name', 'age', 'email', 'username', 'password'];
        fields.forEach(field => {
            const input = document.getElementById(field);
            const errorDiv = document.getElementById(field + 'Error');

            if (input) {
                input.classList.remove('is-invalid');
            }
            if (errorDiv) {
                errorDiv.textContent = '';
            }
        });
    }


        // ==================== ЗАГРУЗКА ТАБЛИЦЫ ====================
    loadUsersTable();
});