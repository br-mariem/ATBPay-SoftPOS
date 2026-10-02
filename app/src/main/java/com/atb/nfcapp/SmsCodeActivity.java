package com.atb.nfcapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class SmsCodeActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private TextView tvSubLabel;
    private TextView tvCodeText;
    private Button btnValidate;
    private String typedCode = "";

    private SmsSender smsSender;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sms_code);

        btnBack = findViewById(R.id.btnBack);
        tvSubLabel = findViewById(R.id.tvSubLabel);
        tvCodeText = findViewById(R.id.tvCodeText);
        btnValidate = findViewById(R.id.btnValidate);

        smsSender = new MockSmsSender(this);

        SharedPreferences prefs = getSharedPreferences("ATB_CONFIGS", MODE_PRIVATE);
        final String savedGsm = prefs.getString("GSM_NUMBER", "+216 29968399");
        tvSubLabel.setText("Un code de validation a été envoyé par SMS au " + savedGsm);

        final TextView tvCodeHint = findViewById(R.id.tvCodeHint);

        smsSender.sendVerificationCode(savedGsm, new SmsSender.SmsCallback() {
            @Override
            public void onCodeSent() {
                if (tvCodeHint != null) {
                    String debugCode = smsSender.getExpectedCodeForDebug();
                    if (debugCode != null) {
                        tvCodeHint.setText("(Simulé : saisissez le code " + debugCode + ")");
                    }
                }
            }

            @Override
            public void onVerificationSuccess() {}

            @Override
            public void onVerificationFailed(String errorMessage) {
                Toast.makeText(SmsCodeActivity.this, "Échec d'envoi du code : " + errorMessage, Toast.LENGTH_LONG).show();
            }
        });

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        int[] keyIds = {
                R.id.btnKey0, R.id.btnKey1, R.id.btnKey2, R.id.btnKey3,
                R.id.btnKey4, R.id.btnKey5, R.id.btnKey6, R.id.btnKey7,
                R.id.btnKey8, R.id.btnKey9
        };
        for (int i = 0; i < keyIds.length; i++) {
            final String value = String.valueOf(i);
            Button keyButton = findViewById(keyIds[i]);
            if (keyButton != null) {
                keyButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        handleKeypress(value);
                    }
                });
            }
        }

        Button btnClear = findViewById(R.id.btnKeyClear);
        if (btnClear != null) {
            btnClear.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    typedCode = "";
                    updateCodeDisplay();
                }
            });
        }

        Button btnDelete = findViewById(R.id.btnKeyDelete);
        if (btnDelete != null) {
            btnDelete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (typedCode.length() > 0) {
                        typedCode = typedCode.substring(0, typedCode.length() - 1);
                        updateCodeDisplay();
                    }
                }
            });
        }

        btnValidate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (typedCode.length() < 4) {
                    Toast.makeText(SmsCodeActivity.this, "Veuillez entrer un code à 4 chiffres", Toast.LENGTH_SHORT).show();
                    return;
                }

                final Intent currentIntent = getIntent();
                final String mode = currentIntent.getStringExtra("MODE");

                smsSender.verifyCode(typedCode, new SmsSender.SmsCallback() {
                    @Override
                    public void onCodeSent() {}

                    @Override
                    public void onVerificationSuccess() {
                        handleVerificationSuccess(mode, currentIntent);
                    }

                    @Override
                    public void onVerificationFailed(String errorMessage) {
                        handleVerificationFailure(mode, currentIntent, errorMessage);
                    }
                });
            }
        });
    }

    private void handleVerificationSuccess(String mode, Intent currentIntent) {

        if (mode != null) {
            Toast.makeText(SmsCodeActivity.this, "Identité confirmée par SMS.", Toast.LENGTH_SHORT).show();
            setResult(RESULT_OK);
            finish();
            return;
        }

        final double amount = currentIntent.getDoubleExtra("AMOUNT", 0.001);
        final String txType = currentIntent.getStringExtra("TX_TYPE");

        Toast.makeText(SmsCodeActivity.this, "Code SMS Validé !", Toast.LENGTH_SHORT).show();

        if (txType == null || !txType.equalsIgnoreCase("annulation")) {
            Intent receiptIntent = new Intent(SmsCodeActivity.this, ReceiptActivity.class);
            receiptIntent.putExtra("AMOUNT", amount);
            receiptIntent.putExtra("TX_TYPE", txType);
            receiptIntent.putExtra("IS_APPROVED", true);
            receiptIntent.putExtra("CARD_TYPE", "Visa Debit");
            startActivity(receiptIntent);
        } else {
            Intent intentConfirm = new Intent(SmsCodeActivity.this, ConfirmTransactionActivity.class);
            intentConfirm.putExtra("AMOUNT", amount);
            intentConfirm.putExtra("TX_TYPE", "annulation");

            java.text.SimpleDateFormat dateFormater = new java.text.SimpleDateFormat("dd/MM/yy", java.util.Locale.getDefault());
            java.text.SimpleDateFormat timeFormater = new java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault());
            java.util.Date systemDate = new java.util.Date();

            intentConfirm.putExtra("DATE", dateFormater.format(systemDate));
            intentConfirm.putExtra("TIME", timeFormater.format(systemDate));
            intentConfirm.putExtra("CARD_NUMBER", "474200******3016");

            long randomNum = (long) (Math.random() * 9000000000L) + 1000000000L;
            intentConfirm.putExtra("REFERENCE", String.format(java.util.Locale.US, "%010d", randomNum));

            startActivity(intentConfirm);
        }

        setResult(RESULT_OK);
        finish();
    }

    private void handleVerificationFailure(String mode, Intent currentIntent, String errorMessage) {

        if (mode != null) {
            Toast.makeText(SmsCodeActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            typedCode = "";
            updateCodeDisplay();
            return;
        }

        final double amount = currentIntent.getDoubleExtra("AMOUNT", 0.001);
        final String txType = currentIntent.getStringExtra("TX_TYPE");

        new androidx.appcompat.app.AlertDialog.Builder(SmsCodeActivity.this)
                .setTitle("Code incorrect")
                .setMessage("Code de sécurité incorrect. Souhaitez-vous simuler un REFUS de la transaction par le serveur ?")
                .setPositiveButton("Oui, Refus", new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface dialog, int which) {
                        Intent receiptIntent = new Intent(SmsCodeActivity.this, ReceiptActivity.class);
                        receiptIntent.putExtra("AMOUNT", amount);
                        receiptIntent.putExtra("TX_TYPE", txType);
                        receiptIntent.putExtra("IS_APPROVED", false);
                        receiptIntent.putExtra("CARD_TYPE", "VISA");
                        startActivity(receiptIntent);

                        setResult(RESULT_CANCELED);
                        finish();
                    }
                })
                .setNegativeButton("Réessayer", null)
                .show();
    }

    private void handleKeypress(String val) {
        if (typedCode.length() < 4) {
            typedCode += val;
            updateCodeDisplay();
        }
    }

    private void updateCodeDisplay() {
        if (typedCode.isEmpty()) {
            tvCodeText.setText("____");
        } else {
            StringBuilder display = new StringBuilder();
            for (int i = 0; i < 4; i++) {
                if (i < typedCode.length()) {
                    display.append(typedCode.charAt(i));
                } else {
                    display.append("_");
                }
                if (i < 3) display.append(" ");
            }
            tvCodeText.setText(display.toString());
        }
    }
}