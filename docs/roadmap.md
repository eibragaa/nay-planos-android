# Roadmap do Nay Planos Android

## Estado de execução

- [x] Repositório local inicializado.
- [x] Identidade provisória e decisões arquiteturais documentadas.
- [x] Esqueleto Compose, tema, navegação inicial e CI criados.
- [x] Modelos puros e validador inicial de semana criados.
- [x] Contrato inicial Room, mapeadores e gravação transacional criados.
- [x] Editor de rascunho, política de referências e primeiro fluxo Compose/Room criados.
- [x] Seletor de galeria, armazenamento privado, thumbnails e histórico inicial criados.
- [x] Gerador DOCX OOXML mínimo, incorporação de miniaturas e compartilhamento via `FileProvider` criados.
- [x] Pré-visualização acessível de thumbnails na tela de revisão.
- [x] Captura pela câmera e cancelamento/limpeza do arquivo de captura.
- [x] Política de atualização, parser da release oficial e workflow de APK assinado com checksum.
- [ ] Verificador criptográfico integrado, download/instalação assistida e tela de atualização.
- [ ] Confirmar nome, visibilidade e `applicationId` antes de publicar o repositório remoto.
- [ ] Validar build no Android SDK/CI.

## Sprint 0 — Fundação e decisões

Objetivo: criar um repositório Android independente, com identidade e gates de qualidade.

Entregas:

- `applicationId` inicial `br.com.eibragaa.nayplanos`;
- Kotlin, Jetpack Compose, Material 3 e estrutura mínima compilável;
- CI para testes, lint e build de desenvolvimento;
- regras de engenharia, privacidade e distribuição documentadas;
- identidade visual inicial e tokens de tema;
- nenhuma chave ou segredo versionado.

Aceite: o projeto abre no Android Studio, executa uma tela inicial, tem testes/lint configurados e o CI bloqueia PR quebrado.

## Sprint 1 — Arquitetura e navegação

Objetivo: estabelecer o esqueleto de produto sem lógica específica de IA.

Entregas:

- navegação entre Início, Novo plano, Histórico e Configurações;
- ViewModels e estados selados;
- design system reutilizável;
- tema claro/escuro;
- estados de vazio, carregamento, erro e sucesso;
- acessibilidade básica desde a primeira tela.

Aceite: o fluxo navega por TalkBack, fonte ampliada e rotação sem perder estado transitório importante.

## Sprint 2 — Domínio e persistência local

Objetivo: permitir criar e salvar um rascunho semanal completo.

Entregas:

- semana, dia, turma, professora e atividade;
- feriado e dia sem imagem;
- Room e migrações;
- rascunho automático;
- duplicação de semana;
- testes das regras de datas e dos cinco dias.

Aceite: uma semana pode ser criada, editada, fechada e reaberta sem rede.

## Sprint 3 — Imagens e revisão

Objetivo: receber as referências visuais e transformá-las em entradas revisáveis.

Entregas:

- seleção pela galeria e câmera;
- armazenamento local e thumbnails;
- associação imagem/dia;
- remoção e substituição;
- descrição manual/editável;
- validação de imagem ilegível ou ausente.

Aceite: a professora consegue montar uma semana real apenas pelo aparelho e revisar cada dia antes da geração.

## Sprint 4 — Análise assistida e serviço de geração

Objetivo: sugerir descrições pedagógicas sem expor segredos no APK.

Entregas:

- contrato JSON versionado entre app e serviço;
- serviço de análise de imagem com autenticação e limites;
- seleção controlada dos objetivos EI02;
- revisão obrigatória da sugestão;
- logs sem imagens ou dados pessoais desnecessários.

Aceite: a falha do serviço não perde o rascunho e nunca publica texto sem confirmação da usuária.

## Sprint 5 — DOCX, compartilhamento e histórico

Objetivo: gerar o documento institucional final.

Entregas:

- gerador baseado no modelo oficial;
- validação das cinco tabelas e capacidades do padrão;
- visualização do resumo;
- criação do DOCX;
- compartilhamento Android;
- histórico e reexportação.

Aceite: o documento abre corretamente, mantém o modelo visual e contém os cinco dias e textos revisados.

## Sprint 6 — AAA de UX, acessibilidade e robustez

Objetivo: elevar o produto de funcional para publicável.

Entregas:

- TalkBack no fluxo completo;
- alvos de toque mínimos de 48 dp;
- escala de fonte até 200%;
- contraste em estados claros, escuros e de erro;
- layouts para tablet e dobrável;
- reduce motion;
- testes UI e screenshot tests.

Aceite: nenhuma falha crítica de acessibilidade, sobreposição ou perda de dados no fluxo principal.

## Sprint 7 — Release e atualização segura

Objetivo: distribuir builds de teste e produção com atualização confiável.

Entregas:

- flavor Play e flavor GitHub;
- keystore própria;
- checksum obrigatório;
- validação de package, versão, assinatura e downgrade;
- GitHub Actions com gates separados para PR e release;
- notas de versão e artefatos rastreáveis.

Aceite: atualização válida instala; APK truncado, checksum ausente, assinatura divergente, downgrade e release sem assinatura são bloqueados.

## Sprint 8 — Beta e publicação

Objetivo: validar em aparelhos reais e preparar a Play Store.

Entregas:

- matriz de aparelhos e versões Android;
- testes offline e de ciclo de vida;
- política de privacidade e Data Safety coerentes;
- screenshots e ficha da loja;
- rollout gradual;
- plano de suporte e hotfix.

Aceite: beta sem bloqueadores críticos, release assinada e checklist de publicação concluído.

## Modelo de orquestração

- O agente principal decide arquitetura, contratos, integração, segurança e aceite.
- Agentes econômicos recebem tarefas pequenas com escopo de arquivos disjunto.
- Um agente revisor verifica cada bloco antes da integração.
- Refatorações que atravessam muitas camadas permanecem sob coordenação do agente principal.
- Nenhum agente publica, cria tag, muda versão ou usa segredo de release sem autorização.
