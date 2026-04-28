package com.promoit.otp.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class AppConfig {

    private static final Properties jwtProps = new Properties();
    private static final Properties emailProps = new Properties();
    private static final Properties smsProps = new Properties();
    private static final Properties telegramProps = new Properties();

    public static void load() {
        try {
            loadProperties(jwtProps, "jwt.properties");
            loadProperties(emailProps, "email.properties");
            loadProperties(smsProps, "sms.properties");
            loadProperties(telegramProps, "telegram.properties");
        } catch (Exception e) {
            throw new RuntimeException("Failed to load configuration", e);
        }
    }

    private static void loadProperties(Properties props, String fileName) {
        try (InputStream is = AppConfig.class.getClassLoader().getResourceAsStream(fileName)) {
            if (is == null) {
                throw new RuntimeException("Resource not found: " + fileName);
            }
            props.load(is);
        } catch (IOException e) {
            throw new RuntimeException("Error loading " + fileName, e);
        }
    }

    // JWT
    public static String getJwtSecret() {
        return jwtProps.getProperty("jwt.secret");
    }
    public static long getJwtExpiration() {
        return Long.parseLong(jwtProps.getProperty("jwt.expiration", "3600000"));
    }

    // Email
    public static Properties getEmailProperties() {
        return emailProps;
    }

    // SMS
    public static String getSmppHost() {
        return smsProps.getProperty("smpp.host");
    }
    public static int getSmppPort() {
        return Integer.parseInt(smsProps.getProperty("smpp.port"));
    }
    public static String getSmppSystemId() {
        return smsProps.getProperty("smpp.system_id");
    }
    public static String getSmppPassword() {
        return smsProps.getProperty("smpp.password");
    }
    public static String getSmppSystemType() {
        return smsProps.getProperty("smpp.system_type");
    }
    public static String getSmppSourceAddr() {
        return smsProps.getProperty("smpp.source_addr");
    }

    // Telegram
    public static String getTelegramBotToken() {
        return telegramProps.getProperty("telegram.bot.token");
    }
    public static String getTelegramChatId() {
        return telegramProps.getProperty("telegram.chat.id");
    }
}