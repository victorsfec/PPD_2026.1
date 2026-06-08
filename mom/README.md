# Simulador MOM — Rede IoT com ActiveMQ

Projeto da disciplina **Programação Paralela e Distribuída** — IFCE Engenharia de Computação.

Simula uma rede IoT onde **Sensores** publicam alertas em um Broker ActiveMQ e **Clientes** assinam tópicos para receber essas notificações em tempo real, seguindo o padrão **Publish/Subscribe (MOM — Message-Oriented Middleware)**.

---

## Arquitetura

```
[SimuladorMOM — GUI Swing]
        |               |
   cria/controla   cria/controla
        |               |
   [Sensor]         [Cliente]
  (Publisher)      (Subscriber)
        |               |
        └──► [ActiveMQ Broker :61616] ◄──┘
```

O `SimuladorMOM` não se comunica diretamente com o Broker — ele apenas instancia e aciona `Sensor` e `Cliente`, que fazem as chamadas JMS de forma independente.

---

## Estrutura do Projeto

```
projeto-mom/
├── lib/
│   └── apache-activemq-5.x/          # Broker ActiveMQ (já incluído no projeto)
│       ├── bin/
│       │   ├── activemq              # Script Linux/macOS
│       │   └── activemq.bat          # Script Windows
│       └── ...
└── src/
    └── br/com/victorsfec/mom/
        ├── app/
        │   └── SimuladorMOM.java     # GUI principal (View + Controller)
        ├── sensor/
        │   └── Sensor.java           # Publisher JMS
        ├── client/
        │   └── Cliente.java          # Subscriber JMS
        └── model/
            └── Mensagem.java         # DTO trafegado pelo Broker
```

---

## Pré-requisitos

| Requisito | Versão mínima | Observação |
|---|---|---|
| Java JDK | 21 | Necessário para compilar e executar |
| Apache ActiveMQ | 5.19.1 | Já incluído em `lib/` — não precisa instalar |
| Maven | 3.6+ | Necessário para o build e geração do JAR |

---

## Como executar

### 1. Iniciar o Broker ActiveMQ

O ActiveMQ já está incluído na pasta `lib/` do projeto. Acesse a pasta do Broker e execute o script de inicialização:

**Windows:**
```bat
cd lib\apache-activemq-5.x
bin\activemq.bat start
```

**Linux / macOS:**
```bash
cd lib/apache-activemq-5.x
bin/activemq start
```

O Broker ficará disponível em `tcp://localhost:61616`.  
O painel de administração web fica em `http://localhost:8161` (usuário: `admin`, senha: `admin`).

### 2. Executar o simulador

```bash
java -jar simulador-mom.jar
```

---

## Funcionalidades

### Sensores
- Criação de múltiplos sensores, cada um monitorando um parâmetro (`temperatura`, `umidade`, `velocidade` etc.)
- Definição de limites mínimo e máximo por sensor
- IDs únicos — o sistema impede duplicatas
- Ao injetar uma leitura fora dos limites, o sensor publica automaticamente um alerta no Broker
- A lista lateral exibe o estado atual de cada sensor com indicador visual `⚠` em caso de alerta

### Clientes
- Criação de múltiplos clientes com IDs únicos
- Consulta em tempo real dos tópicos ativos no Broker (via Advisory Topics do ActiveMQ)
- Seleção de tópico por lista — sem necessidade de digitar nomes manualmente
- Cada cliente pode assinar vários tópicos distintos
- O mesmo cliente não pode assinar o mesmo tópico duas vezes (evita listeners duplicados)
- Alertas recebidos são exibidos no terminal de logs com sensor de origem e valor da leitura

---

## Fluxo de uso

```
1. Clique em "1. Novo Sensor"
   → Informe ID, tópico, limite mínimo e máximo

2. Clique em "2. Novo Cliente"
   → Informe o ID do cliente

3. Clique em "3. Assinar Tópico"
   → Selecione o cliente e escolha o tópico na lista

4. Clique em "4. Disparar Sensor"
   → Selecione o sensor e informe uma nova leitura
   → Se fora dos limites, o alerta aparece no terminal de todos
     os clientes que assinaram aquele tópico
```

---

## Gerando o `.jar` executável

###  Maven

O `pom.xml` já está configurado com o `maven-assembly-plugin`. Basta rodar na raiz do projeto:

```bash
mvn clean test package verify
```

O arquivo `simulador-mom.jar` será gerado em `target/`. Para executar:

```bash
java -jar target/simulador-mom.jar
```

---

## Observações

- O ActiveMQ **deve estar rodando** antes de iniciar o simulador. Sem o Broker, as tentativas de conexão dos sensores e clientes vão falhar com mensagem de erro no terminal de logs.
- A comunicação usa o protocolo **OpenWire** na porta `61616` (padrão do ActiveMQ).
- A desserialização de objetos Java está habilitada via `setTrustAllPackages(true)`. Em produção, substitua por `setTrustedPackages(List.of("br.com.victorsfec.mom.model"))`.
