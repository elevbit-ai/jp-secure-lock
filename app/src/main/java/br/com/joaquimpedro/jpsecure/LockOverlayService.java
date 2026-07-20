package br.com.joaquimpedro.jpsecure;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

/**
 * Overlay de bloqueio estável: PIN digitável sem recriar a janela a cada segundo.
 * Criado por Joaquim Pedro de Morais Filho
 */
public class LockOverlayService extends Service {

    public static final String CHANNEL = "jp_secure_overlay";
    public static final int NOTIF_ID = 7102;
    public static final String ACTION_UNLOCKED = "br.com.joaquimpedro.jpsecure.OVERLAY_UNLOCKED";
    public static final String ACTION_WIPED = "br.com.joaquimpedro.jpsecure.OVERLAY_WIPED";

    private static volatile boolean sRunning = false;
    private static volatile boolean sAttached = false;
    private static volatile boolean sUserTyping = false;

    private WindowManager windowManager;
    private View overlayView;
    private EditText etPin;
    private TextView tvAttempts;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean attached = false;

    /** Só verifica se ainda precisa do overlay; NÃO recria a UI. */
    private final Runnable healthCheck = new Runnable() {
        @Override
        public void run() {
            SecureVault v = new SecureVault(LockOverlayService.this);
            if (!v.requiresPinScreen()) {
                stopSelf();
                return;
            }
            if (!canDrawOverlays()) {
                // sem overlay: deixa o MainActivity (watchdog)
                handler.postDelayed(this, 2500);
                return;
            }
            if (!attached) {
                attachOverlay();
            }
            // Nunca updateViewLayout em loop (isso fecha o teclado)
            handler.postDelayed(this, 2500);
        }
    };

    public static boolean isRunning() {
        return sRunning;
    }

    public static boolean isAttached() {
        return sAttached;
    }

    public static boolean isUserTyping() {
        return sUserTyping;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        sRunning = true;
        ensureChannel();
        startForeground(NOTIF_ID, buildNotification());

        SecureVault vault = new SecureVault(this);
        if (!vault.requiresPinScreen()) {
            stopSelf();
            return START_NOT_STICKY;
        }

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        if (!attached) {
            attachOverlay();
        }
        handler.removeCallbacks(healthCheck);
        handler.postDelayed(healthCheck, 2500);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacks(healthCheck);
        detachOverlay();
        sRunning = false;
        sUserTyping = false;
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void attachOverlay() {
        if (attached || !canDrawOverlays()) return;
        try {
            LayoutInflater inflater = LayoutInflater.from(this);
            overlayView = inflater.inflate(R.layout.overlay_lock, null);
            etPin = overlayView.findViewById(R.id.overlay_pin);
            tvAttempts = overlayView.findViewById(R.id.overlay_attempts);
            Button btn = overlayView.findViewById(R.id.overlay_unlock);

            SecureVault vault = new SecureVault(this);
            tvAttempts.setText(getString(R.string.attempts_left, vault.remainingAttempts()));

            btn.setOnClickListener(v -> tryUnlock());
            etPin.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == EditorInfo.IME_ACTION_DONE
                        || (event != null && event.getAction() == KeyEvent.ACTION_DOWN
                        && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                    tryUnlock();
                    return true;
                }
                return false;
            });
            etPin.setOnFocusChangeListener((v, hasFocus) -> sUserTyping = hasFocus);
            etPin.setOnClickListener(v -> {
                sUserTyping = true;
                showKeyboard();
            });

            // NÃO roubar foco do root — só do campo PIN
            windowManager.addView(overlayView, buildParams());
            attached = true;
            sAttached = true;

            etPin.post(() -> {
                etPin.requestFocus();
                showKeyboard();
            });
        } catch (Exception e) {
            attached = false;
            sAttached = false;
        }
    }

    private void showKeyboard() {
        if (etPin == null) return;
        etPin.requestFocus();
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.showSoftInput(etPin, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    private void tryUnlock() {
        SecureVault vault = new SecureVault(this);
        String pin = etPin != null ? etPin.getText().toString() : "";
        SecureVault.UnlockResult r = vault.unlock(pin);

        if (r.ok) {
            sUserTyping = false;
            if (etPin != null) etPin.setText("");
            Toast.makeText(this, r.message, Toast.LENGTH_SHORT).show();
            sendBroadcast(new Intent(ACTION_UNLOCKED).setPackage(getPackageName()));
            new AntiTheftController(this).stopKioskMode(null);
            detachOverlay();
            stopSelf();
            Intent i = new Intent(this, MainActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_SINGLE_TOP
                    | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            i.putExtra(MainActivity.EXTRA_SESSION_UNLOCKED, true);
            i.putExtra(MainActivity.EXTRA_UNLOCK_PLAIN, r.plain != null ? r.plain : "");
            startActivity(i);
            return;
        }

        if (r.wiped) {
            sUserTyping = false;
            if (etPin != null) etPin.setText("");
            Toast.makeText(this, r.message, Toast.LENGTH_LONG).show();
            AntiTheftController at = new AntiTheftController(this);
            if (vault.isAntiTheftWipeEnabled() && at.isAdminActive()) {
                at.executeFactoryResetOnly();
            } else {
                at.lockScreen();
            }
            sendBroadcast(new Intent(ACTION_WIPED).setPackage(getPackageName()));
            if (tvAttempts != null) tvAttempts.setText(R.string.state_wiped);
            return;
        }

        // Erro: limpa PIN mas NÃO destroi a janela / teclado
        if (etPin != null) {
            etPin.setText("");
            etPin.post(this::showKeyboard);
        }
        Toast.makeText(this, r.message, Toast.LENGTH_SHORT).show();
        if (tvAttempts != null) {
            tvAttempts.setText(getString(R.string.attempts_left, vault.remainingAttempts()));
        }
    }

    private void detachOverlay() {
        if (attached && overlayView != null && windowManager != null) {
            try {
                windowManager.removeView(overlayView);
            } catch (Exception ignored) {
            }
        }
        attached = false;
        sAttached = false;
        overlayView = null;
        etPin = null;
        tvAttempts = null;
    }

    private WindowManager.LayoutParams buildParams() {
        int type = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;

        // Flags estáveis para digitar: SEM recriar, COM teclado
        int flags = WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                | WindowManager.LayoutParams.FLAG_FULLSCREEN
                | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
                | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;

        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                type,
                flags,
                PixelFormat.TRANSLUCENT);
        lp.gravity = Gravity.TOP | Gravity.START;
        lp.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE
                | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            lp.layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }
        return lp;
    }

    private boolean canDrawOverlays() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this);
    }

    private void ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationChannel ch = new NotificationChannel(
                CHANNEL, getString(R.string.lock_channel), NotificationManager.IMPORTANCE_LOW);
        NotificationManager nm = getSystemService(NotificationManager.class);
        if (nm != null) nm.createNotificationChannel(ch);
    }

    private Notification buildNotification() {
        Intent open = new Intent(this, MainActivity.class);
        open.putExtra(MainActivity.EXTRA_FORCE_LOCK, true);
        PendingIntent pi = PendingIntent.getActivity(
                this, 1, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        return new NotificationCompat.Builder(this, CHANNEL)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.lock_overlay_notif))
                .setSmallIcon(android.R.drawable.ic_lock_lock)
                .setContentIntent(pi)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    public static void start(Context ctx) {
        if (sRunning && sAttached) return; // já está — não reinicia (evita travar teclado)
        Intent i = new Intent(ctx, LockOverlayService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ctx.startForegroundService(i);
        } else {
            ctx.startService(i);
        }
    }

    /** Força start mesmo se já running (ex.: boot). */
    public static void startForce(Context ctx) {
        Intent i = new Intent(ctx, LockOverlayService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ctx.startForegroundService(i);
        } else {
            ctx.startService(i);
        }
    }

    public static void stop(Context ctx) {
        sRunning = false;
        sAttached = false;
        sUserTyping = false;
        ctx.stopService(new Intent(ctx, LockOverlayService.class));
    }

    public static boolean hasOverlayPermission(Context ctx) {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(ctx);
    }

    public static Intent createOverlaySettingsIntent(Context ctx) {
        Intent i = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                android.net.Uri.parse("package:" + ctx.getPackageName()));
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return i;
    }
}
