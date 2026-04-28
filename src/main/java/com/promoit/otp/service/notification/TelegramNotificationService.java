package com.promoit.otp.service.notification;

import com.promoit.otp.config.AppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

public class TelegramNotificationService implements NotificationService {
    private static final Logger logger = LoggerFactory.getLogger(TelegramNotificationService.class);
    private final String botToken;
    private final String chatId;
    private final HttpClient httpClient;

    public TelegramNotificationService() {
        botToken = AppConfig.getTelegramBotToken();
        chatId = AppConfig.getTelegramChatId();
        httpClient = HttpClient.newHttpClient();
    }

    @Override
    public void send(String destination, String code) {
        String text = String.format("Your verification code: %s", code);
        String encodedText = URLEncoder.encode(text, StandardCharsets.UTF_8);
        String url = String.format("https://api.telegram.org/bot%s/sendMessage?chat_id=%s&text=%s",
                botToken, chatId, encodedText);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                logger.info("Telegram message sent");
            } else {
                logger.error("Telegram API error: {}", response.statusCode());
            }
        } catch (IOException | InterruptedException e) {
            logger.error("Telegram sending failed", e);
            Thread.currentThread().interrupt();
        }
    }
}