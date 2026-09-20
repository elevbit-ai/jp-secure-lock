# Política de Segurança — JP Secure Lock

Autor / mantenedor: **Joaquim Pedro de Morais Filho**

## Divulgação responsável

Se você encontrar uma vulnerabilidade, **não abra uma issue pública**. Use um dos canais privados:

- **GitHub Security Advisories:** aba *Security → Report a vulnerability* deste repositório.
- Descreva: versão afetada, passos de reprodução, impacto e (se possível) uma prova de conceito.

Compromisso de resposta: confirmação de recebimento em até **5 dias úteis** e um plano de correção em até **30 dias**, conforme a gravidade. Crédito ao pesquisador é dado por padrão, salvo pedido em contrário.

## Versões suportadas

| Versão | Suporte de segurança |
|--------|----------------------|
| 2.0.x  | ✅ Ativa             |
| 1.x    | ⚠️ Apenas correções críticas |

## Modelo de segurança (o que o app faz)

- **Cofre:** AES-256-GCM com IV aleatório por operação. Chave de dados (DEK) aleatória, protegida por uma KEK derivada do PIN via **PBKDF2-HMAC-SHA256 (210.000 iterações)**.
- **Selagem por hardware (esquema 2):** a DEK protegida e o hash de recuperação são adicionalmente cifrados por uma chave AES-256 **não exportável** no Android Keystore, com **StrongBox** quando o aparelho oferece. Isso remove o oráculo de força bruta offline.
- **Armazenamento:** `EncryptedSharedPreferences` (chave-mestra no Keystore). Metadados e contador de tentativas ficam cifrados e autenticados.
- **Anti-roubo:** 3 PINs errados → apagamento criptográfico do cofre; com Device Admin, `lockNow` + `wipeData` (factory reset oficial).
- **Superfície reduzida:** `allowBackup=false`, `usesCleartextTraffic=false`, `FLAG_SECURE`, sem tráfego de rede (100% offline), R8/ofuscação no release.

## Modelo de ameaças (honesto)

### Contra o que protege bem

| Ameaça | Proteção |
|--------|----------|
| Ladrão comum sem PIN | Aparelho inutilizável; após 3 erros, dados apagados. |
| Extração do arquivo de dados (aparelho com root) | Inútil sem a chave do hardware: **sem ataque offline** ao PIN (esquema 2). |
| Reset do contador de tentativas por edição de arquivo | Bloqueado: contador em armazenamento cifrado/autenticado. |
| Captura de tela da tela de PIN | Bloqueada por `FLAG_SECURE`. |
| Timing attack no código de recuperação | Comparação em tempo constante. |

### Limites conhecidos (nenhum software é "inviolável")

- **Sem Device Owner**, o Android **não permite** prender 100% o usuário: o sistema sempre reserva rotas (modo de segurança, recovery/fastboot, reflash). Para o máximo, use [Device Owner](DEVICE_OWNER.md).
- Um atacante com **fastboot/recovery desbloqueado** pode reflashar e apagar o aparelho — mas **não recupera os dados cifrados** (o objetivo anti-roubo, proteger os dados, permanece).
- A força do cofre depende do **PIN escolhido**. O app exige 6+ dígitos e rejeita PINs previsíveis, mas um PIN fraco reduz a segurança.
- A selagem por hardware depende de um Keystore íntegro; em aparelhes sem TEE confiável, o app recai no esquema 1 (apenas PBKDF2), ainda protegido pelo limite de 3 tentativas.
- O código de recuperação é a chave-mestra de reativação: **guarde-o offline**.

### Fora de escopo

- Comprometimento do próprio SO/kernel (rootkit ativo em execução) antes do bloqueio.
- Ataques físicos de laboratório ao silício (extração invasiva de chaves do elemento seguro).
- "Destruir hardware" / "queimar RAM": impossível e não é objetivo do projeto.

## Boas práticas para o usuário

1. Use um PIN de 6+ dígitos, não óbvio.
2. Guarde o código de recuperação **fora do aparelho**.
3. Ative Device Admin (ou Device Owner) para o modo máximo.
4. Faça backup antes de ativar o anti-roubo com factory reset.
5. Baixe o APK apenas das fontes oficiais e verifique o SHA-256 e a assinatura.
