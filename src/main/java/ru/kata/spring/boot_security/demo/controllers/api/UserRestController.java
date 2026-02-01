package ru.kata.spring.boot_security.demo.controllers.api;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import ru.kata.spring.boot_security.demo.model.Role;
import ru.kata.spring.boot_security.demo.model.User;
import ru.kata.spring.boot_security.demo.service.RoleService;
import ru.kata.spring.boot_security.demo.service.UserService;

import javax.validation.Valid;
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

    // POST /api/users
//        {
//        "username": "newuser4",
//            "password": "password123",
//            "email": "new@example.com",
//            "name": "New User",
//            "age": 30,
//            "roles": [
//        {
//            "id": 2
//        }
//    ]
//    }
    @PostMapping
    public ResponseEntity<User> createUser(@Valid @RequestBody User parsedUser) {
        User user = new User();

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

    // DELETE /api/users/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable int id) {
        User user = userService.getUserByIdWithRoles(id);
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