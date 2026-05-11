package br.com.victorsfec.dara.server;

import br.com.victorsfec.dara.common.IClientCallback;
import br.com.victorsfec.dara.common.IServerOperations;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;

/**
 * Fila de espera. Pareia dois jogadores assim que se conectam e inicia a partida.
 */
public class ServerOperationsImpl extends UnicastRemoteObject implements IServerOperations {
    
    private static final List<WaitingPlayer> waitingClients = new ArrayList<>();

    public ServerOperationsImpl() throws RemoteException {
        super();
    }

    @Override
    public void connect(String playerName, IClientCallback callback) throws RemoteException {
        try {
            WaitingPlayer newPlayer = new WaitingPlayer(playerName, callback);
            
            synchronized (waitingClients) {
                waitingClients.add(newPlayer);
                callback.receiveInfo("Conectado. Aguardando oponente...");

                if (waitingClients.size() >= 2) {
                    WaitingPlayer p1 = waitingClients.remove(0);
                    WaitingPlayer p2 = waitingClients.remove(0);

                    GameSessionRMI gameSession = new GameSessionRMI(p1, p2);
                    new Thread(gameSession).start();
                }
            }
        } catch (Exception e) {
            try { callback.receiveError("Erro do servidor."); } catch (RemoteException re) {}
        }
    }
}