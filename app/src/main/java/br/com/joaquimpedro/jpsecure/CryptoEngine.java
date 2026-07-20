package br.com.joaquimpedro.jpsecure;

import android.util.Base64;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Engine de criptografia de ponta a ponta (no aparelho).
 * AES-256-GCM + PBKDF2-HMAC-SHA256.
 *
 * NÃO causa dano a hardware/RAM do sistema.
 * O "surto de memória" é limpeza segura de chaves em RAM do app (overwrite + GC).
 *
 * Criado por Joaquim Pedro de Morais Filho
 */
public final class CryptoEngine {

    private static final int SALT_LEN = 16;
    private static final int IV_LEN = 12;
    private static final int KEY_LEN = 256;
    private static final int GCM_TAG = 128;
    private static final int PBKDF2_ITERS = 120_000;
    private static final SecureRandom RNG = new SecureRandom();

    private CryptoEngine() {}

    public static byte[] randomBytes(int n) {
        byte[] b = new byte[n];
        RNG.nextBytes(b);
        return b;
    }

    public static String b64(byte[] raw) {
        return Base64.encodeToString(raw, Base64.NO_WRAP);
    }

    public static byte[] unb64(String s) {
        return Base64.decode(s, Base64.NO_WRAP);
    }

    public static byte[] sha256(byte[] data) throws GeneralSecurityException {
        return MessageDigest.getInstance("SHA-256").digest(data);
    }

    public static String hashPinOrRecovery(String secret, byte[] salt) throws GeneralSecurityException {
        byte[] derived = pbkdf2(secret.toCharArray(), salt);
        try {
            return b64(sha256(derived));
        } finally {
            secureWipe(derived);
        }
    }

    public static byte[] pbkdf2(char[] password, byte[] salt) throws GeneralSecurityException {
        PBEKeySpec spec = new PBEKeySpec(password, salt, PBKDF2_ITERS, KEY_LEN);
        try {
            SecretKeyFactory f = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            return f.generateSecret(spec).getEncoded();
        } finally {
            spec.clearPassword();
        }
    }

    public static class WrappedKey {
        public final byte[] salt;
        public final byte[] iv;
        public final byte[] cipher;

        public WrappedKey(byte[] salt, byte[] iv, byte[] cipher) {
            this.salt = salt;
            this.iv = iv;
            this.cipher = cipher;
        }
    }

    /** Gera DEK aleatória e a protege com a senha (KEK derivada). */
    public static WrappedKey wrapNewDek(String pin) throws GeneralSecurityException {
        byte[] salt = randomBytes(SALT_LEN);
        byte[] dek = randomBytes(32);
        byte[] kek = pbkdf2(pin.toCharArray(), salt);
        try {
            byte[] iv = randomBytes(IV_LEN);
            byte[] cipher = aesGcm(true, kek, iv, dek);
            return new WrappedKey(salt, iv, cipher);
        } finally {
            secureWipe(dek);
            secureWipe(kek);
        }
    }

    public static byte[] unwrapDek(String pin, WrappedKey wrapped) throws GeneralSecurityException {
        byte[] kek = pbkdf2(pin.toCharArray(), wrapped.salt);
        try {
            return aesGcm(false, kek, wrapped.iv, wrapped.cipher);
        } finally {
            secureWipe(kek);
        }
    }

    public static String encryptUtf8(byte[] dek, String plain) throws GeneralSecurityException {
        byte[] iv = randomBytes(IV_LEN);
        byte[] pt = plain.getBytes(StandardCharsets.UTF_8);
        try {
            byte[] ct = aesGcm(true, dek, iv, pt);
            ByteBuffer buf = ByteBuffer.allocate(iv.length + ct.length);
            buf.put(iv);
            buf.put(ct);
            return b64(buf.array());
        } finally {
            secureWipe(pt);
        }
    }

    public static String decryptUtf8(byte[] dek, String payloadB64) throws GeneralSecurityException {
        byte[] all = unb64(payloadB64);
        byte[] iv = Arrays.copyOfRange(all, 0, IV_LEN);
        byte[] ct = Arrays.copyOfRange(all, IV_LEN, all.length);
        byte[] pt = aesGcm(false, dek, iv, ct);
        try {
            return new String(pt, StandardCharsets.UTF_8);
        } finally {
            secureWipe(pt);
            secureWipe(all);
        }
    }

    private static byte[] aesGcm(boolean encrypt, byte[] key, byte[] iv, byte[] input)
            throws GeneralSecurityException {
        SecretKey sk = new SecretKeySpec(key, "AES");
        Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
        c.init(encrypt ? Cipher.ENCRYPT_MODE : Cipher.DECRYPT_MODE, sk, new GCMParameterSpec(GCM_TAG, iv));
        return c.doFinal(input);
    }

    /**
     * "Surto de memória" legítimo: sobrescreve buffers sensíveis na heap do app
     * e sugere GC. Não afeta o SO, a RAM do sistema nem o hardware.
     */
    public static void memorySurgeWipe(byte[]... buffers) {
        if (buffers != null) {
            for (byte[] b : buffers) {
                secureWipe(b);
            }
        }
        // Passadas extras de alocação/liberação (simulação controlada de pressão de heap do APP)
        for (int i = 0; i < 3; i++) {
            byte[] tmp = new byte[256 * 1024];
            RNG.nextBytes(tmp);
            secureWipe(tmp);
        }
        System.gc();
    }

    public static void secureWipe(byte[] b) {
        if (b == null) return;
        Arrays.fill(b, (byte) 0);
        RNG.nextBytes(b);
        Arrays.fill(b, (byte) 0);
    }

    public static String generateRecoveryCode() {
        // 12 chars legíveis (sem 0/O/1/I)
        final char[] alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
        char[] out = new char[12];
        byte[] rnd = randomBytes(12);
        for (int i = 0; i < 12; i++) {
            out[i] = alphabet[(rnd[i] & 0xFF) % alphabet.length];
        }
        secureWipe(rnd);
        String code = new String(out);
        // Formato XXXX-XXXX-XXXX
        return code.substring(0, 4) + "-" + code.substring(4, 8) + "-" + code.substring(8, 12);
    }
}
