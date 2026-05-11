package br.com.victorsfec.dara.client;

import br.com.victorsfec.dara.game.Board;
import br.com.victorsfec.dara.game.Piece;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Interface Gráfica Principal do Dara.
 * Desenha o tabuleiro dinamicamente e captura os cliques do rato.
 */
@SuppressWarnings("serial")
public class GameFrame extends JFrame {
    private final DaraClientRMI client; 
    private final BoardPanel boardPanel;
    private final JTextArea chatArea;
    private final JTextField chatInput;
    private final JLabel statusLabel;
    private final Board board;
    
    private int selectedRow = -1;
    private int selectedCol = -1;
    private int playerId;
    private boolean myTurn = false;
    private String playerName = "Jogador";
    private String opponentName = "Oponente";

    private JLabel player1ScoreLabel;
    private JLabel player2ScoreLabel;
    private List<Point> validMoves = new ArrayList<>();

    private boolean isPlacementPhase = true;
    private boolean mustCapture = false;
    private int totalPiecesPlaced = 0; 

    public String getPlayerName() { return this.playerName; }
    public void setPlayerName(String name) { this.playerName = name; setTitle("Dara Game - " + this.playerName); }
    public void setOpponentName(String name) { this.opponentName = name; updateScores(0, 0); }
    public void setPlayerId(int id) { this.playerId = id; }
    public int getPlayerId() { return this.playerId; }
    public void updateStatus(String text) { statusLabel.setText(text); }
    public void addChatMessage(String message) { chatArea.append(message + "\n"); }

    public GameFrame(DaraClientRMI client) {
        this.client = client;
        this.board = new Board();
        setTitle("Dara Game"); 

        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                int choice = JOptionPane.showConfirmDialog(GameFrame.this, "Tem certeza que deseja desistir?", "Aviso", JOptionPane.YES_NO_OPTION);
                if (choice == JOptionPane.YES_OPTION) {
                    client.sendForfeit();
                    try { Thread.sleep(200); } catch (InterruptedException e) {}
                    System.exit(0);
                }
            }
        });

        setLayout(new BorderLayout(10, 10));
        boardPanel = new BoardPanel();
        add(boardPanel, BorderLayout.CENTER);

        JPanel eastPanel = new JPanel();
        eastPanel.setLayout(new BoxLayout(eastPanel, BoxLayout.Y_AXIS));
        eastPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 10));

        statusLabel = new JLabel("Conecte a um servidor...", SwingConstants.CENTER);
        statusLabel.setFont(new Font("Arial", Font.BOLD, 14));
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        eastPanel.add(statusLabel);
        eastPanel.add(Box.createRigidArea(new Dimension(0, 10)));

        JPanel scorePanel = new JPanel(new GridLayout(2, 1, 0, 5));
        scorePanel.setBorder(BorderFactory.createTitledBorder("Placar de Movimentos"));
        player1ScoreLabel = new JLabel(playerName + ": 0");
        player2ScoreLabel = new JLabel(opponentName + ": 0");
        scorePanel.add(player1ScoreLabel);
        scorePanel.add(player2ScoreLabel);
        eastPanel.add(scorePanel);
        eastPanel.add(Box.createRigidArea(new Dimension(0, 10)));

        chatArea = new JTextArea(15, 25);
        chatArea.setEditable(false);
        eastPanel.add(new JScrollPane(chatArea));
        eastPanel.add(Box.createRigidArea(new Dimension(0, 5)));

        JPanel chatInputPanel = new JPanel(new BorderLayout());
        chatInput = new JTextField();
        JButton sendButton = new JButton("Enviar");
        sendButton.addActionListener(this::sendChat);
        chatInput.addActionListener(this::sendChat);
        chatInputPanel.add(chatInput, BorderLayout.CENTER);
        chatInputPanel.add(sendButton, BorderLayout.EAST);
        eastPanel.add(chatInputPanel);

        add(eastPanel, BorderLayout.EAST);

        JPanel bottomPanel = new JPanel();
        JButton forfeitButton = new JButton("Desistir");
        forfeitButton.addActionListener(action -> {
            if (JOptionPane.showConfirmDialog(this, "Desistir?", "Aviso", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                client.sendForfeit();
            }
        });
        bottomPanel.add(forfeitButton);
        add(bottomPanel, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(null);
    }

    public void setMustCapture(boolean mustCapture) {
        this.mustCapture = mustCapture;
        if (mustCapture) {
            updateStatus("Seu turno: Capture uma peça do oponente!");
        }
    }

    public void updateBoardPlacement(int row, int col, int pId) {
        board.placePiece(row, col, pId);
        totalPiecesPlaced++;
        if (totalPiecesPlaced >= 24) {
            isPlacementPhase = false;
            updateStatus("Fase de movimentação iniciada!");
            validMoves.clear();
        }
        boardPanel.repaint();
    }

    public void updateBoardRemoval(int row, int col) {
        board.removePiece(row, col);
        this.mustCapture = false;
        boardPanel.repaint();
    }

    public void updateScores(int p1Moves, int p2Moves) {
        if (playerId == 1) {
            player1ScoreLabel.setText("Você (" + playerName + "): " + p1Moves);
            player2ScoreLabel.setText("Oponente (" + opponentName + "): " + p2Moves);
        } else {
            player1ScoreLabel.setText("Oponente (" + opponentName + "): " + p1Moves);
            player2ScoreLabel.setText("Você (" + playerName + "): " + p2Moves);
        }
    }

    public void showValidMoves(List<Point> moves) {
        this.validMoves = moves;
        boardPanel.repaint();
    }

    public void setMyTurn(boolean myTurn) {
        this.myTurn = myTurn;
        if(!mustCapture) {
            updateStatus(myTurn ? "Seu turno, " + playerName + "." : "Turno de " + opponentName + ".");
        }
        
        validMoves.clear();
        if (myTurn && isPlacementPhase && !mustCapture) {
            for (int r = 0; r < Board.ROWS; r++) {
                for (int c = 0; c < Board.COLS; c++) {
                    if (board.canPlacePiece(r, c, playerId)) {
                        validMoves.add(new Point(r, c));
                    }
                }
            }
        }
        boardPanel.repaint();
    }

    public void updateBoard(int startRow, int startCol, int endRow, int endCol) {
        board.performMove(startRow, startCol, endRow, endCol);
        this.selectedRow = -1;
        this.selectedCol = -1;
        validMoves.clear(); 
        boardPanel.repaint();
    }

    private void sendChat(ActionEvent e) {
        String message = chatInput.getText().trim();
        if (!message.isEmpty()) {
            client.sendChatMessage(message);
            chatInput.setText("");
        }
    }

    public void closeApplication() { dispose(); }

    /** Subclasse que desenha a matriz, peças e regista a lógica do rato */
    private class BoardPanel extends JPanel {
        private static final int MARGIN = 40; 

        BoardPanel() {
            setPreferredSize(new Dimension(660, 560));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (!myTurn) return;

                    int bWidth = getWidth() - MARGIN * 2;
                    int bHeight = getHeight() - MARGIN * 2;
                    int cellW = bWidth / Board.COLS; 
                    int cellH = bHeight / Board.ROWS; 

                    int mX = e.getX() - MARGIN;
                    int mY = e.getY() - MARGIN;

                    if (mX < 0 || mX >= bWidth || mY < 0 || mY >= bHeight) return;

                    int col = mX / cellW;
                    int row = mY / cellH;
                    Piece clickedPiece = board.getPieceAt(row, col);

                    // Regras de clique para Captura
                    if (mustCapture) {
                        if (clickedPiece != null && clickedPiece.getPlayerId() != playerId) {
                            client.sendRemove(row, col);
                        } else {
                            JOptionPane.showMessageDialog(GameFrame.this, "Selecione uma peça do OPONENTE!");
                        }
                        return;
                    }

                    // Regras de clique para Colocação
                    if (isPlacementPhase) {
                        if (clickedPiece == null) {
                            if (board.canPlacePiece(row, col, playerId)) {
                                client.sendPlace(row, col); 
                                validMoves.clear();
                            } else {
                                JOptionPane.showMessageDialog(GameFrame.this, "Jogada inválida (Regra do 3).");
                            }
                        }
                        return;
                    }

                    // Regras de clique para Movimentação
                    if (selectedRow == -1) { 
                        if (clickedPiece != null && clickedPiece.getPlayerId() == playerId) {
                            selectedRow = row; 
                            selectedCol = col;
                            client.sendGetValidMoves(row, col); 
                        }
                    } else { 
                        boolean isValidTarget = validMoves.stream().anyMatch(p -> p.x == row && p.y == col);
                        if (isValidTarget) {
                            client.sendMove(selectedRow, selectedCol, row, col);
                        }
                        selectedRow = -1; 
                        selectedCol = -1; 
                        validMoves.clear();
                    }
                    repaint();
                }
            });
        }
        
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int bW = getWidth() - MARGIN * 2;
            int bH = getHeight() - MARGIN * 2;
            int cW = bW / Board.COLS;
            int cH = bH / Board.ROWS;

            // Fundo
            g2d.setColor(Color.WHITE); 
            g2d.fillRect(0, 0, getWidth(), getHeight());
            
            g2d.setPaint(new RadialGradientPaint(new Point2D.Float(getWidth()/2f, getHeight()/2f), Math.max(getWidth(), getHeight()), new float[]{0f, 1f}, new Color[]{new Color(245, 245, 240), new Color(190, 195, 200)}));
            g2d.fillRect(MARGIN, MARGIN, bW, bH);

            // Grade
            g2d.setColor(new Color(60, 60, 65)); 
            g2d.setStroke(new BasicStroke(3)); 
            for (int r = 0; r <= Board.ROWS; r++) {
                g2d.drawLine(MARGIN, MARGIN + r * cH, MARGIN + bW, MARGIN + r * cH);
            }
            for (int c = 0; c <= Board.COLS; c++) {
                g2d.drawLine(MARGIN + c * cW, MARGIN, MARGIN + c * cW, MARGIN + bH);
            }

            // Letras/Números
            g2d.setFont(new Font("Arial", Font.BOLD, 14)); 
            g2d.setColor(new Color(40, 40, 40)); 
            String[] cols = {"A", "B", "C", "D", "E", "F"};
            for (int i=0; i<Board.COLS; i++) {
                g2d.drawString(cols[i], MARGIN + i * cW + (cW / 2) - g2d.getFontMetrics().stringWidth(cols[i]) / 2, MARGIN - 10);
            }
            for (int i=0; i<Board.ROWS; i++) {
                g2d.drawString(String.valueOf(i+1), MARGIN - 15 - g2d.getFontMetrics().stringWidth(String.valueOf(i+1)), MARGIN + i * cH + (cH / 2) + g2d.getFontMetrics().getAscent() / 2);
            }

            // Peças
            int pR = (Math.min(cW, cH) - (Math.min(cW, cH)/8)*2) / 2;
            int sO = (int)(pR * 2 * 0.85) / 2;

            for (int r = 0; r < Board.ROWS; r++) {
                for (int c = 0; c < Board.COLS; c++) {
                    Piece p = board.getPieceAt(r, c);
                    if (p != null) {
                        int cx = MARGIN + c * cW + (cW / 2);
                        int cy = MARGIN + r * cH + (cH / 2);
                        boolean isP1 = p.getPlayerId() == 1; 

                        // Sombra
                        g2d.setColor(new Color(0, 0, 0, 80));
                        if (isP1) {
                            g2d.fillOval(cx - pR + 3, cy - pR + 3, pR*2, pR*2);
                        } else {
                            g2d.fillRect(cx - sO + 3, cy - sO + 3, sO*2, sO*2);
                        }

                        if (isP1) {
                            // Círculo Laranja
                            g2d.setPaint(new RadialGradientPaint(new Point2D.Float(cx, cy), pR, new float[]{0f, 0.7f, 1f}, new Color[]{new Color(240, 150, 70), new Color(213, 94, 0), new Color(140, 50, 0)}));
                            g2d.fillOval(cx - pR, cy - pR, pR*2, pR*2);
                            g2d.setColor(new Color(80, 30, 0)); 
                            g2d.setStroke(new BasicStroke(1)); 
                            g2d.drawOval(cx - pR, cy - pR, pR*2, pR*2);
                        } else {
                            // Quadrado Azul
                            g2d.setPaint(new LinearGradientPaint(new Point2D.Float(cx-sO, cy-sO), new Point2D.Float(cx+sO, cy+sO), new float[]{0f, 0.5f, 1f}, new Color[]{new Color(60, 150, 210), new Color(0, 114, 178), new Color(0, 60, 100)}));
                            g2d.fillRect(cx - sO, cy - sO, sO*2, sO*2);
                            g2d.setColor(new Color(0, 30, 60)); 
                            g2d.setStroke(new BasicStroke(1)); 
                            g2d.drawRect(cx - sO, cy - sO, sO*2, sO*2);
                        }
                    }
                }
            }

            // Destaque de Seleção
            if (selectedRow != -1 && selectedCol != -1) {
                int cx = MARGIN + selectedCol * cW + (cW / 2);
                int cy = MARGIN + selectedRow * cH + (cH / 2);
                g2d.setColor(new Color(240, 228, 66)); 
                g2d.setStroke(new BasicStroke(4));
                
                if (board.getPieceAt(selectedRow, selectedCol).getPlayerId() == 2) {
                    g2d.drawRect(cx - sO - 4, cy - sO - 4, sO*2 + 8, sO*2 + 8);
                } else {
                    g2d.drawOval(cx - pR - 4, cy - pR - 4, (pR + 4) * 2, (pR + 4) * 2);
                }
            }

            // Bolinhas verdes de movimento válido
            for (Point m : validMoves) {
                int cx = MARGIN + m.y * cW + (cW / 2);
                int cy = MARGIN + m.x * cH + (cH / 2);
                
                g2d.setColor(new Color(86, 180, 233));
                g2d.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0, new float[]{10, 10}, 0));
                g2d.drawOval(cx - pR, cy - pR, pR*2, pR*2);
                g2d.fillOval(cx - 6, cy - 6, 12, 12);
            }
        }
    }
}