package com.atb.nfcapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.util.Locale;

public class ConfirmTransactionActivity extends AppCompatActivity {

    private String transactionType;
    private double amount;
    private String cardNumber;
    private String reference;
    private String date;
    private String time;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_confirm_transaction);

        transactionType = getIntent().getStringExtra("TX_TYPE");
        amount = getIntent().getDoubleExtra("AMOUNT", 0.0);
        cardNumber = getIntent().getStringExtra("CARD_NUMBER");
        reference = getIntent().getStringExtra("REFERENCE");
        date = getIntent().getStringExtra("DATE");
        time = getIntent().getStringExtra("TIME");

        if (transactionType == null) transactionType = "Achat";
        if (cardNumber == null) cardNumber = "474200******3016";
        if (reference == null) reference = "0001001046";
        if (date == null) date = "25/05/23";
        if (time == null) time = "16:05:14";

        TextView tvHeaderType = findViewById(R.id.tvHeaderType);
        TextView tvDateValue = findViewById(R.id.tvDateValue);
        TextView tvTimeValue = findViewById(R.id.tvTimeValue);
        TextView tvCardValue = findViewById(R.id.tvCardValue);
        TextView tvRefValue = findViewById(R.id.tvRefValue);
        TextView tvAmountValue = findViewById(R.id.tvAmountValue);
        Button btnConfirm = findViewById(R.id.btnConfirm);
        Button btnCancel = findViewById(R.id.btnCancel);
        android.widget.ImageButton btnBack = findViewById(R.id.btnBack);

        if (tvHeaderType != null) {
            String displayType = transactionType;
            if (displayType.equalsIgnoreCase("annulation")) {
                displayType = "transaction";
            }
            tvHeaderType.setText(displayType.toUpperCase(Locale.ROOT) + " À ANNULER");
        }
        if (tvDateValue != null) {
            tvDateValue.setText(date);
        }
        if (tvTimeValue != null) {
            tvTimeValue.setText(time);
        }
        if (tvCardValue != null) {
            tvCardValue.setText(cardNumber);
        }
        if (tvRefValue != null) {
            tvRefValue.setText(reference);
        }
        if (tvAmountValue != null) {
            tvAmountValue.setText(String.format(Locale.US, "%.3f TND", amount));
        }

        if (btnBack != null) {
            btnBack.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    finish();
                }
            });
        }

        if (btnConfirm != null) {
            btnConfirm.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intentReceipt = new Intent(ConfirmTransactionActivity.this, ReceiptActivity.class);
                    intentReceipt.putExtra("AMOUNT", amount);
                    intentReceipt.putExtra("TX_TYPE", "annulation");
                    intentReceipt.putExtra("IS_APPROVED", true);
                    intentReceipt.putExtra("CARD_TYPE", "VISA");
                    startActivity(intentReceipt);
                    finish();
                }
            });
        }

        if (btnCancel != null) {
            btnCancel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    finish();
                }
            });
        }
    }
}