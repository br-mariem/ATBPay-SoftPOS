package com.atb.nfcapp;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import androidx.appcompat.app.AppCompatActivity;

public class WaitingActivity extends AppCompatActivity {

    private AcquirerService acquirerService;
    private boolean isBound = false;

    private String type, cardNumber, reference, date, time, aid, cardType, cid, tvr, tsi;
    private double amount;

    private final ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            AcquirerConnectionService.LocalBinder binder = (AcquirerConnectionService.LocalBinder) service;
            acquirerService = binder.getService();
            isBound = true;
            processAuthorization();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
            acquirerService = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_waiting);

        type = getIntent().getStringExtra("TX_TYPE");
        amount = getIntent().getDoubleExtra("AMOUNT", 0.0);
        cardNumber = getIntent().getStringExtra("CARD_NUMBER");
        reference = getIntent().getStringExtra("REFERENCE");
        date = getIntent().getStringExtra("DATE");
        time = getIntent().getStringExtra("TIME");
        aid = getIntent().getStringExtra("AID");
        cardType = getIntent().getStringExtra("CARD_TYPE");
        cid = getIntent().getStringExtra("CID");
        tvr = getIntent().getStringExtra("TVR");
        tsi = getIntent().getStringExtra("TSI");
    }

    @Override
    protected void onStart() {
        super.onStart();
        Intent intent = new Intent(this, AcquirerConnectionService.class);
        bindService(intent, connection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (isBound) {
            unbindService(connection);
            isBound = false;
        }
    }

    private void processAuthorization() {
        TransactionRequest request = new TransactionRequest(
                amount, aid, cardNumber, cid, tvr, tsi, cardType, reference
        );

        acquirerService.authorize(request, authResult -> runOnUiThread(() -> {
            TransactionResult result = new TransactionResult(
                    authResult.isApproved(),
                    aid != null ? aid : "",
                    cardNumber != null ? cardNumber : "",
                    cid != null ? cid : "Indisponible",
                    tsi != null ? tsi : "0000",
                    tvr != null ? tvr : "0000000000",
                    cardType != null ? cardType : "Carte de paiement",
                    reference,
                    authResult.getAuthorizationCode(),
                    authResult.getDeclineReason()
            );
            goToReceipt(type, amount, cardNumber, date, time, result);
        }));
    }

    private void goToReceipt(String type, double amount, String cardNumber,
                             String date, String time, TransactionResult result) {

        Intent intent = new Intent(WaitingActivity.this, ReceiptActivity.class);
        intent.putExtra("TX_TYPE", type);
        intent.putExtra("AMOUNT", amount);
        intent.putExtra("IS_APPROVED", result.isApproved());
        intent.putExtra("CARD_TYPE", result.getCardType());
        intent.putExtra("AID", result.getAid());
        intent.putExtra("PAN", result.getPan());
        intent.putExtra("CID", result.getCid());
        intent.putExtra("TSI", result.getTsi());
        intent.putExtra("TVR", result.getTvr());
        intent.putExtra("REFERENCE", result.getReference());
        intent.putExtra("DATE", date);
        intent.putExtra("TIME", time);
        intent.putExtra("AUTH_CODE", result.getAuthorizationCode());
        intent.putExtra("DECLINE_REASON", result.getDeclineReason());
        startActivity(intent);
        finish();
    }
}