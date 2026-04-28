package com.promoit.otp.service;

import com.promoit.otp.dao.OtpCodeDao;
import com.promoit.otp.model.OtpCode;
import com.promoit.otp.model.OtpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class OtpExpiryScheduler {
    private static final Logger logger = LoggerFactory.getLogger(OtpExpiryScheduler.class);
    private final OtpCodeDao otpCodeDao;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public OtpExpiryScheduler(DataSource dataSource) {
        this.otpCodeDao = new OtpCodeDao(dataSource);
    }

    public void start() {
        scheduler.scheduleAtFixedRate(this::markExpiredCodes, 0, 1, TimeUnit.MINUTES);
    }

    private void markExpiredCodes() {
        try {
            List<OtpCode> expiredList = otpCodeDao.findAllActiveExpired(LocalDateTime.now());
            for (OtpCode code : expiredList) {
                otpCodeDao.updateStatus(code.getId(), OtpStatus.EXPIRED);
                logger.info("Code {} (operation {}) marked EXPIRED", code.getId(), code.getOperationId());
            }
        } catch (Exception e) {
            logger.error("Error marking expired codes", e);
        }
    }
}