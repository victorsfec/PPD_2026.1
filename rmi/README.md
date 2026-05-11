# 🎮 Jogo Dara - Multiplayer Distribuído (Java RMI)

Este projeto é uma evolução da implementação do jogo de tabuleiro africano **Dara**, desenvolvido com arquitetura **Cliente-Servidor**. Nesta versão, a comunicação baseada em Sockets TCP puros foi totalmente substituída por **Java RMI (Remote Method Invocation)**. O projeto foi criado para a disciplina de Programação Paralela e Distribuída (PPD) - 2026.1.

---

## 🔄 Sockets vs RMI: O que mudou na Arquitetura?

A transição de Sockets para RMI mudou fundamentalmente a forma como os computadores conversam. As principais diferenças implementadas foram:

1. **Fim do "Parsing" de Strings (Protocolo Manual):** * **Antes (Sockets):** O cliente enviava textos como `MOVE:1:2:3:4`, e o servidor precisava usar `.split(":")` para entender o comando.
   * **Agora (RMI):** Orientação a objetos real na rede. O cliente simplesmente chama um método Java no servidor: `gameSession.sendMove(1, 1, 2, 3, 4)`. O Java cuida da serialização por debaixo dos panos.
2. **Padrão Callback em vez de Threads de Escuta:**
   * **Antes (Sockets):** Cada cliente exigia uma Thread dedicada (`ClientHandler`) presa em um loop infinito (`in.readLine()`) esperando mensagens.
   * **Agora (RMI):** O cliente envia ao servidor o seu próprio objeto "Ouvinte" (`IClientCallback`). Quando o servidor quer atualizar a tela do cliente, ele invoca os métodos desse Callback (Padrão de *Push* ativo), eliminando a necessidade de *polling* contínuo.
3. **Tratamento de Falhas:**
   * **Antes:** As quedas de conexão geravam `SocketException` ou `IOException`.
   * **Agora:** Qualquer falha na rede ou na execução do método remoto lança uma `RemoteException`, permitindo ao servidor encerrar a partida e declarar vitória por W.O. de forma mais controlada.

---

## 🌟 Principais Funcionalidades

* **Arquitetura Baseada em Interfaces:** O sistema é regido por "Contratos" (Interfaces RMI no pacote `common`), garantindo que o Cliente e o Servidor saibam exatamente quais métodos podem invocar mutuamente.
* **Interface Gráfica do Servidor:** O servidor possui uma interface para **Iniciar e Parar** o serviço com um clique, registrando e desregistrando a aplicação do *LocateRegistry* (Porta 1099).
* **Interface Acessível:** Tabuleiro desenhado em Swing com foco em acessibilidade visual (alto contraste, uso de Círculo Laranja vs Quadrado Azul).
* **Sistema de Chat:** Jogadores enviam mensagens que são propagadas remotamente para a tela do adversário em tempo real.
* **Sistema de Logs Físico:** Sempre que uma partida finaliza, o servidor RMI gera automaticamente um log (`log_partidas_rmi.txt`) registrando o resultado, quantidade de movimentos e histórico do chat.
* **Estatísticas de Partida:** Painel exibido no final do jogo via Janela Modal, mostrando movimentos e tentativas de jogadas inválidas.

---

## 🏗️ Qualidade de Código e Sincronização (PPD)

O projeto mantém as melhores práticas de sistemas distribuídos:
* **Thread-Safety (`synchronized`):** Como o RMI é inerentemente multithread (múltiplos clientes chamando métodos no mesmo objeto `GameSessionRMI` simultaneamente), o uso rigoroso de `synchronized` garante a exclusão mútua e evita *Race Conditions* ao alterar o estado do tabuleiro.
* **Separação de Camadas:** * `common`: Contratos RMI.
  * `server`: Implementação dos objetos remotos (`UnicastRemoteObject`).
  * `client`: Stubs RMI e atualização da interface gráfica via `SwingUtilities.invokeLater` (Thread-safe para UI).
  * `game`: Motor de regras isolado.

---

## 🎩 Modo de Apresentação (Cheat Code)

Para facilitar apresentações acadêmicas e testes rápidos sem a necessidade de jogar os longos 24 turnos iniciais:

* Digite **`/endgame`** no chat durante qualquer momento da partida e pressione Enviar.
* **O que acontece:** O Servidor remoto irá acionar o cenário de *Endgame*, esvaziar o tabuleiro e posicionar as peças para um **Xeque-Mate**. O Jogador 1 estará a 1 movimento de vencer a partida, demonstrando rapidamente o sistema de captura, vitória e geração de logs.

---

## 📜 Regras do Jogo Implementadas

O jogo ocorre em um tabuleiro de **5x6** e é dividido em duas fases:
1. **Fase de Colocação:** Cada jogador coloca alternadamente uma de suas 12 peças em espaços vazios. **Regra:** Proibido formar uma linha de 3 peças nesta fase.
2. **Fase de Movimentação:** Move-se as peças para espaços adjacentes (horizontal ou vertical).
3. **Captura:** Formar uma linha de exatamente 3 peças permite remover uma peça do oponente. **Regra:** Proibido formar linhas com 4 ou mais peças.
4. **Condição de Vitória:** O jogo termina quando um dos jogadores for reduzido a 2 peças.

---

## 🛠️ Tecnologias e Pré-requisitos

* **Linguagem:** Java 17+ (Compatível com JDK 21)
* **Gerenciador de Dependências:** Apache Maven 3.8+
* **Interface Gráfica:** Java Swing / AWT (`Graphics2D`)
* **Comunicação:** Java RMI (`java.rmi.*`)

---

## 🚀 Como Compilar

O projeto utiliza o `maven-shade-plugin` para gerar os executáveis `.jar` independentes.
Abra o terminal na pasta raiz do projeto (`rmi/`) e execute:

```bash
mvn clean package

1. Para mesma máquina utilizar os seguintes comandos:
   ```bash
 - Server: java -jar target/rmi-1.0-server.jar
 - Client: java -jar target/rmi-1.0-client.jar
   
2. Para máquinas distintas dentro da mesma rede ou VM:
   ```bash
 - Server: java "-Djava.rmi.server.hostname=IP_DO_SERVER" -jar target/rmi-1.0-server.jar
 - Client: java "-Djava.rmi.server.hostname=IP_DA_MAQUINA_DO_CLIENTE" -jar target/rmi-1.0-client.jar

Obs*: Além de rodar o comando no terminal, o usuário do Client deve lembrar-se de apagar o localhost e digitar o IP do Servidor na tela de login da interface gráfica para estabelecer a conexão inicial.