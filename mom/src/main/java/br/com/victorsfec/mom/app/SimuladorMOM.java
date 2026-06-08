package br.com.victorsfec.mom.app;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import br.com.victorsfec.mom.client.Cliente;
import br.com.victorsfec.mom.sensor.Sensor;

public class SimuladorMOM extends JFrame {

    private Map<String, Sensor>          sensores             = new HashMap<>();
    private Map<String, Cliente>         clientes             = new HashMap<>();
    // Controla quais tópicos cada cliente já assinou para evitar duplicatas
    private Map<String, Set<String>>     assinaturasPorCliente = new HashMap<>();

    private JTextArea                    painelDeLogs;
    private DefaultListModel<String>     modeloSensores;
    private DefaultListModel<String>     modeloClientes;
    private DefaultListModel<String>     modeloTopicos;

    public SimuladorMOM() {
        setTitle("Dashboard IoT - ActiveMQ MOM");
        setSize(1000, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        // Painel Norte — botões de ação
        JPanel painelBotoes = new JPanel(new GridLayout(1, 4, 10, 10));
        painelBotoes.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JButton btnCriarSensor     = new JButton("1. Novo Sensor");
        JButton btnCriarCliente    = new JButton("2. Novo Cliente");
        JButton btnAssinarTopico   = new JButton("3. Assinar Tópico");
        JButton btnAtualizarSensor = new JButton("4. Disparar Sensor");

        painelBotoes.add(btnCriarSensor);
        painelBotoes.add(btnCriarCliente);
        painelBotoes.add(btnAssinarTopico);
        painelBotoes.add(btnAtualizarSensor);
        add(painelBotoes, BorderLayout.NORTH);

        // Painel Leste — listas de sensores, clientes e tópicos
        JPanel painelLateral = new JPanel(new GridLayout(3, 1, 5, 10));
        painelLateral.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 10));
        painelLateral.setPreferredSize(new Dimension(300, 0));

        modeloSensores = new DefaultListModel<>();
        JScrollPane scrollSensores = new JScrollPane(new JList<>(modeloSensores));
        scrollSensores.setBorder(BorderFactory.createTitledBorder(null, " Sensores Ativos",
                TitledBorder.LEFT, TitledBorder.TOP, new Font("Arial", Font.BOLD, 12)));

        modeloClientes = new DefaultListModel<>();
        JScrollPane scrollClientes = new JScrollPane(new JList<>(modeloClientes));
        scrollClientes.setBorder(BorderFactory.createTitledBorder(null, " Clientes (Subscribers)",
                TitledBorder.LEFT, TitledBorder.TOP, new Font("Arial", Font.BOLD, 12)));

        modeloTopicos = new DefaultListModel<>();
        JScrollPane scrollTopicos = new JScrollPane(new JList<>(modeloTopicos));
        scrollTopicos.setBorder(BorderFactory.createTitledBorder(null, " Tópicos Conhecidos",
                TitledBorder.LEFT, TitledBorder.TOP, new Font("Arial", Font.BOLD, 12)));

        painelLateral.add(scrollSensores);
        painelLateral.add(scrollClientes);
        painelLateral.add(scrollTopicos);
        add(painelLateral, BorderLayout.EAST);

        // Painel Central — terminal de logs estilo dark
        painelDeLogs = new JTextArea();
        painelDeLogs.setEditable(false);
        painelDeLogs.setFont(new Font("Consolas", Font.PLAIN, 14));
        painelDeLogs.setBackground(new Color(30, 30, 30));
        painelDeLogs.setForeground(new Color(0, 255, 0));

        JScrollPane scrollPane = new JScrollPane(painelDeLogs);
        scrollPane.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.GRAY),
                "Monitoramento de Eventos da Rede",
                TitledBorder.LEFT, TitledBorder.TOP,
                new Font("Arial", Font.BOLD, 12), Color.BLACK));

        JPanel painelCentralMargin = new JPanel(new BorderLayout());
        painelCentralMargin.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 0));
        painelCentralMargin.add(scrollPane, BorderLayout.CENTER);
        add(painelCentralMargin, BorderLayout.CENTER);

        btnCriarSensor.addActionListener(e     -> criarSensor());
        btnCriarCliente.addActionListener(e    -> criarCliente());
        btnAssinarTopico.addActionListener(e   -> assinarTopico());
        btnAtualizarSensor.addActionListener(e -> atualizarSensor());

        imprimirLog("SISTEMA INICIADO. Certifique-se de que o ActiveMQ está rodando (tcp://localhost:61616).");
    }

    // Garante atualização do JTextArea sempre na EDT, mesmo quando chamado de threads do ActiveMQ
    private void imprimirLog(String texto) {
        SwingUtilities.invokeLater(() -> {
            painelDeLogs.append(texto + "\n");
            painelDeLogs.setCaretPosition(painelDeLogs.getDocument().getLength());
        });
    }

    private void criarSensor() {
        try {
            String id = JOptionPane.showInputDialog(this, "Digite o ID do Sensor (ex: S1, S2...):");
            if (id == null || id.trim().isEmpty()) return;
            id = id.trim();

            if (sensores.containsKey(id)) {
                JOptionPane.showMessageDialog(this,
                    "Já existe um sensor com o ID '" + id + "'.\nEscolha um ID diferente.",
                    "ID duplicado", JOptionPane.WARNING_MESSAGE);
                return;
            }

            String topico = JOptionPane.showInputDialog(this, "Tópico de publicação (ex: temperatura, velocidade, umidade...):");
            if (topico == null || topico.trim().isEmpty()) return;
            topico = topico.trim();

            String minStr = JOptionPane.showInputDialog(this, "Limite Mínimo:");
            String maxStr = JOptionPane.showInputDialog(this, "Limite Máximo:");

            double min = Double.parseDouble(minStr);
            double max = Double.parseDouble(maxStr);

            sensores.put(id, new Sensor(id, topico, min, max));
            modeloSensores.addElement(String.format("%s -> [%s] | Valor: --", id, topico));

            if (!modeloTopicos.contains(topico)) {
                modeloTopicos.addElement(topico);
            }

            imprimirLog(String.format(Locale.US,
                " Sensor cadastrado -> ID: %s | Tópico: %s | Limites: [%.1f - %.1f]",
                id, topico, min, max));

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                "Erro ao criar Sensor. Verifique os valores digitados.", "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void criarCliente() {
        String id = JOptionPane.showInputDialog(this, "Digite o ID do Cliente (ex: C1):");
        if (id == null || id.trim().isEmpty()) return;
        id = id.trim();

        if (clientes.containsKey(id)) {
            JOptionPane.showMessageDialog(this,
                "Já existe um cliente com o ID '" + id + "'.\nEscolha um ID diferente.",
                "ID duplicado", JOptionPane.WARNING_MESSAGE);
            return;
        }

        clientes.put(id, new Cliente(id, this::imprimirLog));
        assinaturasPorCliente.put(id, new HashSet<>());
        modeloClientes.addElement(id);
        imprimirLog(" Cliente cadastrado -> ID: " + id);
    }

    private void assinarTopico() {
        String idCli = JOptionPane.showInputDialog(this, "Qual o ID do Cliente que vai assinar?");
        if (idCli == null || !clientes.containsKey(idCli)) {
            JOptionPane.showMessageDialog(this, "Cliente não encontrado!");
            return;
        }

        final String clienteId = idCli;

        // Captura os tópicos locais na EDT antes de lançar o SwingWorker
        // (DefaultListModel não é thread-safe e não pode ser acessado em background)
        final List<String> topicosLocais = new ArrayList<>();
        for (int i = 0; i < modeloTopicos.size(); i++) {
            topicosLocais.add(modeloTopicos.get(i));
        }

        imprimirLog(" Consultando tópicos disponíveis no Broker ActiveMQ...");

        new SwingWorker<List<String>, Void>() {

            @Override
            protected List<String> doInBackground() throws Exception {
                List<String> fromBroker = clientes.get(clienteId).listarTopicosDisponiveis();
                // Inclui tópicos locais de sensores que ainda não dispararam (ausentes no Broker)
                for (String local : topicosLocais) {
                    if (!fromBroker.contains(local)) fromBroker.add(local);
                }
                return fromBroker;
            }

            @Override
            protected void done() {
                try {
                    List<String> topicos = get();

                    if (topicos.isEmpty()) {
                        JOptionPane.showMessageDialog(SimuladorMOM.this,
                            "Nenhum tópico encontrado.\nCrie um sensor e dispare-o ao menos uma vez.",
                            "Sem tópicos disponíveis", JOptionPane.INFORMATION_MESSAGE);
                        return;
                    }

                    String[] opcoes = topicos.toArray(new String[0]);
                    String topico = (String) JOptionPane.showInputDialog(
                        SimuladorMOM.this,
                        "Selecione o tópico para assinar:",
                        "Tópicos disponíveis no Broker",
                        JOptionPane.PLAIN_MESSAGE,
                        null, opcoes, opcoes[0]);

                    if (topico == null || topico.trim().isEmpty()) return;

                    Set<String> topicosDoCliente = assinaturasPorCliente.get(clienteId);
                    if (topicosDoCliente != null && topicosDoCliente.contains(topico)) {
                        JOptionPane.showMessageDialog(SimuladorMOM.this,
                            "O cliente '" + clienteId + "' já está inscrito no tópico '" + topico + "'.",
                            "Assinatura duplicada", JOptionPane.WARNING_MESSAGE);
                        return;
                    }

                    clientes.get(clienteId).assinar(topico);

                    if (topicosDoCliente != null) topicosDoCliente.add(topico);

                    // Atualiza rótulo do cliente exibindo todos os tópicos assinados
                    for (int i = 0; i < modeloClientes.size(); i++) {
                        String entry = modeloClientes.get(i);
                        if (entry.equals(clienteId) || entry.startsWith(clienteId + " ->")) {
                            modeloClientes.set(i, clienteId + " -> (Ouvindo: "
                                + String.join(", ", assinaturasPorCliente.get(clienteId)) + ")");
                            break;
                        }
                    }

                    if (!modeloTopicos.contains(topico)) modeloTopicos.addElement(topico);

                } catch (Exception e) {
                    JOptionPane.showMessageDialog(SimuladorMOM.this,
                        "Erro ao listar tópicos: " + e.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void atualizarSensor() {
        try {
            if (sensores.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                    "Nenhum sensor cadastrado.\nCrie um sensor antes de disparar.",
                    "Sem sensores", JOptionPane.WARNING_MESSAGE);
                return;
            }

            // Monta opções como "S1 — temperatura" para identificação visual no combo
            String[] ids = sensores.keySet().toArray(new String[0]);
            String[] opcoes = new String[ids.length];
            for (int i = 0; i < ids.length; i++) {
                opcoes[i] = ids[i] + " — " + sensores.get(ids[i]).parametro;
            }

            String selecao = (String) JOptionPane.showInputDialog(
                this, "Selecione o Sensor a disparar:", "Disparar Sensor",
                JOptionPane.PLAIN_MESSAGE, null, opcoes, opcoes[0]);

            if (selecao == null) return;

            // Extrai o ID puro da string "ID — tópico"
            String idSen = selecao.split(" — ")[0];

            if (!sensores.containsKey(idSen)) {
                JOptionPane.showMessageDialog(this,
                    "Sensor '" + idSen + "' não encontrado.", "Sensor inválido", JOptionPane.ERROR_MESSAGE);
                return;
            }

            Sensor sensor = sensores.get(idSen);
            String topico = sensor.parametro;

            if (!modeloTopicos.contains(topico)) {
                JOptionPane.showMessageDialog(this,
                    "O tópico '" + topico + "' do sensor '" + idSen + "' não foi encontrado.\nTente recriar o sensor.",
                    "Tópico inválido", JOptionPane.ERROR_MESSAGE);
                return;
            }

            String valorStr = JOptionPane.showInputDialog(this,
                String.format("Sensor: %s | Tópico: %s | Limites: [%.1f – %.1f]\n\nDigite a nova leitura:",
                    idSen, topico, sensor.limiteMinimo, sensor.limiteMaximo));

            if (valorStr == null || valorStr.trim().isEmpty()) return;

            double valor = Double.parseDouble(valorStr.trim().replace(",", "."));
            sensor.setValor(valor);

            boolean alerta = valor < sensor.limiteMinimo || valor > sensor.limiteMaximo;
            if (alerta) {
                imprimirLog(String.format(
                    " [ALERTA ENVIADO] Sensor '%s' -> valor %.2f fora do intervalo [%.1f – %.1f]. Publicado em '%s'.",
                    idSen, valor, sensor.limiteMinimo, sensor.limiteMaximo, topico));
            } else {
                imprimirLog(String.format(
                    " [OK] Sensor '%s' -> valor %.2f dentro dos limites. Nenhum alerta enviado.",
                    idSen, valor));
            }

            for (int i = 0; i < modeloSensores.size(); i++) {
                if (modeloSensores.get(i).startsWith(idSen + " ->")) {
                    modeloSensores.set(i, String.format("%s -> [%s] | Valor: %.1f%s",
                        idSen, topico, valor, alerta ? " ⚠" : ""));
                    break;
                }
            }

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this,
                "Valor inválido! Digite um número (ex: 37.5).", "Erro de formato", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                "Erro inesperado: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new SimuladorMOM().setVisible(true));
    }
}
