package ru.kata.spring.boot_security.demo.validators;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.kata.spring.boot_security.demo.dao.UserDAOImpl;
import ru.kata.spring.boot_security.demo.model.User;
import ru.kata.spring.boot_security.demo.service.UserService;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

@Component
@Slf4j
public class UniqueUsernameValidator implements
        ConstraintValidator<UniqueUsername, String>,
        ApplicationContextAware {

    private static ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        UniqueUsernameValidator.applicationContext = applicationContext;
    }

    @Override
    public boolean isValid(String username, ConstraintValidatorContext context) {
        log.info("checking isValid username = {}", username);
        if (username == null || username.trim().isEmpty()) {
            return true;
        }

        if ("admin".equals(username) || "user".equals(username)) {
            log.info("Skipping validation for default user and admin: {}", username);
            return true;
        }

        UserService userService = applicationContext.getBean(UserService.class);
        log.info("username unique? -> {}", !userService.existsByUsername(username));
        return !userService.existsByUsername(username);
    }
}