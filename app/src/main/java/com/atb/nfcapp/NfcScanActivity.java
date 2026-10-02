package com.atb.nfcapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.TagLostException;
import android.nfc.tech.IsoDep;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class NfcScanActivity extends AppCompatActivity implements NfcAdapter.ReaderCallback {

    private static final String TAG = "ATBPay_NFC";

    private NfcAdapter nfcAdapter;
    private double amount;
    private String transactionType;
    private volatile boolean transactionInProgress = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nfc_scan);
        Log.d(TAG, "onCreate: écran NFC affiché");

        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (nfcAdapter == null) {
            Toast.makeText(this, "Cet appareil ne possède pas de capteur NFC.", Toast.LENGTH_LONG).show();
            Log.e(TAG, "Aucun NfcAdapter disponible sur cet appareil.");
            finish();
            return;
        }

        amount = getIntent().getDoubleExtra("AMOUNT", 0.0);
        transactionType = getIntent().getStringExtra("TX_TYPE");
        if (transactionType == null) transactionType = "achat";
        Log.d(TAG, "Montant reçu = " + amount + " TND, type = " + transactionType);

        TextView tvAmountDisplay = findViewById(R.id.tvAmountDisplay);
        if (tvAmountDisplay != null) {
            tvAmountDisplay.setText(String.format(Locale.US, "%.3f TND", amount));
        }

        ImageButton btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> cancelScanAndFinish());
        }

        Button btnCancelScan = findViewById(R.id.btnCancelScan);
        if (btnCancelScan != null) {
            btnCancelScan.setOnClickListener(v -> cancelScanAndFinish());
        }
    }

    private void cancelScanAndFinish() {
        Log.d(TAG, "Annulation du scan par l'utilisateur");
        if (nfcAdapter != null) {
            nfcAdapter.disableReaderMode(this);
        }
        Toast.makeText(this, "Paiement annulé.", Toast.LENGTH_SHORT).show();
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!nfcAdapter.isEnabled()) {
            Toast.makeText(this, "Veuillez activer le NFC dans vos paramètres.", Toast.LENGTH_LONG).show();
            Log.e(TAG, "NFC désactivé dans les paramètres système.");
            return;
        }

        Bundle options = new Bundle();
        options.putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, 250);

        nfcAdapter.enableReaderMode(
                this,
                this,
                NfcAdapter.FLAG_READER_NFC_A
                        | NfcAdapter.FLAG_READER_NFC_B
                        | NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK,
                options
        );
        transactionInProgress = false;
        Log.d(TAG, "Reader mode activé, en attente d'une carte...");
    }

    @Override
    protected void onPause() {
        super.onPause();
        nfcAdapter.disableReaderMode(this);
        Log.d(TAG, "Reader mode désactivé (onPause)");
    }

    @Override
    public void onTagDiscovered(Tag tag) {
        Log.d(TAG, "onTagDiscovered() appelé — une puce a été détectée par l'antenne");

        if (transactionInProgress) {
            Log.w(TAG, "Transaction déjà en cours, tag ignoré (anti double-lecture)");
            return;
        }
        transactionInProgress = true;

        IsoDep isoDep = IsoDep.get(tag);
        if (isoDep == null) {
            Log.e(TAG, "IsoDep.get(tag) = null -> la puce ne supporte pas ISO 14443-4 (pas une carte EMV)");
            runOnUiThread(() -> {
                Toast.makeText(this, "Carte non compatible EMV contactless.", Toast.LENGTH_LONG).show();
                transactionInProgress = false;
            });
            return;
        }

        try {
            Log.d(TAG, "Connexion IsoDep...");
            isoDep.connect();
            isoDep.setTimeout(5000);
            Log.d(TAG, "IsoDep connecté. Lancement du flux EMV (montant=" + amount + ")");

            long amountInCents = Math.round(amount * 1000);
            EmvTransactionManager manager = new EmvTransactionManager(isoDep, amountInCents);
            EmvReadResult result = manager.readCard();

            Log.d(TAG, "Lecture EMV réussie -> AID=" + result.getAidHex()
                    + " PAN=" + result.getMaskedPan()
                    + " Label=" + result.getApplicationLabel()
                    + " CID=" + result.getCidHex()
                    + " TVR=" + result.getTvrHex()
                    + " TSI=" + result.getTsiHex());

            runOnUiThread(() -> goToWaiting(result));

        } catch (TagLostException e) {
            Log.e(TAG, "TagLostException : carte retirée trop tôt", e);
            runOnUiThread(() -> {
                Toast.makeText(this, "Carte retirée trop tôt. Veuillez réessayer.", Toast.LENGTH_LONG).show();
                transactionInProgress = false;
            });
        } catch (IOException e) {
            Log.e(TAG, "IOException pendant l'échange APDU", e);
            runOnUiThread(() -> {
                Toast.makeText(this, "Erreur de communication NFC : " + e.getMessage(), Toast.LENGTH_LONG).show();
                transactionInProgress = false;
            });
        } catch (EmvException e) {
            Log.e(TAG, "EmvException : " + e.getMessage(), e);
            runOnUiThread(() -> {
                Toast.makeText(this, "Erreur de lecture EMV : " + e.getMessage(), Toast.LENGTH_LONG).show();
                transactionInProgress = false;
            });
        } catch (Exception e) {
            Log.e(TAG, "Exception INATTENDUE pendant la lecture de la carte", e);
            runOnUiThread(() -> {
                Toast.makeText(this, "Erreur inattendue : " + e.getClass().getSimpleName()
                        + " - " + e.getMessage(), Toast.LENGTH_LONG).show();
                transactionInProgress = false;
            });
        } finally {
            try {
                isoDep.close();
            } catch (IOException ignored) {}
        }
    }

    private void goToWaiting(EmvReadResult result) {
        SimpleDateFormat dateFormatter = new SimpleDateFormat("dd/MM/yy", Locale.getDefault());
        SimpleDateFormat timeFormatter = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        Date now = new Date();

        Intent intent = new Intent(NfcScanActivity.this, WaitingActivity.class);
        intent.putExtra("TX_TYPE", transactionType);
        intent.putExtra("AMOUNT", amount);
        intent.putExtra("DATE", dateFormatter.format(now));
        intent.putExtra("TIME", timeFormatter.format(now));
        intent.putExtra("REFERENCE", generateSequentialReference());

        intent.putExtra("AID", result.getAidHex());
        intent.putExtra("CARD_NUMBER", result.getMaskedPan() != null ? result.getMaskedPan() : "Indisponible");
        intent.putExtra("CARD_TYPE", inferCardType(result.getAidHex()));
        intent.putExtra("CID", result.getCidHex() != null ? result.getCidHex() : "Indisponible");
        intent.putExtra("TVR", result.getTvrHex() != null ? result.getTvrHex() : "0000000000");
        intent.putExtra("TSI", result.getTsiHex() != null ? result.getTsiHex() : "0000");

        Log.d(TAG, "Navigation vers WaitingActivity");
        startActivity(intent);
        finish();
    }

    /**
     * Génère une référence de transaction séquentielle (STAN), persistée localement,
     * au lieu d'une valeur aléatoire -- conforme au fonctionnement d'un vrai terminal
     * de paiement. Le STAN boucle sur 6 chiffres (000001 à 999999).
     */
    private String generateSequentialReference() {
        SharedPreferences prefs = getSharedPreferences("ATB_CONFIGS", MODE_PRIVATE);
        int stan = prefs.getInt("TX_STAN_COUNTER", 0) + 1;
        if (stan > 999999) {
            stan = 1;
        }
        prefs.edit().putInt("TX_STAN_COUNTER", stan).apply();
        return String.format(Locale.US, "%06d", stan);
    }

    private String inferCardType(String aidHex) {
        if (aidHex == null) return "Inconnu";
        if (aidHex.startsWith("A000000003")) return "Visa";
        if (aidHex.startsWith("A000000004")) return "Mastercard";
        if (aidHex.startsWith("A000000025")) return "American Express";
        if (aidHex.startsWith("A000000065")) return "JCB";
        return "Carte de paiement";
    }
}