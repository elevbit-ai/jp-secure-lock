# Changelog

Todas as mudanças relevantes deste projeto são documentadas aqui.
O formato segue [Keep a Changelog](https://keepachangelog.com/pt-BR/1.1.0/)
e o versionamento segue [SemVer](https://semver.org/lang/pt-BR/).

## [2.0.0] — 2026-09-19 — Endurecimento de segurança

### Adicionado
- **`KeystoreEngine`**: chave AES-256 não exportável no Android Keystore (StrongBox quando disponível) que sela a DEK protegida pelo PIN e o hash de recuperação. Remove o oráculo de força bruta offline.
- **`EncryptedSharedPreferences`** (via `androidx.security-crypto`) como armazenamento do cofre, com migração automática do formato legado (`v2`).
- Versionamento de esquema de proteção do cofre (1 = PBKDF2; 2 = PBKDF2 + hardware).
- Validação de PIN: mínimo de 6 dígitos e rejeição de PINs repetidos/sequenciais.
- `FLAG_SECURE` na `MainActivity` e no overlay (bloqueia screenshots/preview).
- Comparação em tempo constante para o código de recuperação.
- Documentação: `SECURITY.md` (modelo de ameaças), `CONTRIBUTING.md`, `AUTHORS`, `NOTICE`, roteiros de vídeo e este changelog.

### Alterado
- PBKDF2 elevado de 120.000 → **210.000** iterações.
- Build de release com **R8/ProGuard** (`minifyEnabled` + `shrinkResources`), ofuscação e remoção de logs.
- Falha do subsistema seguro **não** conta mais como PIN incorreto (evita factory reset acidental).
- Website reformulado (bilíngue), seção de segurança honesta e seção de vídeo explicativo.

### Corrigido
- **Vazamento:** a nota do cofre não é mais gravada em texto puro (`owner_note` removido).
- **Bypass:** o contador de tentativas não pode mais ser zerado editando o arquivo de preferências.

### Segurança
- Ver [SECURITY.md](SECURITY.md) para o modelo de ameaças atualizado e os limites conhecidos.

## [1.4.0] — 2026 — Digitação estável + bloqueio no boot
- PIN digitável sem travar o teclado (watchdog suave).
- Overlay estável (sem recriar a janela em loop).
- Bloqueio persistente após reinício (BOOT + SCREEN_ON).

## [1.3.0] – [1.1.0]
- Camada de bloqueio (overlay + HOME + Device Admin + watchdog), cofre AES-256-GCM, site bilíngue e APK assinado.

[2.0.0]: https://github.com/elevbit-ai/jp-secure-lock/releases/tag/v2.0.0
[1.4.0]: https://github.com/elevbit-ai/jp-secure-lock/releases/tag/v1.4.0
