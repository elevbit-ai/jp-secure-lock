package br.com.joaquimpedro.jpsecure;

import android.app.admin.DeviceAdminReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

/**
 * Receptor de administrador do dispositivo (API oficial Android).
 * Permite bloqueio e wipe (factory reset) com consentimento do usuário.
 * Criado por Joaquim Pedro de Morais Filho
 */
public class JpDeviceAdminReceiver extends DeviceAdminReceiver {

    @Override
    public void onEnabled(Context context, Intent intent) {
        Toast.makeText(context, R.string.admin_enabled, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onDisabled(Context context, Intent intent) {
        Toast.makeText(context, R.string.admin_disabled, Toast.LENGTH_LONG).show();
    }

    @Override
    public CharSequence onDisableRequested(Context context, Intent intent) {
        return context.getString(R.string.admin_disable_warning);
    }
}
