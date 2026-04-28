package com.promoit.otp.service.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class FileNotificationService implements NotificationService {
    private static final Logger logger = LoggerFactory.getLogger(FileNotificationService.class);

    @Override
    public void send(String destination, String code) {
        String fileName = "otp_code_" + destination + ".txt";  // destination как идентификатор операции
        try (PrintWriter out = new PrintWriter(new FileWriter(fileName))) {
            out.println("OTP Code: " + code);
            logger.info("OTP saved to file {}", fileName);
        } catch (IOException e) {
            logger.error("Error writing OTP to file", e);
        }
    }
}