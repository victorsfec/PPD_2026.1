package br.com.victorsfec.dara.game;

/**
 * Classe básica pra representar uma peça. Só guarda de quem ela é.
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