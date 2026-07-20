# JP Secure Lock

**Bloqueador anti-roubo Android com criptografia E2E**  
**Android anti-theft security blocker with on-device encryption**

Criado por **Joaquim Pedro de Morais Filho**

[![Version](https://img.shields.io/badge/version-1.1.0-yellow)](#)
[![APK](https://img.shields.io/badge/APK-signed%20v2-green)](#download)
[![License](https://img.shields.io/badge/license-MIT-blue)](LICENSE)

---

## Website (documentação + download)

| Idioma | Link |
|--------|------|
| **PT / EN (site)** | https://elevbit-ai.github.io/jp-secure-lock/ |
| **English** | https://elevbit-ai.github.io/jp-secure-lock/en/ |
| **Repositório** | https://github.com/elevbit-ai/jp-secure-lock |
| **Releases** | https://github.com/elevbit-ai/jp-secure-lock/releases |

---

## Download direto do APK

Nome explicativo do arquivo:

- **[JP-Secure-Lock-AntiTheft-v1.1.0.apk](docs/downloads/JP-Secure-Lock-AntiTheft-v1.1.0.apk)**
- [JP-Secure-Lock-assinado.apk](docs/downloads/JP-Secure-Lock-assinado.apk)

```bash
adb install -r JP-Secure-Lock-AntiTheft-v1.1.0.apk
```

| Campo | Valor |
|-------|--------|
| App | JP Secure Lock |
| Package | `br.com.joaquimpedro.jpsecure` |
| Version | 1.1.0 |
| Signature | APK Signature Scheme v2 |
| Author | Joaquim Pedro de Morais Filho |

---

## O que faz

1. Ativa **bloqueador** com PIN do usuário  
2. Cifra cofre local (**AES-256-GCM** + **PBKDF2**)  
3. Com **Device Admin**: após **3 PIN errados** → wipe de chaves + `lockNow` + **factory reset**  
4. Após desbloqueio legítimo → **liberar e desativar**  

### Eficaz contra roubo de dados

O ladrão fica com o aparelho “como novo” — **sem** seus dados.  
Não queima hardware/RAM (isso não é possível de forma legítima).

---

## Avisos

- Use **somente no seu aparelho**, com consentimento.  
- Modo máximo **apaga tudo** em 3 erros — faça backup.  
- Instalar em dispositivo de terceiros sem permissão é **crime**.

---

## Build

```powershell
cd jp-secure-lock
.\sign_and_build.ps1
```

Não commite `keystore.properties` nem a pasta `signing/`.

---

## Estrutura

```
jp-secure-lock/
├── app/                 # Código Android
├── docs/                # Website + downloads
│   ├── index.html       # Landing PT/EN
│   ├── en/              # English page
│   └── downloads/       # APK assinado
├── sign_and_build.ps1
└── README.md
```

---

## Autor

**Joaquim Pedro de Morais Filho**  
JP Secure Lock — security device / anti-theft blocker for Android.

© 2026
