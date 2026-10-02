package com.atb.nfcapp;

import android.nfc.tech.IsoDep;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class EmvTransactionManager {

    private static final String TAG_LOG = "ATBPay_EMV";

    private static final int TAG_AID = 0x4F;
    private static final int TAG_PDOL = 0x9F38;
    private static final int TAG_CDOL1 = 0x8C;
    private static final int TAG_AFL = 0x94;
    private static final int TAG_AIP = 0x82;
    private static final int TAG_GPO_FORMAT1 = 0x80;
    private static final int TAG_PAN = 0x5A;
    private static final int TAG_TRACK2 = 0x57;
    private static final int TAG_APP_LABEL = 0x50;
    private static final int TAG_APP_PRIORITY = 0x87;
    private static final int TAG_APP_TEMPLATE = 0x61;
    private static final int TAG_EXPIRY_DATE = 0x5F24;
    private static final int TAG_CID = 0x9F27;
    private static final int TAG_ATC = 0x9F36;
    private static final int TAG_AC = 0x9F26;

    // Plafond hors-ligne (en millimes, cohérent avec amountInCents) -- 50,000 TND
    private static final long FLOOR_LIMIT = 50000;

    private final IsoDep isoDep;
    private final long amountInCents;

    public EmvTransactionManager(IsoDep isoDep, long amountInCents) {
        this.isoDep = isoDep;
        this.amountInCents = amountInCents;
    }

    public EmvReadResult readCard() throws IOException, EmvException {

        byte[] ppseResponse = isoDep.transceive(ApduCommandBuilder.selectPpse());
        Log.d(TAG_LOG, "SELECT PPSE response = " + TlvParser.toHex(ppseResponse));
        if (!TlvParser.isSuccess(ppseResponse)) {
            throw new EmvException("Échec SELECT PPSE (carte non EMV ou non contactless)");
        }
        byte[] ppseData = TlvParser.stripStatusWord(ppseResponse);
        List<AppCandidate> candidates = parseApplications(ppseData);
        if (candidates.isEmpty()) {
            throw new EmvException("Aucune application de paiement (AID) trouvée sur la carte");
        }
        candidates.sort((a, b) -> {
            int pa = (a.priority == null || a.priority == 0) ? Integer.MAX_VALUE : a.priority;
            int pb = (b.priority == null || b.priority == 0) ? Integer.MAX_VALUE : b.priority;
            return Integer.compare(pa, pb);
        });
        Log.d(TAG_LOG, "Applications trouvées (triées par priorité) : " + candidates.size());
        for (AppCandidate c : candidates) {
            Log.d(TAG_LOG, "  -> AID=" + TlvParser.toHex(c.aid) + " priorité=" + c.priority);
        }

        byte[] selectedAid = candidates.get(0).aid;
        Log.d(TAG_LOG, "AID sélectionné = " + TlvParser.toHex(selectedAid));
        byte[] selectAidResponse = isoDep.transceive(ApduCommandBuilder.selectAid(selectedAid));
        Log.d(TAG_LOG, "SELECT AID response = " + TlvParser.toHex(selectAidResponse));
        if (!TlvParser.isSuccess(selectAidResponse)) {
            throw new EmvException("Échec SELECT AID");
        }
        byte[] selectAidData = TlvParser.stripStatusWord(selectAidResponse);
        byte[] pdol = TlvParser.findTag(selectAidData, TAG_PDOL);
        byte[] appLabel = TlvParser.findTag(selectAidData, TAG_APP_LABEL);
        Log.d(TAG_LOG, "PDOL = " + TlvParser.toHex(pdol) + " | Label = "
                + (appLabel != null ? new String(appLabel) : "null"));

        // TVR initial : le PAN n'est pas encore connu à ce stade, on suppose qu'il sera présent.
        // Le bit "montant dépasse le plafond" est en revanche déjà connu (le montant est fixé dès le départ).
        byte[] initialTvr = computeTvr(false);
        Log.d(TAG_LOG, "TVR initial (avant GPO) = " + TlvParser.toHex(initialTvr));

        byte[] pdolValues = buildDolValues(pdol, initialTvr);
        byte[] gpoResponse = isoDep.transceive(ApduCommandBuilder.getProcessingOptions(pdolValues));
        Log.d(TAG_LOG, "GPO response = " + TlvParser.toHex(gpoResponse));
        if (!TlvParser.isSuccess(gpoResponse)) {
            throw new EmvException("Échec GET PROCESSING OPTIONS");
        }
        byte[] gpoData = TlvParser.stripStatusWord(gpoResponse);
        byte[] afl;
        byte[] aip;

        if (gpoData.length > 0 && (gpoData[0] & 0xFF) == TAG_GPO_FORMAT1) {
            byte[] rawValue = TlvParser.findTag(gpoData, TAG_GPO_FORMAT1);
            Log.d(TAG_LOG, "GPO en Format 1 (tag 80) détecté, valeur brute = " + TlvParser.toHex(rawValue));
            if (rawValue != null && rawValue.length >= 2) {
                aip = Arrays.copyOfRange(rawValue, 0, 2);
                afl = rawValue.length > 2 ? Arrays.copyOfRange(rawValue, 2, rawValue.length) : null;
            } else {
                aip = null;
                afl = null;
                Log.w(TAG_LOG, "GPO Format 1 avec une valeur trop courte pour contenir AIP+AFL.");
            }
        } else {
            aip = TlvParser.findTag(gpoData, TAG_AIP);
            afl = TlvParser.findTag(gpoData, TAG_AFL);
            Log.d(TAG_LOG, "GPO en Format 2 (tag 77) détecté.");
        }
        Log.d(TAG_LOG, "AIP = " + TlvParser.toHex(aip));
        Log.d(TAG_LOG, "AFL = " + TlvParser.toHex(afl));

        byte[] pan = null;
        byte[] track2 = null;
        byte[] expiryDate = null;
        byte[] cdol1 = null;

        byte[] gpoPan = TlvParser.findTag(gpoData, TAG_PAN);
        if (gpoPan != null) {
            pan = gpoPan;
            Log.d(TAG_LOG, "PAN trouvé directement dans la réponse GPO (tag 5A) = " + TlvParser.toHex(gpoPan));
        }
        byte[] gpoTrack2 = TlvParser.findTag(gpoData, TAG_TRACK2);
        if (gpoTrack2 != null) {
            track2 = gpoTrack2;
            Log.d(TAG_LOG, "Track 2 trouvé directement dans la réponse GPO (tag 57) = " + TlvParser.toHex(gpoTrack2));
        }
        byte[] gpoExpiry = TlvParser.findTag(gpoData, TAG_EXPIRY_DATE);
        if (gpoExpiry != null) {
            expiryDate = gpoExpiry;
            Log.d(TAG_LOG, "Date d'expiration trouvée directement dans la réponse GPO = " + TlvParser.toHex(gpoExpiry));
        }
        byte[] gpoCdol1 = TlvParser.findTag(gpoData, TAG_CDOL1);
        if (gpoCdol1 != null) {
            cdol1 = gpoCdol1;
        }

        if (afl != null) {
            for (Tlv3 entry : parseAfl(afl)) {
                Log.d(TAG_LOG, "AFL entry -> SFI=" + entry.sfi
                        + " records " + entry.startRecord + "-" + entry.endRecord);
                for (int record = entry.startRecord; record <= entry.endRecord; record++) {
                    byte[] recResponse = isoDep.transceive(
                            ApduCommandBuilder.readRecord(record, entry.sfi));
                    Log.d(TAG_LOG, "READ RECORD sfi=" + entry.sfi + " rec=" + record
                            + " response = " + TlvParser.toHex(recResponse));
                    if (TlvParser.isSuccess(recResponse)) {
                        byte[] recData = TlvParser.stripStatusWord(recResponse);

                        byte[] foundPan = TlvParser.findTag(recData, TAG_PAN);
                        if (foundPan != null) {
                            pan = foundPan;
                            Log.d(TAG_LOG, "PAN trouvé (tag 5A) = " + TlvParser.toHex(foundPan));
                        }

                        byte[] foundTrack2 = TlvParser.findTag(recData, TAG_TRACK2);
                        if (foundTrack2 != null) {
                            track2 = foundTrack2;
                            Log.d(TAG_LOG, "Track 2 trouvé (tag 57) = " + TlvParser.toHex(foundTrack2));
                        }

                        byte[] foundExpiry = TlvParser.findTag(recData, TAG_EXPIRY_DATE);
                        if (foundExpiry != null) {
                            expiryDate = foundExpiry;
                            Log.d(TAG_LOG, "Date d'expiration trouvée (YYMMDD) = " + TlvParser.toHex(foundExpiry));
                        }

                        byte[] foundCdol1 = TlvParser.findTag(recData, TAG_CDOL1);
                        if (foundCdol1 != null) {
                            cdol1 = foundCdol1;
                            Log.d(TAG_LOG, "CDOL1 trouvé (tag 8C) = " + TlvParser.toHex(foundCdol1));
                        }
                    } else {
                        Log.w(TAG_LOG, "READ RECORD non réussi (SW != 9000) pour sfi=" + entry.sfi + " rec=" + record);
                    }
                }
            }
        } else {
            Log.w(TAG_LOG, "Pas d'AFL trouvé -> impossible de lire les enregistrements de la carte.");
        }

        if (pan == null && track2 != null) {
            pan = extractPanFromTrack2(track2);
            if (pan != null) {
                Log.d(TAG_LOG, "PAN extrait depuis le Track 2 = " + TlvParser.toHex(pan));
            } else {
                Log.w(TAG_LOG, "Échec de l'extraction du PAN depuis le Track 2.");
            }
        }

        if (expiryDate != null && isExpired(expiryDate)) {
            throw new EmvException("Carte expirée (date d'expiration : " + formatExpiry(expiryDate) + ")");
        }

        // TSI réel : "vérification terminal effectuée" n'est activé que si un contrôle a vraiment
        // eu lieu -- ici, la vérification de la date d'expiration de la carte.
        boolean terminalRiskManagementPerformed = (expiryDate != null);
        byte[] tsi = computeTsi(terminalRiskManagementPerformed);
        Log.d(TAG_LOG, "TSI calculé (contrôle expiration effectué=" + terminalRiskManagementPerformed
                + ") = " + TlvParser.toHex(tsi));

        // TVR final : recalculé maintenant que l'on sait réellement si le PAN a été trouvé.
        byte[] tvr = computeTvr(pan == null);
        Log.d(TAG_LOG, "TVR final (PAN manquant=" + (pan == null) + ") = " + TlvParser.toHex(tvr));

        String cidHex = null;
        if (cdol1 != null) {
            byte[] cdol1Values = buildDolValues(cdol1, tvr);
            byte[] genAcResponse = isoDep.transceive(ApduCommandBuilder.generateAc(cdol1Values));
            Log.d(TAG_LOG, "GENERATE AC response = " + TlvParser.toHex(genAcResponse));
            if (TlvParser.isSuccess(genAcResponse)) {
                byte[] genAcData = TlvParser.stripStatusWord(genAcResponse);
                byte[] cid;
                if (genAcData.length > 0 && (genAcData[0] & 0xFF) == 0x80) {
                    byte[] raw = TlvParser.findTag(genAcData, 0x80);
                    cid = (raw != null && raw.length >= 1) ? new byte[]{raw[0]} : null;
                } else {
                    cid = TlvParser.findTag(genAcData, TAG_CID);
                }
                if (cid != null) {
                    cidHex = TlvParser.toHex(cid);
                    Log.d(TAG_LOG, "CID réel reçu de la carte = " + cidHex);
                }
            } else {
                Log.w(TAG_LOG, "Échec GENERATE AC (SW != 9000)");
            }
        } else {
            Log.w(TAG_LOG, "Pas de CDOL1 trouvé -> GENERATE AC non tenté, CID indisponible.");
        }

        return new EmvReadResult(
                TlvParser.toHex(selectedAid),
                pan != null ? maskPan(TlvParser.toHex(pan)) : null,
                appLabel != null ? new String(appLabel) : null,
                cidHex,
                TlvParser.toHex(tvr),
                TlvParser.toHex(tsi)
        );
    }

    /**
     * Construit le TSI (2 octets) -- reflète les contrôles terminal RÉELLEMENT effectués.
     * Octet 1 bit4 = "Terminal risk management was performed", activé uniquement si la
     * vérification d'expiration a effectivement pu être réalisée sur cette carte.
     */
    private byte[] computeTsi(boolean terminalRiskManagementPerformed) {
        byte[] tsi = new byte[2];
        if (terminalRiskManagementPerformed) {
            tsi[0] |= 0x08;
        }
        return tsi;
    }

    /**
     * Construit le TVR (5 octets) -- calculé par le TERMINAL, jamais envoyé par la carte.
     * Bits statiques (toujours vrais dans ce kernel simplifié) :
     * - Octet 1 bit8 : authentification offline de la carte non effectuée
     * - Octet 3 bit8 : vérification du porteur (CVM) non effectuée
     * - Octet 4 bit4 : transaction forcée en ligne
     * Bits dynamiques (varient réellement selon la transaction) :
     * - Octet 1 bit6 : données ICC manquantes (si le PAN n'a pas pu être lu)
     * - Octet 4 bit8 : montant dépasse le plafond hors-ligne
     */
    private byte[] computeTvr(boolean panMissing) {
        byte[] tvr = new byte[5];
        tvr[0] |= (byte) 0x80;
        if (panMissing) {
            tvr[0] |= 0x20;
        }
        tvr[2] |= (byte) 0x80;
        tvr[3] |= 0x08;
        if (amountInCents > FLOOR_LIMIT) {
            tvr[3] |= (byte) 0x80;
        }
        return tvr;
    }

    private byte[] extractPanFromTrack2(byte[] track2) {
        StringBuilder hex = new StringBuilder();
        for (byte b : track2) {
            hex.append(String.format("%02X", b));
        }
        String fullHex = hex.toString();
        int separatorIndex = fullHex.indexOf('D');
        if (separatorIndex < 0) return null;
        String panDigits = fullHex.substring(0, separatorIndex);
        if (panDigits.length() < 10) return null;
        if (panDigits.length() % 2 != 0) panDigits = panDigits + "0";
        byte[] result = new byte[panDigits.length() / 2];
        for (int i = 0; i < result.length; i++) {
            result[i] = (byte) Integer.parseInt(panDigits.substring(i * 2, i * 2 + 2), 16);
        }
        return result;
    }

    private byte[] buildDolValues(byte[] dol, byte[] tvrBytes) throws EmvException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        if (dol == null) return out.toByteArray();

        List<DolEntry> requestedTags = parseDol(dol);
        for (DolEntry item : requestedTags) {
            int length = item.length;
            byte[] value;
            switch (item.tag) {
                case 0x9F02: value = amountToBcd(amountInCents, length); break;
                case 0x9F03: value = new byte[length]; break;
                case 0x5F2A: value = new byte[]{0x07, (byte) 0x88}; break;
                case 0x9A: value = new byte[]{0x00, 0x01, 0x01}; break;
                case 0x9C: value = new byte[]{0x00}; break;
                case 0x9F37: value = new byte[]{0x01, 0x02, 0x03, 0x04}; break;
                case 0x9F66: value = new byte[]{(byte) 0x20, 0x00, 0x00, 0x00}; break;
                case 0x9F1A: value = new byte[]{0x07, (byte) 0x88}; break;
                case 0x95: value = tvrBytes; break;
                case 0x9F35: value = new byte[]{0x22}; break;
                case 0x9F34: value = new byte[]{0x00, 0x00, 0x00}; break;
                case 0x9F21: value = new byte[]{0x00, 0x00, 0x00}; break;
                default: value = new byte[length];
            }
            byte[] padded = new byte[length];
            int copyLen = Math.min(length, value.length);
            System.arraycopy(value, 0, padded, length - copyLen, copyLen);
            out.write(padded, 0, padded.length);
        }
        return out.toByteArray();
    }

    private byte[] amountToBcd(long amount, int length) {
        String digits = String.format("%0" + (length * 2) + "d", amount);
        byte[] result = new byte[length];
        for (int i = 0; i < length; i++) {
            int high = digits.charAt(i * 2) - '0';
            int low = digits.charAt(i * 2 + 1) - '0';
            result[i] = (byte) ((high << 4) | low);
        }
        return result;
    }

    private String maskPan(String panHex) {
        String digitsOnly = panHex.replace("F", "");
        if (digitsOnly.length() < 10) return digitsOnly;
        String first = digitsOnly.substring(0, 6);
        String last = digitsOnly.substring(digitsOnly.length() - 4);
        return first + "******" + last;
    }

    private List<DolEntry> parseDol(byte[] dol) {
        List<DolEntry> entries = new ArrayList<>();
        int i = 0;
        while (i < dol.length) {
            int firstByte = dol[i] & 0xFF;
            int tag = firstByte;
            i++;
            if ((firstByte & 0x1F) == 0x1F) {
                int nextByte;
                do {
                    nextByte = dol[i] & 0xFF;
                    tag = (tag << 8) | nextByte;
                    i++;
                } while ((nextByte & 0x80) == 0x80);
            }
            int length = dol[i] & 0xFF;
            i++;
            entries.add(new DolEntry(tag, length));
        }
        return entries;
    }

    private static class DolEntry {
        final int tag;
        final int length;
        DolEntry(int tag, int length) { this.tag = tag; this.length = length; }
    }

    private List<AppCandidate> parseApplications(byte[] ppseData) {
        List<AppCandidate> result = new ArrayList<>();
        for (TlvParser.Tlv tlv : TlvParser.parse(ppseData)) {
            if (tlv.tag == TAG_APP_TEMPLATE) {
                extractApplicationFromTemplate(tlv.value, result);
            } else if (isLikelyConstructed(tlv.tag)) {
                for (byte[] templateValue : TlvParser.findAllTags(tlv.value, TAG_APP_TEMPLATE)) {
                    extractApplicationFromTemplate(templateValue, result);
                }
            }
        }
        return result;
    }

    private void extractApplicationFromTemplate(byte[] templateValue, List<AppCandidate> result) {
        byte[] aid = TlvParser.findTag(templateValue, TAG_AID);
        if (aid == null) return;
        byte[] priorityRaw = TlvParser.findTag(templateValue, TAG_APP_PRIORITY);
        Integer priority = (priorityRaw != null && priorityRaw.length > 0) ? (priorityRaw[0] & 0xFF) : null;
        result.add(new AppCandidate(aid, priority));
    }

    private boolean isLikelyConstructed(int tag) {
        int firstByte = tag > 0xFF ? (tag >> 8) : tag;
        return (firstByte & 0x20) == 0x20;
    }

    private static class AppCandidate {
        final byte[] aid;
        final Integer priority;
        AppCandidate(byte[] aid, Integer priority) { this.aid = aid; this.priority = priority; }
    }

    private boolean isExpired(byte[] expiryDateBcd) {
        if (expiryDateBcd.length < 3) return false;
        int year = 2000 + bcdByteToInt(expiryDateBcd[0]);
        int month = bcdByteToInt(expiryDateBcd[1]);
        java.util.Calendar cardExpiry = java.util.Calendar.getInstance();
        cardExpiry.set(year, month - 1, 1);
        cardExpiry.set(java.util.Calendar.DAY_OF_MONTH, cardExpiry.getActualMaximum(java.util.Calendar.DAY_OF_MONTH));
        cardExpiry.set(java.util.Calendar.HOUR_OF_DAY, 23);
        cardExpiry.set(java.util.Calendar.MINUTE, 59);
        return java.util.Calendar.getInstance().after(cardExpiry);
    }

    private String formatExpiry(byte[] expiryDateBcd) {
        if (expiryDateBcd.length < 2) return TlvParser.toHex(expiryDateBcd);
        int year = bcdByteToInt(expiryDateBcd[0]);
        int month = bcdByteToInt(expiryDateBcd[1]);
        return String.format(java.util.Locale.US, "%02d/%02d", month, year);
    }

    private int bcdByteToInt(byte b) {
        int high = (b >> 4) & 0x0F;
        int low = b & 0x0F;
        return high * 10 + low;
    }

    private List<Tlv3> parseAfl(byte[] afl) {
        List<Tlv3> entries = new ArrayList<>();
        for (int i = 0; i + 3 < afl.length; i += 4) {
            int sfi = (afl[i] & 0xFF) >> 3;
            int startRecord = afl[i + 1] & 0xFF;
            int endRecord = afl[i + 2] & 0xFF;
            entries.add(new Tlv3(sfi, startRecord, endRecord));
        }
        return entries;
    }

    private static class Tlv3 {
        final int sfi, startRecord, endRecord;
        Tlv3(int sfi, int startRecord, int endRecord) {
            this.sfi = sfi; this.startRecord = startRecord; this.endRecord = endRecord;
        }
    }
}