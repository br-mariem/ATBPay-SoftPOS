package com.atb.nfcapp;

import android.content.Context;
import android.widget.Toast;

public class MockSmsSender implements SmsSender {

    private final Context context;
    private String expectedCode;

    public MockSmsSender(Context context) {
        this.context = context;
    }

    @Override
    public void sendVerificationCode(String phoneNumber, SmsCallback callback) {
        int randomCode = (int) (1000 + Math.random() * 9000);
        expectedCode = String.valueOf(randomCode);
        Toast.makeText(context, "✉️ [SMS simulé] Votre code de sécurité est " + expectedCode, Toast.LENGTH_LONG).show();
        callback.onCodeSent();
    }

    @Override
    public void verifyCode(String enteredCode, SmsCallback callback) {
        if (expectedCode != null && enteredCode.equals(expectedCode)) {
            callback.onVerificationSuccess();
        } else {
            callback.onVerificationFailed("Code SMS incorrect. Réessayez.");
        }
    }

    @Override
    public String getExpectedCodeForDebug() {
        return expectedCode;
    }
}