package br.com.victorsfec.dara.common;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Ponto de entrada do Servidor.
 * O cliente usa esta interface para entrar na fila de espera.
 */
public interface IServerOperations extends Remote {
    void connect(String playerName, IClientCallback callback) throws RemoteException;
}