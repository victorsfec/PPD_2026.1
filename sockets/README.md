# 🎮 Jogo Dara - Multiplayer Distribuído (Java Sockets)

Este projeto é uma implementação do jogo de tabuleiro africano **Dara**, desenvolvido com arquitetura **Cliente-Servidor** utilizando **Java Sockets** e interface gráfica em **Java Swing**. O projeto foi criado para a disciplina de Programação Paralela e Distribuída (PPD) - 2026.1.

## 🌟 Principais Funcionalidades

* **Arquitetura Cliente-Servidor:** O servidor gerencia as conexões simultâneas via Sockets, pareia os jogadores em sessões (2 jogadores por partida) e valida as regras do jogo de forma centralizada.
* **Interface Acessível:** Tabuleiro desenhado com foco em acessibilidade visual (alto contraste, uso de formas diferentes como Círculo Laranja vs Quadrado Azul).
* **Sistema de Chat:** Jogadores podem trocar mensagens de texto em tempo real durante a partida através de broadcast do servidor.
* **Sistema de Logs Automático:** O cliente gera arquivos de log diários documentando eventos de rede, portas locais/remotas e troca de mensagens.
* **Tratamento de Desconexões:** Se um jogador desistir ou a conexão cair (SocketException/IOException), a sessão é encerrada de forma limpa e o oponente é declarado vencedor automaticamente.
* **Estatísticas de Partida:** Ao final do jogo, um painel exibe o número de movimentos, tentativas de jogadas inválidas e o histórico do chat.

---

## 🏗️ Qualidade de Código e Arquitetura (PPD)

O projeto foi desenvolvido focando nas melhores práticas de engenharia de software e sistemas distribuídos:
* **Thread-Safety e Sincronização:** Uso de blocos `synchronized` no Servidor (`GameSession`) para garantir a exclusão mútua e evitar Condições de Corrida (Race Conditions) quando múltiplos clientes enviam comandos simultaneamente.
* **Clean Code:** Refatoração de métodos, uso de operadores ternários e simplificação de blocos lógicos para um código conciso e de fácil manutenção.
* **Separação de Responsabilidades:** O `Board.java` atua como motor de regras isolado no backend, enquanto o `GameFrame.java` lida apenas com a renderização (`Graphics2D`) validada pelo servidor.
* **Documentação Humanizada:** O código-fonte está amplamente comentado com foco nas decisões arquiteturais (o *porquê* de usar Threads, Sockets e filas), facilitando a leitura e a avaliação técnica.

---

## 🎩 Modo de Apresentação (Cheat Code)

Para facilitar apresentações acadêmicas e testes rápidos sem a necessidade de jogar uma partida inteira (que pode levar muito tempo), foi implementado um comando secreto:

* Digite **`/endgame`** no chat durante qualquer momento da partida e pressione Enviar.
* **O que acontece:** O Servidor irá limpar o tabuleiro virtualmente e montar instantaneamente um cenário de **Xeque-Mate**. 
* O Jogador 1 (quem digitou o comando) terá a vez e estará a apenas 1 movimento de formar uma trinca e capturar a peça letal do Jogador 2, encerrando o jogo em segundos e demonstrando todas as telas de vitória/derrota/estatísticas.

---

## 📜 Regras do Jogo Implementadas

O jogo ocorre em um tabuleiro de **5x6** e é dividido em duas fases:
1. **Fase de Colocação:** Cada jogador coloca alternadamente uma de suas 12 peças em espaços vazios. **Regra de validação:** Não é permitido formar uma linha de 3 peças nesta fase.
2. **Fase de Movimentação:** Os jogadores movem suas peças para espaços adjacentes (horizontal ou vertical).
3. **Captura:** Formar uma linha de exatamente 3 peças permite ao jogador remover uma peça do oponente do tabuleiro. **Regra:** Não é permitido formar linhas com 4 ou mais peças.
4. **Condição de Vitória:** O jogo termina quando um dos jogadores for reduzido a 2 peças (impossibilitado de formar uma linha de 3).

---

## 🛠️ Tecnologias e Pré-requisitos

* **Linguagem:** Java 17+ (Compatível com JDK 21)
* **Gerenciador de Dependências e Build:** Apache Maven 3.8+
* **Interface Gráfica:** Java Swing / AWT (`Graphics2D`)
* **Comunicação:** Sockets TCP/IP puros e nativos do Java (`java.net.Socket` e `java.net.ServerSocket`)

---

## 🚀 Como Compilar

O projeto utiliza o `maven-shade-plugin` para empacotar o código e gerar dois executáveis `.jar` separados (um para o cliente e outro para o servidor), contendo os respectivos manifestos.

Abra o terminal na pasta raiz do projeto (`sockets/`) e execute:

```bash
mvn clean package