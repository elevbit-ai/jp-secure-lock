package br.com.joaquimpedro.jpsecure;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import androidx.core.app.NotificationCompat;

import java.util.List;

/**
 * Watchdog suave: só reabre o bloqueio se o usuário saiu do app.
 * NÃO interrompe a digitação do PIN.
 */
public class LockWatchdogService extends Service {

    public static final String CHANNEL = "jp_secure_lock";
    public static final int NOTIF_ID = 7101;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            SecureVault vault = new SecureVault(LockWatchdogService.this);
            if (!vault.requiresPinScreen()) {
                LockOverlayService.stop(LockWatchdogService.this);
                stopSelf();
                return;
            }

            // Se o usuário está digitando no overlay, não faça nada
            if (LockOverlayService.isUserTyping()) {
                handler.postDelayed(this, 2000);
                return;
            }

            boolean hasOverlay = LockOverlayService.hasOverlayPermission(LockWatchdogService.this);
            if (hasOverlay) {
                // Overlay estável — só garante que o serviço roda
                if (!LockOverlayService.isRunning() || !LockOverlayService.isAttached()) {
                    LockOverlayService.startForce(LockWatchdogService.this);
                }
                // Só reabre MainActivity se outro app estiver no topo E overlay falhou
                if (!isOurAppOnTop() && !LockOverlayService.isAttached()) {
                    openLockActivity();
                }
            } else {
                // Sem overlay: reabre a activity se saiu
                if (!isOurAppOnTop()) {
                    openLockActivity();
                }
            }

            handler.postDelayed(this, 2000);
        }
    };

    private void openLockActivity() {
        Intent i = new Intent(this, MainActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_SINGLE_TOP
                | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        i.putExtra(MainActivity.EXTRA_FORCE_LOCK, true);
        startActivity(i);
    }

    @SuppressWarnings("deprecation")
    private boolean isOurAppOnTop() {
        try {
            ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
            if (am == null) return false;
            List<ActivityManager.RunningAppProcessInfo> procs = am.getRunningAppProcesses();
            if (procs == null) return false;
            String pkg = getPackageName();
            for (ActivityManager.RunningAppProcessInfo p : procs) {
                if (p.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
                        && p.processName != null
                        && p.processName.equals(pkg)) {
                    return true;
                }
            }
        } catch (Exception ignored) {
        }
        return LockOverlayService.isAttached() || LockOverlayService.isUserTyping();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        ensureChannel();
        Intent open = new Intent(this, MainActivity.class);
        open.putExtra(MainActivity.EXTRA_FORCE_LOCK, true);
        PendingIntent pi = PendingIntent.getActivity(
                this, 0, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification n = new NotificationCompat.Builder(this, CHANNEL)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.lock_notif))
                .setSmallIcon(android.R.drawable.ic_lock_lock)
                .setContentIntent(pi)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();

        startForeground(NOTIF_ID, n);
        handler.removeCallbacks(tick);
        handler.postDelayed(tick, 1500);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacks(tick);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationChannel ch = new NotificationChannel(
                CHANNEL, getString(R.string.lock_channel), NotificationManager.IMPORTANCE_LOW);
        NotificationManager nm = getSystemService(NotificationManager.class);
        if (nm != null) nm.createNotificationChannel(ch);
    }

    public static void start(Context ctx) {
        Intent i = new Intent(ctx, LockWatchdogService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ctx.startForegroundService(i);
        } else {
            ctx.startService(i);
        }
    }

    public static void stop(Context ctx) {
        LockOverlayService.stop(ctx);
        ctx.stopService(new Intent(ctx, LockWatchdogService.class));
    }
}
