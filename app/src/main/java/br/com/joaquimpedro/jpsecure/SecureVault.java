package br.com.joaquimpedro.jpsecure;

import android.content.Context;
import android.content.SharedPreferences;

import java.security.GeneralSecurityException;

/**
 * Cofre e estados do bloqueador de produção.
 * INACTIVE → ACTIVE (PIN) → sessão desbloqueada em memória → RELEASED
 * 3 erros → WIPED (recovery) ou factory reset se Device Admin.
 *
 * Criado por Joaquim Pedro de Morais Filho
 */
public class SecureVault {

    public static final int MAX_ATTEMPTS = 3;

    public enum State {
        INACTIVE,
        ACTIVE,
        WIPED,
        RELEASED
    }

    private static final String PREFS = "jp_secure_vault_v2";
    private static final String K_STATE = "state";
    private static final String K_SALT = "dek_salt";
    private static final String K_IV = "dek_iv";
    private static final String K_WRAP = "dek_wrap";
    private static final String K_VAULT = "vault_ct";
    private static final String K_FAILS = "fails";
    private static final String K_REC_SALT = "rec_salt";
    private static final String K_REC_HASH = "rec_hash";
    private static final String K_OWNER = "owner_note";
    private static final String K_CREATED = "created_by";
    private static final String K_ANTITHEFT = "anti_theft_wipe";
    private static final String K_BLOCKER = "blocker_active";

    private final SharedPreferences prefs;

    public SecureVault(Context ctx) {
        prefs = ctx.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        if (!prefs.contains(K_CREATED)) {
            prefs.edit()
                    .putString(K_CREATED, "Joaquim Pedro de Morais Filho")
                    .apply();
        }
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
        return prefs.getString(K_CREATED, "Joaquim Pedro de Morais Filho");
    }

    public String activate(String pin, String secretNote) throws GeneralSecurityException {
        if (pin == null || pin.length() < 4) {
            throw new IllegalArgumentException("PIN mínimo: 4 dígitos");
        }
        CryptoEngine.WrappedKey wrapped = CryptoEngine.wrapNewDek(pin);
        byte[] dek = CryptoEngine.unwrapDek(pin, wrapped);
        try {
            String vaultCt = CryptoEngine.encryptUtf8(dek, secretNote == null ? "" : secretNote);
            String recovery = CryptoEngine.generateRecoveryCode();
            byte[] recSalt = CryptoEngine.randomBytes(16);
            String recHash = CryptoEngine.hashPinOrRecovery(normalizeRecovery(recovery), recSalt);

            prefs.edit()
                    .putString(K_STATE, State.ACTIVE.name())
                    .putString(K_SALT, CryptoEngine.b64(wrapped.salt))
                    .putString(K_IV, CryptoEngine.b64(wrapped.iv))
                    .putString(K_WRAP, CryptoEngine.b64(wrapped.cipher))
                    .putString(K_VAULT, vaultCt)
                    .putString(K_REC_SALT, CryptoEngine.b64(recSalt))
                    .putString(K_REC_HASH, recHash)
                    .putString(K_OWNER, secretNote == null ? "" : secretNote)
                    .putInt(K_FAILS, 0)
                    .putBoolean(K_BLOCKER, true)
                    .putBoolean(K_ANTITHEFT, true)
                    .apply();
            return recovery;
        } finally {
            CryptoEngine.memorySurgeWipe(dek);
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

        try {
            CryptoEngine.WrappedKey wrapped = loadWrapped();
            byte[] dek = CryptoEngine.unwrapDek(pin, wrapped);
            try {
                String plain = CryptoEngine.decryptUtf8(dek, prefs.getString(K_VAULT, ""));
                prefs.edit().putInt(K_FAILS, 0).apply();
                return new UnlockResult(true, false, plain, "Desbloqueado.");
            } finally {
                CryptoEngine.memorySurgeWipe(dek);
            }
        } catch (Exception e) {
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
    }

    public void triggerCryptographicWipe() {
        prefs.edit()
                .putString(K_STATE, State.WIPED.name())
                .remove(K_SALT)
                .remove(K_IV)
                .remove(K_WRAP)
                .remove(K_VAULT)
                .putString(K_OWNER, "")
                .putInt(K_FAILS, MAX_ATTEMPTS)
                .putBoolean(K_BLOCKER, true)
                .apply();
        CryptoEngine.memorySurgeWipe(new byte[32], new byte[32], new byte[64]);
    }

    public String recover(String recoveryCode, String newPin, String secretNote)
            throws GeneralSecurityException {
        String storedHash = prefs.getString(K_REC_HASH, null);
        String saltB64 = prefs.getString(K_REC_SALT, null);
        if (storedHash == null || saltB64 == null) {
            throw new GeneralSecurityException("Sem âncora de recuperação.");
        }
        byte[] salt = CryptoEngine.unb64(saltB64);
        String tryHash = CryptoEngine.hashPinOrRecovery(normalizeRecovery(recoveryCode), salt);
        if (!storedHash.equals(tryHash)) {
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
                .putString(K_OWNER, "")
                .putInt(K_FAILS, 0)
                .putBoolean(K_BLOCKER, false)
                .putBoolean(K_ANTITHEFT, false)
                .apply();
        CryptoEngine.memorySurgeWipe(new byte[64]);
    }

    /** Reativa configuração após liberação (não é demo). */
    public void clearForNewSetup() {
        prefs.edit().clear()
                .putString(K_CREATED, "Joaquim Pedro de Morais Filho")
                .putString(K_STATE, State.INACTIVE.name())
                .apply();
    }

    private CryptoEngine.WrappedKey loadWrapped() {
        return new CryptoEngine.WrappedKey(
                CryptoEngine.unb64(prefs.getString(K_SALT, "")),
                CryptoEngine.unb64(prefs.getString(K_IV, "")),
                CryptoEngine.unb64(prefs.getString(K_WRAP, ""))
        );
    }

    private static String normalizeRecovery(String code) {
        return code == null ? "" : code.replace("-", "").replace(" ", "").trim().toUpperCase();
    }
}
