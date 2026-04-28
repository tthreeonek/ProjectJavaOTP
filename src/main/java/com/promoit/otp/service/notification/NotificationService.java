package com.promoit.otp.service.notification;

public interface NotificationService {
    void send(String destination, String code);
}