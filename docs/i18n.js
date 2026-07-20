const I18N = {
  pt: {
    nav_how: 'Como funciona', nav_sec: 'Segurança', nav_dl: 'Download', nav_gh: 'GitHub',
    kicker: 'Android · Anti-Theft · E2E Crypto',
    hero_title: 'Bloqueador de segurança para liberação e proteção de aparelho',
    hero_lead: 'JP Secure Lock protege o dispositivo com PIN, criptografia AES-256-GCM e resposta anti-roubo: após 3 senhas erradas, executa wipe criptográfico e factory reset oficial do Android.',
    cta_dl: 'Baixar APK assinado v1.1.0',
    hero_meta: 'Criado por Joaquim Pedro de Morais Filho · Pacote br.com.joaquimpedro.jpsecure · Assinado (APK Signature v2)',
    c1_t: 'Criptografia E2E no aparelho', c1_b: 'AES-256-GCM + PBKDF2-HMAC-SHA256. Chaves derivadas do PIN; cofre local sem servidor.',
    c2_t: 'Bloqueador anti-roubo', c2_b: '3 PIN errados → limpeza de chaves, lockNow e factory reset (Device Admin oficial).',
    c3_t: 'Desativar após liberar', c3_b: 'Com PIN correto, libere o aparelho e desative a proteção + remova o administrador.',
    c4_t: 'Recuperação', c4_b: 'Recovery code gerado na ativação. Use se o wipe do cofre ocorreu sem factory reset.',
    how_title: 'Como funciona',
    s1: 'Instale o APK e abra o JP Secure Lock.',
    s2: 'Ative o Administrador do dispositivo (tela oficial do Android).',
    s3: 'Defina o PIN e marque o modo anti-roubo máximo.',
    s4: 'Guarde o recovery code offline (exibido só uma vez).',
    s5: 'Em roubo/tentativa: 3 erros = apaga todos os dados do aparelho.',
    s6: 'Após uso legítimo: desbloqueie e libere/desative.',
    sec_title: 'O que é (e o que não é)',
    th_item: 'Item', th_real: 'Realidade',
    r1a: '3 erros → factory reset', r1b: 'Sim (Device Admin + consentimento)',
    r2a: 'Eficaz contra roubo de dados', r2b: 'Sim — aparelho volta “como novo”',
    r3a: 'Queima RAM / destrói hardware', r3b: 'Não — impossível e não legítimo',
    r4a: 'Uso em aparelho de terceiros', r4b: 'Proibido sem consentimento (crime)',
    warn: 'Aviso: no modo máximo, 3 PIN errados apagam fotos, apps e contas. Faça backup. Teste com cuidado.',
    dl_title: 'Download direto', dl_lead: 'APK release assinado · v1.1.0 · ~14 MB',
    dl_sub: 'Nome explicativo · anti-theft · assinado por Joaquim Pedro',
    cta_dl2: 'Download',
    gh_title: 'Repositórios no GitHub',
    repo1: 'Código-fonte Android · website · APK · README',
    repo2: 'Versões publicadas e assets de download',
    repo3: 'Este site de documentação e download',
    author_k: 'Autor',
    author_b: 'JP Secure Lock — dispositivo de segurança / bloqueador anti-roubo para Android, com criptografia de ponta e liberação controlada.',
  },
  en: {
    nav_how: 'How it works', nav_sec: 'Security', nav_dl: 'Download', nav_gh: 'GitHub',
    kicker: 'Android · Anti-Theft · E2E Crypto',
    hero_title: 'Security blocker for device protection and controlled release',
    hero_lead: 'JP Secure Lock protects the device with a PIN, AES-256-GCM encryption, and an anti-theft response: after 3 wrong passwords it runs a cryptographic wipe and official Android factory reset.',
    cta_dl: 'Download signed APK v1.1.0',
    hero_meta: 'Created by Joaquim Pedro de Morais Filho · Package br.com.joaquimpedro.jpsecure · Signed (APK Signature v2)',
    c1_t: 'On-device E2E encryption', c1_b: 'AES-256-GCM + PBKDF2-HMAC-SHA256. Keys derived from PIN; local vault, no server.',
    c2_t: 'Anti-theft blocker', c2_b: '3 wrong PINs → key wipe, lockNow, and factory reset (official Device Admin).',
    c3_t: 'Deactivate after release', c3_b: 'With the correct PIN, release the device and remove admin protection.',
    c4_t: 'Recovery', c4_b: 'Recovery code issued at activation. Use if vault wipe ran without factory reset.',
    how_title: 'How it works',
    s1: 'Install the APK and open JP Secure Lock.',
    s2: 'Enable Device Administrator (official Android screen).',
    s3: 'Set your PIN and enable maximum anti-theft mode.',
    s4: 'Store the recovery code offline (shown once).',
    s5: 'On theft attempt: 3 failures erase all device data.',
    s6: 'After legitimate use: unlock and release/deactivate.',
    sec_title: 'What it is (and is not)',
    th_item: 'Item', th_real: 'Reality',
    r1a: '3 failures → factory reset', r1b: 'Yes (Device Admin + consent)',
    r2a: 'Effective against data theft', r2b: 'Yes — device returns “like new”',
    r3a: 'Burns RAM / destroys hardware', r3b: 'No — impossible and not legitimate',
    r4a: 'Use on someone else’s phone', r4b: 'Forbidden without consent (crime)',
    warn: 'Warning: in maximum mode, 3 wrong PINs erase photos, apps, and accounts. Back up first. Test carefully.',
    dl_title: 'Direct download', dl_lead: 'Signed release APK · v1.1.0 · ~14 MB',
    dl_sub: 'Descriptive filename · anti-theft · signed by Joaquim Pedro',
    cta_dl2: 'Download',
    gh_title: 'GitHub repositories',
    repo1: 'Android source · website · APK · README',
    repo2: 'Published versions and download assets',
    repo3: 'This documentation and download site',
    author_k: 'Author',
    author_b: 'JP Secure Lock — Android security / anti-theft blocker with strong encryption and controlled release.',
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
  localStorage.setItem('jp_secure_lang', lang);
}

const saved = localStorage.getItem('jp_secure_lang');
const start = saved || ((navigator.language || '').toLowerCase().startsWith('en') ? 'en' : 'pt');
setLang(start);
