package br.com.victorsfec.dara.server;

import br.com.victorsfec.dara.game.Board;
import br.com.victorsfec.dara.game.Piece;
import br.com.victorsfec.dara.shared.Protocol;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import java.util.stream.Collectors;

public class GameSession implements Runnable {
    private final ClientHandler player1;
    private final ClientHandler player2;
    private final Board board;
    private int currentPlayer;

    private int player1MoveCount = 0;
    private int player2MoveCount = 0;
    private int player1InvalidAttempts = 0;
    private int player2InvalidAttempts = 0;
    private final List<String> chatHistory = new ArrayList<>();
    private String winnerInfo = "O jogo encerrou inesperadamente.";
    private boolean gameEnded = false;

    private String player1Name;
    private String player2Name;

    // Regras do Dara
    private int totalPiecesPlaced = 0;
    private boolean waitingForCapture = false;

    public GameSession(ClientHandler player1, ClientHandler player2) {
        this.player1 = player1;
        this.player2 = player2;
        this.board = new Board();
        this.currentPlayer = 1;

        this.player1.setGameSession(this);
        this.player2.setGameSession(this);
    }

    @Override
    public void run() {
        if (player1.getPlayerName().equals(player2.getPlayerName())) {
            String originalName = player1.getPlayerName();
            player1.setPlayerName(originalName + " (1)");
            player2.setPlayerName(originalName + " (2)");
        }
        
        this.player1Name = player1.getPlayerName();
        this.player2Name = player2.getPlayerName();

        player1.sendMessage(Protocol.WELCOME + Protocol.SEPARATOR + "1" + Protocol.SEPARATOR + this.player1Name);
        player2.sendMessage(Protocol.WELCOME + Protocol.SEPARATOR + "2" + Protocol.SEPARATOR + this.player2Name);
        
        player1.sendMessage(Protocol.OPPONENT_FOUND + Protocol.SEPARATOR + player2Name);
        player2.sendMessage(Protocol.OPPONENT_FOUND + Protocol.SEPARATOR + player1Name);
        player1.sendMessage(Protocol.GAME_START);
        player2.sendMessage(Protocol.GAME_START);
        updateTurn();
    }

    private void broadcastScoreUpdate() {
        String message = Protocol.UPDATE_SCORE + Protocol.SEPARATOR + player1MoveCount + Protocol.SEPARATOR + player2MoveCount;
        player1.sendMessage(message);
        player2.sendMessage(message);
    }

    private void switchTurn() {
        currentPlayer = (currentPlayer == 1) ? 2 : 1;
        updateTurn();
    }

    private void updateTurn() {
        if (currentPlayer == 1) {
            player1.sendMessage(Protocol.SET_TURN + Protocol.SEPARATOR + "YOUR_TURN");
            player2.sendMessage(Protocol.SET_TURN + Protocol.SEPARATOR + "OPPONENT_TURN");
        } else {
            player2.sendMessage(Protocol.SET_TURN + Protocol.SEPARATOR + "YOUR_TURN");
            player1.sendMessage(Protocol.SET_TURN + Protocol.SEPARATOR + "OPPONENT_TURN");
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
                        
                        String msg = Protocol.PIECE_PLACED + Protocol.SEPARATOR + r + Protocol.SEPARATOR + c + Protocol.SEPARATOR + currentPlayer;
                        player1.sendMessage(msg);
                        player2.sendMessage(msg);
                        
                        switchTurn();
                    } else {
                        if (senderId == 1) player1InvalidAttempts++; else player2InvalidAttempts++;
                        
                        // MENSAGEM ALTERADA AQUI CONFORME O SEU PEDIDO:
                        sender.sendMessage(Protocol.ERROR + Protocol.SEPARATOR + "Jogada inválida. Na fase de colocação não é permitido alinhar 3 peças. E também após colocar as 12 peças não é permitido alinhar 4 peças na (horizontal/vertical)");
                    }
                }
                break;

            case Protocol.MOVE:
                if (senderId == currentPlayer && totalPiecesPlaced == 24 && !waitingForCapture) {
                    String[] coords = parts[1].split(Protocol.SEPARATOR);
                    int startRow = Integer.parseInt(coords[0]);
                    int startCol = Integer.parseInt(coords[1]);
                    int endRow = Integer.parseInt(coords[2]);
                    int endCol = Integer.parseInt(coords[3]);

                    List<Point> validMoves = board.getValidMoves(startRow, startCol);
                    boolean isValid = validMoves.stream().anyMatch(p -> p.x == endRow && p.y == endCol);

                    if (isValid) {
                        board.performMove(startRow, startCol, endRow, endCol);
                        if (senderId == 1) player1MoveCount++; else player2MoveCount++;
                        broadcastScoreUpdate();

                        String moveMsg = Protocol.VALID_MOVE + Protocol.SEPARATOR + parts[1];
                        sender.sendMessage(moveMsg);
                        opponent.sendMessage(Protocol.OPPONENT_MOVED + Protocol.SEPARATOR + parts[1]);

                        if (board.formsCaptureLine(endRow, endCol, currentPlayer)) {
                            waitingForCapture = true;
                            sender.sendMessage(Protocol.MUST_REMOVE);
                        } else {
                            switchTurn();
                        }
                    } else {
                        if (senderId == 1) player1InvalidAttempts++; else player2InvalidAttempts++;
                        sender.sendMessage(Protocol.ERROR + Protocol.SEPARATOR + "Movimento inválido. Lembre-se: não é permitido alinhar 4 peças (horizontal/vertical) e você só pode mover para casas adjacentes vazias!");
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
                        
                        String msg = Protocol.PIECE_REMOVED + Protocol.SEPARATOR + r + Protocol.SEPARATOR + c;
                        player1.sendMessage(msg);
                        player2.sendMessage(msg);
                        
                        int opponentId = (currentPlayer == 1) ? 2 : 1;
                        if (board.isGameOver(opponentId)) {
                            winnerInfo = (currentPlayer == 1 ? player1Name : player2Name) + " venceu por eliminar as peças do adversário!";
                            endGame(sender, opponent, Protocol.VICTORY, Protocol.DEFEAT);
                        } else {
                            switchTurn();
                        }
                    } else {
                        sender.sendMessage(Protocol.ERROR + Protocol.SEPARATOR + "Selecione uma peça válida do OPONENTE.");
                    }
                }
                break;

            case Protocol.GET_VALID_MOVES:
                if (senderId == currentPlayer && totalPiecesPlaced == 24 && !waitingForCapture) {
                    String[] coords = parts[1].split(Protocol.SEPARATOR);
                    int r = Integer.parseInt(coords[0]);
                    int c = Integer.parseInt(coords[1]);
                    
                    List<Point> moves = board.getValidMoves(r, c);
                    String movesStr = moves.stream().map(p -> p.x + "," + p.y).collect(Collectors.joining(";"));
                    sender.sendMessage(Protocol.VALID_MOVES_LIST + Protocol.SEPARATOR + movesStr);
                }
                break;

            case Protocol.CHAT:
                broadcastChat(parts[1], senderId);
                break;

            case Protocol.FORFEIT:
                handleForfeit(sender);
                break;
        }
    }

    private void handleForfeit(ClientHandler forfeiter) {
        if (gameEnded) return;
        ClientHandler winner = (forfeiter == player1) ? player2 : player1;
        winnerInfo = winner.getPlayerName() + " ganhou pela desistência do oponente.";
        endGame(winner, forfeiter, Protocol.OPPONENT_FORFEIT, Protocol.DEFEAT + Protocol.SEPARATOR + "Você desistiu da partida.");
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
        if (loseMessage != null && !loseMessage.isEmpty()) {
            loser.sendMessage(loseMessage);
        }
    }
    
    private void broadcastChat(String chatMessage, int senderId) {
        String senderName = (senderId == 1) ? player1Name : player2Name;
        String formattedMessage = Protocol.CHAT_MESSAGE + Protocol.SEPARATOR + senderName + ": " + chatMessage;
        player1.sendMessage(formattedMessage);
        player2.sendMessage(formattedMessage);
        chatHistory.add(senderName + ": " + chatMessage);
    }
    
    private void sendGameOverStats() {
        String chatLog = String.join("|", chatHistory);
        StringJoiner stats = new StringJoiner(Protocol.SEPARATOR);
        
        stats.add(winnerInfo)
             .add(String.valueOf(player1MoveCount))
             .add(String.valueOf(player1InvalidAttempts))
             .add(String.valueOf(player2MoveCount))
             .add(String.valueOf(player2InvalidAttempts))
             .add(chatLog);
        
        String message = Protocol.GAME_OVER_STATS + Protocol.SEPARATOR + stats.toString();
        player1.sendMessage(message);
        player2.sendMessage(message);
    }
}