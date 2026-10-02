package com.atb.nfcapp;

public class TransactionResult {
    private boolean approved;
    private String aid;
    private String pan;
    private String cid;
    private String tsi;
    private String tvr;
    private String cardType;
    private String reference;
    private String authorizationCode;
    private String declineReason;

    public TransactionResult(boolean approved, String aid, String pan, String cid,
                             String tsi, String tvr, String cardType, String reference,
                             String authorizationCode, String declineReason) {
        this.approved = approved;
        this.aid = aid;
        this.pan = pan;
        this.cid = cid;
        this.tsi = tsi;
        this.tvr = tvr;
        this.cardType = cardType;
        this.reference = reference;
        this.authorizationCode = authorizationCode;
        this.declineReason = declineReason;
    }

    public boolean isApproved() { return approved; }
    public String getAid() { return aid; }
    public String getPan() { return pan; }
    public String getCid() { return cid; }
    public String getTsi() { return tsi; }
    public String getTvr() { return tvr; }
    public String getCardType() { return cardType; }
    public String getReference() { return reference; }
    public String getAuthorizationCode() { return authorizationCode; }
    public String getDeclineReason() { return declineReason; }
}