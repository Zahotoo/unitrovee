package com.unitrovee.auth.verification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("!prod")
public class ConsoleVerificationEmailSender implements VerificationEmailSender {

    @Override
    public void sendVerificationCode(String email, String code) {
        log.info("DEV email verification code for {}: {}", email, code);
    }
}
