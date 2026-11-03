# Decisões de produto

## Identidade inicial

- Nome do produto: Nay Planos
- Repositório proposto: `nay-planos-android`
- `applicationId` inicial: `br.com.eibragaa.nayplanos`
- Distribuição prevista: Google Play e GitHub Releases
- Status: valores provisórios até confirmação do mantenedor antes da publicação remota

## Limites do MVP

O primeiro fluxo de valor é montar uma semana e gerar um DOCX revisável. Contas, colaboração entre professoras, sincronização automática e Google Drive não fazem parte do primeiro MVP.

## Decisão sobre análise de imagens

O APK não armazenará chaves de provedor de IA. A análise assistida será isolada atrás de um serviço autenticado e poderá ser desligada sem impedir a edição manual e a geração do documento.

## Decisão sobre distribuição

O GitHub Releases será tratado como canal de sideload. A Play Store terá seu próprio flavor e não dependerá da permissão de instalar APK externo. A assinatura do Nay Planos será diferente da assinatura do Meu Ponto.

## Decisões ainda abertas

- nome e visibilidade definitivos do repositório;
- domínio ou provedor do serviço de geração;
- contato público para privacidade e suporte;
- modelo final do DOCX e política de armazenamento das imagens enviadas para análise.
