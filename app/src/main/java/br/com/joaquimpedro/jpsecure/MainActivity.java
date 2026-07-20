package br.com.joaquimpedro.jpsecure;

import android.app.ActivityManager;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

/**
 * JP Secure Lock — camada inviolável de bloqueio.
 * Bloqueado: overlay + kiosk + HOME + watchdog; nada além do PIN.
 * Criado por Joaquim Pedro de Morais Filho
 */
public class MainActivity extends AppCompatActivity {

    public static final String EXTRA_FORCE_LOCK = "force_lock";
    public static final String EXTRA_SESSION_UNLOCKED = "session_unlocked";
    public static final String EXTRA_UNLOCK_PLAIN = "unlock_plain";

    private SecureVault vault;
    private AntiTheftController antiTheft;

    private LinearLayout panelSetup, panelUnlock, panelUnlocked, panelWiped, panelReleased, panelWipeAnim;
    private LinearLayout headerExtras;
    private EditText etSetupPin, etSetupPin2, etSecret, etUnlockPin, etRecovery, etNewPin;
    private TextView tvStatus, tvAttempts, tvUnlockedNote, tvWipeLog, tvCreator, tvAdminStatus, tvLockTitle, tvLockLevel;
    private ProgressBar wipeBar;
    private CheckBox cbAntiTheft;
    private Button btnActivate, btnUnlock, btnRelease, btnRecover, btnRelock, btnNewSetup, btnEnableAdmin, btnOverlay, btnHome;

    private String lastPlain;
    private boolean sessionUnlocked = false;
    private boolean pendingActivateAfterAdmin = false;
    private boolean wipeInProgress = false;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean kioskStarted = false;
    /** Reassert suave: só se perdeu o foco e o usuário NÃO está digitando. */
    private final Runnable reassertLock = new Runnable() {
        @Override
        public void run() {
            if (!mustStayOnPinScreen()) return;
            if (LockOverlayService.isUserTyping()) {
                handler.postDelayed(this, 2000);
                return;
            }
            applyLockWindowFlags(true);
            if (!kioskStarted) {
                antiTheft.enableKioskAndLockTask(MainActivity.this);
                kioskStarted = true;
            }
            // Preferir overlay estável; não chamar startActivity em loop
            if (LockOverlayService.hasOverlayPermission(MainActivity.this)) {
                if (!LockOverlayService.isAttached()) {
                    LockOverlayService.startForce(MainActivity.this);
                }
            } else if (!hasWindowFocus()) {
                bringSelfToFront();
            }
            handler.postDelayed(this, 2500);
        }
    };

    private final ActivityResultLauncher<Intent> adminLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                updateAdminStatus();
                if (antiTheft.isAdminActive() && pendingActivateAfterAdmin) {
                    pendingActivateAfterAdmin = false;
                    ensureOverlayThenActivate();
                } else if (pendingActivateAfterAdmin) {
                    pendingActivateAfterAdmin = false;
                    toast(getString(R.string.admin_required_for_max));
                }
            });

    private final ActivityResultLauncher<Intent> overlayLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (LockOverlayService.hasOverlayPermission(this) && pendingActivateAfterAdmin) {
                    // se veio do fluxo de ativação
                }
                updateAdminStatus();
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        vault = new SecureVault(this);
        antiTheft = new AntiTheftController(this);
        bindViews();
        wire();

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (mustStayOnPinScreen()) {
                    toast(getString(R.string.lock_no_exit));
                    return;
                }
                finish();
            }
        });

        applyIntentExtras(getIntent());
        render();
        syncLockMode();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        applyIntentExtras(intent);
        render();
        syncLockMode();
    }

    private void applyIntentExtras(Intent intent) {
        if (intent == null) return;
        if (intent.getBooleanExtra(EXTRA_FORCE_LOCK, false)) {
            sessionUnlocked = false;
        }
        if (intent.getBooleanExtra(EXTRA_SESSION_UNLOCKED, false)) {
            sessionUnlocked = true;
            lastPlain = intent.getStringExtra(EXTRA_UNLOCK_PLAIN);
            antiTheft.stopKioskMode(this);
            LockWatchdogService.stop(this);
        }
        // HOME launcher: se bloqueado, força PIN
        if (Intent.ACTION_MAIN.equals(intent.getAction())
                && intent.hasCategory(Intent.CATEGORY_HOME)
                && vault.requiresPinScreen()) {
            sessionUnlocked = false;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (vault.isFullyLocked() && !sessionUnlocked) {
            sessionUnlocked = false;
        }
        updateAdminStatus();
        // Não re-render agressivo se o campo PIN tem foco (evita fechar teclado)
        if (etUnlockPin == null || !etUnlockPin.hasFocus()) {
            render();
        }
        syncLockMode();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Não rouba o teclado com bringSelfToFront em loop.
        // Se o bloqueio está ativo e o usuário saiu, o overlay + watchdog cuidam.
        if (mustStayOnPinScreen()) {
            if (LockOverlayService.hasOverlayPermission(this)) {
                LockOverlayService.start(this);
            }
            LockWatchdogService.start(this);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mustStayOnPinScreen()) {
            if (LockOverlayService.hasOverlayPermission(this)) {
                LockOverlayService.startForce(this);
            }
            LockWatchdogService.start(this);
        }
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(reassertLock);
        super.onDestroy();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (mustStayOnPinScreen()) {
            if (keyCode == KeyEvent.KEYCODE_BACK
                    || keyCode == KeyEvent.KEYCODE_HOME
                    || keyCode == KeyEvent.KEYCODE_APP_SWITCH
                    || keyCode == KeyEvent.KEYCODE_MENU) {
                toast(getString(R.string.lock_no_exit));
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    private boolean mustStayOnPinScreen() {
        if (wipeInProgress) return true;
        if (vault.getState() == SecureVault.State.WIPED) return true;
        return vault.isFullyLocked() && !sessionUnlocked;
    }

    private void syncLockMode() {
        boolean lock = mustStayOnPinScreen();
        applyLockWindowFlags(lock);
        handler.removeCallbacks(reassertLock);
        if (lock) {
            if (!kioskStarted) {
                antiTheft.enableKioskAndLockTask(this);
                kioskStarted = true;
            }
            if (LockOverlayService.hasOverlayPermission(this)) {
                LockOverlayService.start(this);
            }
            LockWatchdogService.start(this);
            handler.postDelayed(reassertLock, 2500);
        } else {
            kioskStarted = false;
            antiTheft.stopKioskMode(this);
            LockWatchdogService.stop(this);
            LockOverlayService.stop(this);
        }
    }

    private void applyLockWindowFlags(boolean lock) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(lock);
            setTurnScreenOn(lock);
        }
        if (lock) {
            getWindow().addFlags(
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                            | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                            | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                            | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                            | WindowManager.LayoutParams.FLAG_FULLSCREEN);
        } else {
            getWindow().clearFlags(
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                            | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                            | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                            | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                            | WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
    }

    private void bringSelfToFront() {
        try {
            Intent i = new Intent(this, MainActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                    | Intent.FLAG_ACTIVITY_SINGLE_TOP
                    | Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            i.putExtra(EXTRA_FORCE_LOCK, true);
            startActivity(i);
            ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
            if (am != null) {
                am.moveTaskToFront(getTaskId(), ActivityManager.MOVE_TASK_NO_USER_ACTION);
            }
        } catch (Exception ignored) {
        }
    }

    private void bindViews() {
        panelSetup = findViewById(R.id.panel_setup);
        panelUnlock = findViewById(R.id.panel_unlock);
        panelUnlocked = findViewById(R.id.panel_unlocked);
        panelWiped = findViewById(R.id.panel_wiped);
        panelReleased = findViewById(R.id.panel_released);
        panelWipeAnim = findViewById(R.id.panel_wipe_anim);
        headerExtras = findViewById(R.id.header_extras);

        etSetupPin = findViewById(R.id.et_setup_pin);
        etSetupPin2 = findViewById(R.id.et_setup_pin2);
        etSecret = findViewById(R.id.et_secret);
        etUnlockPin = findViewById(R.id.et_unlock_pin);
        etRecovery = findViewById(R.id.et_recovery);
        etNewPin = findViewById(R.id.et_new_pin);

        tvStatus = findViewById(R.id.tv_status);
        tvAttempts = findViewById(R.id.tv_attempts);
        tvUnlockedNote = findViewById(R.id.tv_unlocked_note);
        tvWipeLog = findViewById(R.id.tv_wipe_log);
        tvCreator = findViewById(R.id.tv_creator);
        tvAdminStatus = findViewById(R.id.tv_admin_status);
        tvLockTitle = findViewById(R.id.tv_lock_title);
        tvLockLevel = findViewById(R.id.tv_lock_level);
        wipeBar = findViewById(R.id.wipe_bar);
        cbAntiTheft = findViewById(R.id.cb_antitheft);

        btnActivate = findViewById(R.id.btn_activate);
        btnUnlock = findViewById(R.id.btn_unlock);
        btnRelease = findViewById(R.id.btn_release);
        btnRecover = findViewById(R.id.btn_recover);
        btnRelock = findViewById(R.id.btn_relock);
        btnNewSetup = findViewById(R.id.btn_new_setup);
        btnEnableAdmin = findViewById(R.id.btn_enable_admin);
        btnOverlay = findViewById(R.id.btn_overlay);
        btnHome = findViewById(R.id.btn_home);

        tvCreator.setText(getString(R.string.creator_line, vault.getCreator()));
        if (cbAntiTheft != null) cbAntiTheft.setChecked(true);
    }

    private void wire() {
        btnActivate.setOnClickListener(v -> onActivateClicked());
        btnUnlock.setOnClickListener(v -> onUnlock());
        btnRelease.setOnClickListener(v -> onRelease());
        btnRecover.setOnClickListener(v -> onRecover());
        btnEnableAdmin.setOnClickListener(v -> requestAdmin(false));
        if (btnOverlay != null) {
            btnOverlay.setOnClickListener(v ->
                    startActivity(LockOverlayService.createOverlaySettingsIntent(this)));
        }
        if (btnHome != null) {
            btnHome.setOnClickListener(v -> {
                Intent i = new Intent(Settings.ACTION_HOME_SETTINGS);
                try {
                    startActivity(i);
                } catch (Exception e) {
                    startActivity(new Intent(Settings.ACTION_SETTINGS));
                }
                toast(getString(R.string.set_home_hint));
            });
        }
        if (btnRelock != null) {
            btnRelock.setOnClickListener(v -> {
                sessionUnlocked = false;
                toast(getString(R.string.relocked));
                render();
                syncLockMode();
            });
        }
        if (btnNewSetup != null) {
            btnNewSetup.setOnClickListener(v -> {
                vault.clearForNewSetup();
                sessionUnlocked = false;
                render();
                syncLockMode();
            });
        }
        etUnlockPin.setOnEditorActionListener((v, actionId, event) -> {
            onUnlock();
            return true;
        });
        etUnlockPin.setOnFocusChangeListener((v, hasFocus) -> {
            // Enquanto digita no app, evita reassert agressivo
        });
    }

    private void updateAdminStatus() {
        if (tvAdminStatus != null) {
            if (antiTheft.isDeviceOwner()) {
                tvAdminStatus.setText(R.string.admin_status_owner);
                tvAdminStatus.setTextColor(0xFF4ADE80);
            } else if (antiTheft.isAdminActive()) {
                tvAdminStatus.setText(R.string.admin_status_on);
                tvAdminStatus.setTextColor(0xFF4ADE80);
            } else {
                tvAdminStatus.setText(R.string.admin_status_off);
                tvAdminStatus.setTextColor(0xFFFCA5A5);
            }
        }
        if (tvLockLevel != null) {
            tvLockLevel.setText(antiTheft.getLockStrengthLabel(this)
                    + " · Overlay: "
                    + (LockOverlayService.hasOverlayPermission(this) ? "ON" : "OFF"));
        }
    }

    private void hideAll() {
        panelSetup.setVisibility(android.view.View.GONE);
        panelUnlock.setVisibility(android.view.View.GONE);
        panelUnlocked.setVisibility(android.view.View.GONE);
        panelWiped.setVisibility(android.view.View.GONE);
        panelReleased.setVisibility(android.view.View.GONE);
        panelWipeAnim.setVisibility(android.view.View.GONE);
    }

    private void render() {
        hideAll();
        updateAdminStatus();
        SecureVault.State st = vault.getState();
        boolean pinOnly = mustStayOnPinScreen();

        if (headerExtras != null) {
            headerExtras.setVisibility(pinOnly ? android.view.View.GONE : android.view.View.VISIBLE);
        }
        if (tvLockTitle != null) {
            tvLockTitle.setVisibility(pinOnly ? android.view.View.VISIBLE : android.view.View.GONE);
        }

        if (wipeInProgress) {
            tvStatus.setText(R.string.state_wiping);
            panelWipeAnim.setVisibility(android.view.View.VISIBLE);
            return;
        }

        switch (st) {
            case INACTIVE:
                tvStatus.setText(R.string.state_inactive);
                panelSetup.setVisibility(android.view.View.VISIBLE);
                break;
            case ACTIVE:
                if (!sessionUnlocked) {
                    tvStatus.setText(R.string.state_active_blocker);
                    panelUnlock.setVisibility(android.view.View.VISIBLE);
                    tvAttempts.setText(getString(R.string.attempts_left, vault.remainingAttempts()));
                    if (etUnlockPin != null) etUnlockPin.requestFocus();
                } else {
                    tvStatus.setText(R.string.state_session);
                    panelUnlocked.setVisibility(android.view.View.VISIBLE);
                    tvUnlockedNote.setText(lastPlain == null || lastPlain.isEmpty()
                            ? getString(R.string.empty_vault)
                            : lastPlain);
                }
                break;
            case WIPED:
                tvStatus.setText(R.string.state_wiped);
                panelWiped.setVisibility(android.view.View.VISIBLE);
                break;
            case RELEASED:
                tvStatus.setText(R.string.state_released);
                panelReleased.setVisibility(android.view.View.VISIBLE);
                break;
        }
    }

    private void onActivateClicked() {
        // Exige overlay + admin para modo inviolável
        if (!LockOverlayService.hasOverlayPermission(this)) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.overlay_title)
                    .setMessage(R.string.overlay_required)
                    .setCancelable(false)
                    .setPositiveButton(R.string.overlay_grant, (d, w) ->
                            startActivity(LockOverlayService.createOverlaySettingsIntent(this)))
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
            return;
        }

        boolean wantMax = cbAntiTheft == null || cbAntiTheft.isChecked();
        if (wantMax && !antiTheft.isAdminActive()) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.admin_title)
                    .setMessage(R.string.admin_activate_confirm)
                    .setCancelable(false)
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(R.string.admin_grant, (d, w) -> requestAdmin(true))
                    .show();
            return;
        }
        doActivate();
    }

    private void ensureOverlayThenActivate() {
        if (!LockOverlayService.hasOverlayPermission(this)) {
            toast(getString(R.string.overlay_required_short));
            startActivity(LockOverlayService.createOverlaySettingsIntent(this));
            return;
        }
        doActivate();
    }

    private void requestAdmin(boolean thenActivate) {
        pendingActivateAfterAdmin = thenActivate;
        adminLauncher.launch(antiTheft.createEnableAdminIntent());
    }

    private void doActivate() {
        String p1 = etSetupPin.getText().toString().trim();
        String p2 = etSetupPin2.getText().toString().trim();
        String note = etSecret.getText().toString();

        if (p1.length() < 4) {
            toast(getString(R.string.err_pin_short));
            return;
        }
        if (!p1.equals(p2)) {
            toast(getString(R.string.err_pin_mismatch));
            return;
        }

        boolean maxMode = cbAntiTheft == null || cbAntiTheft.isChecked();
        new AlertDialog.Builder(this)
                .setTitle(R.string.danger_title)
                .setMessage(getString(R.string.danger_confirm)
                        + "\n\n" + getString(R.string.inviolable_setup_note)
                        + (antiTheft.isDeviceOwner() ? "\n\n" + getString(R.string.owner_active_note) : "\n\n" + getString(R.string.owner_adb_note)))
                .setCancelable(false)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.danger_accept, (d, w) -> finishActivate(p1, note, maxMode))
                .show();
    }

    private void finishActivate(String pin, String note, boolean antiTheftWipe) {
        try {
            String recovery = vault.activate(pin, note);
            vault.setAntiTheftWipeEnabled(antiTheftWipe);
            vault.setBlockerActive(true);
            lastPlain = null;
            sessionUnlocked = false;

            new AlertDialog.Builder(this)
                    .setTitle(R.string.recovery_title)
                    .setMessage(getString(R.string.recovery_msg, recovery)
                            + (antiTheftWipe ? "\n\n" + getString(R.string.recovery_antitheft_extra) : ""))
                    .setCancelable(false)
                    .setPositiveButton(R.string.recovery_ok_lock, (d, w) -> {
                        sessionUnlocked = false;
                        // Definir como launcher HOME
                        toast(getString(R.string.set_home_hint));
                        try {
                            startActivity(new Intent(Settings.ACTION_HOME_SETTINGS));
                        } catch (Exception ignored) {
                        }
                        render();
                        syncLockMode();
                        antiTheft.enableKioskAndLockTask(this);
                        LockOverlayService.start(this);
                        if (antiTheft.isAdminActive()) {
                            antiTheft.lockScreen();
                        }
                    })
                    .show();
        } catch (Exception e) {
            toast(e.getMessage() != null ? e.getMessage() : "Erro ao ativar");
        }
    }

    private void onUnlock() {
        if (wipeInProgress) return;
        String pin = etUnlockPin.getText().toString();
        SecureVault.UnlockResult r = vault.unlock(pin);
        etUnlockPin.setText("");

        if (r.ok) {
            lastPlain = r.plain;
            sessionUnlocked = true;
            kioskStarted = false;
            antiTheft.stopKioskMode(this);
            LockWatchdogService.stop(this);
            LockOverlayService.stop(this);
            handler.removeCallbacks(reassertLock);
            applyLockWindowFlags(false);
            toast(r.message);
            render();
            syncLockMode();
            return;
        }

        if (r.wiped) {
            sessionUnlocked = false;
            final boolean doFactory = vault.isAntiTheftWipeEnabled() && antiTheft.isAdminActive();
            wipeInProgress = true;
            render();
            runWipeThenAct(doFactory, () -> {
                wipeInProgress = false;
                if (doFactory) {
                    boolean ok = antiTheft.executeFactoryResetOnly();
                    if (!ok) {
                        antiTheft.lockScreen();
                        toast(getString(R.string.wipe_partial));
                    }
                } else {
                    antiTheft.lockScreen();
                    toast(r.message);
                }
                render();
                syncLockMode();
            });
            return;
        }

        toast(r.message);
        tvAttempts.setText(getString(R.string.attempts_left, vault.remainingAttempts()));
        sessionUnlocked = false;
        syncLockMode();
    }

    private void onRelease() {
        if (!sessionUnlocked) {
            toast(getString(R.string.unlock_first));
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.release_title)
                .setMessage(R.string.release_msg)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.release_confirm, (d, w) -> {
                    antiTheft.stopKioskMode(this);
                    vault.releaseAndDeactivate();
                    antiTheft.removeAdminIfPossible();
                    lastPlain = null;
                    sessionUnlocked = false;
                    LockWatchdogService.stop(this);
                    LockOverlayService.stop(this);
                    handler.removeCallbacks(reassertLock);
                    applyLockWindowFlags(false);
                    toast(getString(R.string.release_ok));
                    render();
                    syncLockMode();
                })
                .show();
    }

    private void onRecover() {
        String code = etRecovery.getText().toString();
        String newPin = etNewPin.getText().toString().trim();
        if (newPin.length() < 4) {
            toast(getString(R.string.err_pin_short));
            return;
        }
        try {
            String newRecovery = vault.recover(code, newPin, lastPlain != null ? lastPlain : "");
            vault.setBlockerActive(true);
            etRecovery.setText("");
            etNewPin.setText("");
            sessionUnlocked = false;
            new AlertDialog.Builder(this)
                    .setTitle(R.string.recovery_title)
                    .setMessage(getString(R.string.recovery_msg, newRecovery))
                    .setCancelable(false)
                    .setPositiveButton(R.string.recovery_ok_lock, (d, w) -> {
                        render();
                        syncLockMode();
                    })
                    .show();
        } catch (Exception e) {
            toast(e.getMessage() != null ? e.getMessage() : "Falha na recuperação");
        }
    }

    private void runWipeThenAct(boolean factoryMode, Runnable done) {
        wipeBar.setProgress(0);
        tvWipeLog.setText("");
        panelWipeAnim.setVisibility(android.view.View.VISIBLE);

        final String[] lines = factoryMode ? new String[]{
                "> 3x PIN inválido",
                "> Wipe criptográfico",
                "> lockNow + factory reset",
                "> JP Secure Lock"
        } : new String[]{
                "> 3x PIN inválido",
                "> Wipe criptográfico",
                "> Use recovery",
                "> JP Secure Lock"
        };

        final int[] i = {0};
        Runnable step = new Runnable() {
            @Override
            public void run() {
                if (i[0] < lines.length) {
                    tvWipeLog.append(lines[i[0]] + "\n");
                    wipeBar.setProgress((int) (((i[0] + 1) / (float) lines.length) * 100));
                    i[0]++;
                    handler.postDelayed(this, 180);
                } else {
                    handler.postDelayed(done, 250);
                }
            }
        };
        handler.post(step);
    }

    private void toast(String m) {
        Toast.makeText(this, m, Toast.LENGTH_SHORT).show();
    }
}
