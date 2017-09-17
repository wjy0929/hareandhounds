package com.oose2017.jwu107.hareandhounds;

/**
 * Created by jingyiwu on 9/13/17.
 */
public class GameBoard {
    private String gameId;
    private String pieceType;
    private int x;
    private int y;

    public  GameBoard(String gameId, String pieceType, int x, int y){
        this.gameId = gameId;
        this.pieceType = pieceType;
        this.x = x;
        this.y = y;
    }

    public void setGameId(String gameId){
        this.gameId = gameId;
    }

    public String getGameId(){
        return gameId;
    }

    public void setPieceType(String pieceType){
        this.pieceType = pieceType;
    }

    public String getPieceType(){
        return pieceType;
    }

    public void setX(int x){
        this.x = x;
    }

    public int getX(){
        return x;
    }

    public void setY(int y){
        this.y = y;
    }

    public int getY(){
        return y;
    }
}
