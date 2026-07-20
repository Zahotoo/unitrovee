package com.unitrovee.auth.verification;

public interface VerificationEmailSender {

    void sendVerificationCode(String email, String code);
}
