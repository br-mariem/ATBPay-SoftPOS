package com.atb.nfcapp;

/**
 * Représente une demande d'autorisation, regroupant les données réelles
 * issues de la lecture EMV, prête à être transmise à un acquéreur.
 */
public class TransactionRequest {
    private final double amount;
    private final String aid;
    private final String maskedPan;
    private final String cid;
    private final String tvr;
    private final String tsi;
    private final String cardType;
    private final String reference;

    public TransactionRequest(double amount, String aid, String maskedPan, String cid,
                              String tvr, String tsi, String cardType, String reference) {
        this.amount = amount;
        this.aid = aid;
        this.maskedPan = maskedPan;
        this.cid = cid;
        this.tvr = tvr;
        this.tsi = tsi;
        this.cardType = cardType;
        this.reference = reference;
    }

    public double getAmount() { return amount; }
    public String getAid() { return aid; }
    public String getMaskedPan() { return maskedPan; }
    public String getCid() { return cid; }
    public String getTvr() { return tvr; }
    public String getTsi() { return tsi; }
    public String getCardType() { return cardType; }
    public String getReference() { return reference; }
}