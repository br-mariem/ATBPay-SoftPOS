package com.atb.nfcapp;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

public class ApduCommandBuilder {

    public static byte[] selectPpse() {
        byte[] ppseName = "2PAY.SYS.DDF01".getBytes(StandardCharsets.US_ASCII);
        return buildSelect(ppseName);
    }

    public static byte[] selectAid(byte[] aid) {
        return buildSelect(aid);
    }

    private static byte[] buildSelect(byte[] data) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(0x00);
        out.write(0xA4);
        out.write(0x04);
        out.write(0x00);
        out.write(data.length);
        out.write(data, 0, data.length);
        out.write(0x00);
        return out.toByteArray();
    }

    public static byte[] getProcessingOptions(byte[] pdolValues) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] wrapped = wrapTag(0x83, pdolValues);
        out.write(0x80);
        out.write(0xA8);
        out.write(0x00);
        out.write(0x00);
        out.write(wrapped.length);
        out.write(wrapped, 0, wrapped.length);
        out.write(0x00);
        return out.toByteArray();
    }

    public static byte[] readRecord(int recordNumber, int sfi) {
        int p2 = (sfi << 3) | 0x04;
        return new byte[]{
                (byte) 0x00, (byte) 0xB2,
                (byte) recordNumber, (byte) p2,
                (byte) 0x00
        };
    }

    /**
     * GENERATE AC : demande à la carte de générer un cryptogramme de transaction.
     * P1 = 0x80 -> demande d'ARQC (autorisation en ligne), sans CDA.
     */
    public static byte[] generateAc(byte[] cdol1Values) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(0x80);
        out.write(0xAE);
        out.write(0x80);
        out.write(0x00);
        out.write(cdol1Values.length);
        out.write(cdol1Values, 0, cdol1Values.length);
        out.write(0x00);
        return out.toByteArray();
    }

    private static byte[] wrapTag(int tag, byte[] value) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(tag);
        out.write(value.length);
        out.write(value, 0, value.length);
        return out.toByteArray();
    }
}