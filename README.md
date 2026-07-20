# JP Secure Lock

**Bloqueador anti-roubo Android com criptografia E2E**  
Criado por **Joaquim Pedro de Morais Filho**

## Website publicado (online agora)

| Página | Link |
|--------|------|
| **Site PT/EN (live)** | https://cdn.jsdelivr.net/gh/elevbit-ai/jp-secure-lock@gh-pages/index.html |
| **English** | https://cdn.jsdelivr.net/gh/elevbit-ai/jp-secure-lock@gh-pages/en/index.html |
| **GitHub Pages** (pode demorar / instável na API) | https://elevbit-ai.github.io/jp-secure-lock/ |
| **Repositório** | https://github.com/elevbit-ai/jp-secure-lock |
| **Release** | https://github.com/elevbit-ai/jp-secure-lock/releases/tag/v1.1.0 |

## Download direto do APK

**Nome explicativo:**

- https://github.com/elevbit-ai/jp-secure-lock/raw/main/docs/downloads/JP-Secure-Lock-AntiTheft-v1.1.0.apk
- https://github.com/elevbit-ai/jp-secure-lock/raw/main/docs/downloads/JP-Secure-Lock-assinado.apk

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

## O que faz

1. Ativa **bloqueador** com PIN  
2. Cifra cofre (**AES-256-GCM** + **PBKDF2**)  
3. Com **Device Admin**: 3 PIN errados → wipe + factory reset  
4. Após desbloqueio → **liberar e desativar**

## Avisos

- Use só no **seu** aparelho, com backup  
- Modo máximo apaga tudo em 3 erros  
- Não danifica hardware/RAM  

## Build

```powershell
.\sign_and_build.ps1
```

## Autor

**Joaquim Pedro de Morais Filho** — JP Secure Lock  
© 2026 · MIT
