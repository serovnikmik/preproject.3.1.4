package ru.kata.spring.boot_security.demo.controllers.api;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import ru.kata.spring.boot_security.demo.model.Role;
import ru.kata.spring.boot_security.demo.model.User;
import ru.kata.spring.boot_security.demo.service.RoleService;
import ru.kata.spring.boot_security.demo.service.UserService;
import ru.kata.spring.boot_security.demo.validators.OnCreate;

import javax.validation.Valid;
import javax.validation.groups.Default;
import java.util.*;

@RestController
@RequestMapping("/api/users")
@Slf4j
public class UserRestController {

    private UserService userService;
    private RoleService roleService;

    @Autowired
    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    @Autowired
    public void setRoleService(RoleService roleService) {
        this.roleService = roleService;
    }

    // GET /api/users
    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        List<User> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    // GET /api/users/{id}/simple
    @GetMapping("/{id}/simple")
    public ResponseEntity<?> getUserByIdSimple(@PathVariable int id) {
        User user = userService.getUserByIdWithRoles(id);
        if (user == null) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "User not found");
            error.put("message", "User with id " + id + " does not exist");
            error.put("timestamp", String.valueOf(System.currentTimeMillis()));
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }

        // Создаем Map с нужными полями
        Map<String, Object> simpleUser = new LinkedHashMap<>();
        simpleUser.put("id", user.getId());
        simpleUser.put("name", user.getName());
        simpleUser.put("age", user.getAge());
        simpleUser.put("email", user.getEmail());
        simpleUser.put("username", user.getUsername());
        simpleUser.put("password", user.getPassword()); // Будет закодированный пароль!

        // Роли: только ID
        List<Map<String, Long>> roles = new ArrayList<>();
        for (Role role : user.getRoles()) {
            Map<String, Long> roleMap = new HashMap<>();
            roleMap.put("id", role.getId());
            roles.add(roleMap);
        }
        simpleUser.put("roles", roles);

        return ResponseEntity.ok(simpleUser);
    }

    // GET /api/users/{id}
    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(@PathVariable int id) {
        User user = userService.getUserByIdWithRoles(id);
        if (user == null) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "User not found");
            error.put("message", "User with id " + id + " does not exist");
            error.put("timestamp", String.valueOf(System.currentTimeMillis()));

            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
        return ResponseEntity.ok(user);
    }


    @PostMapping
    public ResponseEntity<User> createUser(@Validated({Default.class, OnCreate.class}) @RequestBody User parsedUser) {
        User user = new User();w

        user.setUsername(parsedUser.getUsername());
        user.setPassword(parsedUser.getPassword());
        user.setEmail(parsedUser.getEmail());
        user.setName(parsedUser.getName());
        user.setAge(parsedUser.getAge());

        if (parsedUser.getRoles() == null || parsedUser.getRoles().isEmpty()) {
            Role userRole = roleService.getRoleByName("ROLE_USER");
            if (userRole != null) {
                user.setRoles(Set.of(userRole));
            }
        } else {
            Set<Role> roles = new HashSet<>();
            for (Role role : parsedUser.getRoles()) {
                if (role.getId() != 0) {
                    Role fullRole = roleService.getRoleById(role.getId());
                    if (fullRole != null) {
                        roles.add(fullRole);
                    }
                }
            }
            user.setRoles(roles);
        }

        userService.save(user);

        User savedUser = userService.getUserByIdWithRoles(user.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(savedUser);
    }

    // PUT /api/users/{id}
    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable int id,
                                        @Validated(Default.class) @RequestBody User parsedUser) {

        try {
            // 1. Проверяем существование пользователя
            User existingUser = userService.getUserByIdWithRoles(id);
            if (existingUser == null) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "User not found");
                error.put("message", "User with id " + id + " does not exist");
                error.put("timestamp", System.currentTimeMillis());
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
            }

            // 2. Проверка уникальности username (только если изменился)
            if (!existingUser.getUsername().equals(parsedUser.getUsername())) {
                User userWithSameUsername = userService.getUserByUsername(parsedUser.getUsername());
                if (userWithSameUsername != null && userWithSameUsername.getId() != id) {
                    Map<String, String> error = new HashMap<>();
                    error.put("error", "Username already exists");
                    error.put("message", "Username '" + parsedUser.getUsername() +
                            "' is already taken by another user (ID: " + userWithSameUsername.getId() + ")");
                    error.put("timestamp", String.valueOf(System.currentTimeMillis()));
                    return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
                }
            }

            // 3. Создаем новый объект для обновления
            User updatedUser = new User();
            updatedUser.setId(id);
            updatedUser.setUsername(parsedUser.getUsername());
            updatedUser.setPassword(parsedUser.getPassword());
            updatedUser.setEmail(parsedUser.getEmail());
            updatedUser.setName(parsedUser.getName());
            updatedUser.setAge(parsedUser.getAge());

            // 4. Обработка ролей
            if (parsedUser.getRoles() == null || parsedUser.getRoles().isEmpty()) {
                // Сохраняем текущие роли
                updatedUser.setRoles(existingUser.getRoles());
            } else {
                // Загружаем полные объекты Role из БД
                Set<Role> roles = new HashSet<>();
                for (Role role : parsedUser.getRoles()) {
                    if (role.getId() != 0) {
                        Role fullRole = roleService.getRoleById(role.getId());
                        if (fullRole != null) {
                            roles.add(fullRole);
                        }
                    }
                }
                updatedUser.setRoles(roles);
            }

            // 5. Сохраняем обновленного пользователя
            userService.update(updatedUser);

            // 6. Получаем обновленного пользователя для ответа
            User savedUser = userService.getUserByIdWithRoles(id);

            // 7. Возвращаем ответ
            return ResponseEntity.ok(savedUser);

        } catch (Exception e) {
            // Обработка неожиданных ошибок
            Map<String, String> error = new HashMap<>();
            error.put("error", "Internal server error");
            error.put("message", e.getMessage());
            error.put("timestamp", String.valueOf(System.currentTimeMillis()));

            e.printStackTrace(); // Для отладки

            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(error);
        }
    }

    // DELETE /api/users/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable int id) {
        if (!userService.existsById(id)) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(Map.of(
                            "error", "User not found",
                            "message", "User with id " + id + " does not exist",
                            "timestamp", System.currentTimeMillis()
                    ));
        }

        userService.delete(id);
        return ResponseEntity.noContent().build();
    }
}