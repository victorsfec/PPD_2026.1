package br.com.victorsfec.dara.client;

import br.com.victorsfec.dara.common.IClientCallback;
import br.com.victorsfec.dara.common.IGameSession;
import java.awt.Point;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/**
 * O "Ouvinte" local do cliente.
 * Recebe as notificações remotas do Servidor RMI e atualiza os painéis do Swing em segurança.
 */
public class ClientCallbackImpl extends UnicastRemoteObject implements IClientCallback {
    private final GameFrame gameFrame;
    private final DaraClientRMI client;

    public ClientCallbackImpl(GameFrame frame, DaraClientRMI client) throws RemoteException {
        super();
        this.gameFrame = frame;
        this.client = client;
    }

    @Override
    public void startGame(IGameSession gameSession, int playerId, String playerName, String opponentName) throws RemoteException {
        client.setGameSession(gameSession, playerName);
        SwingUtilities.invokeLater(() -> {
            gameFrame.setPlayerId(playerId);
            gameFrame.setOpponentName(opponentName);
            gameFrame.updateStatus("Oponente encontrado: " + opponentName + ". Iniciando partida...");
        });
    }

    @Override
    public void updateScore(int p1Moves, int p2Moves) throws RemoteException {
        SwingUtilities.invokeLater(() -> gameFrame.updateScores(p1Moves, p2Moves));
    }

    @Override
    public void receivePiecePlaced(int row, int col, int ownerId) throws RemoteException {
        SwingUtilities.invokeLater(() -> gameFrame.updateBoardPlacement(row, col, ownerId));
    }

    @Override
    public void receiveMove(int startRow, int startCol, int endRow, int endCol) throws RemoteException {
        SwingUtilities.invokeLater(() -> gameFrame.updateBoard(startRow, startCol, endRow, endCol));
    }

    @Override
    public void receiveMustRemove() throws RemoteException {
        SwingUtilities.invokeLater(() -> gameFrame.setMustCapture(true));
    }

    @Override
    public void receivePieceRemoved(int row, int col) throws RemoteException {
        SwingUtilities.invokeLater(() -> gameFrame.updateBoardRemoval(row, col));
    }

    @Override
    public void receiveValidMovesList(List<Point> moves) throws RemoteException {
        SwingUtilities.invokeLater(() -> gameFrame.showValidMoves(moves));
    }

    @Override
    public void setTurn(boolean isMyTurn) throws RemoteException {
        SwingUtilities.invokeLater(() -> gameFrame.setMyTurn(isMyTurn));
    }

    @Override
    public void receiveInfo(String message) throws RemoteException {
        SwingUtilities.invokeLater(() -> gameFrame.updateStatus(message));
    }

    @Override
    public void receiveError(String message) throws RemoteException {
        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(gameFrame, message, "Erro", JOptionPane.ERROR_MESSAGE));
    }

    @Override
    public void receiveChatMessage(String message) throws RemoteException {
        SwingUtilities.invokeLater(() -> gameFrame.addChatMessage(message));
    }

    @Override
    public void notifyGameEnd(String message, String title, int messageType, String stats) throws RemoteException {
        SwingUtilities.invokeLater(() -> {
            client.setGameIsOver();
            JOptionPane.showMessageDialog(gameFrame, message, title, messageType);
            new ResultsDialog(gameFrame, stats).setVisible(true);
            gameFrame.closeApplication();
            System.exit(0);
        });
    }
}