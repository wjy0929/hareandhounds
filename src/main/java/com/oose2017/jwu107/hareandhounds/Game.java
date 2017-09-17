package com.oose2017.jwu107.hareandhounds;

/**
 * Created by jingyiwu on 9/13/17.
 */
public class Game {

    private String gameId;
    private String playerId;
    private String pieceType;

    public Game(String gameId, String playerId, String pieceType) {
        this.gameId = gameId;
        this.playerId = playerId;
        this.pieceType = pieceType;
    }

    public void setGameId(String id){
        this.gameId = id;
    }
    public String getGameId() {
        return gameId;
    }

    public void setPlayerId(String id){
        this.playerId = id;
    }

    public String getPlayerId() {
        return playerId;
    }

    public void setPieceType(String type){
        this.pieceType = type;
    }

    public String getPieceType() {
        return pieceType;
    }


    @Override
    public String toString() {
        return "Game{" +
                "gameId='" + gameId + '\'' +
                ", playerId='" + playerId + '\'' +
                ", pieceType=" + pieceType +
                '}';
    }
}
