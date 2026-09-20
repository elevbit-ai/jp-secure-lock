# Roteiros dos vídeos explicativos — JP Secure Lock

Autor: **Joaquim Pedro de Morais Filho**

Estes roteiros são prontos para gravação de tela (screen recording) do aparelho.
Sugestão de ferramentas: gravador de tela nativo do Android + narração, ou OBS via
espelhamento (scrcpy). Resolução vertical 1080×1920, 30 fps. Trilha discreta.

---

## Vídeo 1 — Visão geral (60–90 s)

**Objetivo:** explicar o que é e por que é seguro, sem jargão.

| Tempo | Cena | Narração (PT) |
|------:|------|---------------|
| 0–5s | Logo JP Secure Lock sobre fundo escuro | "JP Secure Lock: seu Android inutilizável nas mãos erradas." |
| 5–20s | App aberto, tela de configuração | "Você define um PIN. Ele protege um cofre criptografado com AES-256 — a chave nunca sai do hardware seguro do aparelho." |
| 20–40s | Simulação de tela bloqueada; tentativa de abrir outro app | "Bloqueado, nenhum outro app abre. Home, Voltar e Recentes não liberam." |
| 40–55s | Contador de tentativas 3→2→1 | "Três PINs errados e o protocolo anti-roubo apaga os dados. O ladrão fica com um aparelho zerado." |
| 55–75s | Tela de recuperação | "Perdeu o PIN? Um código de recuperação, guardado offline, reativa a proteção." |
| 75–90s | Autor + site | "Criado por Joaquim Pedro de Morais Filho. Baixe em elevbit-ai.github.io/jp-secure-lock." |

**Legenda EN (para versão internacional):** traduzir cada linha; manter o nome do autor.

---

## Vídeo 2 — Tutorial de configuração passo a passo (2–3 min)

**Objetivo:** levar o usuário do zero à proteção máxima, com avisos.

1. **Instalação** (0:00–0:20)
   - Mostrar `adb install -r` ou instalação do APK. Citar verificação de SHA-256.
2. **Permissão de overlay** (0:20–0:45)
   - Botão "Exibir sobre outros apps" → tela do sistema → ativar. Explicar: "É o que mantém o bloqueio por cima de tudo."
3. **Administrador do dispositivo** (0:45–1:10)
   - Botão "Ativar Administrador" → tela oficial → autorizar. Explicar `lockNow` + factory reset.
4. **App como HOME** (1:10–1:30)
   - Definir JP Secure Lock como tela inicial. "O botão Home volta para o bloqueador."
5. **Definir PIN forte** (1:30–1:55)
   - Digitar PIN de 6+ dígitos. Mostrar rejeição de PIN fraco (ex.: 123456).
6. **Código de recuperação** (1:55–2:20)
   - Tela do código. **Aviso em destaque:** "Anote AGORA, offline. Ele aparece só uma vez."
7. **Aviso final e teste** (2:20–2:50)
   - Reforçar: backup antes; 3 erros apagam tudo; use só no próprio aparelho.
8. **Encerramento** (2:50–3:00)
   - Autor + repositório + site.

---

## Vídeo 3 (opcional) — Modo Device Owner / kiosk (1–2 min)

- Pré-requisito: aparelho sem contas, USB debugging.
- Comando `adb shell dpm set-device-owner ...` (mostrar `DEVICE_OWNER.md`).
- Demonstrar kiosk real: status bar e keyguard controlados.
- Como remover: `dpm remove-active-admin`.
- Aviso: passo avançado; pode exigir reset se feito errado.

---

## Onde publicar e incorporar

1. Suba os vídeos no YouTube (não listados ou públicos).
2. No arquivo `docs/index.html`, substitua o `data-embed` do bloco de vídeo pelo ID:
   ```html
   <div class="video-embed" data-embed="SEU_ID_DO_YOUTUBE"></div>
   ```
   O site carrega a miniatura e só chama o player ao clicar (privacidade + performance).
