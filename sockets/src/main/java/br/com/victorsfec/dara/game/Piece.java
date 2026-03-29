package br.com.victorsfec.dara.game;

/**
 * POJO que representa uma peça isolada do Tabuleiro.
 */
public class Piece {
    private final int playerId;

    public Piece(int playerId) {
        this.playerId = playerId;
    }

    public int getPlayerId() {
        return playerId;
    }
}