package com.atb.nfcapp;

public class EmvReadResult {
    private final String aidHex;
    private final String maskedPan;
    private final String applicationLabel;
    private final String cidHex;
    private final String tvrHex;
    private final String tsiHex;

    public EmvReadResult(String aidHex, String maskedPan, String applicationLabel,
                         String cidHex, String tvrHex, String tsiHex) {
        this.aidHex = aidHex;
        this.maskedPan = maskedPan;
        this.applicationLabel = applicationLabel;
        this.cidHex = cidHex;
        this.tvrHex = tvrHex;
        this.tsiHex = tsiHex;
    }

    public String getAidHex() { return aidHex; }
    public String getMaskedPan() { return maskedPan; }
    public String getApplicationLabel() { return applicationLabel; }
    public String getCidHex() { return cidHex; }
    public String getTvrHex() { return tvrHex; }
    public String getTsiHex() { return tsiHex; }
}