package com.atb.nfcapp;

/**
 * Abstraction du service d'autorisation bancaire (acquéreur).
 * Permet de remplacer la simulation locale (MockAcquirerService) par un vrai
 * acquéreur plus tard, sans modifier le code des écrans qui l'utilisent.
 */
public interface AcquirerService {

    interface AuthorizationCallback {
        void onResult(AuthorizationResult result);
    }

    void authorize(TransactionRequest request, AuthorizationCallback callback);
}