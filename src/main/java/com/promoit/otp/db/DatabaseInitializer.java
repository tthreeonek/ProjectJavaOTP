package com.promoit.otp.db;

import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

public class DatabaseInitializer {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseInitializer.class);
    private final DataSource dataSource;

    public DatabaseInitializer(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void init() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            // Создание таблиц
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS users (
                    id SERIAL PRIMARY KEY,
                    username VARCHAR(50) UNIQUE NOT NULL,
                    password_hash VARCHAR(255) NOT NULL,
                    role VARCHAR(20) NOT NULL CHECK (role IN ('USER', 'ADMIN'))
                );
                """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS otp_config (
                    id INTEGER PRIMARY KEY DEFAULT 1 CHECK (id = 1),
                    code_length INT NOT NULL DEFAULT 6,
                    ttl_seconds INT NOT NULL DEFAULT 300
                );
                """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS otp_codes (
                    id SERIAL PRIMARY KEY,
                    operation_id VARCHAR(100) NOT NULL,
                    user_id INT REFERENCES users(id) ON DELETE CASCADE,
                    code VARCHAR(20) NOT NULL,
                    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
                        CHECK (status IN ('ACTIVE', 'EXPIRED', 'USED')),
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    expires_at TIMESTAMP NOT NULL
                );
                """);

            // Вставка конфигурации по умолчанию, если нет
            stmt.execute("""
                INSERT INTO otp_config (id, code_length, ttl_seconds)
                VALUES (1, 6, 300)
                ON CONFLICT (id) DO NOTHING;
                """);

            // Создание администратора по умолчанию (admin/admin), если ещё нет
            String adminUser = "admin";
            String adminPass = "admin";
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM users WHERE role = 'ADMIN'");
            rs.next();
            if (rs.getInt(1) == 0) {
                String hashed = BCrypt.hashpw(adminPass, BCrypt.gensalt());
                PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO users (username, password_hash, role) VALUES (?, ?, 'ADMIN')");
                ps.setString(1, adminUser);
                ps.setString(2, hashed);
                ps.executeUpdate();
                logger.info("Default admin created (admin/admin)");
            }

        } catch (Exception e) {
            logger.error("Database initialization failed", e);
            throw new RuntimeException(e);
        }
    }
}