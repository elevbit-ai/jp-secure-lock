package br.com.joaquimpedro.jpsecure;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Reabre a camada de bloqueio em boot, tela ligada e usuário presente.
 */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;
        SecureVault vault = new SecureVault(context);
        if (!vault.requiresPinScreen()) return;

        // Boot / tela ligada: força camadas de bloqueio
        LockOverlayService.startForce(context);
        LockWatchdogService.start(context);

        Intent i = new Intent(context, MainActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP
                | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        i.putExtra(MainActivity.EXTRA_FORCE_LOCK, true);
        context.startActivity(i);
    }
}
