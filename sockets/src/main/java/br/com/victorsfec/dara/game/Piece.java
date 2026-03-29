package br.com.victorsfec.dara.game;

public class Piece {
    private final int playerId;

    public Piece(int playerId) {
        this.playerId = playerId;
    }

    public int getPlayerId() {
        return playerId;
    }
}