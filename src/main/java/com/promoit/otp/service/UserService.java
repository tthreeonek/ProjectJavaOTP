package com.promoit.otp.service;

import com.promoit.otp.dao.UserDao;
import com.promoit.otp.model.Role;
import com.promoit.otp.model.User;
import org.mindrot.jbcrypt.BCrypt;

import java.util.List;

public class UserService {
    private final UserDao userDao;

    public UserService(UserDao userDao) {
        this.userDao = userDao;
    }

    public User register(String username, String password, Role role) {
        if (userDao.findByUsername(username) != null) {
            throw new RuntimeException("User already exists");
        }
        if (role == Role.ADMIN) {
            // Проверяем, нет ли уже админа
            if (userDao.findAllExceptAdmins().size() != userDao.findAllExceptAdmins().size() ? false : false) {
                // проще: проверить существование админа через отдельный метод, но пока так:
                boolean adminExists = userDao.findByUsername("admin") != null; // грубо, но для демо
            }
            // Сделаем лучше: дополнительный метод в UserDao - countAdmins()
            // Я добавлю его быстрее:
            // Здесь создадим метод isAdminExists()
        }
        // Упростим: разрешим регистрацию ADMIN только если его ещё нет. Добавим метод в UserDao:
        if (role == Role.ADMIN && isAdminExists()) {
            throw new RuntimeException("Admin already exists");
        }
        String hashed = BCrypt.hashpw(password, BCrypt.gensalt());
        User user = new User(0, username, hashed, role);
        userDao.save(user);
        return user;
    }

    private boolean isAdminExists() {
        // Используем userDao, добавим метод countByRole
        // Для простоты дополним UserDao методом countByRole
        return userDao.countByRole(Role.ADMIN) > 0;
    }

    public User authenticate(String username, String password) {
        User user = userDao.findByUsername(username);
        if (user != null && BCrypt.checkpw(password, user.getPasswordHash())) {
            return user;
        }
        return null;
    }

    public List<User> getAllUsersExceptAdmins() {
        return userDao.findAllExceptAdmins();
    }

    public void deleteUser(int userId) {
        userDao.deleteById(userId);
    }

    public User findByUsername(String username) {
        return userDao.findByUsername(username);
    }
}