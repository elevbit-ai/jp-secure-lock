# Contribuindo com o JP Secure Lock

Obrigado pelo interesse! Este projeto é de autoria de **Joaquim Pedro de Morais Filho**.

## Como contribuir

1. Abra uma *issue* descrevendo o bug ou a proposta antes de um PR grande.
2. Faça um *fork*, crie um branch descritivo (`fix/...`, `feat/...`).
3. Mantenha o estilo do código existente (Java, indentação de 4 espaços, comentários em português).
4. Descreva claramente o impacto de segurança de qualquer mudança que toque em
   `CryptoEngine`, `KeystoreEngine`, `SecureVault` ou `AntiTheftController`.

## Vulnerabilidades

**Não** relate falhas de segurança em issues públicas. Siga [SECURITY.md](SECURITY.md).

## Build

```bash
./gradlew assembleDebug     # build de desenvolvimento
./gradlew assembleRelease   # build assinado (requer keystore.properties)
./gradlew lint              # análise estática
```

## Padrões de commit

Use mensagens no imperativo e com escopo, por exemplo:
`feat(vault): selar hash de recuperação pelo Keystore`.

## Código de conduta

Seja respeitoso e objetivo. Discussões técnicas são bem-vindas; ataques pessoais não.
