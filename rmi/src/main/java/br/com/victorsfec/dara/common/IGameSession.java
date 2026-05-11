package br.com.victorsfec.dara.common;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Ações do Jogador durante a partida.
 * Substitui os envios de string via Sockets (ex: "MOVE:X:Y").
 */
public interface IGameSession extends Remote {
    void sendPlace(int playerId, int row, int col) throws RemoteException;
    void sendMove(int playerId, int startRow, int startCol, int endRow, int endCol) throws RemoteException;
    void sendRemove(int playerId, int row, int col) throws RemoteException;
    void sendGetValidMoves(int playerId, int row, int col) throws RemoteException;
    void sendChatMessage(int playerId, String message) throws RemoteException;
    void sendForfeit(int playerId) throws RemoteException;
}