package br.com.victorsfec.dara.server;

import javax.swing.*;
import java.awt.*;
import br.com.victorsfec.dara.shared.Protocol;
import java.io.BufferedReader;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;

public class DaraServer {
    private static final List<ClientHandler> waitingClients = new ArrayList<>();
    // NOVA LISTA: Mantém o registro de todos os clientes conectados (jogando ou não)
    private static final List<ClientHandler> activeClients = new ArrayList<>();
    
    // Variáveis de controle do servidor
    private static ServerSocket serverSocket;
    private static volatile boolean isRunning = false;
    private static Thread serverThread;

    // Componentes da Interface Gráfica
    private static JFrame frame;
    private static JLabel statusLabel;
    private static JButton toggleButton;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(DaraServer::createAndShowGUI);
    }

    private static void createAndShowGUI() {
        frame = new JFrame("Status do Servidor Dara");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(450, 200);
        frame.setLayout(new BorderLayout(10, 10));

        statusLabel = new JLabel("Servidor parado.", SwingConstants.CENTER);
        statusLabel.setFont(new Font("Arial", Font.BOLD, 16));
        frame.add(statusLabel, BorderLayout.CENTER);

        toggleButton = new JButton("Iniciar Servidor");
        toggleButton.setFont(new Font("Arial", Font.BOLD, 14));
        toggleButton.addActionListener(e -> toggleServer());
        
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(toggleButton);
        frame.add(buttonPanel, BorderLayout.SOUTH);

        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private static void toggleServer() {
        if (isRunning) {
            stopServer();
        } else {
            Object portStr = JOptionPane.showInputDialog(frame, "Digite a porta para iniciar o servidor:", "Configuração do Servidor Dara", JOptionPane.QUESTION_MESSAGE, null, null, "12345");
            
            if (portStr != null) {
                try {
                    int port = Integer.parseInt(portStr.toString());
                    if (port <= 0 || port > 65535) {
                        throw new NumberFormatException();
                    }
                    startServer(port);
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(frame, "Porta inválida. O servidor não será iniciado.", "Erro", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }

    private static void startServer(int port) {
        try {
            serverSocket = new ServerSocket(port);
            isRunning = true;
            
            statusLabel.setText("Servidor online na porta " + port + ". Aguardando...");
            toggleButton.setText("Parar Servidor");

            serverThread = new Thread(() -> runServerLogic(port));
            serverThread.start();
            
            System.out.println("Dara Server em execução na porta " + port + "...");
        } catch (IOException e) {
            JOptionPane.showMessageDialog(frame, "Não foi possível iniciar o servidor na porta " + port + ".\nEla pode já estar em uso.", "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    // NOVO MÉTODO: Permite que um cliente seja removido das listas ao desconectar
    public static void removeClient(ClientHandler client) {
        synchronized (activeClients) {
            activeClients.remove(client);
        }
        synchronized (waitingClients) {
            waitingClients.remove(client);
        }
    }

    private static void stopServer() {
        isRunning = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        // NOVO: Derruba todos os jogadores que estão em partidas ativas
        synchronized (activeClients) {
            for (ClientHandler client : activeClients) {
                client.sendMessage(Protocol.ERROR + Protocol.SEPARATOR + "O servidor foi encerrado pelo administrador. A partida foi interrompida.");
                client.shutdown();
            }
            activeClients.clear();
        }
        
        synchronized (waitingClients) {
            waitingClients.clear();
        }

        statusLabel.setText("Servidor parado.");
        toggleButton.setText("Iniciar Servidor");
        System.out.println("Dara Server parado.");
    }

    private static void runServerLogic(int port) {
        try {
            while (isRunning) {
                Socket clientSocket = serverSocket.accept(); 
                System.out.println("Novo cliente conectado: " + clientSocket.getInetAddress());

                ClientHandler clientHandler = new ClientHandler(clientSocket);
                
                // Registra o cliente na lista global assim que ele conecta
                synchronized (activeClients) {
                    activeClients.add(clientHandler);
                }

                try {
                    BufferedReader in = clientHandler.getInputStream();
                    String nameLine = in.readLine();

                    if (nameLine != null && nameLine.startsWith(Protocol.SET_NAME)) {
                        String playerName = nameLine.split(Protocol.SEPARATOR, 2)[1];
                        clientHandler.setPlayerName(playerName);
                        System.out.println("SERVER: Nome do jogador definido como: " + playerName);

                        synchronized (waitingClients) {
                            waitingClients.add(clientHandler);
                            clientHandler.start(); 
                            
                            if (waitingClients.size() >= 2) {
                                ClientHandler player1 = waitingClients.remove(0);
                                ClientHandler player2 = waitingClients.remove(0);
                                System.out.println("Pareando jogadores '" + player1.getPlayerName() + "' e '" + player2.getPlayerName() + "'.");
                                
                                GameSession gameSession = new GameSession(player1, player2);
                                new Thread(gameSession).start();
                            }
                        }
                    } else {
                        System.err.println("Erro: Primeira mensagem do cliente não foi SET_NAME. Desconectando.");
                        clientSocket.close();
                    }
                } catch (IOException e) {
                    System.err.println("Erro ao comunicar com o cliente. Desconectando. " + e.getMessage());
                    clientSocket.close();
                }
            }
        } catch (SocketException e) {
            if (isRunning) {
                System.err.println("Erro no socket do servidor: " + e.getMessage());
            }
        } catch (IOException e) {
            System.err.println("Erro no servidor Dara: " + e.getMessage());
            e.printStackTrace();
        }
    }
}