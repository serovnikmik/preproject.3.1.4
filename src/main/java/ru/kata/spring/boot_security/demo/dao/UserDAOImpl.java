package ru.kata.spring.boot_security.demo.dao;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.stereotype.Repository;
import ru.kata.spring.boot_security.demo.model.User;

import javax.persistence.*;
import java.util.List;

@Repository
@Slf4j
public class UserDAOImpl implements UserDAO {

    @PersistenceContext
    private EntityManager em;

    @Autowired
    public UserDAOImpl(LocalContainerEntityManagerFactoryBean lcemfb) {
        EntityManagerFactory emf = lcemfb.getObject();
        em = emf.createEntityManager();
    }

    @Override
    public List<User> getAllUsers() {
        TypedQuery<User> q = em.createQuery("SELECT u FROM User u ORDER BY u.id", User.class);
        return q.getResultList();
    }

    @Override
    public User getUser(int userID) {
        User userToFind = new User();
        try {
            userToFind = em.find(User.class, userID);
        } catch (Exception e) {
            log.error("Failed to get user with id={}", userID, e);
        }
        return userToFind;
    }

    @Override
    public void save(User user) {
        try {
            em.persist(user);
            log.info("User saved: {}", user.getUsername());
        } catch (EntityExistsException e) {
            log.error("Failed to save user with username='{}'. User already exists",
                    user.getUsername(), e);
        }
    }

    @Override
    public void update(User user) {
        try {
            em.merge(user);
            log.info("User updated: {}", user.getUsername());
        } catch (Exception e) {
            log.error("Failed to update user with id={}, username='{}'",
                    user.getId(), user.getUsername(), e);
        }
    }

    @Override
    public void saveUsers(List<User> list) {
        try {
            for (User user : list) {
                em.persist(user);
            }
            log.info("Saved {} users", list.size());
        } catch (EntityExistsException e) {
            log.error("Some users already exist (same parameters)", e);
        }
    }

    @Override
    public void deleteAllUsers() {
        try {
            em.createNativeQuery("TRUNCATE TABLE user").executeUpdate();
            log.info("All users deleted");
        } catch (Exception e) {
            log.error("Failed to delete all users", e);
        }
    }

    @Override
    public void delete(int id) {
        try {
            em.createQuery("DELETE FROM User u WHERE u.id = :id")
                    .setParameter("id", id)
                    .executeUpdate();
            log.info("User deleted with id={}", id);
        } catch (Exception e) {
            log.error("Failed to delete user with id={}", id, e);
        }
    }

    @Override
    public User getUserByUsername(String username) {
        User user = null;
        try {
            TypedQuery<User> q = em.createQuery(
                    "SELECT u FROM User u WHERE u.username = :username", User.class);
            q.setParameter("username", username);
            user = q.getSingleResult();
            log.debug("Found user by username: {}", username);
        } catch (NoResultException e) {
            log.debug("User with username '{}' not found", username);
        } catch (Exception e) {
            log.error("Failed to find user with username='{}'", username, e);
        }
        return user;
    }

    @Override
    public User getUserByUsernameWithRoles(String username) {
        User user = null;
        try {
            TypedQuery<User> q = em.createQuery(
                    "SELECT DISTINCT u FROM User u LEFT JOIN FETCH u.roles WHERE u.username = :username",
                    User.class);
            q.setParameter("username", username);
            user = q.getSingleResult();
            log.debug("Found user with roles by username: {}", username);
        } catch (NoResultException e) {
            log.debug("User with username '{}' not found", username);
        } catch (Exception e) {
            log.error("Failed to find user with username='{}'", username, e);
        }
        return user;
    }

    @Override
    public User getUserByIdWithRoles(int id) {
        User user = null;
        try {
            TypedQuery<User> q = em.createQuery(
                    "SELECT u FROM User u LEFT JOIN FETCH u.roles WHERE u.id = :id",
                    User.class);
            q.setParameter("id", id);
            user = q.getSingleResult();
            log.debug("Found user with roles by id: {}", id);
        } catch (NoResultException e) {
            log.debug("User with id '{}' not found", id);
        } catch (Exception e) {
            log.error("Failed to find user with id={}", id, e);
        }
        return user;
    }

    @Override
    public List<User> getAllUsersWithRoles() {
        try {
            TypedQuery<User> q = em.createQuery(
                    "SELECT DISTINCT u FROM User u LEFT JOIN FETCH u.roles ORDER BY u.id",
                    User.class);
            List<User> users = q.getResultList();
            log.debug("Found {} users with roles", users.size());
            return users;
        } catch (Exception e) {
            log.error("Failed to get all users with roles", e);
            return List.of();
        }
    }

    public boolean existsByUsername(String username) {
        try {
            Long count = em.createQuery(
                            "SELECT COUNT(u) FROM User u WHERE u.username = :username", Long.class)
                    .setParameter("username", username)
                    .getSingleResult();
            return count > 0;
        } catch (Exception e) {
            log.error("Failed to check if username exists: '{}'", username, e);
            return false;
        }
    }

    public boolean existsById(int id) {
        try {
            Long count = em.createQuery(
                            "SELECT COUNT(u) FROM User u WHERE u.id = :id", Long.class)
                    .setParameter("id", id)
                    .getSingleResult();
            return count > 0;
        } catch (Exception e) {
            log.error("Failed to check if username exists: '{}'", id, e);
            return false;
        }
    }
}