package com.promoit.otp.api;

import com.google.gson.JsonObject;
import com.promoit.otp.dao.OtpConfigDao;
import com.promoit.otp.model.OtpConfig;
import com.promoit.otp.model.User;
import com.promoit.otp.service.AuthService;
import com.promoit.otp.service.UserService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import io.jsonwebtoken.Claims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class AdminHandler implements HttpHandler {
    private static final Logger logger = LoggerFactory.getLogger(AdminHandler.class);
    private final UserService userService;
    private final OtpConfigDao configDao;
    private final AuthService authService;

    public AdminHandler(UserService userService, OtpConfigDao configDao, AuthService authService) {
        this.userService = userService;
        this.configDao = configDao;
        this.authService = authService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        // Проверка токена и роли ADMIN
        String token = extractToken(exchange);
        if (token == null) {
            sendError(exchange, 401, "Missing token");
            return;
        }
        Claims claims;
        try {
            claims = authService.parseToken(token);
        } catch (Exception e) {
            sendError(exchange, 403, "Invalid token");
            return;
        }
        if (!"ADMIN".equals(claims.get("role"))) {
            sendError(exchange, 403, "Forbidden");
            return;
        }

        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();
        // ожидаем /api/admin/...
        try {
            if ("PUT".equals(method) && path.equals("/api/admin/config")) {
                updateConfig(exchange);
            } else if ("GET".equals(method) && path.equals("/api/admin/users")) {
                listUsers(exchange);
            } else if ("DELETE".equals(method) && path.matches("/api/admin/users/\\d+")) {
                deleteUser(exchange, path);
            } else {
                exchange.sendResponseHeaders(404, -1);
            }
        } catch (Exception e) {
            logger.error("Admin handler error", e);
            sendError(exchange, 500, "Internal error");
        }
    }

    private void updateConfig(HttpExchange exchange) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        OtpConfig config = JsonUtil.fromJson(body, OtpConfig.class);
        configDao.updateConfig(config);
        sendResponse(exchange, 200, JsonUtil.toJson(config));
        logger.info("OTP config updated: length={}, ttl={}", config.getCodeLength(), config.getTtlSeconds());
    }

    private void listUsers(HttpExchange exchange) throws IOException {
        List<User> users = userService.getAllUsersExceptAdmins();
        sendResponse(exchange, 200, JsonUtil.toJson(users));
        logger.info("Admin requested user list");
    }

    private void deleteUser(HttpExchange exchange, String path) throws IOException {
        int userId = Integer.parseInt(path.substring("/api/admin/users/".length()));
        userService.deleteUser(userId);
        sendResponse(exchange, 200, "{\"message\":\"User deleted\"}");
        logger.info("Admin deleted user id={}", userId);
    }

    private String extractToken(HttpExchange exchange) {
        String auth = exchange.getRequestHeaders().getFirst("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            return auth.substring(7);
        }
        return null;
    }

    private void sendResponse(HttpExchange exchange, int code, String response) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(code, response.getBytes().length);
        OutputStream os = exchange.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }

    private void sendError(HttpExchange exchange, int code, String message) throws IOException {
        JsonObject err = new JsonObject();
        err.addProperty("error", message);
        String json = JsonUtil.toJson(err);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(code, json.getBytes().length);
        OutputStream os = exchange.getResponseBody();
        os.write(json.getBytes());
        os.close();
    }
}