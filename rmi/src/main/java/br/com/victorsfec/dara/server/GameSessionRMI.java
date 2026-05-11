package br.com.victorsfec.dara.server;

import br.com.victorsfec.dara.common.IClientCallback;
import br.com.victorsfec.dara.common.IGameSession;
import br.com.victorsfec.dara.game.Board;
import br.com.victorsfec.dara.game.Piece;

import java.awt.Point;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.StringJoiner;

/**
 * O motor central da partida distribuída.
 * Processa as jogadas, valida as regras e gera um log físico ao final da partida.
 */
public class GameSessionRMI extends UnicastRemoteObject implements IGameSession, Runnable {
    private final IClientCallback player1;
    private final IClientCallback player2;
    private final Board board;
    private int currentPlayer = 1; 

    private int p1Moves = 0, p2Moves = 0, p1Invalid = 0, p2Invalid = 0;
    private final List<String> chatHistory = new ArrayList<>();
    
    private String winnerInfo = "O jogo encerrou inesperadamente.";
    private boolean gameEnded = false;
    private String player1Name, player2Name;

    private int totalPiecesPlaced = 0;
    private boolean waitingForCapture = false;

    public GameSessionRMI(WaitingPlayer p1, WaitingPlayer p2) throws RemoteException {
        super();
        this.player1 = p1.callback;
        this.player2 = p2.callback;
        this.player1Name = p1.name;
        this.player2Name = p2.name;
        this.board = new Board();
    }

    @Override
    public void run() {
        try {
            // Garante que nomes iguais fiquem diferenciados
            if (player1Name.equals(player2Name)) {
                player1Name += " (1)";
                player2Name += " (2)";
            }
            player1.startGame(this, 1, player1Name, player2Name);
            player2.startGame(this, 2, player2Name, player1Name);
            updateTurn();
        } catch (RemoteException e) {
            handleDisconnect(null);
        }
    }

    @Override
    public synchronized void sendPlace(int playerId, int row, int col) throws RemoteException {
        if (gameEnded) return;
        IClientCallback sender = (playerId == 1) ? player1 : player2;

        if (playerId != currentPlayer || totalPiecesPlaced >= 24 || waitingForCapture) {
            sender.receiveError("Ação não permitida neste momento."); 
            return;
        }

        if (board.canPlacePiece(row, col, currentPlayer)) {
            board.placePiece(row, col, currentPlayer);
            totalPiecesPlaced++;
            
            player1.receivePiecePlaced(row, col, currentPlayer);
            player2.receivePiecePlaced(row, col, currentPlayer);
            switchTurn();
        } else {
            if (playerId == 1) p1Invalid++; else p2Invalid++;
            sender.receiveError("Jogada inválida (Regra do 3 na fase de colocação).");
        }
    }

    @Override
    public synchronized void sendMove(int playerId, int startRow, int startCol, int endRow, int endCol) throws RemoteException {
        if (gameEnded) return;
        IClientCallback sender = (playerId == 1) ? player1 : player2;

        if (playerId != currentPlayer || totalPiecesPlaced < 24 || waitingForCapture) {
            sender.receiveError("Ação não permitida."); 
            return;
        }

        List<Point> validMoves = board.getValidMoves(startRow, startCol);
        boolean isValid = validMoves.stream().anyMatch(p -> p.x == endRow && p.y == endCol);

        if (isValid) {
            board.performMove(startRow, startCol, endRow, endCol);
            if (playerId == 1) p1Moves++; else p2Moves++;
            
            player1.updateScore(p1Moves, p2Moves);
            player2.updateScore(p1Moves, p2Moves);
            player1.receiveMove(startRow, startCol, endRow, endCol);
            player2.receiveMove(startRow, startCol, endRow, endCol);

            if (board.formsCaptureLine(endRow, endCol, currentPlayer)) {
                waitingForCapture = true; 
                sender.receiveMustRemove();
            } else {
                switchTurn();
            }
        } else {
            if (playerId == 1) p1Invalid++; else p2Invalid++;
            sender.receiveError("Movimento inválido.");
        }
    }

    @Override
    public synchronized void sendRemove(int playerId, int row, int col) throws RemoteException {
        if (gameEnded || playerId != currentPlayer || !waitingForCapture) return;
        IClientCallback sender = (playerId == 1) ? player1 : player2;
        
        Piece p = board.getPieceAt(row, col);
        if (p != null && p.getPlayerId() != currentPlayer) {
            board.removePiece(row, col);
            waitingForCapture = false;
            
            player1.receivePieceRemoved(row, col);
            player2.receivePieceRemoved(row, col);
            
            int opponentId = (currentPlayer == 1) ? 2 : 1;
            if (board.isGameOver(opponentId)) {
                winnerInfo = (currentPlayer == 1 ? player1Name : player2Name) + " venceu!";
                IClientCallback opponent = (playerId == 1) ? player2 : player1;
                endGame(sender, opponent, "Parabéns, você ganhou!", "Você perdeu a partida.");
            } else {
                switchTurn();
            }
        } else {
            sender.receiveError("Selecione peça inimiga.");
        }
    }

    @Override
    public synchronized void sendGetValidMoves(int playerId, int row, int col) throws RemoteException {
        if (gameEnded || playerId != currentPlayer || totalPiecesPlaced < 24 || waitingForCapture) return;
        IClientCallback sender = (playerId == 1) ? player1 : player2;
        sender.receiveValidMovesList(board.getValidMoves(row, col));
    }

    @Override
    public synchronized void sendChatMessage(int playerId, String message) throws RemoteException {
        if (message.equalsIgnoreCase("/endgame")) {
            injectEndgameScenario();
            broadcastChat("🌟 MODO DE APRESENTAÇÃO: Cenário de Fim de Jogo montado!", 1);
        } else {
            broadcastChat(message, playerId);
        }
    }

    @Override
    public synchronized void sendForfeit(int playerId) throws RemoteException {
        if (gameEnded) return;
        IClientCallback forfeiter = (playerId == 1) ? player1 : player2;
        IClientCallback winner = (forfeiter == player1) ? player2 : player1;
        winnerInfo = (winner == player1 ? player1Name : player2Name) + " ganhou pela desistência do oponente.";
        endGame(winner, forfeiter, "Seu oponente desistiu. Você ganhou!", "Você desistiu.");
    }

    public synchronized void handleDisconnect(IClientCallback disconnectedPlayer) {
        if (gameEnded) return;
        IClientCallback winner = (disconnectedPlayer == player1) ? player2 : player1;
        winnerInfo = (winner == player1 ? player1Name : player2Name) + " ganhou devido a desconexão do oponente.";
        endGame(winner, disconnectedPlayer, "Seu oponente desconectou. Você ganhou!", "");
    }

    /**
     * Finaliza a partida, avisa os jogadores e GRAVA O LOG NO FICHEIRO
     */
    private void endGame(IClientCallback winner, IClientCallback loser, String winMessage, String loseMessage) {
        if (gameEnded) return;
        gameEnded = true;
        
        String stats = generateStats();

        // =====================================================================
        // BLOCO DE CRIAÇÃO DO LOG DE PARTIDA
        // =====================================================================
        try {
            // O parâmetro 'true' no FileWriter faz com que os logs antigos não sejam apagados (append)
            FileWriter fw = new FileWriter("log_partidas_rmi.txt", true);
            PrintWriter pw = new PrintWriter(fw);
            
            pw.println("========================================");
            pw.println("DATA/HORA: " + new Date());
            pw.println("PARTIDA: " + player1Name + " vs " + player2Name);
            pw.println("RESULTADO: " + winnerInfo);
            pw.println("ESTATÍSTICAS (P1_Mov:P1_Erro:P2_Mov:P2_Erro): " + stats);
            pw.println("========================================\n");
            
            pw.close();
            System.out.println("Log da partida salvo com sucesso em 'log_partidas_rmi.txt'");
        } catch (Exception e) {
            System.err.println("Erro ao gravar log no disco: " + e.getMessage());
        }
        // =====================================================================

        // Dispara o alerta para fechar a tela dos clientes
        try { 
            if(winner != null) winner.notifyGameEnd(winMessage, "Vitória", 1, stats); 
        } catch (RemoteException e) {}
        
        try { 
            if(loser != null && !loseMessage.isEmpty()) loser.notifyGameEnd(loseMessage, "Derrota", 2, stats); 
        } catch (RemoteException e) {}
    }

    private void updateTurn() {
        try { player1.setTurn(currentPlayer == 1); } catch (RemoteException e) { handleDisconnect(player1); }
        try { player2.setTurn(currentPlayer == 2); } catch (RemoteException e) { handleDisconnect(player2); }
    }

    private void switchTurn() {
        currentPlayer = (currentPlayer == 1) ? 2 : 1;
        updateTurn();
    }
    
    private void broadcastChat(String msg, int senderId) {
        String senderName = (senderId == 1) ? player1Name : player2Name;
        String formattedMessage = senderName + ": " + msg;
        chatHistory.add(formattedMessage);
        
        try { player1.receiveChatMessage(formattedMessage); } catch (RemoteException e) { handleDisconnect(player1); }
        try { player2.receiveChatMessage(formattedMessage); } catch (RemoteException e) { handleDisconnect(player2); }
    }

    private String generateStats() {
        String chatLog = String.join("|", chatHistory); 
        StringJoiner stats = new StringJoiner(":");
        stats.add(winnerInfo).add(String.valueOf(p1Moves)).add(String.valueOf(p1Invalid))
             .add(String.valueOf(p2Moves)).add(String.valueOf(p2Invalid)).add(chatLog);
        return stats.toString();
    }

    private synchronized void injectEndgameScenario() {
        if (gameEnded) return;
        try {
            for (int r = 0; r < Board.ROWS; r++) {
                for (int c = 0; c < Board.COLS; c++) {
                    if (board.getPieceAt(r, c) != null) {
                        board.removePiece(r, c);
                        player1.receivePieceRemoved(r, c);
                        player2.receivePieceRemoved(r, c);
                    }
                }
            }

            int p1 = 1, p2 = 2;
            for (int r = 0; r < 4; r++) {
                for (int c = 0; c < 6; c++) {
                    int owner = ((r + c) % 2 == 0) ? p1 : p2;
                    board.placePiece(r, c, owner);
                    totalPiecesPlaced++;
                    player1.receivePiecePlaced(r, c, owner);
                    player2.receivePiecePlaced(r, c, owner);
                }
            }

            for (int r = 0; r < 4; r++) {
                for (int c = 0; c < 6; c++) {
                    board.removePiece(r, c);
                    player1.receivePieceRemoved(r, c);
                    player2.receivePieceRemoved(r, c);
                }
            }

            board.placePiece(4, 0, p2); sendPlaceToUI(4, 0, p2);
            board.placePiece(4, 2, p2); sendPlaceToUI(4, 2, p2);
            board.placePiece(4, 4, p2); sendPlaceToUI(4, 4, p2);

            board.placePiece(0, 0, p1); sendPlaceToUI(0, 0, p1);
            board.placePiece(0, 1, p1); sendPlaceToUI(0, 1, p1);
            board.placePiece(1, 2, p1); sendPlaceToUI(1, 2, p1);
            board.placePiece(2, 5, p1); sendPlaceToUI(2, 5, p1);

            currentPlayer = 1;
            waitingForCapture = false;
            updateTurn();
        } catch (RemoteException e) { handleDisconnect(null); }
    }
    
    private void sendPlaceToUI(int r, int c, int owner) throws RemoteException {
        player1.receivePiecePlaced(r, c, owner);
        player2.receivePiecePlaced(r, c, owner);
    }
}