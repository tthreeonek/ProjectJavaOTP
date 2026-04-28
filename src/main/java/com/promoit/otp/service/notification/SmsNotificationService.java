package com.promoit.otp.service.notification;

import com.promoit.otp.config.AppConfig;
import org.jsmpp.bean.*;
import org.jsmpp.session.BindParameter;
import org.jsmpp.session.SMPPSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;

public class SmsNotificationService implements NotificationService {
    private static final Logger logger = LoggerFactory.getLogger(SmsNotificationService.class);
    private final String host;
    private final int port;
    private final String systemId;
    private final String password;
    private final String systemType;
    private final String sourceAddr;

    public SmsNotificationService() {
        host = AppConfig.getSmppHost();
        port = AppConfig.getSmppPort();
        systemId = AppConfig.getSmppSystemId();
        password = AppConfig.getSmppPassword();
        systemType = AppConfig.getSmppSystemType();
        sourceAddr = AppConfig.getSmppSourceAddr();
    }

    @Override
    public void send(String destination, String code) {
        SMPPSession session = new SMPPSession();
        try {
            BindParameter bindParam = new BindParameter(
                    BindType.BIND_TX,
                    systemId,
                    password,
                    systemType,
                    TypeOfNumber.UNKNOWN,
                    NumberingPlanIndicator.UNKNOWN,
                    sourceAddr
            );
            session.connectAndBind(host, port, bindParam);

            String message = "Your code: " + code;
            session.submitShortMessage(
                    systemType,
                    TypeOfNumber.UNKNOWN,
                    NumberingPlanIndicator.UNKNOWN,
                    sourceAddr,
                    TypeOfNumber.UNKNOWN,
                    NumberingPlanIndicator.UNKNOWN,
                    destination,
                    new ESMClass(),
                    (byte) 0,
                    (byte) 1,
                    null, null,
                    new RegisteredDelivery(SMSCDeliveryReceipt.DEFAULT),
                    (byte) 0,
                    new GeneralDataCoding(Alphabet.ALPHA_DEFAULT),
                    (byte) 0,
                    message.getBytes(StandardCharsets.UTF_8)
            );
            logger.info("SMS sent to {}", destination);
        } catch (Exception e) {
            logger.error("SMS sending failed", e);
        } finally {
            session.unbindAndClose();
        }
    }
}