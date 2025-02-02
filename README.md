# 🪙 CryptoBot
![Em Desenvolvimento](https://img.shields.io/badge/Status-Em%20Desenvolvimento-brightgreen?style=for-the-badge)
![Java](https://img.shields.io/badge/Java-21-blue?style=for-the-badge&logo=java)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4-green?style=for-the-badge&logo=spring)
![Maven](https://img.shields.io/badge/Maven-3.9-blue?style=for-the-badge&logo=apache-maven)
![Binance API](https://img.shields.io/badge/Binance-API-yellow?style=for-the-badge&logo=binance)

O CryptoBot é um bot de trading automatizado capaz de tomar decisões de compra e venda de ativos com base na análise técnica de indicadores e estratégias configuráveis. Desenvolvido exclusivamente para estudos pessoais, o bot foi criado em Java Spring Boot e utiliza a API da Binance para realizar suas operações.
<br/> 

## ⚠️ Atenção
Este projeto foi criado **apenas para fins educacionais** e não deve ser utilizado para operações financeiras reais.
* O CryptoBot não oferece garantias de lucro ou segurança financeira.
*	Não foi projetado para uso em trading real e não possui validações robustas contra riscos de mercado.
*	O uso deste bot é totalmente por conta e risco do usuário, e o desenvolvedor não se responsabiliza por quaisquer perdas financeiras ou danos decorrentes da sua utilização.

## Índice

- [Começando](#-começando)
   - [Pré-requisitos](#-pré-requisitos)
   - [Configuração](#-configuração)
   - [Executando](#-executando)
   - [Exemplo de Execução](#-exemplo-de-execução)
- [Como Funciona](#-como-funciona)
- [Principais Funcionalidades](#-principais-funcionalidades)
- [Estratégias utilizadas](#-estratégias-utilizadas)
   - [Estratégia: Moving Average (Média Móvel Simples)](#-estratégia-moving-average-média-móvel-simples)
   - [Estratégia: Moving Average Antecipation (Média Móvel Antecipada)](#-estratégia-moving-average-antecipation-média-móvel-antecipada)
- [Melhorias Futuras](#-melhorias-futuras)


## 🚀 Começando
### 🛠️ Pré-requisitos
Para utilizar o CryptoBot, é necessário:
1.	**Criar uma conta na Binance**: Acesse Binance e realize seu cadastro.
2.	**Gerar uma API Key**: Acesse esse tutorial no fórum da Binance e siga os passos descritos para gerar sua API Key. Lembre-se de:
    *	Ativar restrição por IP, na opção “Restrict access to trusted IPs only (Recommended)”
    *	Ativar a opção “Enable Stop & Margin Trading”
    *	Salvar em um lugar seguro sua Api Key e Api Secret
4.	**Configurar o arquivo application.properties** ([mais detalhes aqui](https://github.com/biancacmr/crypto-bot/new/main?filename=README.md#%EF%B8%8F-pr%C3%A9-requisitos)).

### ⚙️ Configuração
Antes de executar o bot, precisamos configurar alguns parâmetros. Utilizamos um arquivo de configuração para definir parâmetros como API keys, estratégias e intervalos de execução. Copie o conteúdo do arquivo abaixo em um arquivo com o nome application.properties, na pasta resources do projeto instalado no seu computador.

#### Caminho
```
src/main/resources/application.properties
```

#### O arquivo application.properties

```
spring.application.name=AutomaticCryptoTrader

# ------------------------------------------------------------------------------
#    BINANCE API CONFIGS
# ------------------------------------------------------------------------------

# Dados de login
binance.apiKey=SUA_API_KEY
binance.secretKey=SUA_SECRET_KEY
binance.url=https://api.binance.com

# Código do ativo que irá operar
binance.stockCode=SOL

# Dupla de ativos que vai operar a conversão
binance.operationCode=SOLUSDT

# Períodos dos candles analisados pelo bot
binance.candlePeriod=5m

# Quantidade máxima do ativo que o bot pode operar na compra
binance.tradedQuantity=10

# Valor máximo que o bot irá comprar do ativo
binance.maxBuyValue=100

# Porcentagem máxima de loss para ativar o mecanismo de stop loss
# quando o prejuízo (considerando a última compra) for maior que a porcentagem, vende tudo
binance.stopLossPercentage=2.0

binance.acceptableLossPercentage=0.5

# Caso a estratégia principal (Moving Average Antecipation) seja inconclusiva:
# fallBackAtive TRUE: decide a partir da Moving Average
# fallBackAtive FALSE: não realiza nenhuma ação
binance.fallbackActive=true

# Parâmetros para os indicadores
binance.maFastWindow=7
binance.maSlowWindow=40
binance.rsiWindow=14
binance.volatilityWindow=40
binance.volatilityFactor=0.5

# ------------------------------------------------------------------------------
#    EMAIL CONFIGS
# ------------------------------------------------------------------------------

spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=SEU_EMAIL
spring.mail.password=SUA_SENHA
spring.mail.protocol=smtp
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true

# Preencher com os e-mails que receberão as notificações de compra e venda
receiversList=teste@gmail.com,teste2@gmail.com
```

### 💻 Executando
Siga os passos abaixo para rodar o CryptoBot no seu ambiente local:

1. Clone o repositório
```
git clone https://github.com/biancacmr/crypto-bot.git
cd crypto-bot
```

2. Compile e construa o projeto
   
Como o projeto utiliza Maven, basta rodar o seguinte comando para baixar as dependências e compilar:
```
mvn clean install
```

3. Execute o bot
   
Após a compilação, inicie a aplicação com:
```
mvn spring-boot:run
```

4. Monitoramento

O bot iniciará as operações automaticamente. Você pode acompanhar os logs diretamente no terminal. Os logs também ficam salvos no pasta:
```
logs
```

### 📜 Exemplo de Execução
Abaixo, um exemplo de execução do robô operando o par SOL/USDT na Binance Testnet.
Ele segue a estratégia de Médias Móveis Antecipadas e executa ordens automaticamente com base na análise de mercado.

> Nota: As operações realizadas abaixo foram realizadas no [ambiente de testes](https://www.binance.com/pt/support/faq/como-testar-as-minhas-fun%C3%A7%C3%B5es-na-testnet-da-binance-ab78f9a1b8824cf0a106b4229c76496d) da Binance.

```
---------------------------------------------
Robô iniciando...
---------------------------------------------

Última ordem de COMPRA executada para SOLUSDT:
 | Data: 02:15:54 28-01-2025
 | Preço: 235.049
 | Quantidade: 0.425

Última ordem de VENDA executada para SOLUSDT:
 | Data: 02:24:15 28-01-2025
 | Preço: 235.080
 | Quantidade: 0.850

---------------------------------------------

Executado 27/01/25 23:24:26:894
Posição atual: VENDIDO
Balanço atual: 0.0 (SOL)
 - Preço atual: 235.15
 - Preço mínimo para vender: 233.87474999999998
 - Stop Loss em: 230.349 (2.0%)
0.5

---------------------------------------------

Estratégia executada: Moving Average Antecipation
(SOLUSDT)
 | Última Média Rápida: 234.90571428571428
 | Última Média Lenta: 231.79575
 | Última Volatilidade: 3.783059540527944
 | Diferença Atual: 3.109964285714284
 | Diferença para antecipação: 1.891529770263972
 | Gradiente Rápido: -0.1328571428571479 (DESCENDO)
 | Gradiente Lento: 0.04449999999999932 (SUBINDO)
 | Decisão: NENHUMA

---------------------------------------------

Estratégia de MA Antecipation inconclusiva
Executando estratégia de fallback...

---------------------------------------

Estratégia executada: Moving Average
(SOLUSDT)
 | Última Média Rápida = 234.90571428571428
 | Última Média Lenta = 231.79575
 | Decisão = COMPRAR

---------------------------------------

Ação final: COMPRAR

---------------------------------------------

Balance{asset='SOL', free=0.0, locked=0.0}

Enviando ordem limitada de COMPRA para SOLUSDT
 - Tipo de Ordem: Por Valor
 - Valor Total (USDT): 100.0
 - Quantidade: 0.424
 - Close price: 235.15
 - Preço Limite: 235.62
Ordem de COMPRA limitada enviada com sucesso:

---------------------------------------------

ORDEM ENVIADA:
 | Status: Completed
 | Side: BUY
 | Ativo: SOLUSDT
 | Quantidade: 0.424
 | Valor na venda: 235.20000000
 | Moeda: SOL
 | Valor em SOL: 99.7248
 | Type: LIMIT
 | Data/Hora: 02:24:27 2025-01-28
 | Complete_order: {"symbol":"SOLUSDT","cummulativeQuoteQty":"99.72480000","side":"BUY","orderListId":-1,"executedQty":"0.42400000","orderId":3283573,"origQty":"0.42400000","clientOrderId":"pvMhn9r7AIK6dt4WC7L4Gz","workingTime":1738031067793,"type":"LIMIT","price":"235.62000000","transactTime":1738031067793,"selfTradePreventionMode":"EXPIRE_MAKER","origQuoteOrderQty":"0.00000000","timeInForce":"GTC","fills":[{"price":"235.20000000","qty":"0.42400000","commission":"0.00000000","commissionAsset":"SOL","tradeId":1057243}],"status":"FILLED"}

---------------------------------------------

Última ordem de COMPRA executada para SOLUSDT:
 | Data: 02:24:27 28-01-2025
 | Preço: 235.200
 | Quantidade: 0.424

Última ordem de VENDA executada para SOLUSDT:
 | Data: 02:24:15 28-01-2025
 | Preço: 235.080
 | Quantidade: 0.850

Balance{asset='SOL', free=0.424, locked=0.0}
---------------------------------------------
```

## 🔄 Como Funciona
O bot executa operações automaticamente em intervalos configuráveis, seguindo a seguinte lógica:
1.	**Atualização dos Dados do Mercado**: Obtém informações sobre preços, ordens abertas e saldo da conta.
2.	**Análise Técnica**: Calcula médias móveis, RSI e outros indicadores configurados.
3.	**Verificação de Estratégias**: Compara os indicadores com as estratégias definidas para tomada de decisão.
4.	**Gerenciamento de Risco**: Aplica o Stop Loss automaticamente se as perdas atingirem um limite predefinido.
5.	**Execução de Ordens**: Compra ou vende ativos conforme as regras estabelecidas. Se nenhuma condição for atendida, o bot mantém a posição atual (comprado ou vendido).
6.	**Envio de Notificações**: Caso uma ordem seja executada, um e-mail é enviado com os detalhes da operação.
7.	**Aguardar Próximo Ciclo**: O bot entra em um estado de espera por um tempo configurável antes de reiniciar o processo.

## 🔧 Principais Funcionalidades
*	**Configuração Dinâmica**: Permite ajustar indicadores, ativos e outras configurações diretamente no arquivo de propriedades.
*	**Moving Average Antecipation**: Estratégia principal baseada na antecipação do cruzamento de médias móveis para tomada de decisão.
*	**Estratégia Fallback (opcional)**: Caso a estratégia principal não retorne um resultado claro, uma segunda estratégia baseada em cruzamento de médias móveis simples pode ser utilizada.
*	**Ajuste Inteligente de Ordens**: Utiliza o RSI (Índice de Força Relativa) para buscar o melhor valor de compra/venda e potencializar os lucros.
*	**Mecanismo de Acceptable Loss**: Impede que ativos sejam vendidos por um valor inferior ao último preço de compra, reduzindo prejuízos.
*	**Stop Loss Fixo**: Permite configurar um limite de perda aceitável. Caso o preço caia além do valor definido, o bot executa uma venda imediata para minimizar perdas.
*	**Notificações por E-mail**: Envio de alertas automáticos ao realizar operações de compra e venda.

## 📊 Estratégias utilizadas
Nosso robô opera com base em **Médias Móveis (Moving Averages)** para identificar tendências de preço e tomar decisões de compra e venda. Ele implementa duas estratégias distintas, uma mais simples e outra mais sofisticada:

### **1. Estratégia: Moving Average (Média Móvel Simples)**  

**Objetivo:** Identificar tendências de alta ou baixa com base na relação entre duas médias móveis.

#### :white_check_mark: Como funciona?
- Calculamos duas médias móveis:
  - **Média Móvel Rápida (MA Rápida)**: Representa um período curto e responde rapidamente às variações de preço.
  - **Média Móvel Lenta (MA Lenta)**: Representa um período maior e reage de forma mais suave às mudanças.
- A decisão é tomada com base na relação entre elas:
  - **Se MA Rápida > MA Lenta** → O mercado está em tendência de alta → **Decisão: COMPRAR**  
  - **Se MA Rápida < MA Lenta** → O mercado está em tendência de baixa → **Decisão: VENDER**  

### **2. Estratégia: Moving Average Antecipation (Média Móvel Antecipada)**  

**Objetivo:** Antecipar cruzamentos das médias móveis antes que eles aconteçam, utilizando volatilidade e gradientes.

#### :white_check_mark: Como funciona?
- Além das médias móveis rápida e lenta, essa estratégia considera:
  - **O gradiente (inclinação) das médias móveis**: Mede a variação das médias ao longo do tempo.
  - **A volatilidade do mercado**: Mede o quão instável o preço está.
  - **A diferença entre as médias móveis**: Avalia quão distante a MA Rápida está da MA Lenta.
- Regras de decisão:
  - **Compra antecipada**: Se a MA Rápida está subindo e se aproximando da MA Lenta com maior força (**gradiente positivo maior que o da MA Lenta**).
  - **Venda antecipada**: Se a MA Rápida está caindo e se afastando da MA Lenta com maior força (**gradiente negativo maior que o da MA Lenta**).
  - **Sem decisão**: Se a diferença entre as médias ainda está dentro do intervalo de volatilidade esperado.

## ✅ Melhorias Futuras
- [ ]	Trailing Stop
- [ ]	Take Profit
- [ ]	Novas estratégias (Vortex, MACD, RSI)
- [ ]	Algoritmo de identificação do tipo de mercado para ajuste automático de estratégia
- [ ]	Machine Learning para otimizar a tomada de decisões
- [ ]	Interface gráfica para facilitar interação com o bot
- [ ]	Opção de rodar mais de uma moeda ao mesmo tempo com parâmetros diferentes
  
## Tecnologias Utilizadas

Este projeto foi desenvolvido com as seguintes tecnologias e ferramentas:

- **[Spring Boot 3.4](https://spring.io/projects/spring-boot)**: Framework Java utilizado.
- **[Maven 3.9](https://maven.apache.org/)**: Gerenciamento de dependências.
- **[SLF4J](https://www.slf4j.org/)**: Interface de log para Java, utilizada para gerar logs de aplicação.
- **[Logback](https://logback.qos.ch/)**: Framework de logging que implementa a interface SLF4J, utilizado para gerenciar e formatar os logs da aplicação.
- **[Eclipse Angus Email](https://www.eclipse.org/angus/)**: Biblioteca para envio de e-mails, utilizada para integrar funcionalidades de envio de mensagens dentro da aplicação.

## 📄 Licença
Este projeto está sob a licença [MIT](https://opensource.org/licenses/MIT) - veja o arquivo LICENSE.md para detalhes.
