package com.promoit.otp.model;

public class OtpConfig {
    private int id = 1;
    private int codeLength = 6;
    private int ttlSeconds = 300;

    public OtpConfig() {}
    public OtpConfig(int codeLength, int ttlSeconds) {
        this.codeLength = codeLength;
        this.ttlSeconds = ttlSeconds;
    }
    // геттеры/сеттеры
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getCodeLength() { return codeLength; }
    public void setCodeLength(int codeLength) { this.codeLength = codeLength; }
    public int getTtlSeconds() { return ttlSeconds; }
    public void setTtlSeconds(int ttlSeconds) { this.ttlSeconds = ttlSeconds; }
}