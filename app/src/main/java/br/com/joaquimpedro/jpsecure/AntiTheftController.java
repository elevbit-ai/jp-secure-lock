package br.com.joaquimpedro.jpsecure;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

/**
 * Camada inviolável de bloqueio (APIs oficiais Android):
 * - Device Admin: lockNow / wipeData
 * - Device Owner: Lock Task (kiosk), status bar, keyguard
 * - App: overlay + launcher HOME + watchdog
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

    public boolean isDeviceOwner() {
        return dpm != null && dpm.isDeviceOwnerApp(app.getPackageName());
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
     * Ativa modo kiosk / lock task (máximo controle se Device Owner).
     */
    public void enableKioskAndLockTask(Activity activity) {
        try {
            if (isDeviceOwner()) {
                String pkg = app.getPackageName();
                dpm.setLockTaskPackages(admin, new String[]{pkg});
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    try {
                        dpm.setStatusBarDisabled(admin, true);
                    } catch (Exception e) {
                        Log.w(TAG, "setStatusBarDisabled", e);
                    }
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    try {
                        dpm.setKeyguardDisabled(admin, true);
                    } catch (Exception e) {
                        Log.w(TAG, "setKeyguardDisabled", e);
                    }
                }
            }
            if (activity != null) {
                try {
                    activity.startLockTask();
                } catch (Exception e) {
                    Log.w(TAG, "startLockTask (pode precisar Device Owner ou pin)", e);
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "enableKioskAndLockTask", e);
        }
    }

    public void stopKioskMode(Activity activity) {
        try {
            if (activity != null) {
                try {
                    activity.stopLockTask();
                } catch (Exception ignored) {
                }
            }
            if (isDeviceOwner()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    try {
                        dpm.setStatusBarDisabled(admin, false);
                    } catch (Exception ignored) {
                    }
                    try {
                        dpm.setKeyguardDisabled(admin, false);
                    } catch (Exception ignored) {
                    }
                }
                try {
                    dpm.setLockTaskPackages(admin, new String[0]);
                } catch (Exception ignored) {
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "stopKioskMode", e);
        }
    }

    public boolean executeHighImpactResponse(SecureVault vault) {
        if (vault.getState() != SecureVault.State.WIPED) {
            vault.triggerCryptographicWipe();
        }
        CryptoEngine.memorySurgeWipe(new byte[64], new byte[128], new byte[256]);
        lockScreen();
        return executeFactoryResetOnly();
    }

    public boolean executeFactoryResetOnly() {
        if (!isAdminActive()) {
            Log.w(TAG, "Device Admin inativo — sem factory reset");
            return false;
        }
        try {
            lockScreen();
            dpm.wipeData(0);
            return true;
        } catch (SecurityException e) {
            Log.e(TAG, "wipeData não permitido", e);
            return false;
        } catch (Exception e) {
            Log.e(TAG, "wipeData erro", e);
            return false;
        }
    }

    public void removeAdminIfPossible() {
        if (!isAdminActive()) return;
        try {
            if (isDeviceOwner()) {
                // Device Owner não remove por removeActiveAdmin — precisa clear-device-owner
                Log.w(TAG, "Device Owner: remova via adb shell dpm remove-active-admin ...");
                return;
            }
            dpm.removeActiveAdmin(admin);
        } catch (Exception e) {
            Log.w(TAG, "removeActiveAdmin", e);
        }
    }

    public String getLockStrengthLabel(Context ctx) {
        if (isDeviceOwner()) {
            return ctx.getString(R.string.lock_level_owner);
        }
        if (isAdminActive() && LockOverlayService.hasOverlayPermission(ctx)) {
            return ctx.getString(R.string.lock_level_strong);
        }
        if (isAdminActive() || LockOverlayService.hasOverlayPermission(ctx)) {
            return ctx.getString(R.string.lock_level_medium);
        }
        return ctx.getString(R.string.lock_level_basic);
    }
}
