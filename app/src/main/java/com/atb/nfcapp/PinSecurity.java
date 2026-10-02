package com.atb.nfcapp;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public class PinSecurity {

    private static final String ALGORITHM = "SHA-256";
    private static final int SALT_LENGTH_BYTES = 16;
    private static final String RECOVERY_CODE_ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";

    public static String generateSalt() {
        SecureRandom random = new SecureRandom();
        byte[] saltBytes = new byte[SALT_LENGTH_BYTES];
        random.nextBytes(saltBytes);
        return bytesToHex(saltBytes);
    }

    public static String hash(String pin, String salt) {
        try {
            MessageDigest digest = MessageDigest.getInstance(ALGORITHM);
            digest.update(salt.getBytes());
            byte[] hashed = digest.digest(pin.getBytes());
            return bytesToHex(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Algorithme de hash indisponible : " + ALGORITHM, e);
        }
    }

    public static String generateRecoveryCode() {
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            if (i == 4) sb.append('-');
            int index = random.nextInt(RECOVERY_CODE_ALPHABET.length());
            sb.append(RECOVERY_CODE_ALPHABET.charAt(index));
        }
        return sb.toString();
    }

    public static String normalizeRecoveryCode(String input) {
        return input.trim().toUpperCase(java.util.Locale.US).replace(" ", "");
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}