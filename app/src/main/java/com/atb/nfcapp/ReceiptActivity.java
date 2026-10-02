package com.atb.nfcapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ReceiptActivity extends AppCompatActivity {

    private ImageView ivResultIcon;
    private TextView tvResultTitle;
    private TextView tvTxType;
    private TextView tvAmount;
    private TextView tvDate;
    private TextView tvTime;
    private TextView tvCardType;
    private TextView tvAid;
    private TextView tvPan;
    private TextView tvCid;
    private TextView tvTsi;
    private TextView tvTvr;
    private TextView tvReference;
    private Button btnOk;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_receipt);

        ivResultIcon = findViewById(R.id.ivResultIcon);
        tvResultTitle = findViewById(R.id.tvResultTitle);
        tvTxType = findViewById(R.id.tvTxType);
        tvAmount = findViewById(R.id.tvAmount);
        tvDate = findViewById(R.id.tvDate);
        tvTime = findViewById(R.id.tvTime);
        tvCardType = findViewById(R.id.tvCardType);
        tvAid = findViewById(R.id.tvAid);
        tvPan = findViewById(R.id.tvPan);
        tvCid = findViewById(R.id.tvCid);
        tvTsi = findViewById(R.id.tvTsi);
        tvTvr = findViewById(R.id.tvTvr);
        tvReference = findViewById(R.id.tvReference);
        btnOk = findViewById(R.id.btnOk);

        Intent intent = getIntent();
        double amount = intent.getDoubleExtra("AMOUNT", 0.001);
        String txType = intent.getStringExtra("TX_TYPE");
        if (txType == null) txType = "achat";

        boolean isApproved = intent.getBooleanExtra("IS_APPROVED", true);

        SimpleDateFormat dateFormater = new SimpleDateFormat("dd/MM/yy", Locale.getDefault());
        SimpleDateFormat timeFormater = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        Date systemDate = new Date();

        if (tvDate != null) {
            tvDate.setText("DATE: " + dateFormater.format(systemDate));
        }
        if (tvTime != null) {
            tvTime.setText("TEMPS: " + timeFormater.format(systemDate));
        }

        // CORRIGÉ : on lit les VRAIES données transmises par WaitingActivity
        // (issues de la lecture NFC réelle), au lieu de les déduire du type de carte.
        String cardTypeStr = intent.getStringExtra("CARD_TYPE");
        if (cardTypeStr == null) cardTypeStr = "Carte de paiement";

        String panStr = intent.getStringExtra("PAN");
        if (panStr == null || panStr.isEmpty()) panStr = "Indisponible";

        String aidStr = intent.getStringExtra("AID");
        if (aidStr == null || aidStr.isEmpty()) aidStr = "Indisponible";

        if (tvCardType != null) {
            tvCardType.setText(cardTypeStr);
        }
        if (tvPan != null) {
            tvPan.setText(panStr);
        }
        if (tvAid != null) {
            tvAid.setText(aidStr);
        }

        // CID, TSI, TVR : eux aussi transmis par WaitingActivity (valeurs actuellement
        // fixes côté EmvTransactionManager, en attendant l'implémentation de GENERATE AC)
        String cidStr = intent.getStringExtra("CID");
        String tsiStr = intent.getStringExtra("TSI");
        String tvrStr = intent.getStringExtra("TVR");

        if (tvCid != null) {
            tvCid.setText(cidStr != null ? cidStr : "80");
        }
        if (tvTsi != null) {
            tvTsi.setText(tsiStr != null ? tsiStr : "0000");
        }
        if (tvTvr != null) {
            tvTvr.setText(tvrStr != null ? tvrStr : "0000000000");
        }

        String intentRef = intent.getStringExtra("REFERENCE");
        if (intentRef != null && !intentRef.isEmpty()) {
            if (tvReference != null) tvReference.setText(intentRef);
        } else {
            long randomNum = (long)(Math.random() * 9000000000L) + 1000000000L;
            String referenceStr = String.format(Locale.US, "%010d", randomNum);
            if (tvReference != null) {
                tvReference.setText(referenceStr);
            }
        }

        if (tvAmount != null) {
            tvAmount.setText(String.format(Locale.US, "%.3f TND", amount));
        }

        if (tvTxType != null) {
            tvTxType.setText(txType.toUpperCase(Locale.ROOT));
        }

        if (isApproved) {
            if (ivResultIcon != null) {
                ivResultIcon.setImageResource(R.drawable.ic_success);
            }
            if (tvResultTitle != null) {
                if (txType.equalsIgnoreCase("annulation")) {
                    tvResultTitle.setText("ANNULATION EFFECTUÉE AVEC SUCCÈS");
                    tvResultTitle.setTextColor(android.graphics.Color.parseColor("#1E1B4B"));
                } else {
                    tvResultTitle.setText("PAIEMENT EFFECTUÉ AVEC SUCCÈS");
                    tvResultTitle.setTextColor(android.graphics.Color.parseColor("#065F46"));
                }
            }

            SharedPreferences prefs = getSharedPreferences("ATB_CONFIGS", MODE_PRIVATE);
            float currentBalance = prefs.getFloat("CARD_BALANCE", 2450.850f);
            float updatedBalance;
            if (txType.equalsIgnoreCase("achat")) {
                updatedBalance = currentBalance + (float) amount;
            } else {
                updatedBalance = currentBalance - (float) amount;
                if (updatedBalance < 0) updatedBalance = 0f;
            }
            prefs.edit().putFloat("CARD_BALANCE", updatedBalance).apply();

            // CORRIGÉ : le PAN réel est maintenant sauvegardé dans l'historique
            try {
                String txHistoryStr = prefs.getString("TX_HISTORY", null);
                org.json.JSONArray txArray;
                if (txHistoryStr == null) {
                    txArray = new org.json.JSONArray();
                    org.json.JSONObject init1 = new org.json.JSONObject();
                    init1.put("id", "tx_init1");
                    init1.put("title", "Achat");
                    init1.put("date", "24/05/23 15:19:16");
                    init1.put("amount", -12.000);
                    init1.put("type", "debit");
                    init1.put("reference", "000193");
                    init1.put("approved", true);
                    init1.put("pan", "Indisponible");
                    txArray.put(init1);

                    org.json.JSONObject init2 = new org.json.JSONObject();
                    init2.put("id", "tx_init2");
                    init2.put("title", "Monoprix Tunis");
                    init2.put("date", "30/06/26 14:32:00");
                    init2.put("amount", -68.450);
                    init2.put("type", "debit");
                    init2.put("reference", "000194");
                    init2.put("approved", true);
                    init2.put("pan", "Indisponible");
                    txArray.put(init2);

                    org.json.JSONObject init3 = new org.json.JSONObject();
                    init3.put("id", "tx_init3");
                    init3.put("title", "Ola Energy Lac 2");
                    init3.put("date", "29/06/26 18:10:00");
                    init3.put("amount", -50.000);
                    init3.put("type", "debit");
                    init3.put("reference", "000195");
                    init3.put("approved", true);
                    init3.put("pan", "Indisponible");
                    txArray.put(init3);
                } else {
                    txArray = new org.json.JSONArray(txHistoryStr);
                }

                org.json.JSONObject newTx = new org.json.JSONObject();
                newTx.put("id", "tx_" + System.currentTimeMillis());
                newTx.put("title", txType.equalsIgnoreCase("achat") ? "Achat" : "Annulation");
                newTx.put("date", dateFormater.format(systemDate) + " " + timeFormater.format(systemDate));
                newTx.put("amount", txType.equalsIgnoreCase("achat") ? amount : -amount);
                newTx.put("type", txType.equalsIgnoreCase("achat") ? "credit" : "debit");
                newTx.put("reference", String.format(Locale.US, "%06d", (int)(Math.random() * 1000000)));
                newTx.put("approved", isApproved);
                newTx.put("pan", panStr); // <-- le vrai PAN lu sur la carte, désormais sauvegardé

                org.json.JSONArray newArray = new org.json.JSONArray();
                newArray.put(newTx);
                for (int i = 0; i < txArray.length(); i++) {
                    newArray.put(txArray.get(i));
                }

                prefs.edit().putString("TX_HISTORY", newArray.toString()).apply();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            if (ivResultIcon != null) {
                ivResultIcon.setImageResource(R.drawable.ic_error);
            }
            if (tvResultTitle != null) {
                tvResultTitle.setText("PAIEMENT REFUSÉ");
                tvResultTitle.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
            }
        }

        if (btnOk != null) {
            btnOk.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intentDashboard = new Intent(ReceiptActivity.this, DashboardActivity.class);
                    intentDashboard.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intentDashboard);
                    finish();
                }
            });
        }
    }
}