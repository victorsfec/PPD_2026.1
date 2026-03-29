package br.com.victorsfec.dara.shared;

public class Protocol {
    public static final String SEPARATOR = ":";
    
    // Comandos Cliente -> Servidor
    public static final String MOVE = "MOVE"; 
    public static final String PLACE = "PLACE"; // Fase de Colocação
    public static final String REMOVE = "REMOVE"; // Fase de Captura
    public static final String CHAT = "CHAT"; 
    public static final String FORFEIT = "FORFEIT"; 
    public static final String SET_NAME = "SET_NAME"; 
    public static final String GET_VALID_MOVES = "GET_VALID_MOVES"; 

    // Comandos Servidor -> Cliente
    public static final String GAME_OVER_STATS = "GAME_OVER_STATS"; 
    public static final String WELCOME = "WELCOME"; 
    public static final String GAME_START = "GAME_START";  
    public static final String OPPONENT_FOUND = "OPPONENT_FOUND"; 
    public static final String VALID_MOVE = "VALID_MOVE"; 
    public static final String PIECE_PLACED = "PIECE_PLACED"; 
    public static final String PIECE_REMOVED = "PIECE_REMOVED"; 
    public static final String MUST_REMOVE = "MUST_REMOVE"; 
    public static final String OPPONENT_MOVED = "OPPONENT_MOVED"; 
    public static final String SET_TURN = "SET_TURN"; 
    public static final String CHAT_MESSAGE = "CHAT_MESSAGE"; 
    public static final String VICTORY = "VICTORY"; 
    public static final String DEFEAT = "DEFEAT"; 
    public static final String OPPONENT_FORFEIT = "OPPONENT_FORFEIT"; 
    public static final String INFO = "INFO"; 
    public static final String ERROR = "ERROR"; 
    public static final String VALID_MOVES_LIST = "VALID_MOVES_LIST"; 
    public static final String UPDATE_SCORE = "UPDATE_SCORE"; 
}