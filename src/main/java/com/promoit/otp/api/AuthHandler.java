package com.promoit.otp.api;

import com.promoit.otp.model.Role;
import com.promoit.otp.model.User;
import com.promoit.otp.service.AuthService;
import com.promoit.otp.service.UserService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.gson.JsonObject;
import java.io.*;
import java.nio.charset.StandardCharsets;

public class AuthHandler implements HttpHandler {
    private static final Logger logger = LoggerFactory.getLogger(AuthHandler.class);
    private final UserService userService;
    private final AuthService authService;

    public AuthHandler(UserService userService, AuthService authService) {
        this.userService = userService;
        this.authService = authService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();
        try {
            if ("POST".equals(method) && path.equals("/api/register")) {
                handleRegister(exchange);
            } else if ("POST".equals(method) && path.equals("/api/login")) {
                handleLogin(exchange);
            } else {
                exchange.sendResponseHeaders(404, -1);
            }
        } catch (Exception e) {
            logger.error("Error in auth handler", e);
            exchange.sendResponseHeaders(500, -1);
        }
    }

    private void handleRegister(HttpExchange exchange) throws IOException {
        // читаем тело
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        JsonObject json = JsonUtil.fromJson(body, JsonObject.class);
        String username = json.get("username").getAsString();
        String password = json.get("password").getAsString();
        Role role = Role.valueOf(json.get("role").getAsString().toUpperCase());

        try {
            User user = userService.register(username, password, role);
            String token = authService.generateToken(user);
            JsonObject resp = new JsonObject();
            resp.addProperty("token", token);
            sendResponse(exchange, 201, JsonUtil.toJson(resp));
        } catch (RuntimeException e) {
            sendError(exchange, 400, e.getMessage());
        }
    }

    private void handleLogin(HttpExchange exchange) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        JsonObject json = JsonUtil.fromJson(body, JsonObject.class);
        String username = json.get("username").getAsString();
        String password = json.get("password").getAsString();

        User user = userService.authenticate(username, password);
        if (user != null) {
            String token = authService.generateToken(user);
            JsonObject resp = new JsonObject();
            resp.addProperty("token", token);
            sendResponse(exchange, 200, JsonUtil.toJson(resp));
        } else {
            sendError(exchange, 401, "Invalid credentials");
        }
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