package com.promoit.otp.dao;

import com.promoit.otp.model.OtpCode;
import com.promoit.otp.model.OtpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class OtpCodeDao {
    private static final Logger logger = LoggerFactory.getLogger(OtpCodeDao.class);
    private final DataSource dataSource;

    public OtpCodeDao(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void save(OtpCode code) {
        String sql = "INSERT INTO otp_codes (operation_id, user_id, code, status, expires_at) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, code.getOperationId());
            ps.setInt(2, code.getUserId());
            ps.setString(3, code.getCode());
            ps.setString(4, code.getStatus().name());
            ps.setTimestamp(5, Timestamp.valueOf(code.getExpiresAt()));
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) {
                code.setId(keys.getInt(1));
            }
        } catch (SQLException e) {
            logger.error("Error saving OTP code", e);
            throw new RuntimeException(e);
        }
    }

    public OtpCode findActiveByOperationAndUser(String operationId, int userId) {
        String sql = "SELECT * FROM otp_codes WHERE operation_id = ? AND user_id = ? AND status = 'ACTIVE' ORDER BY created_at DESC LIMIT 1";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, operationId);
            ps.setInt(2, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return mapCode(rs);
            }
        } catch (SQLException e) {
            logger.error("Error finding active OTP", e);
        }
        return null;
    }

    public void updateStatus(int codeId, OtpStatus status) {
        String sql = "UPDATE otp_codes SET status = ? WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setInt(2, codeId);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error updating OTP status", e);
        }
    }

    public List<OtpCode> findAllActiveExpired(LocalDateTime now) {
        List<OtpCode> list = new ArrayList<>();
        String sql = "SELECT * FROM otp_codes WHERE status = 'ACTIVE' AND expires_at < ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(now));
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(mapCode(rs));
            }
        } catch (SQLException e) {
            logger.error("Error finding expired codes", e);
        }
        return list;
    }

    public void deleteByUserId(int userId) {
        String sql = "DELETE FROM otp_codes WHERE user_id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error deleting OTP codes for user", e);
        }
    }

    private OtpCode mapCode(ResultSet rs) throws SQLException {
        OtpCode code = new OtpCode();
        code.setId(rs.getInt("id"));
        code.setOperationId(rs.getString("operation_id"));
        code.setUserId(rs.getInt("user_id"));
        code.setCode(rs.getString("code"));
        code.setStatus(OtpStatus.valueOf(rs.getString("status")));
        code.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        code.setExpiresAt(rs.getTimestamp("expires_at").toLocalDateTime());
        return code;
    }
}