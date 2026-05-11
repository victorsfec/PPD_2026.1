package br.com.victorsfec.dara.game;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

/**
 * Matriz 5x6 que controla a física e as regras do Dara.
 * Utilizada localmente no Cliente para visualização, e centralmente no Servidor
 * para validar e autorizar movimentos contra fraudes.
 */
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

    // Varre as redondezas para contar peças adjacentes da mesma cor
    private int countLine(int row, int col, int dRow, int dCol, int playerId) {
        int count = 1; 
        int r = row + dRow, c = col + dCol;
        
        while (isValidCoordinate(r, c) && grid[r][c] != null && grid[r][c].getPlayerId() == playerId) {
            count++; 
            r += dRow; 
            c += dCol;
        }
        
        r = row - dRow; 
        c = col - dCol;
        
        while (isValidCoordinate(r, c) && grid[r][c] != null && grid[r][c].getPlayerId() == playerId) {
            count++; 
            r -= dRow; 
            c -= dCol;
        }
        return count;
    }

    // Identifica quebra de regras de alinhamento
    private boolean formsInvalidAlignment(int row, int col, int playerId, boolean isPlacementPhase) {
        int countH = countLine(row, col, 0, 1, playerId);
        int countV = countLine(row, col, 1, 0, playerId);

        // Regra 1: Jamais pode alinhar 4 ou mais peças
        if (countH >= 4 || countV >= 4) return true;
        
        // Regra 2: Na Fase de Colocação, jamais pode alinhar 3 peças
        if (isPlacementPhase && (countH >= 3 || countV >= 3)) return true;

        return false; 
    }

    public boolean canPlacePiece(int row, int col, int playerId) {
        if (!isValidCoordinate(row, col) || grid[row][col] != null) return false;
        
        // Simula o movimento virtualmente para ver se não transgride as regras
        grid[row][col] = new Piece(playerId); 
        boolean isInvalid = formsInvalidAlignment(row, col, playerId, true);
        grid[row][col] = null; 
        
        return !isInvalid;
    }

    public void placePiece(int row, int col, int playerId) {
        if (isValidCoordinate(row, col)) {
            grid[row][col] = new Piece(playerId);
        }
    }

    public void removePiece(int row, int col) {
        if (isValidCoordinate(row, col)) {
            grid[row][col] = null;
        }
    }

    public void performMove(int startRow, int startCol, int endRow, int endCol) {
        Piece p = grid[startRow][startCol];
        grid[startRow][startCol] = null;
        grid[endRow][endCol] = p;
    }

    // Acha todas as casas vazias em volta que não quebrem regras
    public List<Point> getValidMoves(int startRow, int startCol) {
        List<Point> validMoves = new ArrayList<>();
        Piece p = getPieceAt(startRow, startCol);
        
        if (p == null) return validMoves;
        
        int playerId = p.getPlayerId();
        int[][] directions = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}}; 
        
        for (int[] dir : directions) {
            int r = startRow + dir[0];
            int c = startCol + dir[1];
            
            if (isValidCoordinate(r, c) && grid[r][c] == null) {
                
                grid[startRow][startCol] = null;
                grid[r][c] = new Piece(playerId);
                
                boolean isInvalid = formsInvalidAlignment(r, c, playerId, false);
                
                grid[r][c] = null;
                grid[startRow][startCol] = p;

                if (!isInvalid) validMoves.add(new Point(r, c));
            }
        }
        return validMoves;
    }

    // Confere se um jogador acabou de alinhar 3 peças exatas
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

    // Jogo acaba quando alguém tem 2 ou menos peças
    public boolean isGameOver(int playerIdToCheck) {
        return countPieces(playerIdToCheck) <= 2;
    }
}