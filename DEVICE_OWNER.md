# JP Secure Lock — Modo inviolável (Device Owner)

Para o bloqueio **máximo** (nenhum app abre sem o PIN):

## Requisitos
- Aparelho **sem contas** Google no momento do comando (ou recém-formatado)
- USB debugging (ADB)
- App instalado

## Comando

```bash
adb install -r JP-Secure-Lock-AntiTheft-v1.3.0.apk
adb shell dpm set-device-owner br.com.joaquimpedro.jpsecure/.JpDeviceAdminReceiver
```

## O que o Device Owner libera
- Lock Task (kiosk) real
- Desativar status bar
- Desativar keyguard do sistema (opcional)
- `wipeData` / factory reset em 3 erros

## Remover Device Owner

```bash
adb shell dpm remove-active-admin br.com.joaquimpedro.jpsecure/.JpDeviceAdminReceiver
```

Ou desative a proteção no app após digitar o PIN e libere o aparelho.

## Sem Device Owner
Ainda assim o app usa:
1. Overlay fullscreen (sobre todos os apps)
2. Launcher HOME
3. Watchdog em primeiro plano
4. Device Admin (lockNow + wipe)

Isso cobre a maior parte dos usos, mas o Android **não permite** 100% de prisão do usuário sem Device Owner (proteção do sistema).
