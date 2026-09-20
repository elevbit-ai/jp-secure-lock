package br.com.joaquimpedro.jpsecure;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import java.security.GeneralSecurityException;

/**
 * Cofre e máquina de estados do bloqueador.
 *
 * <p>Estados: INACTIVE → ACTIVE (PIN) → sessão desbloqueada em memória → RELEASED.
 * Em 3 PINs incorretos: WIPED (recuperável por código) ou factory reset se
 * o Device Admin estiver ativo.
 *
 * <p><b>Segurança do armazenamento (v3):</b>
 * <ul>
 *   <li>Preferências guardadas em {@link EncryptedSharedPreferences} (chave-mestra
 *       no Android Keystore) — o contador de tentativas não pode ser zerado
 *       editando o arquivo, fechando o bypass clássico do wipe.</li>
 *   <li>Esquema 2: a DEK protegida pelo PIN e o hash de recuperação são
 *       adicionalmente <i>selados</i> pela chave não exportável do hardware
 *       ({@link KeystoreEngine}). Extrair o arquivo não permite mais ataque
 *       offline ao PIN.</li>
 *   <li>A nota do cofre é gravada <b>apenas cifrada</b> (o campo em texto puro
 *       das versões antigas foi removido).</li>
 * </ul>
 *
 * Criado por Joaquim Pedro de Morais Filho
 */
public class SecureVault {

    public static final int MAX_ATTEMPTS = 3;
    public static final int MIN_PIN_LENGTH = 6;

    public enum State {
        INACTIVE,
        ACTIVE,
        WIPED,
        RELEASED
    }

    /** Esquema de proteção do cofre. 1 = PBKDF2; 2 = PBKDF2 + selagem por hardware. */
    private static final int SCHEME_LEGACY = 1;
    private static final int SCHEME_HARDWARE = 2;

    private static final String CREATOR = "Joaquim Pedro de Morais Filho";

    private static final String PREFS_ENC = "jp_secure_vault_v3";
    private static final String PREFS_LEGACY = "jp_secure_vault_v2";

    private static final String K_STATE = "state";
    private static final String K_SCHEME = "scheme";
    private static final String K_SALT = "dek_salt";
    private static final String K_IV = "dek_iv";
    private static final String K_WRAP = "dek_wrap";
    private static final String K_VAULT = "vault_ct";
    private static final String K_FAILS = "fails";
    private static final String K_REC_SALT = "rec_salt";
    private static final String K_REC_HASH = "rec_hash";
    private static final String K_CREATED = "created_by";
    private static final String K_ANTITHEFT = "anti_theft_wipe";
    private static final String K_BLOCKER = "blocker_active";

    private final SharedPreferences prefs;

    public SecureVault(Context ctx) {
        this.prefs = buildPrefs(ctx.getApplicationContext());
        migrateLegacyIfNeeded(ctx.getApplicationContext());
        if (!prefs.contains(K_CREATED)) {
            prefs.edit().putString(K_CREATED, CREATOR).apply();
        }
    }

    /** Preferências cifradas (Keystore). Em falha de plataforma, usa modo padrão. */
    private static SharedPreferences buildPrefs(Context app) {
        try {
            MasterKey masterKey = new MasterKey.Builder(app)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();
            return EncryptedSharedPreferences.create(
                    app,
                    PREFS_ENC,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM);
        } catch (Throwable t) {
            // Fallback resiliente: mesmo nome de arquivo, sem cifra do container.
            return app.getSharedPreferences(PREFS_ENC, Context.MODE_PRIVATE);
        }
    }

    /** Copia um cofre pré-existente (v2, esquema legado) para o novo container. */
    private void migrateLegacyIfNeeded(Context app) {
        if (prefs.contains(K_STATE)) return; // já migrado / instalado no novo formato
        SharedPreferences old = app.getSharedPreferences(PREFS_LEGACY, Context.MODE_PRIVATE);
        if (!old.contains(K_STATE)) return;
        SharedPreferences.Editor e = prefs.edit();
        e.putString(K_STATE, old.getString(K_STATE, State.INACTIVE.name()));
        e.putInt(K_SCHEME, SCHEME_LEGACY);
        copyString(old, e, K_SALT);
        copyString(old, e, K_IV);
        copyString(old, e, K_WRAP);
        copyString(old, e, K_VAULT);
        copyString(old, e, K_REC_SALT);
        copyString(old, e, K_REC_HASH);
        e.putInt(K_FAILS, old.getInt(K_FAILS, 0));
        e.putBoolean(K_ANTITHEFT, old.getBoolean(K_ANTITHEFT, true));
        e.putBoolean(K_BLOCKER, old.getBoolean(K_BLOCKER, false));
        e.putString(K_CREATED, CREATOR);
        e.apply();
    }

    private static void copyString(SharedPreferences from, SharedPreferences.Editor to, String key) {
        String v = from.getString(key, null);
        if (v != null) to.putString(key, v);
    }

    public boolean isAntiTheftWipeEnabled() {
        return prefs.getBoolean(K_ANTITHEFT, true);
    }

    public void setAntiTheftWipeEnabled(boolean enabled) {
        prefs.edit().putBoolean(K_ANTITHEFT, enabled).apply();
    }

    public boolean isBlockerActive() {
        return prefs.getBoolean(K_BLOCKER, false) && getState() == State.ACTIVE;
    }

    public void setBlockerActive(boolean active) {
        prefs.edit().putBoolean(K_BLOCKER, active).apply();
    }

    /** Precisa da tela de PIN e não pode sair. */
    public boolean requiresPinScreen() {
        State s = getState();
        return (s == State.ACTIVE && prefs.getBoolean(K_BLOCKER, false))
                || s == State.WIPED;
    }

    public boolean isFullyLocked() {
        return getState() == State.ACTIVE && prefs.getBoolean(K_BLOCKER, false);
    }

    public State getState() {
        String s = prefs.getString(K_STATE, State.INACTIVE.name());
        try {
            return State.valueOf(s);
        } catch (Exception e) {
            return State.INACTIVE;
        }
    }

    public int getFails() {
        return prefs.getInt(K_FAILS, 0);
    }

    public int remainingAttempts() {
        return Math.max(0, MAX_ATTEMPTS - getFails());
    }

    public String getCreator() {
        return prefs.getString(K_CREATED, CREATOR);
    }

    private int scheme() {
        return prefs.getInt(K_SCHEME, SCHEME_LEGACY);
    }

    /** Valida a robustez mínima do PIN escolhido na ativação. */
    public static String validatePin(String pin) {
        if (pin == null || pin.length() < MIN_PIN_LENGTH) {
            return "PIN deve ter pelo menos " + MIN_PIN_LENGTH + " dígitos.";
        }
        if (isWeakPin(pin)) {
            return "PIN muito previsível (repetido ou sequencial). Escolha outro.";
        }
        return null;
    }

    private static boolean isWeakPin(String pin) {
        boolean allSame = true;
        boolean asc = true;
        boolean desc = true;
        for (int i = 1; i < pin.length(); i++) {
            char prev = pin.charAt(i - 1);
            char cur = pin.charAt(i);
            if (cur != prev) allSame = false;
            if (cur != prev + 1) asc = false;
            if (cur != prev - 1) desc = false;
        }
        return allSame || asc || desc;
    }

    public String activate(String pin, String secretNote) throws GeneralSecurityException {
        String err = validatePin(pin);
        if (err != null) {
            throw new IllegalArgumentException(err);
        }
        boolean hardware = KeystoreEngine.isAvailable();
        int useScheme = hardware ? SCHEME_HARDWARE : SCHEME_LEGACY;

        CryptoEngine.WrappedKey wrapped = CryptoEngine.wrapNewDek(pin);
        byte[] dek = CryptoEngine.unwrapDek(pin, wrapped);
        try {
            String vaultCt = CryptoEngine.encryptUtf8(dek, secretNote == null ? "" : secretNote);
            String recovery = CryptoEngine.generateRecoveryCode();
            byte[] recSalt = CryptoEngine.randomBytes(16);
            String recHash = CryptoEngine.hashPinOrRecovery(normalizeRecovery(recovery), recSalt);

            String wrapStored = (useScheme == SCHEME_HARDWARE)
                    ? CryptoEngine.sealB64(wrapped.cipher)
                    : CryptoEngine.b64(wrapped.cipher);
            String recStored = (useScheme == SCHEME_HARDWARE)
                    ? CryptoEngine.sealB64(recHash.getBytes(java.nio.charset.StandardCharsets.UTF_8))
                    : recHash;

            prefs.edit()
                    .putString(K_STATE, State.ACTIVE.name())
                    .putInt(K_SCHEME, useScheme)
                    .putString(K_SALT, CryptoEngine.b64(wrapped.salt))
                    .putString(K_IV, CryptoEngine.b64(wrapped.iv))
                    .putString(K_WRAP, wrapStored)
                    .putString(K_VAULT, vaultCt)
                    .putString(K_REC_SALT, CryptoEngine.b64(recSalt))
                    .putString(K_REC_HASH, recStored)
                    .putInt(K_FAILS, 0)
                    .putBoolean(K_BLOCKER, true)
                    .putBoolean(K_ANTITHEFT, true)
                    .apply();
            return recovery;
        } finally {
            CryptoEngine.secureWipeAll(dek);
        }
    }

    public static class UnlockResult {
        public final boolean ok;
        public final boolean wiped;
        public final String plain;
        public final String message;

        public UnlockResult(boolean ok, boolean wiped, String plain, String message) {
            this.ok = ok;
            this.wiped = wiped;
            this.plain = plain;
            this.message = message;
        }
    }

    public UnlockResult unlock(String pin) {
        State st = getState();
        if (st == State.INACTIVE || st == State.RELEASED) {
            return new UnlockResult(false, false, null, "Proteção inativa.");
        }
        if (st == State.WIPED) {
            return new UnlockResult(false, true, null, "Cofre em wipe. Use recovery.");
        }
        if (pin == null || pin.isEmpty()) {
            return new UnlockResult(false, false, null, "Digite o PIN.");
        }

        // Falha do armazenamento seguro NÃO conta como PIN errado (evita wipe indevido).
        CryptoEngine.WrappedKey wrapped;
        try {
            wrapped = loadWrapped();
        } catch (Exception e) {
            return new UnlockResult(false, false, null,
                    "Erro no armazenamento seguro. Tente novamente.");
        }

        byte[] dek;
        try {
            dek = CryptoEngine.unwrapDek(pin, wrapped); // PIN errado → GCM tag inválida
        } catch (Exception wrongPin) {
            int fails = getFails() + 1;
            prefs.edit().putInt(K_FAILS, fails).apply();
            if (fails >= MAX_ATTEMPTS) {
                triggerCryptographicWipe();
                return new UnlockResult(false, true, null,
                        "3 tentativas falhas. Protocolo anti-roubo acionado.");
            }
            return new UnlockResult(false, false, null,
                    "PIN incorreto. Restantes: " + (MAX_ATTEMPTS - fails));
        }

        try {
            String plain = CryptoEngine.decryptUtf8(dek, prefs.getString(K_VAULT, ""));
            prefs.edit().putInt(K_FAILS, 0).apply();
            return new UnlockResult(true, false, plain, "Desbloqueado.");
        } catch (Exception e) {
            // PIN correto (DEK abriu) mas cofre corrompido: não penaliza tentativas.
            return new UnlockResult(false, false, null,
                    "Cofre inacessível. Use o código de recuperação.");
        } finally {
            CryptoEngine.secureWipeAll(dek);
        }
    }

    public void triggerCryptographicWipe() {
        prefs.edit()
                .putString(K_STATE, State.WIPED.name())
                .remove(K_SALT)
                .remove(K_IV)
                .remove(K_WRAP)
                .remove(K_VAULT)
                .putInt(K_FAILS, MAX_ATTEMPTS)
                .putBoolean(K_BLOCKER, true)
                .apply();
        CryptoEngine.secureWipeAll(new byte[32], new byte[32], new byte[64]);
    }

    public String recover(String recoveryCode, String newPin, String secretNote)
            throws GeneralSecurityException {
        String storedHash = readRecoveryHash();
        String saltB64 = prefs.getString(K_REC_SALT, null);
        if (storedHash == null || saltB64 == null) {
            throw new GeneralSecurityException("Sem âncora de recuperação.");
        }
        byte[] salt = CryptoEngine.unb64(saltB64);
        String tryHash = CryptoEngine.hashPinOrRecovery(normalizeRecovery(recoveryCode), salt);
        if (!CryptoEngine.constantTimeEquals(storedHash, tryHash)) {
            throw new GeneralSecurityException("Código de recuperação inválido.");
        }
        return activate(newPin, secretNote != null ? secretNote : "");
    }

    public void releaseAndDeactivate() {
        prefs.edit()
                .putString(K_STATE, State.RELEASED.name())
                .remove(K_SALT)
                .remove(K_IV)
                .remove(K_WRAP)
                .remove(K_VAULT)
                .remove(K_REC_SALT)
                .remove(K_REC_HASH)
                .remove(K_SCHEME)
                .putInt(K_FAILS, 0)
                .putBoolean(K_BLOCKER, false)
                .putBoolean(K_ANTITHEFT, false)
                .apply();
        CryptoEngine.secureWipeAll(new byte[64]);
    }

    /** Reativa configuração após liberação. */
    public void clearForNewSetup() {
        prefs.edit().clear()
                .putString(K_CREATED, CREATOR)
                .putString(K_STATE, State.INACTIVE.name())
                .apply();
    }

    private CryptoEngine.WrappedKey loadWrapped() throws GeneralSecurityException {
        byte[] cipher = (scheme() == SCHEME_HARDWARE)
                ? CryptoEngine.unsealB64(prefs.getString(K_WRAP, ""))
                : CryptoEngine.unb64(prefs.getString(K_WRAP, ""));
        return new CryptoEngine.WrappedKey(
                CryptoEngine.unb64(prefs.getString(K_SALT, "")),
                CryptoEngine.unb64(prefs.getString(K_IV, "")),
                cipher
        );
    }

    private String readRecoveryHash() throws GeneralSecurityException {
        String stored = prefs.getString(K_REC_HASH, null);
        if (stored == null) return null;
        if (scheme() == SCHEME_HARDWARE) {
            return new String(CryptoEngine.unsealB64(stored),
                    java.nio.charset.StandardCharsets.UTF_8);
        }
        return stored;
    }

    private static String normalizeRecovery(String code) {
        return code == null ? "" : code.replace("-", "").replace(" ", "").trim().toUpperCase();
    }
}
