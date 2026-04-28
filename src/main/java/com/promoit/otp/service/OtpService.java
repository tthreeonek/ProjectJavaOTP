package com.promoit.otp.service;

import com.promoit.otp.dao.OtpCodeDao;
import com.promoit.otp.dao.OtpConfigDao;
import com.promoit.otp.model.OtpCode;
import com.promoit.otp.model.OtpConfig;
import com.promoit.otp.model.OtpStatus;
import com.promoit.otp.service.notification.NotificationService;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class OtpService {
    private final OtpConfigDao configDao;
    private final OtpCodeDao codeDao;
    private final Map<String, NotificationService> notificationServices; // key: "sms","email","telegram","file"

    private final SecureRandom random = new SecureRandom();

    public OtpService(OtpConfigDao configDao, OtpCodeDao codeDao,
                      Map<String, NotificationService> notificationServices) {
        this.configDao = configDao;
        this.codeDao = codeDao;
        this.notificationServices = notificationServices;
    }

    public void generateAndSend(String operationId, int userId, List<String> channels, String destination) {
        OtpConfig config = configDao.getConfig();
        String code = generateCode(config.getCodeLength());
        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(config.getTtlSeconds());

        OtpCode otpCode = new OtpCode();
        otpCode.setOperationId(operationId);
        otpCode.setUserId(userId);
        otpCode.setCode(code);
        otpCode.setStatus(OtpStatus.ACTIVE);
        otpCode.setExpiresAt(expiresAt);
        otpCode.setCreatedAt(LocalDateTime.now());
        codeDao.save(otpCode);

        for (String channel : channels) {
            NotificationService ns = notificationServices.get(channel.toLowerCase());
            if (ns != null) {
                ns.send(destination, code);
            }
        }
    }

    public boolean validateCode(String operationId, int userId, String code) {
        OtpCode existing = codeDao.findActiveByOperationAndUser(operationId, userId);
        if (existing == null) {
            return false;
        }
        if (existing.getExpiresAt().isBefore(LocalDateTime.now())) {
            codeDao.updateStatus(existing.getId(), OtpStatus.EXPIRED);
            return false;
        }
        if (existing.getCode().equals(code)) {
            codeDao.updateStatus(existing.getId(), OtpStatus.USED);
            return true;
        }
        return false;
    }

    private String generateCode(int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }
}