package br.com.victorsfec.dara.game;

/**
 * Representa uma peça de jogo no tabuleiro.
 * Armazena apenas a quem a peça pertence (Jogador 1 ou 2).
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