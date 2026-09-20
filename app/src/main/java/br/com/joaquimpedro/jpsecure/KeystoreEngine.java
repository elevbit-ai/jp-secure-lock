package br.com.joaquimpedro.jpsecure;

import android.os.Build;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;

import java.io.ByteArrayOutputStream;
import java.security.GeneralSecurityException;
import java.security.KeyStore;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/**
 * Camada de proteção ligada ao hardware (Android Keystore / TEE / StrongBox).
 *
 * <p>Gera uma chave AES-256 <b>não exportável</b> que nunca sai do elemento seguro
 * do aparelho. Essa chave funciona como uma "pimenta" (pepper): o cofre derivado
 * do PIN é adicionalmente selado com ela. Consequência prática:
 *
 * <ul>
 *   <li>Extrair o arquivo de preferências de um aparelho com root NÃO basta para
 *       um ataque offline — sem a chave do TEE o blob é inútil.</li>
 *   <li>O ataque fica limitado ao próprio aparelho, onde o limite de 3 tentativas
 *       e o wipe anti-roubo se aplicam.</li>
 * </ul>
 *
 * Criado por Joaquim Pedro de Morais Filho.
 */
public final class KeystoreEngine {

    private static final String PROVIDER = "AndroidKeyStore";
    private static final String KEY_ALIAS = "jp_secure_lock_pepper_v1";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_TAG_BITS = 128;
    private static final int GCM_IV_BYTES = 12;

    private KeystoreEngine() {}

    /**
     * @return {@code true} se o aparelho oferece o Keystore de hardware. Em falha
     *         de plataforma, o cofre ainda funciona apenas com PBKDF2 (esquema 1).
     */
    public static boolean isAvailable() {
        try {
            getOrCreateKey();
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    private static SecretKey getOrCreateKey() throws GeneralSecurityException {
        try {
            KeyStore ks = KeyStore.getInstance(PROVIDER);
            ks.load(null);
            KeyStore.Entry entry = ks.getEntry(KEY_ALIAS, null);
            if (entry instanceof KeyStore.SecretKeyEntry) {
                return ((KeyStore.SecretKeyEntry) entry).getSecretKey();
            }
            return generateKey(true);
        } catch (GeneralSecurityException e) {
            throw e;
        } catch (Exception e) {
            throw new GeneralSecurityException("Keystore indisponível", e);
        }
    }

    private static SecretKey generateKey(boolean tryStrongBox) throws GeneralSecurityException {
        try {
            KeyGenerator kg = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, PROVIDER);
            KeyGenParameterSpec.Builder b = new KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .setRandomizedEncryptionRequired(true);

            if (tryStrongBox && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                b.setIsStrongBoxBacked(true);
            }
            kg.init(b.build());
            return kg.generateKey();
        } catch (Exception e) {
            // StrongBox pode não existir neste aparelho: repete sem exigir
            // hardware dedicado (TEE comum ainda protege a chave).
            if (tryStrongBox) {
                return generateKey(false);
            }
            throw new GeneralSecurityException("Falha ao gerar chave do Keystore", e);
        }
    }

    /** Sela {@code plaintext} com a chave do hardware. Formato: [IV(12) | ciphertext+tag]. */
    public static byte[] seal(byte[] plaintext) throws GeneralSecurityException {
        try {
            Cipher c = Cipher.getInstance(TRANSFORMATION);
            c.init(Cipher.ENCRYPT_MODE, getOrCreateKey());
            byte[] iv = c.getIV();
            byte[] ct = c.doFinal(plaintext);
            ByteArrayOutputStream out = new ByteArrayOutputStream(iv.length + ct.length);
            out.write(iv, 0, iv.length);
            out.write(ct, 0, ct.length);
            return out.toByteArray();
        } catch (GeneralSecurityException e) {
            throw e;
        } catch (Exception e) {
            throw new GeneralSecurityException("seal falhou", e);
        }
    }

    /** Abre um blob produzido por {@link #seal(byte[])}. */
    public static byte[] unseal(byte[] sealed) throws GeneralSecurityException {
        if (sealed == null || sealed.length <= GCM_IV_BYTES) {
            throw new GeneralSecurityException("blob do Keystore inválido");
        }
        try {
            byte[] iv = new byte[GCM_IV_BYTES];
            System.arraycopy(sealed, 0, iv, 0, GCM_IV_BYTES);
            byte[] ct = new byte[sealed.length - GCM_IV_BYTES];
            System.arraycopy(sealed, GCM_IV_BYTES, ct, 0, ct.length);
            Cipher c = Cipher.getInstance(TRANSFORMATION);
            c.init(Cipher.DECRYPT_MODE, getOrCreateKey(), new GCMParameterSpec(GCM_TAG_BITS, iv));
            return c.doFinal(ct);
        } catch (GeneralSecurityException e) {
            throw e;
        } catch (Exception e) {
            throw new GeneralSecurityException("unseal falhou", e);
        }
    }
}
