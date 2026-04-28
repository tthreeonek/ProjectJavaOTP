package com.promoit.otp.model;

import java.time.LocalDateTime;

public class OtpCode {
    private int id;
    private String operationId;
    private int userId;
    private String code;
    private OtpStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;

    // конструкторы, геттеры, сеттеры
    public OtpCode() {}
    public OtpCode(int id, String operationId, int userId, String code, OtpStatus status,
                   LocalDateTime createdAt, LocalDateTime expiresAt) {
        this.id = id;
        this.operationId = operationId;
        this.userId = userId;
        this.code = code;
        this.status = status;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }
    // ... геттеры и сеттеры
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getOperationId() { return operationId; }
    public void setOperationId(String operationId) { this.operationId = operationId; }
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public OtpStatus getStatus() { return status; }
    public void setStatus(OtpStatus status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
}