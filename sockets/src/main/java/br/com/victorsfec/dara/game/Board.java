package br.com.victorsfec.dara.game;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

public class Board {
    public static final int ROWS = 5;
    public static final int COLS = 6;
    private final Piece[][] grid;

    public Board() {
        grid = new Piece[ROWS][COLS];
    }

    public Piece getPieceAt(int row, int col) {
        if (isValidCoordinate(row, col)) return grid[row][col];
        return null;
    }

    private boolean isValidCoordinate(int row, int col) {
        return row >= 0 && row < ROWS && col >= 0 && col < COLS;
    }

    // Conta quantas peças contíguas do mesmo jogador existem em uma dada direção
    private int countLine(int row, int col, int dRow, int dCol, int playerId) {
        int count = 1;
        
        // Direção positiva
        int r = row + dRow;
        int c = col + dCol;
        while (isValidCoordinate(r, c) && grid[r][c] != null && grid[r][c].getPlayerId() == playerId) {
            count++;
            r += dRow;
            c += dCol;
        }
        
        // Direção negativa (oposta)
        r = row - dRow;
        c = col - dCol;
        while (isValidCoordinate(r, c) && grid[r][c] != null && grid[r][c].getPlayerId() == playerId) {
            count++;
            r -= dRow;
            c -= dCol;
        }
        
        return count;
    }

    // Validação central de regras restritas do Dara (ignora diagonais)
    private boolean formsInvalidAlignment(int row, int col, int playerId, boolean isPlacementPhase) {
        int countH = countLine(row, col, 0, 1, playerId); // Horizontal
        int countV = countLine(row, col, 1, 0, playerId); // Vertical

        // REGRA 1: Alinhar 4 ou mais peças na HORIZONTAL ou VERTICAL é SEMPRE inválido
        if (countH >= 4 || countV >= 4) return true;

        // REGRA 2: Na FASE DE COLOCAÇÃO, não pode alinhar 3 na Horizontal ou Vertical
        if (isPlacementPhase && (countH >= 3 || countV >= 3)) return true;

        return false; // A jogada é válida (inclui diagonais de qualquer tamanho)
    }

    // Fase 1: Verifica se a peça pode ser colocada
    public boolean canPlacePiece(int row, int col, int playerId) {
        if (!isValidCoordinate(row, col) || grid[row][col] != null) return false;
        
        grid[row][col] = new Piece(playerId); // Simula a colocação
        boolean isInvalid = formsInvalidAlignment(row, col, playerId, true);
        grid[row][col] = null; // Reverte a simulação
        
        return !isInvalid;
    }

    public void placePiece(int row, int col, int playerId) {
        if (isValidCoordinate(row, col)) grid[row][col] = new Piece(playerId);
    }

    public void removePiece(int row, int col) {
        if (isValidCoordinate(row, col)) grid[row][col] = null;
    }

    public void performMove(int startRow, int startCol, int endRow, int endCol) {
        Piece p = grid[startRow][startCol];
        grid[startRow][startCol] = null;
        grid[endRow][endCol] = p;
    }

    // Fase 2: Pega movimentos válidos simulando as jogadas e verificando as regras
    public List<Point> getValidMoves(int startRow, int startCol) {
        List<Point> validMoves = new ArrayList<>();
        Piece p = getPieceAt(startRow, startCol);
        if (p == null) return validMoves;
        
        int playerId = p.getPlayerId();
        int[][] directions = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}}; // Movimentos ortogonais apenas
        
        for (int[] dir : directions) {
            int r = startRow + dir[0];
            int c = startCol + dir[1];
            
            if (isValidCoordinate(r, c) && grid[r][c] == null) {
                // Simula o movimento
                grid[startRow][startCol] = null;
                grid[r][c] = new Piece(playerId);
                
                // Valida as regras para a nova posição
                boolean isInvalid = formsInvalidAlignment(r, c, playerId, false);
                
                // Reverte a simulação
                grid[r][c] = null;
                grid[startRow][startCol] = p;

                // Se a jogada não gerar uma formação ilegal, adiciona na lista de permitidos
                if (!isInvalid) {
                    validMoves.add(new Point(r, c));
                }
            }
        }
        return validMoves;
    }

    // Verifica formação de 3 peças apenas para realizar capturas (Horizontal/Vertical)
    public boolean formsCaptureLine(int row, int col, int playerId) {
        int countH = countLine(row, col, 0, 1, playerId);
        int countV = countLine(row, col, 1, 0, playerId);
        
        return countH == 3 || countV == 3;
    }

    public int countPieces(int playerId) {
        int count = 0;
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                if (grid[r][c] != null && grid[r][c].getPlayerId() == playerId) count++;
            }
        }
        return count;
    }

    public boolean isGameOver(int playerIdToCheck) {
        return countPieces(playerIdToCheck) <= 2;
    }
}