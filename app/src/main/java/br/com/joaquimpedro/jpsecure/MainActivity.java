package br.com.joaquimpedro.jpsecure;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

/**
 * JP Secure Lock — bloqueador anti-roubo com criptografia E2E.
 * Criado por Joaquim Pedro de Morais Filho
 */
public class MainActivity extends AppCompatActivity {

    private SecureVault vault;
    private AntiTheftController antiTheft;

    private LinearLayout panelSetup, panelUnlock, panelUnlocked, panelWiped, panelReleased, panelWipeAnim;
    private EditText etSetupPin, etSetupPin2, etSecret, etUnlockPin, etRecovery, etNewPin;
    private TextView tvStatus, tvAttempts, tvUnlockedNote, tvWipeLog, tvCreator, tvRecoveryOnce, tvAdminStatus;
    private ProgressBar wipeBar;
    private CheckBox cbAntiTheft;
    private Button btnActivate, btnUnlock, btnRelease, btnRecover, btnResetDemo, btnForceWipeDemo, btnEnableAdmin;

    private String lastPlain;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean pendingActivateAfterAdmin = false;

    private final ActivityResultLauncher<Intent> adminLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                updateAdminStatus();
                if (antiTheft.isAdminActive() && pendingActivateAfterAdmin) {
                    pendingActivateAfterAdmin = false;
                    doActivate();
                } else if (pendingActivateAfterAdmin) {
                    pendingActivateAfterAdmin = false;
                    toast(getString(R.string.admin_required_for_max));
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        vault = new SecureVault(this);
        antiTheft = new AntiTheftController(this);
        bindViews();
        wire();
        render();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateAdminStatus();
    }

    private void bindViews() {
        panelSetup = findViewById(R.id.panel_setup);
        panelUnlock = findViewById(R.id.panel_unlock);
        panelUnlocked = findViewById(R.id.panel_unlocked);
        panelWiped = findViewById(R.id.panel_wiped);
        panelReleased = findViewById(R.id.panel_released);
        panelWipeAnim = findViewById(R.id.panel_wipe_anim);

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
        tvRecoveryOnce = findViewById(R.id.tv_recovery_once);
        tvAdminStatus = findViewById(R.id.tv_admin_status);
        wipeBar = findViewById(R.id.wipe_bar);
        cbAntiTheft = findViewById(R.id.cb_antitheft);

        btnActivate = findViewById(R.id.btn_activate);
        btnUnlock = findViewById(R.id.btn_unlock);
        btnRelease = findViewById(R.id.btn_release);
        btnRecover = findViewById(R.id.btn_recover);
        btnResetDemo = findViewById(R.id.btn_reset_demo);
        btnForceWipeDemo = findViewById(R.id.btn_force_fail);
        btnEnableAdmin = findViewById(R.id.btn_enable_admin);

        tvWipeLog.setMovementMethod(new ScrollingMovementMethod());
        tvCreator.setText(getString(R.string.creator_line, vault.getCreator()));
        if (cbAntiTheft != null) {
            cbAntiTheft.setChecked(true);
        }
        updateAdminStatus();
    }

    private void wire() {
        btnActivate.setOnClickListener(v -> onActivateClicked());
        btnUnlock.setOnClickListener(v -> onUnlock());
        btnRelease.setOnClickListener(v -> onRelease());
        btnRecover.setOnClickListener(v -> onRecover());
        btnEnableAdmin.setOnClickListener(v -> requestAdmin(false));
        btnResetDemo.setOnClickListener(v -> {
            antiTheft.removeAdminIfPossible();
            vault.hardResetDemo();
            lastPlain = null;
            toast(getString(R.string.reset_ok));
            render();
        });
        btnForceWipeDemo.setOnClickListener(v -> {
            etUnlockPin.setText("__wrong__");
            onUnlock();
        });
    }

    private void updateAdminStatus() {
        if (tvAdminStatus == null) return;
        if (antiTheft.isAdminActive()) {
            tvAdminStatus.setText(R.string.admin_status_on);
            tvAdminStatus.setTextColor(0xFF4ADE80);
        } else {
            tvAdminStatus.setText(R.string.admin_status_off);
            tvAdminStatus.setTextColor(0xFFFCA5A5);
        }
    }

    private void hideAll() {
        panelSetup.setVisibility(View.GONE);
        panelUnlock.setVisibility(View.GONE);
        panelUnlocked.setVisibility(View.GONE);
        panelWiped.setVisibility(View.GONE);
        panelReleased.setVisibility(View.GONE);
        panelWipeAnim.setVisibility(View.GONE);
    }

    private void render() {
        hideAll();
        updateAdminStatus();
        SecureVault.State st = vault.getState();
        switch (st) {
            case INACTIVE:
                tvStatus.setText(R.string.state_inactive);
                panelSetup.setVisibility(View.VISIBLE);
                break;
            case ACTIVE:
                tvStatus.setText(vault.isBlockerActive()
                        ? R.string.state_active_blocker
                        : R.string.state_active);
                panelUnlock.setVisibility(View.VISIBLE);
                tvAttempts.setText(getString(R.string.attempts_left, vault.remainingAttempts()));
                break;
            case WIPED:
                tvStatus.setText(R.string.state_wiped);
                panelWiped.setVisibility(View.VISIBLE);
                break;
            case RELEASED:
                tvStatus.setText(R.string.state_released);
                panelReleased.setVisibility(View.VISIBLE);
                break;
        }
    }

    private void onActivateClicked() {
        boolean wantMax = cbAntiTheft == null || cbAntiTheft.isChecked();
        if (wantMax && !antiTheft.isAdminActive()) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.admin_title)
                    .setMessage(R.string.admin_activate_confirm)
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(R.string.admin_grant, (d, w) -> requestAdmin(true))
                    .show();
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
        if (maxMode) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.danger_title)
                    .setMessage(R.string.danger_confirm)
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(R.string.danger_accept, (d, w) -> finishActivate(p1, note, true))
                    .show();
        } else {
            finishActivate(p1, note, false);
        }
    }

    private void finishActivate(String pin, String note, boolean antiTheftWipe) {
        try {
            String recovery = vault.activate(pin, note);
            vault.setAntiTheftWipeEnabled(antiTheftWipe);
            vault.setBlockerActive(true);
            lastPlain = null;
            new AlertDialog.Builder(this)
                    .setTitle(R.string.recovery_title)
                    .setMessage(getString(R.string.recovery_msg, recovery)
                            + (antiTheftWipe ? "\n\n" + getString(R.string.recovery_antitheft_extra) : ""))
                    .setCancelable(false)
                    .setPositiveButton(android.R.string.ok, (d, w) -> {
                        tvRecoveryOnce.setText(getString(R.string.recovery_saved_hint));
                        // Trava a tela ao ativar o bloqueador (anti-roubo imediato)
                        if (antiTheft.isAdminActive()) {
                            antiTheft.lockScreen();
                        }
                        render();
                    })
                    .show();
        } catch (Exception e) {
            toast(e.getMessage() != null ? e.getMessage() : "Erro ao ativar");
        }
    }

    private void onUnlock() {
        String pin = etUnlockPin.getText().toString();
        SecureVault.UnlockResult r = vault.unlock(pin);
        etUnlockPin.setText("");
        if (r.ok) {
            lastPlain = r.plain;
            hideAll();
            panelUnlocked.setVisibility(View.VISIBLE);
            tvStatus.setText(R.string.state_session);
            tvUnlockedNote.setText(r.plain == null || r.plain.isEmpty()
                    ? getString(R.string.empty_vault)
                    : r.plain);
            toast(r.message);
            return;
        }
        if (r.wiped) {
            // 3 erros: resposta anti-roubo
            final boolean doFactory = vault.isAntiTheftWipeEnabled() && antiTheft.isAdminActive();
            runWipeAnimation(doFactory, () -> {
                if (doFactory) {
                    // Factory reset oficial — apaga TODOS os dados do aparelho
                    boolean wiped = antiTheft.executeHighImpactResponse(vault);
                    if (!wiped) {
                        // fallback: já wipe do cofre; trava tela se possível
                        vault.triggerCryptographicWipe();
                        antiTheft.lockScreen();
                        toast(getString(R.string.wipe_partial));
                        render();
                    }
                    // se wipeData ok, o processo encerra no reset
                } else {
                    vault.triggerCryptographicWipe();
                    CryptoEngine.memorySurgeWipe(new byte[128]);
                    antiTheft.lockScreen();
                    toast(r.message + " " + getString(R.string.wipe_app_only));
                    render();
                }
            });
            return;
        }
        toast(r.message);
        tvAttempts.setText(getString(R.string.attempts_left, vault.remainingAttempts()));
        // Em falha parcial, só atualiza contador (não reseta ainda)
    }

    private void onRelease() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.release_title)
                .setMessage(R.string.release_msg)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.release_confirm, (d, w) -> {
                    vault.releaseAndDeactivate();
                    antiTheft.removeAdminIfPossible();
                    lastPlain = null;
                    toast(getString(R.string.release_ok));
                    render();
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
            String newRecovery = vault.recover(code, newPin, lastPlain != null ? lastPlain : "Recuperado");
            vault.setBlockerActive(true);
            etRecovery.setText("");
            etNewPin.setText("");
            new AlertDialog.Builder(this)
                    .setTitle(R.string.recovery_title)
                    .setMessage(getString(R.string.recovery_msg, newRecovery))
                    .setPositiveButton(android.R.string.ok, (d, w) -> render())
                    .show();
        } catch (Exception e) {
            toast(e.getMessage() != null ? e.getMessage() : "Falha na recuperação");
        }
    }

    private void runWipeAnimation(boolean factoryMode, Runnable done) {
        hideAll();
        panelWipeAnim.setVisibility(View.VISIBLE);
        tvStatus.setText(R.string.state_wiping);
        wipeBar.setProgress(0);
        tvWipeLog.setText("");

        final String[] lines = factoryMode ? new String[]{
                "> AUTH_FAIL x3 — AMEAÇA DETECTADA",
                "> JP Secure Lock · BLOQUEADOR ANTI-ROUBO",
                "> Destruindo chaves AES-256-GCM (cofre)",
                "> Limpeza segura de buffers do processo",
                "> Device Admin: lockNow()",
                "> Device Admin: wipeData() FACTORY RESET",
                "> Todos os dados do aparelho serão apagados",
                "> Hardware intacto · dados irrecuperáveis sem backup",
                "> Autor: Joaquim Pedro de Morais Filho"
        } : new String[]{
                "> AUTH_FAIL x3",
                "> Wipe criptográfico do cofre do app",
                "> Device Admin ausente — sem factory reset",
                "> Ative o Admin na próxima ativação para modo máximo",
                "> Autor: Joaquim Pedro de Morais Filho"
        };

        final int[] i = {0};
        Runnable step = new Runnable() {
            @Override
            public void run() {
                if (i[0] < lines.length) {
                    tvWipeLog.append(lines[i[0]] + "\n");
                    wipeBar.setProgress((int) (((i[0] + 1) / (float) lines.length) * 100));
                    i[0]++;
                    handler.postDelayed(this, 320);
                } else {
                    handler.postDelayed(done, 500);
                }
            }
        };
        handler.post(step);
    }

    private void toast(String m) {
        Toast.makeText(this, m, Toast.LENGTH_LONG).show();
    }
}
