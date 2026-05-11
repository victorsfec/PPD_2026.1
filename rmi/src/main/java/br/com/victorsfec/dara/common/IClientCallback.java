package br.com.victorsfec.dara.common;

import java.awt.Point;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

/**
 * Padrão Callback: O Servidor invoca estes métodos remotamente 
 * para atualizar a Interface Gráfica do Cliente em tempo real.
 */
public interface IClientCallback extends Remote {
    void startGame(IGameSession gameSession, int playerId, String playerName, String opponentName) throws RemoteException;
    void updateScore(int p1Moves, int p2Moves) throws RemoteException;
    void receivePiecePlaced(int row, int col, int ownerId) throws RemoteException;
    void receiveMustRemove() throws RemoteException;
    void receivePieceRemoved(int row, int col) throws RemoteException;
    void receiveMove(int startRow, int startCol, int endRow, int endCol) throws RemoteException;
    void receiveValidMovesList(List<Point> moves) throws RemoteException;
    void setTurn(boolean isMyTurn) throws RemoteException;
    void receiveInfo(String message) throws RemoteException;
    void receiveError(String message) throws RemoteException;
    void receiveChatMessage(String message) throws RemoteException;
    void notifyGameEnd(String message, String title, int messageType, String stats) throws RemoteException;
}