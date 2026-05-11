package br.com.victorsfec.dara.server;

import br.com.victorsfec.dara.common.IServerOperations;
import javax.swing.*;
import java.awt.*;
import java.rmi.Naming;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;

/**
 * Interface Gráfica do Servidor.
 * Permite iniciar e parar o serviço RMI através de botões, semelhante à versão com Sockets.
 */
public class DaraServerRMI {

    private JFrame frame;
    private JLabel statusLabel;
    private JButton toggleButton;
    
    private boolean isRunning = false;
    private int port = 1099;
    private IServerOperations serverOps;
    private String url;

    public DaraServerRMI() {
        createGUI();
    }

    // Cria a janela igual à da imagem que você enviou
    private void createGUI() {
        frame = new JFrame("Status do Servidor Dara");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(400, 200);
        frame.setLayout(new BorderLayout(10, 10));

        // Painel central com o texto de status
        JPanel centerPanel = new JPanel(new GridBagLayout());
        statusLabel = new JLabel("Servidor parado.");
        statusLabel.setFont(new Font("Arial", Font.BOLD, 18));
        centerPanel.add(statusLabel);

        // Painel inferior com o botão
        JPanel bottomPanel = new JPanel();
        toggleButton = new JButton("Iniciar Servidor");
        toggleButton.setFont(new Font("Arial", Font.PLAIN, 14));
        toggleButton.addActionListener(e -> toggleServer());
        bottomPanel.add(toggleButton);

        frame.add(centerPanel, BorderLayout.CENTER);
        frame.add(bottomPanel, BorderLayout.SOUTH);

        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    // Decide se deve ligar ou desligar dependendo do estado atual
    private void toggleServer() {
        if (!isRunning) {
            startServer();
        } else {
            stopServer();
        }
    }

    // Lógica para INICIAR o servidor RMI
    private void startServer() {
        Object portStr = JOptionPane.showInputDialog(frame, "Digite a porta RMI:", "Configuração do Servidor", JOptionPane.QUESTION_MESSAGE, null, null, String.valueOf(port));
        if (portStr == null) return; // Se o utilizador cancelar

        try {
            port = Integer.parseInt(portStr.toString());
            url = "rmi://localhost:" + port + "/DaraServer";

            // Tenta criar o registro RMI. Se já existir (por ter ligado/desligado antes), ele ignora o erro e reaproveita.
            try {
                LocateRegistry.createRegistry(port);
            } catch (java.rmi.server.ExportException ex) {
                // Registro já está a rodar nesta máquina, não faz mal.
            }

            // Instancia a fila de espera e a publica na rede
            serverOps = new ServerOperationsImpl();
            Naming.rebind(url, serverOps);

            isRunning = true;
            statusLabel.setText("<html><center>Servidor online na porta " + port + ".<br>Aguardando jogadores...</center></html>");
            toggleButton.setText("Parar Servidor");

        } catch (Exception e) {
            JOptionPane.showMessageDialog(frame, "Erro ao iniciar o servidor:\n" + e.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Lógica para PARAR o servidor RMI
    private void stopServer() {
        try {
            // Remove o serviço da rede (os clientes deixam de o encontrar)
            if (url != null) {
                Naming.unbind(url);
            }
            // Força a paragem do objeto que estava a escutar as conexões
            if (serverOps != null) {
                UnicastRemoteObject.unexportObject(serverOps, true);
            }
            
            isRunning = false;
            statusLabel.setText("Servidor parado.");
            toggleButton.setText("Iniciar Servidor");
            
        } catch (Exception e) {
            JOptionPane.showMessageDialog(frame, "Erro ao parar o servidor:\n" + e.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        // Garante que a interface gráfica é carregada de forma segura
        SwingUtilities.invokeLater(DaraServerRMI::new);
    }
}