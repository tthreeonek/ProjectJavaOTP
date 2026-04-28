package com.promoit.otp.api;

import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.promoit.otp.model.User;
import com.promoit.otp.service.AuthService;
import com.promoit.otp.service.OtpService;
import com.promoit.otp.service.UserService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import io.jsonwebtoken.Claims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class UserHandler implements HttpHandler {
    private static final Logger logger = LoggerFactory.getLogger(UserHandler.class);
    private final OtpService otpService;
    private final AuthService authService;
    private final UserService userService;

    public UserHandler(OtpService otpService, AuthService authService, UserService userService) {
        this.otpService = otpService;
        this.authService = authService;
        this.userService = userService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
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

        String username = claims.getSubject();
        User user = userService.findByUsername(username); // нужен метод в UserService
        if (user == null) {
            sendError(exchange, 401, "User not found");
            return;
        }

        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();

        try {
            if ("POST".equals(method) && path.equals("/api/user/generate-otp")) {
                generateOtp(exchange, user.getId());
            } else if ("POST".equals(method) && path.equals("/api/user/validate-otp")) {
                validateOtp(exchange, user.getId());
            } else {
                exchange.sendResponseHeaders(404, -1);
            }
        } catch (Exception e) {
            logger.error("User handler error", e);
            sendError(exchange, 500, "Internal error");
        }
    }

    private void generateOtp(HttpExchange exchange, int userId) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        JsonObject json = JsonUtil.fromJson(body, JsonObject.class);
        String operationId = json.get("operationId").getAsString();
        List<String> channels = JsonUtil.fromJson(json.get("channels").toString(), new TypeToken<List<String>>(){}.getType());
        // destination: обычно берется из профиля пользователя (email/phone). Для упрощения передадим как поле.
        String destination = json.has("destination") ? json.get("destination").getAsString() : "default";

        otpService.generateAndSend(operationId, userId, channels, destination);
        JsonObject resp = new JsonObject();
        resp.addProperty("message", "OTP generated and sent");
        sendResponse(exchange, 200, JsonUtil.toJson(resp));
        logger.info("OTP generated for user {} operation {}", userId, operationId);
    }

    private void validateOtp(HttpExchange exchange, int userId) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        JsonObject json = JsonUtil.fromJson(body, JsonObject.class);
        String operationId = json.get("operationId").getAsString();
        String code = json.get("code").getAsString();

        boolean valid = otpService.validateCode(operationId, userId, code);
        JsonObject resp = new JsonObject();
        resp.addProperty("valid", valid);
        sendResponse(exchange, 200, JsonUtil.toJson(resp));
        logger.info("OTP validation for user {} operation {}: {}", userId, operationId, valid);
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