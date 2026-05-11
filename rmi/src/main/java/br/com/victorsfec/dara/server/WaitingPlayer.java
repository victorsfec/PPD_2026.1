package br.com.victorsfec.dara.server;

import br.com.victorsfec.dara.common.IClientCallback;

/** Classe para guardar os dados de um jogador enquanto ele está na fila de espera. */
public class WaitingPlayer {
    public String name;
    public IClientCallback callback;
    
    public WaitingPlayer(String n, IClientCallback c) { 
        name = n; 
        callback = c; 
    }
}