/* JP Secure Lock — i18n + video facade · Joaquim Pedro de Morais Filho */
const I18N = {
  pt: {
    nav_video: 'Vídeo', nav_how: 'Como funciona', nav_sec: 'Segurança', nav_dl: 'Download', nav_gh: 'GitHub',
    kicker: 'Android · Anti-Roubo · Cripto selada por hardware',
    hero_title: 'Seu Android, inutilizável nas mãos erradas',
    hero_lead: 'JP Secure Lock protege o aparelho com PIN, criptografia AES-256-GCM cujas chaves nunca saem do hardware seguro, e uma resposta anti-roubo: após 3 senhas erradas, apaga o cofre e executa o factory reset oficial do Android.',
    cta_dl: 'Baixar / Instalar',
    hero_meta: 'Criado por Joaquim Pedro de Morais Filho · br.com.joaquimpedro.jpsecure · Código aberto (MIT)',
    t1: '🔐 AES-256-GCM', t2: '🧱 Android Keystore / StrongBox', t3: '📵 100% offline', t4: '🛡️ Device Admin oficial',
    video_title: 'Vídeos explicativos',
    video_sub: 'Assista à visão geral e ao tutorial de configuração. O player só carrega ao clicar (privacidade e performance).',
    video_1: '▶ Vídeo explicativo — animado, com narração em português',
    video_2: 'Tutorial em vídeo — gravação de tela (em breve)',
    video_note: 'O vídeo explicativo roda no navegador e pode ser gravado em tela para virar um MP4. Roteiros em docs/VIDEO_SCRIPTS.md.',
    feat_title: 'Camadas de proteção',
    c1_t: 'Cofre criptográfico', c1_b: 'AES-256-GCM + PBKDF2 (210k). A nota do dono é cifrada; nada em texto puro.',
    c2_t: 'Chaves no hardware', c2_b: 'Selo por chave não exportável do Android Keystore. Extrair o arquivo não quebra o PIN offline.',
    c3_t: 'Resposta anti-roubo', c3_b: '3 PIN errados → wipe do cofre + lockNow + factory reset (Device Admin oficial).',
    c4_t: 'Liberação e recuperação', c4_b: 'Libere com o PIN e desative a proteção. Código de recuperação offline para reativar.',
    how_title: 'Como funciona',
    s1: 'Instale o APK e abra o JP Secure Lock.',
    s2: 'Permita exibir sobre outros apps e ative o Administrador do dispositivo.',
    s3: 'Defina o app como HOME para o botão Início voltar ao bloqueador.',
    s4: 'Crie um PIN forte (6+ dígitos) e guarde o código de recuperação offline.',
    s5: 'Em tentativa de roubo: 3 erros apagam todos os dados do aparelho.',
    s6: 'Após uso legítimo: desbloqueie e libere/desative a proteção.',
    sec_title: 'Segurança, com honestidade',
    sec_sub: 'Nenhum software é "impossível de hackear". Veja exatamente o que este protege — e os limites conhecidos.',
    th_item: 'Cenário', th_real: 'Realidade',
    r1a: 'Ladrão comum sem o PIN', r1b: 'Aparelho inutilizável; dados apagados após 3 erros',
    r2a: 'Extração do arquivo em aparelho com root', r2b: 'Sem ataque offline: chave presa ao hardware',
    r3a: 'Zerar o contador editando o arquivo', r3b: 'Bloqueado: armazenamento cifrado e autenticado',
    r4a: 'Reflash por fastboot/recovery', r4b: 'Pode apagar o aparelho — mas não recupera os dados cifrados',
    r5a: '"Queimar" RAM / destruir hardware', r5b: 'Impossível e não é objetivo do projeto',
    warn: 'Atenção: no modo máximo, 3 PIN errados apagam fotos, apps e contas. Faça backup. Use apenas no seu aparelho ou com consentimento explícito do dono.',
    sec_more: 'Modelo de ameaças completo (SECURITY.md) →',
    dl_title: 'Download e build', dl_lead: 'APK assinado disponível, ou compile a versão 2.0.0 a partir do código.',
    dl_sub: 'Build assinada 2.0.0 · APK Signature v2 · ~10 MB', cta_dl2: 'Baixar APK',
    dl_rel: 'Releases',
    dl_code: 'adb install -r JP-Secure-Lock-AntiTheft-v2.0.0.apk\n# SHA-256: c75f3f74bfc22d91f2a0fe96d16e53f24ad5f16f7aeafcc1eb3182a5d173185f\napksigner verify --print-certs JP-Secure-Lock-AntiTheft-v2.0.0.apk',
    dl_build: '# Compilar a versão 2.0.0 (endurecida) do código:\ngit clone https://github.com/elevbit-ai/jp-secure-lock\ncd jp-secure-lock && ./gradlew assembleRelease',
    gh_title: 'No GitHub',
    repo1: 'Código-fonte Android · documentação · APK · README',
    repo2: 'Modelo de ameaças e divulgação responsável',
    repo3: 'Versões publicadas e assets de download',
    author_k: 'Autor',
    author_b: 'Criador e mantenedor do JP Secure Lock — bloqueador anti-roubo para Android com criptografia selada por hardware e liberação controlada.',
  },
  en: {
    nav_video: 'Video', nav_how: 'How it works', nav_sec: 'Security', nav_dl: 'Download', nav_gh: 'GitHub',
    kicker: 'Android · Anti-Theft · Hardware-sealed crypto',
    hero_title: 'Your Android, useless in the wrong hands',
    hero_lead: 'JP Secure Lock protects the device with a PIN, AES-256-GCM encryption whose keys never leave the secure hardware, and an anti-theft response: after 3 wrong passwords it wipes the vault and runs the official Android factory reset.',
    cta_dl: 'Download / Install',
    hero_meta: 'Created by Joaquim Pedro de Morais Filho · br.com.joaquimpedro.jpsecure · Open source (MIT)',
    t1: '🔐 AES-256-GCM', t2: '🧱 Android Keystore / StrongBox', t3: '📵 100% offline', t4: '🛡️ Official Device Admin',
    video_title: 'Explainer videos',
    video_sub: 'Watch the overview and the setup tutorial. The player loads only on click (privacy and performance).',
    video_1: '▶ Explainer video — animated, narrated in Portuguese',
    video_2: 'Video tutorial — screen recording (coming soon)',
    video_note: 'The explainer video runs in the browser and can be screen-recorded into an MP4. Scripts in docs/VIDEO_SCRIPTS.md.',
    feat_title: 'Protection layers',
    c1_t: 'Cryptographic vault', c1_b: 'AES-256-GCM + PBKDF2 (210k). The owner note is encrypted; nothing in plaintext.',
    c2_t: 'Keys in hardware', c2_b: 'Sealed by a non-exportable Android Keystore key. Extracting the file does not break the PIN offline.',
    c3_t: 'Anti-theft response', c3_b: '3 wrong PINs → vault wipe + lockNow + factory reset (official Device Admin).',
    c4_t: 'Release and recovery', c4_b: 'Unlock with the PIN and deactivate protection. Offline recovery code to reactivate.',
    how_title: 'How it works',
    s1: 'Install the APK and open JP Secure Lock.',
    s2: 'Allow drawing over other apps and enable Device Administrator.',
    s3: 'Set the app as HOME so the Home button returns to the blocker.',
    s4: 'Create a strong PIN (6+ digits) and store the recovery code offline.',
    s5: 'On a theft attempt: 3 failures erase all device data.',
    s6: 'After legitimate use: unlock and release/deactivate protection.',
    sec_title: 'Security, honestly',
    sec_sub: 'No software is "impossible to hack". See exactly what this protects — and the known limits.',
    th_item: 'Scenario', th_real: 'Reality',
    r1a: 'Common thief without the PIN', r1b: 'Device unusable; data erased after 3 failures',
    r2a: 'File extraction on a rooted device', r2b: 'No offline attack: key bound to hardware',
    r3a: 'Reset the counter by editing the file', r3b: 'Blocked: encrypted and authenticated storage',
    r4a: 'Reflash via fastboot/recovery', r4b: 'May erase the device — but cannot recover the encrypted data',
    r5a: '"Burn" RAM / destroy hardware', r5b: 'Impossible and not a project goal',
    warn: 'Warning: in maximum mode, 3 wrong PINs erase photos, apps, and accounts. Back up first. Use only on your own device or with the owner’s explicit consent.',
    sec_more: 'Full threat model (SECURITY.md) →',
    dl_title: 'Download and build', dl_lead: 'Signed APK available, or build version 2.0.0 from source.',
    dl_sub: 'Signed build 2.0.0 · APK Signature v2 · ~10 MB', cta_dl2: 'Download APK',
    dl_rel: 'Releases',
    dl_code: 'adb install -r JP-Secure-Lock-AntiTheft-v2.0.0.apk\n# SHA-256: c75f3f74bfc22d91f2a0fe96d16e53f24ad5f16f7aeafcc1eb3182a5d173185f\napksigner verify --print-certs JP-Secure-Lock-AntiTheft-v2.0.0.apk',
    dl_build: '# Build the hardened 2.0.0 from source:\ngit clone https://github.com/elevbit-ai/jp-secure-lock\ncd jp-secure-lock && ./gradlew assembleRelease',
    gh_title: 'On GitHub',
    repo1: 'Android source · documentation · APK · README',
    repo2: 'Threat model and responsible disclosure',
    repo3: 'Published versions and download assets',
    author_k: 'Author',
    author_b: 'Creator and maintainer of JP Secure Lock — an Android anti-theft blocker with hardware-sealed encryption and controlled release.',
  },
};

function setLang(lang) {
  const dict = I18N[lang] || I18N.pt;
  document.documentElement.lang = lang === 'en' ? 'en' : 'pt-BR';
  document.querySelectorAll('[data-i18n]').forEach((el) => {
    const k = el.getAttribute('data-i18n');
    if (dict[k] != null) el.textContent = dict[k];
  });
  const pt = document.getElementById('btn-pt');
  const en = document.getElementById('btn-en');
  if (pt && en) {
    pt.classList.toggle('on', lang === 'pt');
    en.classList.toggle('on', lang === 'en');
  }
  try { localStorage.setItem('jp_secure_lang', lang); } catch (e) {}
}

// Video facade: only loads the YouTube player when the user clicks/keys.
function activateVideo(el) {
  const id = el.getAttribute('data-embed');
  if (!id) return; // no video assigned yet
  const iframe = document.createElement('iframe');
  iframe.src = 'https://www.youtube-nocookie.com/embed/' + encodeURIComponent(id) + '?autoplay=1&rel=0';
  iframe.title = 'JP Secure Lock';
  iframe.allow = 'accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture';
  iframe.allowFullscreen = true;
  el.innerHTML = '';
  el.appendChild(iframe);
}

document.querySelectorAll('.video-embed').forEach((el) => {
  el.addEventListener('click', () => activateVideo(el));
  el.addEventListener('keydown', (e) => {
    if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); activateVideo(el); }
  });
});

let saved = null;
try { saved = localStorage.getItem('jp_secure_lang'); } catch (e) {}
const start = saved || ((navigator.language || '').toLowerCase().startsWith('en') ? 'en' : 'pt');
setLang(start);
