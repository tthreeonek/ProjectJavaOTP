package com.promoit.otp;

import com.promoit.otp.api.HttpServerSetup;
import com.promoit.otp.config.AppConfig;
import com.promoit.otp.db.DatabaseInitializer;
import com.promoit.otp.service.OtpExpiryScheduler;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.Properties;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws IOException {
        // Загрузка конфигурации БД
        Properties dbProps = new Properties();
        try (InputStream is = Main.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (is == null) {
                throw new RuntimeException("db.properties not found in classpath");
            }
            dbProps.load(is);
        }

        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(dbProps.getProperty("db.url"));
        hikariConfig.setUsername(dbProps.getProperty("db.username"));
        hikariConfig.setPassword(dbProps.getProperty("db.password"));
        hikariConfig.setMaximumPoolSize(Integer.parseInt(dbProps.getProperty("db.pool.size", "5")));
        HikariDataSource dataSource = new HikariDataSource(hikariConfig);

        // Инициализация БД (создание таблиц, admin по умолчанию)
        DatabaseInitializer initializer = new DatabaseInitializer(dataSource);
        initializer.init();

        // Загрузка остальных конфигов
        AppConfig.load();

        // Запуск планировщика устаревших кодов
        OtpExpiryScheduler scheduler = new OtpExpiryScheduler(dataSource);
        scheduler.start();

        // HTTP-сервер
        HttpServer server = HttpServer.create(new InetSocketAddress(8081), 0);
        HttpServerSetup.setupEndpoints(server, dataSource);

        server.setExecutor(java.util.concurrent.Executors.newFixedThreadPool(10));
        server.start();
        logger.info("OTP Service started on port 8081");
    }
}