package br.com.victorsfec.dara.client;

import br.com.victorsfec.dara.common.IClientCallback;
import br.com.victorsfec.dara.common.IGameSession;
import br.com.victorsfec.dara.common.IServerOperations;

import javax.swing.*;
import java.awt.GridLayout;
import java.rmi.Naming;
import java.rmi.server.UnicastRemoteObject;

/**
 * Ponto de partida do Cliente.
 * Pede os dados de conexão, acha o servidor via RMI e encaminha comandos da Interface.
 */
public class DaraClientRMI {
    private IGameSession gameSessionStub;
    private ClientCallbackImpl callbackImpl;
    private final GameFrame gameFrame;
    private String playerName, serverAddress;
    private int serverPort;
    private volatile boolean gameIsOver = false;

    public DaraClientRMI() {
        gameFrame = new GameFrame(this);

        JTextField nameField = new JTextField("Jogador");
        JTextField ipField = new JTextField("localhost");
        JTextField portField = new JTextField("1099");

        JPanel panel = new JPanel(new GridLayout(0, 1));
        panel.add(new JLabel("Seu Nome:")); 
        panel.add(nameField);
        panel.add(new JLabel("Endereço IP do Servidor:")); 
        panel.add(ipField);
        panel.add(new JLabel("Porta RMI do Servidor:")); 
        panel.add(portField);

        int result = JOptionPane.showConfirmDialog(null, panel, "Conectar ao Jogo Dara (RMI)", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            this.playerName = nameField.getText();
            this.serverAddress = ipField.getText();
            
            if (this.playerName.trim().isEmpty() || this.serverAddress.trim().isEmpty()) {
                System.exit(0);
            }
            
            try {
                this.serverPort = Integer.parseInt(portField.getText());
                gameFrame.setPlayerName(this.playerName);
                gameFrame.setVisible(true);
                connect();
            } catch (NumberFormatException e) { 
                System.exit(0); 
            }
        } else { 
            System.exit(0); 
        }
    }

    // Localiza o servidor e regista a escuta
    public void connect() {
        if (gameIsOver) return;
        try {
            String url = "rmi://" + serverAddress + ":" + serverPort + "/DaraServer";
            IServerOperations serverStub = (IServerOperations) Naming.lookup(url);
            
            callbackImpl = new ClientCallbackImpl(gameFrame, this);
            IClientCallback callbackStub = (IClientCallback) UnicastRemoteObject.toStub(callbackImpl);
            
            serverStub.connect(playerName, callbackStub);
            gameFrame.updateStatus("Conectado ao RMI. Aguardando oponente...");
        } catch (Exception e) {
            gameFrame.updateStatus("Falha na conexão.");
            JOptionPane.showMessageDialog(gameFrame, "Erro: " + e.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void setGameSession(IGameSession gameSession, String newPlayerName) {
        this.gameSessionStub = gameSession;
        this.playerName = newPlayerName;
        gameFrame.setPlayerName(newPlayerName);
    }

    public void setGameIsOver() {
        this.gameIsOver = true;
        try { 
            if(callbackImpl != null) UnicastRemoteObject.unexportObject(callbackImpl, true); 
        } catch (Exception e) {}
    }

    // --- MÉTODOS DE AÇÃO (Chamados pelos cliques no GameFrame e enviam as requisições RMI) ---

    public void sendPlace(int row, int col) {
        try { 
            if(gameSessionStub != null) gameSessionStub.sendPlace(gameFrame.getPlayerId(), row, col); 
        } catch (Exception e) {}
    }
    
    public void sendMove(int startRow, int startCol, int endRow, int endCol) {
        try { 
            if(gameSessionStub != null) gameSessionStub.sendMove(gameFrame.getPlayerId(), startRow, startCol, endRow, endCol); 
        } catch (Exception e) {}
    }
    
    public void sendRemove(int row, int col) {
        try { 
            if(gameSessionStub != null) gameSessionStub.sendRemove(gameFrame.getPlayerId(), row, col); 
        } catch (Exception e) {}
    }
    
    public void sendChatMessage(String message) {
        try { 
            if(gameSessionStub != null) gameSessionStub.sendChatMessage(gameFrame.getPlayerId(), message); 
        } catch (Exception e) {}
    }
    
    public void sendForfeit() {
        try { 
            if(gameSessionStub != null) gameSessionStub.sendForfeit(gameFrame.getPlayerId()); 
        } catch (Exception e) {}
    }
    
    public void sendGetValidMoves(int row, int col) {
        try { 
            if(gameSessionStub != null) gameSessionStub.sendGetValidMoves(gameFrame.getPlayerId(), row, col); 
        } catch (Exception e) {}
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(DaraClientRMI::new);
    }
}