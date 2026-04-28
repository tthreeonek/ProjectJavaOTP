package com.promoit.otp.dao;

import com.promoit.otp.model.OtpConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class OtpConfigDao {
    private static final Logger logger = LoggerFactory.getLogger(OtpConfigDao.class);
    private final DataSource dataSource;

    public OtpConfigDao(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public OtpConfig getConfig() {
        String sql = "SELECT code_length, ttl_seconds FROM otp_config WHERE id = 1";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new OtpConfig(rs.getInt("code_length"), rs.getInt("ttl_seconds"));
            }
        } catch (SQLException e) {
            logger.error("Error getting OTP config", e);
        }
        return new OtpConfig(); // вернуть дефолтный, если нет
    }

    public void updateConfig(OtpConfig config) {
        String sql = "UPDATE otp_config SET code_length = ?, ttl_seconds = ? WHERE id = 1";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, config.getCodeLength());
            ps.setInt(2, config.getTtlSeconds());
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error updating OTP config", e);
            throw new RuntimeException(e);
        }
    }
}