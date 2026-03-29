package br.com.victorsfec.dara.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHandler extends Thread {
    private final Socket clientSocket;
    private PrintWriter out;  
    private BufferedReader in; 
    private GameSession gameSession; 
    private String playerName = "Jogador Anônimo"; 

    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
    }

    public void setPlayerName(String name) { this.playerName = name; }
    public String getPlayerName() { return playerName; }
    public void setGameSession(GameSession gameSession) { this.gameSession = gameSession; }

    public BufferedReader getInputStream() throws IOException {
        if (in == null) in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        return in;
    }

    public PrintWriter getOutputStream() throws IOException {
        if (out == null) out = new PrintWriter(clientSocket.getOutputStream(), true);
        return out;
    }

    @Override
    public void run() {
        try {
            getOutputStream();
            getInputStream();
            if (gameSession == null) sendMessage("INFO:Aguardando oponente...");
            
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                if (gameSession != null) {
                    gameSession.processMessage(inputLine, this);
                }
            }
        } catch (IOException e) {
            System.out.println("Cliente desconectado: " + playerName + " (" + clientSocket.getInetAddress() + ")");
            if (gameSession != null) {
                gameSession.handleDisconnect(this);
            }
        } finally {
            try {
                if (clientSocket != null && !clientSocket.isClosed()) {
                    clientSocket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            // NOVO: Remove o cliente da lista global do servidor
            DaraServer.removeClient(this);
        }
    }

    public void sendMessage(String message) {
        if (out != null) {
            System.out.println("SERVER -> " + playerName + ": " + message);
            out.println(message);
        }
    }

    public void shutdown() {
        try {
            if (clientSocket != null && !clientSocket.isClosed()) {
                clientSocket.close();
            }
        } catch (IOException e) {
            System.err.println("Erro durante o desligamento do cliente: " + e.getMessage());
        }
    }
}