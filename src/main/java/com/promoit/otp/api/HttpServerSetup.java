package com.promoit.otp.api;

import com.promoit.otp.dao.OtpCodeDao;
import com.promoit.otp.dao.OtpConfigDao;
import com.promoit.otp.dao.UserDao;
import com.promoit.otp.service.*;
import com.promoit.otp.service.notification.*;
import com.sun.net.httpserver.HttpServer;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

public class HttpServerSetup {
    public static void setupEndpoints(HttpServer server, DataSource dataSource) {
        // DAO
        UserDao userDao = new UserDao(dataSource);
        OtpConfigDao configDao = new OtpConfigDao(dataSource);
        OtpCodeDao codeDao = new OtpCodeDao(dataSource);

        // Сервисы
        UserService userService = new UserService(userDao);
        AuthService authService = new AuthService();

        // Уведомления
        Map<String, NotificationService> notifications = new HashMap<>();
        notifications.put("email", new EmailNotificationService());
        notifications.put("sms", new SmsNotificationService());
        notifications.put("telegram", new TelegramNotificationService());
        notifications.put("file", new FileNotificationService());

        OtpService otpService = new OtpService(configDao, codeDao, notifications);

        // Регистрируем контексты
        server.createContext("/api/register", new AuthHandler(userService, authService));
        server.createContext("/api/login", new AuthHandler(userService, authService));
        server.createContext("/api/admin", new AdminHandler(userService, configDao, authService));
        server.createContext("/api/user", new UserHandler(otpService, authService, userService));
    }
}