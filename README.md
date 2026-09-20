<div align="center">

# JP Secure Lock

**Bloqueador anti-roubo para Android com criptografia ponta-a-ponta no aparelho.**

Criado por **Joaquim Pedro de Morais Filho**

[![Plataforma](https://img.shields.io/badge/Android-8.0%2B%20(API%2026)-3DDC84?logo=android&logoColor=white)](#requisitos)
[![Versão](https://img.shields.io/badge/versão-2.0.0-eab308)](CHANGELOG.md)
[![Licença](https://img.shields.io/badge/licença-MIT-blue)](LICENSE)
[![Criptografia](https://img.shields.io/badge/cripto-AES--256--GCM%20%2B%20Keystore-8b5cf6)](SECURITY.md)

[Website](https://elevbit-ai.github.io/jp-secure-lock/) ·
[Segurança](SECURITY.md) ·
[Modo Device Owner](DEVICE_OWNER.md) ·
[Changelog](CHANGELOG.md)

</div>

---

## O que é

JP Secure Lock transforma um aparelho Android em um dispositivo que **só é utilizável com o PIN correto**. Ele combina três camadas independentes:

1. **Cofre criptográfico** — a nota/identificador do dono é cifrada com AES-256-GCM; a chave é derivada do PIN (PBKDF2-HMAC-SHA256, 210.000 iterações) e **selada pelo hardware seguro** do aparelho (Android Keystore / StrongBox).
2. **Camada de bloqueio** — overlay em tela cheia + app como HOME + modo kiosk (Lock Task) + watchdog em primeiro plano. Sem o PIN, nenhum outro app fica acessível.
3. **Resposta anti-roubo** — após **3 PINs incorretos**, executa apagamento criptográfico do cofre e, se o Device Admin estiver ativo, `lockNow` + factory reset oficial do Android.

> **Honestidade primeiro:** nenhum software é "impossível de hackear". Este projeto **eleva a barreira ao nível do que apps de segurança sérios usam** e documenta com clareza o que protege e o que não protege. Veja o [modelo de ameaças](SECURITY.md).

## Destaques da versão 2.0.0 (endurecimento de segurança)

- 🔐 **Chaves ligadas ao hardware.** A DEK protegida pelo PIN e o hash de recuperação são selados por uma chave AES-256 **não exportável** no Android Keystore (StrongBox quando disponível). Extrair o arquivo de um aparelho com root **não permite mais ataque offline** ao PIN.
- 🧱 **Armazenamento cifrado e à prova de adulteração.** As preferências passam a usar `EncryptedSharedPreferences`. O contador de tentativas não pode mais ser zerado editando o arquivo — o bypass clássico do wipe foi fechado.
- 🧼 **Fim do vazamento em texto puro.** A nota do cofre era gravada também sem cifra; agora é armazenada **apenas cifrada**.
- ⏱️ **Comparação em tempo constante** no código de recuperação (mitiga timing attacks).
- 🔢 **PIN mais forte por padrão:** mínimo de 6 dígitos e rejeição de PINs previsíveis (repetidos/sequenciais).
- 📵 **`FLAG_SECURE`** na tela de PIN e no overlay (bloqueia screenshots e preview em recentes).
- 🛡️ **Falha do subsistema seguro não conta como PIN errado** — evita factory reset acidental por erro de plataforma.
- 📦 **Build de produção com R8/ProGuard**, remoção de logs e ofuscação.

Detalhes completos em [CHANGELOG.md](CHANGELOG.md).

## Arquitetura

```
MainActivity ─┬─ SecureVault ─┬─ CryptoEngine (AES-256-GCM, PBKDF2, wipe seguro)
              │               └─ KeystoreEngine (chave não exportável do hardware)
              ├─ AntiTheftController (Device Admin / Device Owner / kiosk)
              ├─ LockOverlayService  (overlay de PIN em tela cheia)
              ├─ LockWatchdogService (reabre o bloqueio se o usuário sair)
              └─ BootReceiver        (restaura o bloqueio no boot / tela ligada)
```

| Estado | Significado |
|---|---|
| `INACTIVE` | Sem proteção; configure o PIN. |
| `ACTIVE`   | Bloqueado; exige PIN (3 erros disparam o anti-roubo). |
| `WIPED`    | Cofre apagado; recuperável pelo código offline. |
| `RELEASED` | Proteção desativada após liberação legítima. |

## Requisitos

- Android 8.0+ (API 26).
- Para o modo máximo: **Device Admin** (concedido pelo usuário) ou **Device Owner** (via ADB em aparelho recém-configurado, sem contas).

## Instalação

**APK assinado v2.0.0** — página de [Releases](https://github.com/elevbit-ai/jp-secure-lock/releases) ou pasta [`docs/downloads`](docs/downloads). Assinado por *CN=Joaquim Pedro de Morais Filho*.

```bash
adb install -r JP-Secure-Lock-AntiTheft-v2.0.0.apk
# SHA-256: c75f3f74bfc22d91f2a0fe96d16e53f24ad5f16f7aeafcc1eb3182a5d173185f
```

**Compilar a versão 2.0.0 a partir do código:**

```bash
git clone https://github.com/elevbit-ai/jp-secure-lock
cd jp-secure-lock
# crie keystore.properties (storeFile, storePassword, keyAlias, keyPassword)
./gradlew assembleRelease
# APK em app/build/outputs/apk/release/
```

Verifique a integridade do APK antes de instalar:

```bash
sha256sum JP-Secure-Lock-AntiTheft-v2.0.0.apk
apksigner verify --print-certs JP-Secure-Lock-AntiTheft-v2.0.0.apk
```

## Configuração (camada máxima)

1. **Exibir sobre outros apps** (overlay) — obrigatório.
2. **Administrador do dispositivo** — habilita `lockNow` + factory reset.
3. **App como HOME** — o botão Home volta ao bloqueador.
4. **Definir PIN** e guardar o **código de recuperação** (mostrado uma única vez, offline).

Para o bloqueio total (kiosk real), use **Device Owner**: veja [DEVICE_OWNER.md](DEVICE_OWNER.md).

## Uso responsável e legal

⚠️ **No modo máximo, 3 PINs errados apagam TODOS os dados do aparelho.** Faça backup e teste com cuidado.

Use **apenas no seu próprio aparelho** ou em dispositivos que você administra com **consentimento explícito** do dono. Instalar bloqueio/apagamento em aparelho de terceiros sem autorização é crime. Este software não danifica hardware nem "queima" RAM — isso é impossível e não é o objetivo.

## Segurança e divulgação responsável

Encontrou uma vulnerabilidade? Não abra issue pública. Veja [SECURITY.md](SECURITY.md) para o canal de divulgação responsável e o modelo de ameaças completo.

## Licença

[MIT](LICENSE) © 2026 **Joaquim Pedro de Morais Filho**. Veja também [AUTHORS](AUTHORS) e [NOTICE](NOTICE).
