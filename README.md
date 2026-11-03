# Nay Planos Android

Aplicativo Android nativo para criar, revisar, gerar e compartilhar planos de aula semanais da turma Pré-Maternal.

## Direção do produto

O fluxo principal será:

1. escolher a semana e a turma;
2. anexar uma imagem para cada dia;
3. revisar as descrições sugeridas;
4. gerar o plano no modelo institucional;
5. salvar, compartilhar ou duplicar o planejamento.

O aplicativo será separado do `Meu Ponto`. O repositório `eibragaa/meu-ponto-android` será usado apenas como referência de práticas de arquitetura, CI/CD e distribuição.

## Estado atual

O projeto está com a fundação das Sprints 0–3 e o primeiro bloco da Sprint 5 implementados:
editor semanal, persistência local, imagens pela galeria/câmera, histórico, miniaturas, DOCX e
compartilhamento. A política e o workflow de release seguro já estão preparados; a verificação
criptográfica integrada ao fluxo de atualização e a validação no Android SDK ainda são gates de
release.

## Princípios

- dados do planejamento serão locais por padrão;
- nenhuma chave de IA ou assinatura será embutida no APK;
- toda sugestão gerada por imagem poderá ser revisada antes de virar documento;
- regras de negócio serão puras e testadas;
- cada release será assinada, verificável e reproduzível;
- acessibilidade será requisito de aceite, não acabamento posterior.

Consulte [`docs/roadmap.md`](docs/roadmap.md) e [`docs/architecture.md`](docs/architecture.md).
