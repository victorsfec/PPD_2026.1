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
 * Interface Gráfica Principal.
 * O método paintComponent desta classe foi mantido exatamente como o original, 
 * preservando todo o belíssimo design acessível e as sombras do tabuleiro.
 */
@SuppressWarnings("serial")
public class GameFrame extends JFrame {
    private final DaraClient client; 
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

    public GameFrame(DaraClient client) {
        this.client = client;
        this.board = new Board();
        setTitle("Dara Game - Design Acessível"); 

        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                int choice = JOptionPane.showConfirmDialog(GameFrame.this, "Você tem certeza que deseja desistir?", "Desistência", JOptionPane.YES_NO_OPTION);
                if (choice == JOptionPane.YES_OPTION) {
                    client.sendForfeit();
                    try { Thread.sleep(200); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
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

        statusLabel = new JLabel("Conecte a um servidor para iniciar.", SwingConstants.CENTER);
        statusLabel.setFont(new Font("Arial", Font.BOLD, 14));
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        eastPanel.add(statusLabel);
        eastPanel.add(Box.createRigidArea(new Dimension(0, 10)));

        JPanel scorePanel = new JPanel(new GridLayout(2, 1, 0, 5));
        scorePanel.setBorder(BorderFactory.createTitledBorder("Placar de Movimentos"));
        player1ScoreLabel = new JLabel(playerName + ": 0");
        player2ScoreLabel = new JLabel(opponentName + ": 0");
        player1ScoreLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        player2ScoreLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        scorePanel.add(player1ScoreLabel);
        scorePanel.add(player2ScoreLabel);
        eastPanel.add(scorePanel);
        eastPanel.add(Box.createRigidArea(new Dimension(0, 10)));

        chatArea = new JTextArea(15, 25);
        chatArea.setEditable(false);
        JScrollPane chatScrollPane = new JScrollPane(chatArea);
        eastPanel.add(chatScrollPane);
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
        JButton forfeitButton = new JButton("Desistir do jogo");
        forfeitButton.addActionListener(action -> {
            int choice = JOptionPane.showConfirmDialog(this, "Você tem certeza que deseja desistir?", "Desistência", JOptionPane.YES_NO_OPTION);
            if (choice == JOptionPane.YES_OPTION) client.sendForfeit();
        });
        bottomPanel.add(forfeitButton);
        add(bottomPanel, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(null);
    }

    public void setMustCapture(boolean mustCapture) {
        this.mustCapture = mustCapture;
        if (mustCapture) updateStatus("Seu turno: Capture uma peça do oponente!");
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
        
        // Calcula e mostra os locais válidos de colocação (Fase 1) automaticamente
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

    private class BoardPanel extends JPanel {
        private static final int MARGIN = 40; 

        BoardPanel() {
            setPreferredSize(new Dimension(660, 560));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (!myTurn) return;

                    int boardWidth = getWidth() - MARGIN * 2;
                    int boardHeight = getHeight() - MARGIN * 2;
                    
                    int cellWidth = boardWidth / Board.COLS; 
                    int cellHeight = boardHeight / Board.ROWS; 

                    int mouseX = e.getX() - MARGIN;
                    int mouseY = e.getY() - MARGIN;

                    if (mouseX < 0 || mouseX >= boardWidth || mouseY < 0 || mouseY >= boardHeight) return;

                    int col = mouseX / cellWidth;
                    int row = mouseY / cellHeight;

                    Piece clickedPiece = board.getPieceAt(row, col);

                    if (mustCapture) {
                        if (clickedPiece != null && clickedPiece.getPlayerId() != playerId) {
                            client.sendRemove(row, col);
                        } else {
                            JOptionPane.showMessageDialog(GameFrame.this, "Selecione uma peça do OPONENTE para remover!");
                        }
                        return;
                    }

                    if (isPlacementPhase) {
                        if (clickedPiece == null) {
                            if (board.canPlacePiece(row, col, playerId)) {
                                client.sendPlace(row, col);
                                validMoves.clear();
                            } else {
                                JOptionPane.showMessageDialog(GameFrame.this, "Jogada inválida! Não é permitido formar linha de 3 na fase de colocação.");
                            }
                        }
                        return;
                    }

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

            int boardWidth = getWidth() - MARGIN * 2;
            int boardHeight = getHeight() - MARGIN * 2;
            int cellWidth = boardWidth / Board.COLS;
            int cellHeight = boardHeight / Board.ROWS;

            // 1. Fundo Branco Básico
            g2d.setColor(Color.WHITE); 
            g2d.fillRect(0, 0, getWidth(), getHeight());

            // 2. Tabuleiro Acessível (Bege/Cinza Neutro Claro) - Alto Contraste
            Point2D centerBg = new Point2D.Float(getWidth() / 2f, getHeight() / 2f);
            float radiusBg = Math.max(getWidth(), getHeight());
            g2d.setPaint(new RadialGradientPaint(centerBg, radiusBg, 
                new float[]{0.0f, 1.0f}, 
                new Color[]{new Color(245, 245, 240), new Color(190, 195, 200)}));
            g2d.fillRect(MARGIN, MARGIN, boardWidth, boardHeight);

            // 3. Desenhando a Grade 
            g2d.setColor(new Color(60, 60, 65)); 
            g2d.setStroke(new BasicStroke(3)); 
            
            for (int row = 0; row <= Board.ROWS; row++) {
                int y = MARGIN + row * cellHeight;
                g2d.drawLine(MARGIN, y, MARGIN + boardWidth, y);
            }
            for (int col = 0; col <= Board.COLS; col++) {
                int x = MARGIN + col * cellWidth;
                g2d.drawLine(x, MARGIN, x, MARGIN + boardHeight);
            }

            // 4. Letras e Números
            g2d.setFont(new Font("Arial", Font.BOLD, 14));
            g2d.setColor(new Color(40, 40, 40)); 
            String[] cols = {"A", "B", "C", "D", "E", "F"};
            for (int i = 0; i < Board.COLS; i++) {
                int cx = MARGIN + i * cellWidth + (cellWidth / 2);
                int letterWidth = g2d.getFontMetrics().stringWidth(cols[i]);
                g2d.drawString(cols[i], cx - letterWidth / 2, MARGIN - 10);
            }
            for (int i = 0; i < Board.ROWS; i++) {
                int cy = MARGIN + i * cellHeight + (cellHeight / 2);
                String number = String.valueOf(i + 1);
                int numberWidth = g2d.getFontMetrics().stringWidth(number);
                g2d.drawString(number, MARGIN - 15 - numberWidth, cy + g2d.getFontMetrics().getAscent() / 2);
            }

            int margin_piece = Math.min(cellWidth, cellHeight) / 8;
            int pieceDiameter = Math.min(cellWidth, cellHeight) - (2 * margin_piece);
            int pieceRadius = pieceDiameter / 2;
            int squareSize = (int) (pieceDiameter * 0.85); 
            int squareOffset = squareSize / 2;

            // 5. Renderizando Peças
            for (int row = 0; row < Board.ROWS; row++) {
                for (int col = 0; col < Board.COLS; col++) {
                    int cx = MARGIN + col * cellWidth + (cellWidth / 2);
                    int cy = MARGIN + row * cellHeight + (cellHeight / 2);

                    Piece piece = board.getPieceAt(row, col);
                    if (piece != null) {
                        boolean isPlayer1 = piece.getPlayerId() == 1; 
                        
                        // Sombra das peças
                        g2d.setColor(new Color(0, 0, 0, 80));
                        if (isPlayer1) {
                            g2d.fillOval(cx - pieceRadius + 3, cy - pieceRadius + 3, pieceDiameter, pieceDiameter);
                        } else {
                            g2d.fillRect(cx - squareOffset + 3, cy - squareOffset + 3, squareSize, squareSize);
                        }

                        if (isPlayer1) {
                            // --- JOGADOR 1: BOLINHA LARANJA (#D55E00) ---
                            Point2D baseCenter = new Point2D.Float(cx, cy);
                            float[] distBaseW = {0.0f, 0.7f, 1.0f};
                            Color[] colorsBaseW = {new Color(240, 150, 70), new Color(213, 94, 0), new Color(140, 50, 0)};
                            g2d.setPaint(new RadialGradientPaint(baseCenter, pieceRadius, distBaseW, colorsBaseW));
                            g2d.fillOval(cx - pieceRadius, cy - pieceRadius, pieceDiameter, pieceDiameter);

                            // Brilho
                            float glossRad = pieceRadius / 2.0f;
                            Point2D glossCenter = new Point2D.Float(cx - pieceRadius/3.0f, cy - pieceRadius/3.0f);
                            float[] distGloss = {0.0f, 1.0f};
                            Color[] colorsGloss = {new Color(255, 255, 255, 200), new Color(255, 255, 255, 0)};
                            g2d.setPaint(new RadialGradientPaint(glossCenter, glossRad, distGloss, colorsGloss));
                            g2d.fillOval((int)(glossCenter.getX()-glossRad), (int)(glossCenter.getY()-glossRad), (int)glossRad*2, (int)glossRad*2);
                            
                            // Borda
                            g2d.setColor(new Color(80, 30, 0));
                            g2d.setStroke(new BasicStroke(1));
                            g2d.drawOval(cx - pieceRadius, cy - pieceRadius, pieceDiameter, pieceDiameter);

                        } else {
                            // --- JOGADOR 2: QUADRADO AZUL ESCURO (#0072B2) ---
                            Point2D startL = new Point2D.Float(cx - squareOffset, cy - squareOffset);
                            Point2D endL = new Point2D.Float(cx + squareOffset, cy + squareOffset);
                            float[] distBaseB = {0.0f, 0.5f, 1.0f};
                            Color[] colorsBaseB = {new Color(60, 150, 210), new Color(0, 114, 178), new Color(0, 60, 100)};
                            g2d.setPaint(new LinearGradientPaint(startL, endL, distBaseB, colorsBaseB));
                            g2d.fillRect(cx - squareOffset, cy - squareOffset, squareSize, squareSize);

                            // Brilho
                            g2d.setPaint(new LinearGradientPaint(startL, new Point2D.Float(cx, cy), new float[]{0.0f, 1.0f}, new Color[]{new Color(255, 255, 255, 120), new Color(255, 255, 255, 0)}));
                            g2d.fillRect(cx - squareOffset + 2, cy - squareOffset + 2, squareSize / 2 - 2, squareSize / 2 - 2);
                            
                            // Borda
                            g2d.setColor(new Color(0, 30, 60));
                            g2d.setStroke(new BasicStroke(1));
                            g2d.drawRect(cx - squareOffset, cy - squareOffset, squareSize, squareSize);
                        }
                    }
                }
            }

            // 6. Destacando a peça selecionada (Amarelo Acessível - #F0E442)
            if (selectedRow != -1 && selectedCol != -1) {
                int cx = MARGIN + selectedCol * cellWidth + (cellWidth / 2);
                int cy = MARGIN + selectedRow * cellHeight + (cellHeight / 2);
                
                g2d.setColor(new Color(240, 228, 66)); // Amarelo Dourado
                g2d.setStroke(new BasicStroke(4));
                
                Piece selectedPiece = board.getPieceAt(selectedRow, selectedCol);
                if (selectedPiece != null && selectedPiece.getPlayerId() == 2) {
                    g2d.drawRect(cx - squareOffset - 4, cy - squareOffset - 4, squareSize + 8, squareSize + 8);
                    
                    // Bordas pretas finas (Duplo Contorno para máximo contraste em qualquer fundo)
                    g2d.setColor(Color.BLACK);
                    g2d.setStroke(new BasicStroke(1));
                    g2d.drawRect(cx - squareOffset - 6, cy - squareOffset - 6, squareSize + 12, squareSize + 12);
                    g2d.drawRect(cx - squareOffset - 2, cy - squareOffset - 2, squareSize + 4, squareSize + 4);
                } else {
                    g2d.drawOval(cx - pieceRadius - 4, cy - pieceRadius - 4, (pieceRadius + 4) * 2, (pieceRadius + 4) * 2);
                    
                    g2d.setColor(Color.BLACK);
                    g2d.setStroke(new BasicStroke(1));
                    g2d.drawOval(cx - pieceRadius - 6, cy - pieceRadius - 6, (pieceRadius + 6) * 2, (pieceRadius + 6) * 2);
                    g2d.drawOval(cx - pieceRadius - 2, cy - pieceRadius - 2, (pieceRadius + 2) * 2, (pieceRadius + 2) * 2);
                }
            }

            // 7. Destacando os movimentos válidos (Acessível: Cor + Forma Tracejada + Ponto Central)
            for (Point move : validMoves) {
                int moveRow = move.x;
                int moveCol = move.y;
                int cx = MARGIN + moveCol * cellWidth + (cellWidth / 2);
                int cy = MARGIN + moveRow * cellHeight + (cellHeight / 2);
                
                // Cor Azul Celeste (Okabe-Ito)
                g2d.setColor(new Color(86, 180, 233));
                
                // Forma 1: Linha Tracejada (indicando espaço reservado/alvo de clique)
                Stroke dashed = new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0, new float[]{10, 10}, 0);
                g2d.setStroke(dashed);
                g2d.drawOval(cx - pieceRadius, cy - pieceRadius, pieceDiameter, pieceDiameter);
                
                // Forma 2: Ponto central sólido para reforçar a indicação de destino independente da cor
                g2d.fillOval(cx - 6, cy - 6, 12, 12);
            }
        }
    }
}