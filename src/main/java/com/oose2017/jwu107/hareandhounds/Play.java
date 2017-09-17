package com.oose2017.jwu107.hareandhounds;

/**
 * Created by jingyiwu on 9/13/17.
 */
public class Play {
    private String gameId;
    private String playerId;
    private int fromX;
    private int fromY;
    private int toX;
    private int toY;

    public Play(String gameId, String playerId, int fromX, int fromY, int toX, int toY){
        this.gameId = gameId;
        this.playerId = playerId;
        this.fromX = fromX;
        this.fromY = fromY;
        this.toX = toX;
        this.toY = toY;
    }

    public void setGameId(String id){
        this.gameId = id;
    }
    public String getGameId() {
        return gameId;
    }

    public void setPlayerId(String playerId){
        this.playerId = playerId;
    }

    public String getPlayerId(){
        return playerId;
    }

    public void setFromX(int fromX){
        this.fromX = fromX;
    }

    public int getFromX(){
        return fromX;
    }

    public void setFromY(int fromY){
        this.fromY = fromY;
    }

    public int getFromY(){
        return fromY;
    }

    public void setToX(int toX){
        this.toX = toX;
    }

    public int getToX(){
        return toX;
    }

    public void setToY(int toY){
        this.toY = toY;
    }

    public int getToY(){
        return toY;
    }
}
