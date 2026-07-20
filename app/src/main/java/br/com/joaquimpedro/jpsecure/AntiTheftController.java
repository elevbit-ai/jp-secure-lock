package br.com.joaquimpedro.jpsecure;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

/**
 * Bloqueador anti-roubo via APIs oficiais do Android.
 *
 * Nível alto de impacto (com Device Admin ativo):
 *  1) limpeza segura de chaves na memória do app
 *  2) wipe criptográfico do cofre
 *  3) lockNow() — trava a tela do sistema
 *  4) wipeData() — factory reset (apaga TODOS os dados do aparelho)
 *
 * Não danifica hardware/RAM fisicamente. O "destrutivo" é perda de DADOS
 * (padrão de anti-roubo empresarial / Find My Device).
 *
 * Criado por Joaquim Pedro de Morais Filho
 */
public final class AntiTheftController {

    private static final String TAG = "JPSecureAntiTheft";

    private final Context app;
    private final DevicePolicyManager dpm;
    private final ComponentName admin;

    public AntiTheftController(Context context) {
        this.app = context.getApplicationContext();
        this.dpm = (DevicePolicyManager) app.getSystemService(Context.DEVICE_POLICY_SERVICE);
        this.admin = new ComponentName(app, JpDeviceAdminReceiver.class);
    }

    public ComponentName getAdminComponent() {
        return admin;
    }

    public boolean isAdminActive() {
        return dpm != null && dpm.isAdminActive(admin);
    }

    public Intent createEnableAdminIntent() {
        Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
        intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, admin);
        intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                app.getString(R.string.admin_explain));
        return intent;
    }

    public void lockScreen() {
        if (!isAdminActive()) return;
        try {
            dpm.lockNow();
        } catch (SecurityException e) {
            Log.w(TAG, "lockNow blocked", e);
        }
    }

    /**
     * Resposta anti-roubo de alto impacto após 3 PIN errados.
     * @return true se factory reset foi solicitado com sucesso
     */
    public boolean executeHighImpactResponse(SecureVault vault) {
        // 1) wipe criptográfico local + limpeza de buffers do processo
        vault.triggerCryptographicWipe();
        CryptoEngine.memorySurgeWipe(new byte[64], new byte[128], new byte[256]);

        if (!isAdminActive()) {
            Log.w(TAG, "Device Admin inativo — apenas wipe do cofre do app");
            return false;
        }

        // 2) trava imediata
        try {
            dpm.lockNow();
        } catch (Exception e) {
            Log.w(TAG, "lockNow", e);
        }

        // 3) factory reset (destrutivo para DADOS, oficial Android)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                // API 34+: wipe com flags padrão
                dpm.wipeData(0);
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                dpm.wipeData(0);
            } else {
                dpm.wipeData(0);
            }
            return true;
        } catch (SecurityException e) {
            Log.e(TAG, "wipeData não permitido", e);
            return false;
        }
    }

    /** Remove Device Admin ao liberar/desativar proteção. */
    public void removeAdminIfPossible() {
        if (!isAdminActive()) return;
        try {
            dpm.removeActiveAdmin(admin);
        } catch (Exception e) {
            Log.w(TAG, "removeActiveAdmin", e);
        }
    }
}
