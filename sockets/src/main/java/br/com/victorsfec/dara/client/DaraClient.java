package br.com.victorsfec.dara.client;

import br.com.victorsfec.dara.shared.Protocol;
import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class DaraClient {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private final GameFrame gameFrame;
    private String lastGameStats;
    private volatile boolean gameIsOver = false;

    private String playerName;
    private String serverAddress;
    private int serverPort;
    private volatile boolean isTryingToReconnect = false;
    
    private final String logFileName;

    public DaraClient() {
        // --- ADAPTAÇÃO PARA CRIAÇÃO DA PASTA LOG ---
        String executionPath = "";
        try {
            executionPath = new File(DaraClient.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParent();
            executionPath += File.separator; 
        } catch (Exception e) {
            System.err.println("Não foi possível determinar o caminho de execução. O log será salvo no diretório padrão.");
        }

        // Verifica se a pasta 'logs' existe, senão cria
        File logFolder = new File(executionPath + "logs");
        if (!logFolder.exists()) {
            logFolder.mkdirs();
        }

        // Monta o nome do arquivo dentro da pasta 'logs'
        String fileTimestamp = new SimpleDateFormat("dd_MM_yyyy").format(new Date());
        // Usamos logFolder.getAbsolutePath() para garantir o caminho correto em qualquer SO
        this.logFileName = logFolder.getAbsolutePath() + File.separator + "dara_client_log_" + fileTimestamp + ".txt";
        // ------------------------------------------

        gameFrame = new GameFrame(this);

        JTextField nameField = new JTextField("Jogador");
        JTextField ipField = new JTextField("localhost");
        JTextField portField = new JTextField("12345");

        JPanel panel = new JPanel(new GridLayout(0, 1));
        panel.add(new JLabel("Seu Nome:"));
        panel.add(nameField);
        panel.add(new JLabel("Endereço IP do Servidor:"));
        panel.add(ipField);
        panel.add(new JLabel("Porta do Servidor:"));
        panel.add(portField);

        int result = JOptionPane.showConfirmDialog(null, panel, "Conectar ao Jogo Dara",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            this.playerName = nameField.getText();
            this.serverAddress = ipField.getText();
            String portStr = portField.getText();

            if (this.playerName.trim().isEmpty() || this.serverAddress.trim().isEmpty() || portStr.trim().isEmpty()) {
                JOptionPane.showMessageDialog(null, "Todos os campos devem ser preenchidos.", "Erro de Entrada", JOptionPane.ERROR_MESSAGE);
                System.exit(0);
                return;
            }

            try {
                this.serverPort = Integer.parseInt(portStr);
                if (this.serverPort <= 0 || this.serverPort > 65535) {
                    throw new NumberFormatException();
                }
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(null, "A porta deve ser um número válido entre 1 e 65535.", "Erro de Porta", JOptionPane.ERROR_MESSAGE);
                System.exit(0);
                return;
            }
            
            gameFrame.setPlayerName(this.playerName);
            gameFrame.setVisible(true);
            connect(this.playerName, this.serverAddress, this.serverPort);
        } else {
            System.exit(0);
        }
    }

    private void logToFileAndConsole(String message) {
        System.out.println(message);
        
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        String logEntry = "[" + timestamp + "] " + message;

        try (FileWriter fw = new FileWriter(this.logFileName, true);
             BufferedWriter bw = new BufferedWriter(fw);
             PrintWriter pw = new PrintWriter(bw)) {
             
            pw.println(logEntry);
            
        } catch (IOException e) {
            System.err.println("Falha ao escrever no arquivo de log: " + e.getMessage());
        }
    }

    private void attemptReconnection() {
        if (isTryingToReconnect) return;
        isTryingToReconnect = true;
        new Thread(() -> {
            while (!Thread.currentThread().isInterrupted() && isTryingToReconnect) {
                try {
                    SwingUtilities.invokeLater(() -> gameFrame.updateStatus("Conexão perdida. Tentando reconectar..."));
                    shutdown();

                    Thread.sleep(5000);

                    connect(playerName, serverAddress, serverPort);

                    SwingUtilities.invokeLater(() -> gameFrame.updateStatus("Reconectado! Aguardando oponente..."));
                    isTryingToReconnect = false;
                    break;
                } catch (Exception e) {
                    logToFileAndConsole("Falha na reconexão, tentando novamente em 5s...");
                } 
            }
        }).start();
    }

    private class ServerListener implements Runnable {
        @Override
        public void run() {
            try {
                String serverMessage;
                while ((serverMessage = in.readLine()) != null) {
                    logToFileAndConsole("CLIENT (" + gameFrame.getPlayerName() + "): Mensagem recebida: " + serverMessage);
                    
                    final String messageForUI = serverMessage;
                    SwingUtilities.invokeLater(() -> processServerMessage(messageForUI));
                }
            } catch (IOException e) {
                if (!gameIsOver && !isTryingToReconnect) {
                    logToFileAndConsole("Conexão com o servidor perdida. Iniciando tentativa de reconexão.");
                    attemptReconnection();
                }
            }
        }

        private void processServerMessage(String message) {
            if (gameIsOver) return;

            String[] parts = message.split(Protocol.SEPARATOR, 2);
            String command = parts[0];
            String data = parts.length > 1 ? parts[1] : "";

            switch (command) {
                case Protocol.PIECE_PLACED:
                    String[] placeCoords = data.split(Protocol.SEPARATOR);
                    if (placeCoords.length >= 3) {
                        int r = Integer.parseInt(placeCoords[0]);
                        int c = Integer.parseInt(placeCoords[1]);
                        int pId = Integer.parseInt(placeCoords[2]);
                        gameFrame.updateBoardPlacement(r, c, pId);
                    }
                    break;

                case Protocol.MUST_REMOVE:
                    gameFrame.setMustCapture(true);
                    break;

                case Protocol.PIECE_REMOVED:
                    String[] removeCoords = data.split(Protocol.SEPARATOR);
                    if (removeCoords.length >= 2) {
                        gameFrame.updateBoardRemoval(Integer.parseInt(removeCoords[0]), Integer.parseInt(removeCoords[1]));
                    }
                    break;

                case Protocol.UPDATE_SCORE:
                    String[] scores = data.split(Protocol.SEPARATOR);
                    if (scores.length >= 2) {
                        gameFrame.updateScores(Integer.parseInt(scores[0]), Integer.parseInt(scores[1]));
                    }
                    break;
                case Protocol.VALID_MOVE:
                case Protocol.OPPONENT_MOVED:
                    String[] coords = data.split(Protocol.SEPARATOR);
                    if (coords.length >= 4) {
                        gameFrame.updateBoard(Integer.parseInt(coords[0]), Integer.parseInt(coords[1]), Integer.parseInt(coords[2]), Integer.parseInt(coords[3]));
                    }
                    break;
                case Protocol.VALID_MOVES_LIST:
                    List<Point> moves = new ArrayList<>();
                    if (!data.isEmpty()) {
                        String[] movePairs = data.split(";");
                        for (String pair : movePairs) {
                            String[] moveCoords = pair.split(",");
                            moves.add(new Point(Integer.parseInt(moveCoords[0]), Integer.parseInt(moveCoords[1])));
                        }
                    }
                    gameFrame.showValidMoves(moves);
                    break;
                case Protocol.VICTORY:
                    handleGameEnd("Parabéns, você ganhou!", "Fim de jogo", JOptionPane.INFORMATION_MESSAGE);
                    break;
                case Protocol.DEFEAT:
                    String defeatMessage = !data.isEmpty() ? data : "Você perdeu a partida.";
                    handleGameEnd(defeatMessage, "Fim de jogo", JOptionPane.WARNING_MESSAGE);
                    break;
                case Protocol.OPPONENT_FORFEIT:
                    handleGameEnd("Seu oponente desistiu. Você ganhou!", "Vitória", JOptionPane.INFORMATION_MESSAGE);
                    break;
                case Protocol.GAME_OVER_STATS:
                    lastGameStats = data;
                    break;
                case Protocol.WELCOME:
                    String[] welcomeParts = data.split(Protocol.SEPARATOR, 2);
                    gameFrame.setPlayerId(Integer.parseInt(welcomeParts[0]));
                    if (welcomeParts.length > 1) {
                        String newPlayerName = welcomeParts[1];
                        DaraClient.this.playerName = newPlayerName;
                        gameFrame.setPlayerName(newPlayerName);
                    }
                    break;
                case Protocol.OPPONENT_FOUND:
                    String opponentName = data.isEmpty() ? "Oponente" : data;
                    gameFrame.setOpponentName(opponentName);
                    gameFrame.updateStatus("Oponente encontrado: " + opponentName + ". Iniciando partida de Dara...");
                    break;
                case Protocol.SET_TURN:
                    gameFrame.setMyTurn("YOUR_TURN".equals(data));
                    break;
                case Protocol.ERROR:
                    JOptionPane.showMessageDialog(gameFrame, data, "Erro", JOptionPane.ERROR_MESSAGE);
                    if (data.contains("O servidor foi encerrado pelo administrador")) {
                        gameIsOver = true; 
                        shutdown(); 
                        System.exit(0); 
                    }
                    break;
                case Protocol.INFO:
                    gameFrame.updateStatus(data);
                    break;
                case Protocol.CHAT_MESSAGE:
                    gameFrame.addChatMessage(data);
                    break;
            }
        }

        private void handleGameEnd(String message, String title, int messageType) {
            if (gameIsOver) return;
            gameIsOver = true;

            JOptionPane.showMessageDialog(gameFrame, message, title, messageType);
            if (lastGameStats != null) {
                new ResultsDialog(gameFrame, lastGameStats).setVisible(true);
            }
            gameFrame.closeApplication();
            shutdown();
        }
    }

    public void shutdown() {
        try {
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            logToFileAndConsole("CLIENT: Erro durante fechamento do socket: " + e.getMessage());
        }
    }

    public void connect(String playerName, String serverAddress, int port) {
        try {
            socket = new Socket(serverAddress, port);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            String clientIp = socket.getLocalAddress().getHostAddress();
            int clientPort = socket.getLocalPort();
            String srvIp = socket.getInetAddress().getHostAddress();
            int srvPort = socket.getPort();

            logToFileAndConsole("=========================================");
            logToFileAndConsole("Nova Conexão Estabelecida!");
            logToFileAndConsole("Local (Cliente) -> IP: " + clientIp + " | Porta: " + clientPort);
            logToFileAndConsole("Remoto (Servidor)-> IP: " + srvIp + " | Porta: " + srvPort);
            logToFileAndConsole("=========================================");

            out.println(Protocol.SET_NAME + Protocol.SEPARATOR + playerName);
            new Thread(new ServerListener()).start();

            gameFrame.updateStatus("Conectado. Aguardando por um oponente...");
        } catch (IOException e) {
            JOptionPane.showMessageDialog(gameFrame, "Não foi possível se conectar ao servidor.", "Erro de conexão", JOptionPane.ERROR_MESSAGE);
            if (!isTryingToReconnect) attemptReconnection();
        }
    }

    public void sendPlace(int row, int col) {
        if (out != null) out.println(Protocol.PLACE + Protocol.SEPARATOR + row + Protocol.SEPARATOR + col);
    }

    public void sendRemove(int row, int col) {
        if (out != null) out.println(Protocol.REMOVE + Protocol.SEPARATOR + row + Protocol.SEPARATOR + col);
    }

    public void sendMove(int startRow, int startCol, int endRow, int endCol) {
        if (out != null) out.println(Protocol.MOVE + Protocol.SEPARATOR + startRow + Protocol.SEPARATOR + startCol + Protocol.SEPARATOR + endRow + Protocol.SEPARATOR + endCol);
    }
    
    public void sendChatMessage(String message) {
        if (out != null) out.println(Protocol.CHAT + Protocol.SEPARATOR + message);
    }

    public void sendForfeit() {
        if (out != null) out.println(Protocol.FORFEIT);
    }

    public void sendGetValidMoves(int row, int col) {
        if (out != null) out.println(Protocol.GET_VALID_MOVES + Protocol.SEPARATOR + row + Protocol.SEPARATOR + col);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(DaraClient::new);
    }
}