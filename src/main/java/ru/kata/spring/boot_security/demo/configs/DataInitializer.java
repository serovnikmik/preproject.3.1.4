package ru.kata.spring.boot_security.demo.configs;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.kata.spring.boot_security.demo.model.Role;
import ru.kata.spring.boot_security.demo.model.User;
import ru.kata.spring.boot_security.demo.service.RoleService;
import ru.kata.spring.boot_security.demo.service.UserService;

import java.util.HashSet;
import java.util.Set;

@Component
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final RoleService roleService;
    private final UserService userService;

    @Autowired
    public DataInitializer(RoleService roleService, UserService userService) {
        this.roleService = roleService;
        this.userService = userService;
    }

    @Override
    @Transactional
    public void run(String... args) {
        createDefaultRoles();

        if (!userService.existsByUsername("admin")) {
            createDefaultAdmin();
        }

        if (!userService.existsByUsername("user")) {
            createDefaultUser();
        }
    }

    private void createDefaultRoles() {
        if (roleService.getRoleByName("ROLE_ADMIN") == null) {
            Role adminRole = new Role("ROLE_ADMIN");
            roleService.saveRole(adminRole);
            log.info("Created role: ROLE_ADMIN");
        }

        if (roleService.getRoleByName("ROLE_USER") == null) {
            Role userRole = new Role("ROLE_USER");
            roleService.saveRole(userRole);
            log.info("Created role: ROLE_USER");
        }
    }

    private void createDefaultAdmin() {
        try {
            log.info("Creating admin user via UserService...");

            Set<Role> roles = new HashSet<>();
            roles.add(roleService.getRoleByName("ROLE_ADMIN"));
            roles.add(roleService.getRoleByName("ROLE_USER"));

            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword("admin");
            admin.setName("Administrator");
            admin.setEmail("admin@mail.ru");
            admin.setAge(30);
            admin.setRoles(roles);

            userService.save(admin);

            log.info("Admin created successfully via UserService");
        } catch (Exception e) {
            log.error("Failed to create admin user: {}", e.getMessage(), e);
        }
    }

    private void createDefaultUser() {
        try {
            log.info("Creating default user via UserService...");

            Set<Role> roles = new HashSet<>();
            roles.add(roleService.getRoleByName("ROLE_USER"));

            User user = new User();
            user.setUsername("user");
            user.setPassword("user");
            user.setName("DefaultUser");
            user.setEmail("user@mail.ru");
            user.setAge(40);
            user.setRoles(roles);

            userService.save(user);

            log.info("Default user created successfully via UserService");
        } catch (Exception e) {
            log.error("Failed to create default user: {}", e.getMessage(), e);
        }
    }
}