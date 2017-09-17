package com.oose2017.jwu107.hareandhounds;

/**
 * Created by jingyiwu on 9/13/17.
 */
public class GameState {
    private String gameId;
    private String state;

    public GameState(String gameId, String state){
        this.gameId = gameId;
        this.state = state;
    }

    public void setGameId(String gameId){
        this.gameId = gameId;
    }

    public String getGameId(){
        return gameId;
    }

    public void setState(String state){
        this.state = state;
    }

    public String getState(){
        return state;
    }
}
