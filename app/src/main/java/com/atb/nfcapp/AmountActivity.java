package com.atb.nfcapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.List;
import java.util.Arrays;
import java.util.Collections;
import java.util.ArrayList;

/**
 * Activité de Saisie du Montant (Achat / Annulation)
 * Fichier : AmountActivity.java
 */
public class AmountActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private TextView tvTitle;
    private TextView tvAmountText;
    private Button btnValidate;
    private String typedDigits = "";
    private String transactionType = "achat";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_amount);

        btnBack = findViewById(R.id.btnBack);
        tvTitle = findViewById(R.id.tvTitle);
        tvAmountText = findViewById(R.id.tvAmountText);
        btnValidate = findViewById(R.id.btnValidate);

        transactionType = getIntent().getStringExtra("TX_TYPE");
        if (transactionType == null) transactionType = "achat";

        tvTitle.setText(transactionType.toUpperCase());

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        // Liaison dynamique et écouteurs du clavier numérique tactile
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
                        handleKeypadPress(value);
                    }
                });
            }
        }

        Button btnClear = findViewById(R.id.btnKeyClear);
        if (btnClear != null) {
            btnClear.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    handleClear();
                }
            });
        }

        Button btnDelete = findViewById(R.id.btnKeyDelete);
        if (btnDelete != null) {
            btnDelete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    handleBackspace();
                }
            });
        }

        // Action lors du clic sur Valider : passage direct à la numérisation NFC
        btnValidate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (typedDigits.isEmpty() || Double.parseDouble(typedDigits) <= 0) {
                    Toast.makeText(AmountActivity.this, "Veuillez entrer un montant valide supérieur à 0 TND.", Toast.LENGTH_SHORT).show();
                    return;
                }
                double amount = Double.parseDouble(typedDigits) / 1000.0;
                Intent intentNfc = new Intent(AmountActivity.this, NfcScanActivity.class);
                intentNfc.putExtra("AMOUNT", amount); // CORRIGÉ : double, plus de String.format ici
                intentNfc.putExtra("TX_TYPE", transactionType);
                startActivity(intentNfc);
                finish();
            }
        });
    }

    private void showAnnulationConfirmDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_annulation_confirm, null);
        builder.setView(dialogView);

        final AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        TextView tvConfirmAmount = dialogView.findViewById(R.id.tvConfirmAmount);
        Button btnValider = dialogView.findViewById(R.id.btnValider);
        ImageButton btnClose = dialogView.findViewById(R.id.btnClose);

        double amount = Double.parseDouble(typedDigits) / 1000.0;
        tvConfirmAmount.setText(String.format(Locale.US, "%.3f TND", amount));

        if (btnClose != null) {
            btnClose.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.dismiss();
                }
            });
        }

        btnValider.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                Intent intentReceipt = new Intent(AmountActivity.this, ReceiptActivity.class);
                intentReceipt.putExtra("AMOUNT", amount);
                intentReceipt.putExtra("TX_TYPE", transactionType);
                intentReceipt.putExtra("IS_APPROVED", true);
                intentReceipt.putExtra("CARD_TYPE", "VISA");
                startActivity(intentReceipt);
                finish();
            }
        });

        dialog.show();
    }

    private void showSmsQuestionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_sms_request, null);
        builder.setView(dialogView);

        final AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        TextView tvGsmPhoneNumber = dialogView.findViewById(R.id.tvGsmPhoneNumber);
        View cardGsmOption = dialogView.findViewById(R.id.cardGsmOption);
        Button btnSmsNo = dialogView.findViewById(R.id.btnSmsNo);
        Button btnSmsYes = dialogView.findViewById(R.id.btnSmsYes);

        SharedPreferences prefs = getSharedPreferences("ATB_CONFIGS", MODE_PRIVATE);
        final String savedGsm = prefs.getString("GSM_NUMBER", "+216 29968399");
        tvGsmPhoneNumber.setText(savedGsm);

        cardGsmOption.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                launchSmsVerification();
            }
        });

        btnSmsYes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                launchSmsVerification();
            }
        });

        btnSmsNo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                double amount = Double.parseDouble(typedDigits) / 1000.0;
                Intent intentNfc = new Intent(AmountActivity.this, NfcScanActivity.class);
                intentNfc.putExtra("AMOUNT", amount); // CORRIGÉ ICI AUSSI : même bug que dans Valider
                intentNfc.putExtra("TX_TYPE", transactionType);
                startActivity(intentNfc);
                finish();
            }
        });

        dialog.show();
    }

    private void launchSmsVerification() {
        double amount = Double.parseDouble(typedDigits) / 1000.0;
        Intent intent = new Intent(AmountActivity.this, SmsCodeActivity.class);
        intent.putExtra("AMOUNT", amount);
        intent.putExtra("TX_TYPE", transactionType);
        startActivityForResult(intent, 1001);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1001 && resultCode == RESULT_OK) {
            finish();
        } else if (requestCode == 1002 && resultCode == RESULT_OK) {
            double amount = Double.parseDouble(typedDigits) / 1000.0;
            Intent intentConfirm = new Intent(AmountActivity.this, ConfirmTransactionActivity.class);
            intentConfirm.putExtra("AMOUNT", amount);
            intentConfirm.putExtra("TX_TYPE", "annulation");

            SimpleDateFormat dateFormater = new SimpleDateFormat("dd/MM/yy", Locale.getDefault());
            SimpleDateFormat timeFormater = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
            Date systemDate = new Date();

            intentConfirm.putExtra("DATE", dateFormater.format(systemDate));
            intentConfirm.putExtra("TIME", timeFormater.format(systemDate));
            intentConfirm.putExtra("CARD_NUMBER", "474200******3016");

            long randomNum = (long)(Math.random() * 9000000000L) + 1000000000L;
            intentConfirm.putExtra("REFERENCE", String.format(Locale.US, "%010d", randomNum));

            startActivity(intentConfirm);
            finish();
        }
    }

    private void handleKeypadPress(String val) {
        if (typedDigits.length() >= 9) return;
        typedDigits += val;
        updateAmountDisplay();
    }

    private void handleBackspace() {
        if (!typedDigits.isEmpty()) {
            typedDigits = typedDigits.substring(0, typedDigits.length() - 1);
            updateAmountDisplay();
        }
    }

    private void handleClear() {
        typedDigits = "";
        updateAmountDisplay();
    }

    private void updateAmountDisplay() {
        if (typedDigits.isEmpty()) {
            tvAmountText.setText("0,000 TND");
            return;
        }
        double amount = Double.parseDouble(typedDigits) / 1000.0;
        tvAmountText.setText(String.format("%.3f", amount) + " TND");
    }

    private void showSecureKeyboardDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_secure_keyboard, null);
        builder.setView(dialogView);

        final AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        final ImageView dot1 = dialogView.findViewById(R.id.dot1);
        final ImageView dot2 = dialogView.findViewById(R.id.dot2);
        final ImageView dot3 = dialogView.findViewById(R.id.dot3);
        final ImageView dot4 = dialogView.findViewById(R.id.dot4);
        final ImageView[] dots = {dot1, dot2, dot3, dot4};

        final StringBuilder enteredPin = new StringBuilder();

        int[] buttonIds = {
                R.id.btnSecKey0, R.id.btnSecKey1, R.id.btnSecKey2,
                R.id.btnSecKey3, R.id.btnSecKey4, R.id.btnSecKey5,
                R.id.btnSecKey6, R.id.btnSecKey7, R.id.btnSecKey8,
                R.id.btnSecKey9
        };
        final String[] digitValues = {
                "0", "1", "2", "3", "4", "5", "6", "7", "8", "9"
        };

        for (int i = 0; i < buttonIds.length; i++) {
            Button btn = dialogView.findViewById(buttonIds[i]);
            if (btn != null) {
                final String digitStr = digitValues[i];
                btn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (enteredPin.length() < 4) {
                            enteredPin.append(digitStr);
                            updatePinDots(enteredPin.length(), dots);
                        }
                    }
                });
            }
        }

        Button btnSecKeyClear = dialogView.findViewById(R.id.btnSecKeyClear);
        if (btnSecKeyClear != null) {
            btnSecKeyClear.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    enteredPin.setLength(0);
                    updatePinDots(0, dots);
                }
            });
        }

        Button btnSecKeyConfirm = dialogView.findViewById(R.id.btnSecKeyConfirm);
        if (btnSecKeyConfirm != null) {
            btnSecKeyConfirm.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    SharedPreferences prefs = getSharedPreferences("ATB_CONFIGS", MODE_PRIVATE);
                    String correctCode = prefs.getString("SECURITY_CODE", "0000");
                    if (enteredPin.toString().equals(correctCode)) {
                        dialog.dismiss();
                        Toast.makeText(AmountActivity.this, "Code correct !", Toast.LENGTH_SHORT).show();
                        if (transactionType.equalsIgnoreCase("annulation")) {
                            // ... (ne pas toucher)
                        } else {
                            double amount = Double.parseDouble(typedDigits) / 1000.0;
                            Intent intentNfc = new Intent(AmountActivity.this, NfcScanActivity.class);
                            intentNfc.putExtra("AMOUNT", amount);
                            intentNfc.putExtra("TX_TYPE", transactionType);
                            startActivity(intentNfc);
                            finish();
                        }
                    } else {
                        Toast.makeText(AmountActivity.this, "Code de sécurité incorrect. Réessayez.", Toast.LENGTH_SHORT).show();
                        enteredPin.setLength(0);
                        updatePinDots(0, dots);
                    }
                }
            });
        }

        TextView tvSecCancel = dialogView.findViewById(R.id.tvSecCancel);
        if (tvSecCancel != null) {
            tvSecCancel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.dismiss();
                }
            });
        }

        dialog.show();
    }

    private void updatePinDots(int length, ImageView[] dots) {
        for (int i = 0; i < dots.length; i++) {
            if (dots[i] != null) {
                if (i < length) {
                    dots[i].setImageResource(R.drawable.pin_dot_filled);
                } else {
                    dots[i].setImageResource(R.drawable.pin_dot_empty);
                }
            }
        }
    }
}