package com.atb.nfcapp;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import java.util.Locale;

/**
 * Implémentation simulée (locale) de AcquirerService.
 * Prend une décision d'autorisation réaliste à partir des VRAIES données EMV
 * déjà lues sur la carte (notamment le CID), sans aucun appel réseau.
 *
 * À REMPLACER par une implémentation réelle (ex: HttpAcquirerService avec ISO 8583
 * ou API REST) le jour où un vrai acquéreur est disponible -- une seule ligne à
 * changer dans AcquirerConnectionService.onCreate().
 */
public class MockAcquirerService implements AcquirerService {

    private static final String TAG_LOG = "ATBPay_Acquirer";

    // Plafond arbitraire au-delà duquel on simule un refus (ex: fonds insuffisants)
    private static final double DECLINE_ABOVE_AMOUNT = 5000.0;

    @Override
    public void authorize(TransactionRequest request, AuthorizationCallback callback) {
        Log.d(TAG_LOG, "Décision en cours pour montant=" + request.getAmount()
                + " CID=" + request.getCid());

        // Simule une latence réseau réaliste (comme un vrai aller-retour acquéreur)
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            AuthorizationResult result = decide(request);
            Log.d(TAG_LOG, "Décision rendue : approved=" + result.isApproved()
                    + " code=" + result.getResponseCode());
            callback.onResult(result);
        }, 1200);
    }

    private AuthorizationResult decide(TransactionRequest request) {
        // Si la carte elle-même a refusé la transaction (CID = AAC = "00"),
        // aucune autorisation n'est même tentée : la décision de la carte prime.
        if ("00".equalsIgnoreCase(request.getCid())) {
            return new AuthorizationResult(false, "57", null,
                    "Transaction refusée par la carte (cryptogramme AAC)");
        }

        // Simulation d'un refus pour fonds insuffisants au-delà d'un certain montant
        if (request.getAmount() > DECLINE_ABOVE_AMOUNT) {
            return new AuthorizationResult(false, "51", null,
                    "Fonds insuffisants (simulation)");
        }

        // Sinon, approbation simulée avec un code d'autorisation à 6 chiffres,
        // comme en renverrait un vrai acquéreur.
        String authCode = String.format(Locale.US, "%06d", (int) (Math.random() * 1000000));
        return new AuthorizationResult(true, "00", authCode, null);
    }
}