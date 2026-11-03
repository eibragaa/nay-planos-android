# Instruções do projeto Nay Planos

## Escopo

Este repositório é um aplicativo Android nativo para planejamento pedagógico. Não importar o domínio, as entidades ou o banco do Meu Ponto.

## Regras obrigatórias

1. `ui/` compõe telas e recebe estado e callbacks; não faz rede, arquivo, ZIP, DOCX, banco ou chamada de IA.
2. `domain/` contém modelos e regras puras, sem Android ou Compose.
3. `data/` é responsável por Room, arquivos, imagens, exportação, rede e integração com serviços.
4. Segredos de IA, assinatura e distribuição nunca entram no código ou no Git.
5. Toda regra pura nova recebe teste JVM.
6. Toda tela nova recebe estado explícito, estados de carregamento/erro e suporte a fonte ampliada.
7. Erros devem ser acionáveis; não usar `catch (_: Exception) {}` sem tratamento visível ou registro apropriado.
8. Não alterar `versionName`, `versionCode`, tags, releases ou chaves sem autorização explícita.
9. Mudanças delegadas devem ter escopo de arquivos disjunto, testes e revisão posterior.
10. Antes de cada merge: testes, lint e inspeção do diff.

## Convenções

- Conventional Commits: `feat(app)`, `fix(domain)`, `refactor(ui)`, `test`, `docs`, `chore(ci)`.
- Kotlin idiomático, Compose Material 3 e espaçamentos em múltiplos de 4 dp.
- Strings de interface em recursos Android, com pt-BR como idioma inicial.
- Um arquivo deve ter um assunto principal e funções pequenas.

## Definição de pronto

Uma tarefa só está pronta quando o código está implementado, testado, acessível no fluxo afetado e documentado quando houver decisão arquitetural ou de segurança.
