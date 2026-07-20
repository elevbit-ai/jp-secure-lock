# JP Secure Lock

**Bloqueador anti-roubo Android** · v1.4.0  
Criado por **Joaquim Pedro de Morais Filho**

## Website

- https://cdn.jsdelivr.net/gh/elevbit-ai/jp-secure-lock@gh-pages/index.html
- https://github.com/elevbit-ai/jp-secure-lock

## Download APK v1.4.0

- [JP-Secure-Lock-AntiTheft-v1.4.0.apk](docs/downloads/JP-Secure-Lock-AntiTheft-v1.4.0.apk)
- Raw: https://github.com/elevbit-ai/jp-secure-lock/raw/main/docs/downloads/JP-Secure-Lock-AntiTheft-v1.4.0.apk

`adb install -r JP-Secure-Lock-AntiTheft-v1.4.0.apk`

## v1.4.0 — digitação estável + bloqueio no boot

- PIN digitável sem travar teclado (watchdog suave)
- Overlay não recria a janela em loop
- Ligar/desligar o aparelho mantém o bloqueio (BOOT + SCREEN_ON)
- Overlay + HOME + Device Admin + watchdog

## Setup inviolável

1. Exibir sobre outros apps  
2. Administrador do dispositivo  
3. App como HOME  
4. Ativar PIN  

Device Owner (máximo): ver DEVICE_OWNER.md

© Joaquim Pedro de Morais Filho · MIT
