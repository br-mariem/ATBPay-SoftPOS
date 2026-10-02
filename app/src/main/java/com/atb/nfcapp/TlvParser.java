package com.atb.nfcapp;

import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TlvParser {

    private static final String TAG_LOG = "ATBPay_TLV";

    public static class Tlv {
        public final int tag;
        public final byte[] value;
        public Tlv(int tag, byte[] value) {
            this.tag = tag;
            this.value = value;
        }
    }

    /** Parse une suite d'objets TLV consécutifs (structure "à plat"), de façon défensive. */
    public static List<Tlv> parse(byte[] data) {
        List<Tlv> result = new ArrayList<>();
        int i = 0;
        while (i < data.length) {
            int firstByte = data[i] & 0xFF;
            int tag = firstByte;
            i++;
            if ((firstByte & 0x1F) == 0x1F) {
                int nextByte;
                do {
                    if (i >= data.length) {
                        Log.w(TAG_LOG, "Tag multi-octets tronqué à l'offset " + i + ", arrêt.");
                        return result;
                    }
                    nextByte = data[i] & 0xFF;
                    tag = (tag << 8) | nextByte;
                    i++;
                } while ((nextByte & 0x80) == 0x80);
            }

            if (i >= data.length) {
                Log.w(TAG_LOG, "Longueur manquante après le tag " + Integer.toHexString(tag) + ", arrêt.");
                return result;
            }

            int lengthByte = data[i] & 0xFF;
            i++;
            int length;
            if ((lengthByte & 0x80) == 0x80) {
                int numLengthBytes = lengthByte & 0x7F;
                if (numLengthBytes > 4 || i + numLengthBytes > data.length) {
                    Log.w(TAG_LOG, "Longueur longue forme invalide pour tag " + Integer.toHexString(tag)
                            + " (numLengthBytes=" + numLengthBytes + "), arrêt.");
                    return result;
                }
                length = 0;
                for (int b = 0; b < numLengthBytes; b++) {
                    length = (length << 8) | (data[i] & 0xFF);
                    i++;
                }
            } else {
                length = lengthByte;
            }

            if (length < 0 || i + length > data.length) {
                Log.w(TAG_LOG, "Tag " + Integer.toHexString(tag) + " : longueur invalide (" + length
                        + ") à l'offset " + i + " / " + data.length + " octets -> arrêt du parsing.");
                return result;
            }

            byte[] value = Arrays.copyOfRange(data, i, i + length);
            result.add(new Tlv(tag, value));
            i += length;
        }
        return result;
    }

    public static byte[] findTag(byte[] data, int targetTag) {
        for (Tlv tlv : parse(data)) {
            if (tlv.tag == targetTag) {
                return tlv.value;
            }
            if (isConstructed(tlv.tag)) {
                byte[] nested = findTag(tlv.value, targetTag);
                if (nested != null) return nested;
            }
        }
        return null;
    }

    public static List<byte[]> findAllTags(byte[] data, int targetTag) {
        List<byte[]> results = new ArrayList<>();
        collectTag(data, targetTag, results);
        return results;
    }

    private static void collectTag(byte[] data, int targetTag, List<byte[]> results) {
        for (Tlv tlv : parse(data)) {
            if (tlv.tag == targetTag) {
                results.add(tlv.value);
            }
            if (isConstructed(tlv.tag)) {
                collectTag(tlv.value, targetTag, results);
            }
        }
    }

    private static boolean isConstructed(int tag) {
        int firstByte = tag > 0xFF ? (tag >> 8) : tag;
        return (firstByte & 0x20) == 0x20;
    }

    public static boolean isSuccess(byte[] response) {
        if (response == null || response.length < 2) return false;
        int len = response.length;
        return (response[len - 2] & 0xFF) == 0x90 && (response[len - 1] & 0xFF) == 0x00;
    }

    public static byte[] stripStatusWord(byte[] response) {
        return Arrays.copyOfRange(response, 0, response.length - 2);
    }

    public static String toHex(byte[] bytes) {
        if (bytes == null) return "";
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) sb.append(String.format("%02X", b));
        return sb.toString();
    }
}