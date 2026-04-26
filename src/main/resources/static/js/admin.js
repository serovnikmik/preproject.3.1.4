document.addEventListener('DOMContentLoaded', function() {

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
});