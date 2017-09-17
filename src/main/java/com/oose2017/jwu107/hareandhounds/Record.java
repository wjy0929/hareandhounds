package com.oose2017.jwu107.hareandhounds;

/**
 * Created by jingyiwu on 9/14/17.
 */
public class Record {

    private String gameId;
    private String hound;
    private String hare;

    public Record(String gameId, String hound, String hare){
        this.gameId = gameId;
        this.hound = hound;
        this.hare = hare;
    }

    public void setGameId(String id){
        this.gameId = id;
    }
    public String getGameId() {
        return gameId;
    }

    public void setHound(String hound){
        this.hound = hound;
    }
    public String getHound() {
        return hound;
    }

    public void setHare(String hare){
        this.hare = hare;
    }
    public String getHare() {
        return hare;
    }

}
