# Arquitetura inicial

## Aplicativo Android

```text
app/src/main/java/br/com/eibragaa/nayplanos/
├── MainActivity.kt
├── domain/
│   ├── model/          modelos puros de planejamento
│   ├── validation/     regras de datas, semana e completude
│   └── usecase/        operações de negócio testáveis
├── data/
│   ├── local/          Room, DAOs e migrações
│   ├── image/          cópia privada das imagens e thumbnails
│   ├── files/          DOCX temporários e arquivos auxiliares
│   ├── generator/      cliente do serviço e contrato JSON
│   └── sharing/        FileProvider e compartilhamento Android
├── ui/
│   ├── navigation/     rotas e navegação
│   ├── home/           início e resumo
│   ├── planner/        criação e revisão da semana
│   ├── history/        histórico e duplicação
│   ├── settings/       preferências, privacidade e atualização
│   └── design/         tema, componentes e tokens
└── updater/            consulta, download e instalação segura
```

## Serviço de geração

O app não terá chave de IA nem regra de geração espalhada em composables. O serviço receberá uma requisição autenticada e devolverá um contrato versionado contendo sugestões por dia. O app persistirá a sugestão como rascunho para revisão.

O gerador DOCX deverá ser determinístico: receberá um payload já revisado, aplicará o modelo oficial e devolverá um arquivo validado. A geração não deve depender de uma chamada de IA durante a montagem do documento.

## Dados e privacidade

- o rascunho e as imagens ficam locais por padrão;
- imagens selecionadas são copiadas para o armazenamento privado do app, com referência opaca e thumbnail derivada;
- o picker usa o contrato de seleção do Android, sem permissão de armazenamento amplo;
- o envio para análise será explícito e acompanhado de estado de progresso;
- imagens não serão incluídas em logs;
- o usuário poderá apagar os dados locais;
- backup/exportação será projetado depois do modelo mínimo de dados estar estável.

## Distribuição

O repositório terá uma chave de assinatura própria. O canal GitHub deverá exigir checksum e assinatura verificáveis antes de oferecer a instalação. O canal Play não deve depender da permissão de instalar APKs externos.
