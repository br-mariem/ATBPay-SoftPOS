package com.atb.nfcapp;

/**
 * Réponse d'autorisation, telle qu'un acquéreur la renverrait réellement :
 * approbation ou refus, code réponse normalisé, et code d'autorisation
 * (uniquement présent en cas d'approbation).
 */
public class AuthorizationResult {
    private final boolean approved;
    private final String responseCode;
    private final String authorizationCode;
    private final String declineReason;

    public AuthorizationResult(boolean approved, String responseCode,
                               String authorizationCode, String declineReason) {
        this.approved = approved;
        this.responseCode = responseCode;
        this.authorizationCode = authorizationCode;
        this.declineReason = declineReason;
    }

    public boolean isApproved() { return approved; }
    public String getResponseCode() { return responseCode; }
    public String getAuthorizationCode() { return authorizationCode; }
    public String getDeclineReason() { return declineReason; }
}