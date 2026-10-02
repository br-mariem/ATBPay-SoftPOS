package com.atb.nfcapp;

public interface SmsSender {

    interface SmsCallback {
        void onCodeSent();
        void onVerificationSuccess();
        void onVerificationFailed(String errorMessage);
    }

    void sendVerificationCode(String phoneNumber, SmsCallback callback);
    void verifyCode(String enteredCode, SmsCallback callback);
    String getExpectedCodeForDebug();
}