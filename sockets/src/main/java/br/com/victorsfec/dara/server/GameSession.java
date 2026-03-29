package br.com.victorsfec.dara.server;

import br.com.victorsfec.dara.game.Board;
import br.com.victorsfec.dara.game.Piece;
import br.com.victorsfec.dara.shared.Protocol;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import java.util.stream.Collectors;

/**
 * A Arena do Jogo. Aqui ficam as instâncias dos dois jogadores e o Tabuleiro oficial.
 * Toda jogada vinda de qualquer ClientHandler passa por aqui para ser validada.
 */
public class GameSession implements Runnable {
    private final ClientHandler player1;
    private final ClientHandler player2;
    private final Board board;
    private int currentPlayer; 

    private int p1Moves = 0, p2Moves = 0, p1Invalid = 0, p2Invalid = 0;
    private final List<String> chatHistory = new ArrayList<>();
    
    private String winnerInfo = "O jogo encerrou inesperadamente.";
    private boolean gameEnded = false;
    private String player1Name, player2Name;

    private int totalPiecesPlaced = 0;
    private boolean waitingForCapture = false;

    public GameSession(ClientHandler player1, ClientHandler player2) {
        this.player1 = player1; this.player2 = player2;
        this.board = new Board(); this.currentPlayer = 1;
        this.player1.setGameSession(this); this.player2.setGameSession(this);
    }

    @Override
    public void run() {
        if (player1.getPlayerName().equals(player2.getPlayerName())) {
            player1.setPlayerName(player1.getPlayerName() + " (1)");
            player2.setPlayerName(player2.getPlayerName() + " (2)");
        }
        this.player1Name = player1.getPlayerName(); this.player2Name = player2.getPlayerName();

        player1.sendMessage(Protocol.WELCOME + ":" + "1" + ":" + this.player1Name);
        player2.sendMessage(Protocol.WELCOME + ":" + "2" + ":" + this.player2Name);
        
        player1.sendMessage(Protocol.OPPONENT_FOUND + ":" + player2Name);
        player2.sendMessage(Protocol.OPPONENT_FOUND + ":" + player1Name);
        
        player1.sendMessage(Protocol.GAME_START); player2.sendMessage(Protocol.GAME_START);
        updateTurn();
    }

    private void broadcastScoreUpdate() {
        String msg = Protocol.UPDATE_SCORE + ":" + p1Moves + ":" + p2Moves;
        player1.sendMessage(msg); player2.sendMessage(msg);
    }

    private void switchTurn() {
        currentPlayer = (currentPlayer == 1) ? 2 : 1;
        updateTurn();
    }

    private void updateTurn() {
        if (currentPlayer == 1) {
            player1.sendMessage(Protocol.SET_TURN + ":YOUR_TURN");
            player2.sendMessage(Protocol.SET_TURN + ":OPPONENT_TURN");
        } else {
            player2.sendMessage(Protocol.SET_TURN + ":YOUR_TURN");
            player1.sendMessage(Protocol.SET_TURN + ":OPPONENT_TURN");
        }
    }

    public synchronized void processMessage(String message, ClientHandler sender) {
        if (gameEnded) return;

        String[] parts = message.split(Protocol.SEPARATOR, 2);
        String command = parts[0];
        int senderId = (sender == player1) ? 1 : 2;
        ClientHandler opponent = (sender == player1) ? player2 : player1;

        switch (command) {
            case Protocol.PLACE:
                if (senderId == currentPlayer && totalPiecesPlaced < 24 && !waitingForCapture) {
                    int r = Integer.parseInt(parts[1].split(":")[0]);
                    int c = Integer.parseInt(parts[1].split(":")[1]);

                    if (board.canPlacePiece(r, c, currentPlayer)) {
                        board.placePiece(r, c, currentPlayer);
                        totalPiecesPlaced++;
                        String msg = Protocol.PIECE_PLACED + ":" + r + ":" + c + ":" + currentPlayer;
                        player1.sendMessage(msg); player2.sendMessage(msg);
                        switchTurn();
                    } else {
                        if (senderId == 1) p1Invalid++; else p2Invalid++;
                        sender.sendMessage(Protocol.ERROR + ":Jogada inválida (Regra do 3).");
                    }
                }
                break;

            case Protocol.MOVE:
                if (senderId == currentPlayer && totalPiecesPlaced >= 24 && !waitingForCapture) {
                    String[] coords = parts[1].split(Protocol.SEPARATOR);
                    int startRow = Integer.parseInt(coords[0]), startCol = Integer.parseInt(coords[1]);
                    int endRow = Integer.parseInt(coords[2]), endCol = Integer.parseInt(coords[3]);

                    List<Point> validMoves = board.getValidMoves(startRow, startCol);
                    boolean isValid = validMoves.stream().anyMatch(p -> p.x == endRow && p.y == endCol);

                    if (isValid) {
                        board.performMove(startRow, startCol, endRow, endCol);
                        if (senderId == 1) p1Moves++; else p2Moves++;
                        broadcastScoreUpdate();

                        sender.sendMessage(Protocol.VALID_MOVE + ":" + parts[1]);
                        opponent.sendMessage(Protocol.OPPONENT_MOVED + ":" + parts[1]);

                        if (board.formsCaptureLine(endRow, endCol, currentPlayer)) {
                            waitingForCapture = true; 
                            sender.sendMessage(Protocol.MUST_REMOVE);
                        } else {
                            switchTurn();
                        }
                    } else {
                        if (senderId == 1) p1Invalid++; else p2Invalid++;
                        sender.sendMessage(Protocol.ERROR + ":Movimento inválido.");
                    }
                }
                break;

            case Protocol.REMOVE:
                if (senderId == currentPlayer && waitingForCapture) {
                    int r = Integer.parseInt(parts[1].split(":")[0]);
                    int c = Integer.parseInt(parts[1].split(":")[1]);
                    
                    Piece p = board.getPieceAt(r, c);
                    if (p != null && p.getPlayerId() != currentPlayer) {
                        board.removePiece(r, c);
                        waitingForCapture = false;
                        
                        String msg = Protocol.PIECE_REMOVED + ":" + r + ":" + c;
                        player1.sendMessage(msg); player2.sendMessage(msg);
                        
                        int opponentId = (currentPlayer == 1) ? 2 : 1;
                        if (board.isGameOver(opponentId)) {
                            winnerInfo = (currentPlayer == 1 ? player1Name : player2Name) + " venceu!";
                            endGame(sender, opponent, Protocol.VICTORY, Protocol.DEFEAT);
                        } else {
                            switchTurn();
                        }
                    } else {
                        sender.sendMessage(Protocol.ERROR + ":Selecione peça inimiga.");
                    }
                }
                break;

            case Protocol.GET_VALID_MOVES:
                if (senderId == currentPlayer && totalPiecesPlaced >= 24 && !waitingForCapture) {
                    String[] coords = parts[1].split(Protocol.SEPARATOR);
                    List<Point> moves = board.getValidMoves(Integer.parseInt(coords[0]), Integer.parseInt(coords[1]));
                    String movesStr = moves.stream().map(p -> p.x + "," + p.y).collect(Collectors.joining(";"));
                    sender.sendMessage(Protocol.VALID_MOVES_LIST + ":" + movesStr);
                }
                break;

            case Protocol.CHAT:
                // PARA A APRESENTAÇÃO
                if (parts[1].equalsIgnoreCase("/endgame")) {
                    injectEndgameScenario();
                    broadcastChat("🌟 MODO DE APRESENTAÇÃO: Cenário de Fim de Jogo montado!", 1);
                } else {
                    broadcastChat(parts[1], senderId);
                }
                break;

            case Protocol.FORFEIT:
                handleForfeit(sender); break;
        }
    }

    private void handleForfeit(ClientHandler forfeiter) {
        if (gameEnded) return;
        ClientHandler winner = (forfeiter == player1) ? player2 : player1;
        winnerInfo = winner.getPlayerName() + " ganhou pela desistência do oponente.";
        endGame(winner, forfeiter, Protocol.OPPONENT_FORFEIT, Protocol.DEFEAT + ":Você desistiu.");
    }

    public synchronized void handleDisconnect(ClientHandler disconnectedPlayer) {
        if (gameEnded) return;
        ClientHandler winner = (disconnectedPlayer == player1) ? player2 : player1;
        winnerInfo = winner.getPlayerName() + " ganhou devido a desconexão do oponente.";
        endGame(winner, disconnectedPlayer, Protocol.OPPONENT_FORFEIT, "");
    }

    private void endGame(ClientHandler winner, ClientHandler loser, String winMessage, String loseMessage) {
        if (gameEnded) return;
        gameEnded = true;
        sendGameOverStats(); 
        winner.sendMessage(winMessage);
        if (loseMessage != null && !loseMessage.isEmpty()) loser.sendMessage(loseMessage);
    }
    
    private void broadcastChat(String chatMessage, int senderId) {
        String senderName = (senderId == 1) ? player1Name : player2Name;
        String formattedMessage = Protocol.CHAT_MESSAGE + ":" + senderName + ": " + chatMessage;
        player1.sendMessage(formattedMessage); player2.sendMessage(formattedMessage);
        chatHistory.add(senderName + ": " + chatMessage);
    }
    
    private void sendGameOverStats() {
        String chatLog = String.join("|", chatHistory); 
        StringJoiner stats = new StringJoiner(Protocol.SEPARATOR);
        stats.add(winnerInfo).add(String.valueOf(p1Moves)).add(String.valueOf(p1Invalid))
             .add(String.valueOf(p2Moves)).add(String.valueOf(p2Invalid)).add(chatLog);
        
        String msg = Protocol.GAME_OVER_STATS + ":" + stats.toString();
        player1.sendMessage(msg); player2.sendMessage(msg);
    }

    /**
     * Monta o cenário de vitória instantânea burlado as regras temporariamente.
     */
    private synchronized void injectEndgameScenario() {
        if (gameEnded) return;

        // 1. Limpa qualquer peça que os jogadores já tenham colocado no tabuleiro
        for (int r = 0; r < Board.ROWS; r++) {
            for (int c = 0; c < Board.COLS; c++) {
                if (board.getPieceAt(r, c) != null) {
                    board.removePiece(r, c);
                    String msg = Protocol.PIECE_REMOVED + ":" + r + ":" + c;
                    player1.sendMessage(msg); player2.sendMessage(msg);
                }
            }
        }

        // 2. Dispara 24 mensagens fantasmas para enganar a Interface Gráfica dos Clientes.
        // Isso faz com que a interface saia da "Fase de Colocação" e entre na "Fase de Movimentação".
        int p1 = 1, p2 = 2;
        for (int r = 0; r < 4; r++) {
            for (int c = 0; c < 6; c++) {
                int owner = ((r + c) % 2 == 0) ? p1 : p2; // Padrão xadrez para não formar 3
                board.placePiece(r, c, owner);
                totalPiecesPlaced++;
                player1.sendMessage(Protocol.PIECE_PLACED + ":" + r + ":" + c + ":" + owner);
                player2.sendMessage(Protocol.PIECE_PLACED + ":" + r + ":" + c + ":" + owner);
            }
        }

        // 3. Remove tudo novamente para limpar a visão (A UI continuará na Fase 2)
        for (int r = 0; r < 4; r++) {
            for (int c = 0; c < 6; c++) {
                board.removePiece(r, c);
                String msg = Protocol.PIECE_REMOVED + ":" + r + ":" + c;
                player1.sendMessage(msg); player2.sendMessage(msg);
            }
        }

        // 4. Monta o cenário de endgame
        // O Jogador 2 fica com apenas 3 peças no fundo da tela (perdeu uma, morreu!)
        board.placePiece(4, 0, p2); sendPlaceToUI(4, 0, p2);
        board.placePiece(4, 2, p2); sendPlaceToUI(4, 2, p2);
        board.placePiece(4, 4, p2); sendPlaceToUI(4, 4, p2);

        // O Jogador 1 fica com 2 peças alinhadas (0,0 e 0,1) e a 3º peça logo abaixo pronta para pular!
        board.placePiece(0, 0, p1); sendPlaceToUI(0, 0, p1); // Casa 1A
        board.placePiece(0, 1, p1); sendPlaceToUI(0, 1, p1); // Casa 1B
        board.placePiece(1, 2, p1); sendPlaceToUI(1, 2, p1); // Casa 2C (Basta mover esta para 1C)
        board.placePiece(2, 5, p1); sendPlaceToUI(2, 5, p1); // Peça extra inofensiva

        // Dá a vez e a vitória nas mãos do Jogador 1
        currentPlayer = 1;
        waitingForCapture = false;
        updateTurn();
    }
    
    // Auxiliar para a simulação
    private void sendPlaceToUI(int r, int c, int owner) {
        String msg = Protocol.PIECE_PLACED + ":" + r + ":" + c + ":" + owner;
        player1.sendMessage(msg);
        player2.sendMessage(msg);
    }
}