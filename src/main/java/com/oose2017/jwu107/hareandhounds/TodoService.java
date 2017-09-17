//-------------------------------------------------------------------------------------------------------------//
// Code based on a tutorial by Shekhar Gulati of SparkJava at
// https://blog.openshift.com/developing-single-page-web-applications-using-java-8-spark-mongodb-and-angularjs/
//-------------------------------------------------------------------------------------------------------------//

package com.oose2017.jwu107.hareandhounds;

import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sql2o.Connection;
import org.sql2o.Sql2o;
import org.sql2o.Sql2oException;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.UUID;
import java.util.Map;
import java.util.HashMap;

public class TodoService {

    private Sql2o db;

    private final Logger logger = LoggerFactory.getLogger(TodoService.class);

    private final int[] HOUNDX = {1,0,1};
    private final int[] HOUNDY = {0,1,2};
    private final String[] STATE = {"WAITING_FOR_SECOND_PLAYER", "TURN_HARE", "TURN_HOUND",
                                    "WIN_HARE_BY_ESCAPE", "WIN_HARE_BY_STALLING", "WIN_HOUND"};




    /**
     * Construct the model with a pre-defined datasource. The current implementation
     * also ensures that the DB schema is created if necessary.
     *
     */
    public TodoService(DataSource dataSource) throws TodoServiceException {
        db = new Sql2o(dataSource);

        //Create the schema for the database if necessary. This allows this
        //program to mostly self-contained. But this is not always what you want;
        //sometimes you want to create the schema externally via a script.
        try (Connection conn = db.open()) {
            String sqlCreateGame = "CREATE TABLE IF NOT EXISTS GameTable (gameId TEXT NOT NULL, playerId TEXT NOT NULL, " +
                    "pieceType TEXT NOT NULL)";

            String sqlCreateState = "CREATE TABLE IF NOT EXISTS GameState (gameId TEXT PRIMARY KEY NOT NULL, state TEXT NOT NULL)";

            String sqlCreateBoard = "CREATE TABLE IF NOT EXISTS GameBoard (gameId TEXT NOT NULL, pieceType TEXT NOT NULL, " +
                    "x INTEGER, y INTEGER)";

            String sqlCreateRecord = "CREATE TABLE IF NOT EXISTS GameRecord (gameId TEXT NOT NULL, hound TEXT, hare TEXT)";

            conn.createQuery(sqlCreateGame).executeUpdate();
            conn.createQuery(sqlCreateState).executeUpdate();
            conn.createQuery(sqlCreateBoard).executeUpdate();
            conn.createQuery(sqlCreateRecord).executeUpdate();

        } catch(Sql2oException ex) {
            logger.error("Failed to create schema at startup", ex);
            throw new TodoServiceException("Failed to create schema at startup", ex);
        }
    }


    /**
     * Create a new Game and return all info.
     */
    public Game createNewGame(String body) throws PieceTypeException,TodoServiceException {
        Game newGame = new Gson().fromJson(body, Game.class);

        try{
            String p = newGame.getPieceType();
            if( !p.equals("HOUND") && !p.equals("HARE")){
                throw new PieceTypeException("TodoService.createNewGame: no this pieceType");
            }

        }catch(Exception ex){
            throw new TodoServiceException("TodoService.createNewGame: Failed to create new game", ex);
        }

        // set gameId using uuid and set playerId using piecetype
        String uuid = UUID.randomUUID().toString().replaceAll("-", "");
        newGame.setGameId(uuid);



        newGame.setPlayerId(newGame.getPieceType());


        String sql = "INSERT INTO GameTable (gameId, playerId, pieceType) VALUES (:gameId, :playerId, :pieceType)" ;

        String sqlInsertState = "INSERT INTO GameState (gameId, state) VALUES (:gameId, :state)";

        String sqlInsertBoard = "INSERT INTO GameBoard (gameId, pieceType, x, y) VALUES (:gameId, :pieceType, :x, :y)";

        try (Connection conn = db.open()) {



            // create the new game and init the first player
            conn.createQuery(sql)
                    .bind(newGame)
                    .executeUpdate();

            //set game state
            conn.createQuery(sqlInsertState)
                .addParameter("gameId", newGame.getGameId())
                .addParameter("state", "WAITING_FOR_SECOND_PLAYER")
                .executeUpdate();



            //init game board
            List<GameBoard> board = new ArrayList<>();
            for(int i=0; i<HOUNDX.length; i++){
                board.add(new GameBoard(newGame.getGameId(),"HOUND", HOUNDX[i], HOUNDY[i]));
            }
            board.add(new GameBoard(newGame.getGameId(),"HARE", 4, 1));

            for(int i=0; i< board.size(); i++) {
                conn.createQuery(sqlInsertBoard)
                        .bind(board.get(i))
                        .executeUpdate();
            }

            return newGame;
        } catch(Sql2oException ex) {
            logger.error("TodoService.createNewGame: Failed to create new game", ex);
            throw new TodoServiceException("TodoService.createNewGame: Failed to create new game", ex);
        }
    }


    /**
     * Join a Game and return all info.
     */
    public Game joinGame(String gameId) throws TodoServiceException, InvalidGameIdException,PlayerFullException{
        Game newPlayer= new Game(null,null,null);
        newPlayer.setGameId(gameId);
        String sql = "SELECT * FROM GameTable WHERE gameId = :gameId";
        String sqlInsert = "INSERT INTO GameTable (gameId, playerId, pieceType) VALUES(:gameId, :playerId, :pieceType)";
        try(Connection conn = db.open()){

            List<Game> player1 =  conn.createQuery(sql)
                    .bind(newPlayer)
                    .executeAndFetch(Game.class);

            if(player1.size() == 0){
                throw new InvalidGameIdException("TodoService.joinGame: Invalid game id");
            }

            if(player1.size() == 2){
                throw new PlayerFullException("TodoService.joinGame: Second Player already joined");
            }

            if(player1.size() == 1) {
                if(player1.get(0).getPlayerId().equals("HOUND")){
                    newPlayer.setPlayerId("HARE");
                    newPlayer.setPieceType("HARE");
                }else{
                    newPlayer.setPlayerId("HOUND");
                    newPlayer.setPieceType("HOUND");
                }

                conn.createQuery(sqlInsert)
                        .bind(newPlayer)
                        .executeUpdate();

                // set game state
                String sqlUpdate = "UPDATE GameState SET state = 'TURN_HOUND' WHERE gameId = :gameId ";
                conn.createQuery(sqlUpdate)
                    .bind(newPlayer)
                    .executeUpdate();
            }

            return newPlayer;
        }catch(Sql2oException ex) {
            logger.error("TodoService.joinGame: Failed to join a game", ex);
            throw new TodoServiceException("TodoService.joinGame: Failed to join a game", ex);
        }

    }

    /**
     * return the game state
     */
    public GameState findState(String gameId) throws InvalidGameIdException, TodoServiceException {
        String sql = "SELECT * FROM GameState WHERE gameId = :gameId";
        GameState state = new GameState(null,null);
        state.setGameId(gameId);

        try (Connection conn = db.open()) {
            List<GameState> stateinfo = conn.createQuery(sql)
                                            .bind(state)
                                            .executeAndFetch(GameState.class);

            if(stateinfo.size() == 0){
                throw new InvalidGameIdException(("TodoService.findState : invalid game id"));
            }
            return stateinfo.get(0);
        } catch(Sql2oException ex) {
            logger.error("TodoService.findState: Cannot find the state of game", ex);
            throw new TodoServiceException("TodoService.findState: Cannot find the state of game", ex);
        }
    }

    /**
     * return the game board
     */
    public List<GameBoard> findBoard(String gameId) throws InvalidGameIdException, TodoServiceException {
        String sql = "SELECT * FROM GameBoard WHERE gameId = :gameId";


        try (Connection conn = db.open()) {
            List<GameBoard> board = conn.createQuery(sql)
                                        .addParameter("gameId", gameId)
                                        .executeAndFetch(GameBoard.class);

            if(board.size() < 4){
                throw new InvalidGameIdException(("TodoService.findBoard: invalid game id"));
            }

            return board;
        } catch(Sql2oException ex) {
            logger.error("TodoService.findBoard: Cannot find the board of game", ex);
            throw new TodoServiceException("TodoService.findBoard: Cannot find the board of game", ex);
        }
    }


    /**
     * Play a game
     */

    public GameBoard playGame(String body) throws InvalidGameIdException, InvalidPlayerIdException,
                                                  IllegalMoveException, IncorrectTurnException, TodoServiceException {
        Play play = new Gson().fromJson(body, Play.class);
        String sqlBoard = "SELECT * FROM GameBoard WHERE gameId = :gameId";
        String sqlState = "SELECT * FROM GameState WHERE gameId = :gameId";
        String sqlRecord = "INSERT INTO GameRecord (gameId, hound, hare) VALUES (:gameId, :hound, :hare)";
        String sqlDelete = "DELETE FROM GameRecord WHERE gameId = :gameId";

        try(Connection conn = db.open()){
            List<GameBoard> playBoard = conn.createQuery(sqlBoard)
                                            .addParameter("gameId", play.getGameId())
                                            .executeAndFetch(GameBoard.class);
            List<GameState> state = conn.createQuery(sqlState)
                                        .addParameter("gameId", play.getGameId())
                                        .executeAndFetch(GameState.class);

            // Invalid game id
            if(playBoard == null){
                throw new InvalidGameIdException("TodoService.playGame: Invalid game id");
            }

            // Incorrect turn
            if(state.get(0).getState().equals(STATE[1])){
                if(!play.getPlayerId().equals("HARE")){
                    throw new IncorrectTurnException("TodoService.playGame: Incorrect turn");
                }
            }

            if(state.get(0).getState().equals(STATE[2])){
                if(!play.getPlayerId().equals("HOUND")){
                    throw new IncorrectTurnException("TodoService.playGame: Incorrect turn");
                }
            }

            // When the game is over, they cannot move
            if(state.get(0).getState().equals(STATE[3]) || state.get(0).getState().equals(STATE[4])
                    || state.get(0).getState().equals(STATE[5])){
                throw new IllegalMoveException("TodoService.playGame: GAME OVER");
            }

            //Illegal move
            int fromX = play.getFromX();
            int fromY = play.getFromY();
            int toX = play.getToX();
            int toY = play.getToY();


            if(play.getPlayerId().equals("HARE")){
                if(!legalMove(fromX, fromY, toX, toY)){
                    throw new IllegalMoveException("TodoService.playGame: hare's position is out of board");
                }
            }else{
                if(legalMove(fromX, fromY, toX, toY)){
                    if(fromX > toX){
                        throw new IllegalMoveException("TodoService.playGame: Hounds cannot move backwards");
                    }
                }else{
                    throw new IllegalMoveException("TodoService.playGame: hound's position is out of board");
                }
            }

            if(samePosition(playBoard,toX,toY)){
                throw new IllegalMoveException("TodoService.playGame: the position is occupied");
            }

            ArrayList<Loc> special = new ArrayList<>();
            special.add(new Loc(1,1));
            special.add(new Loc(2,0));
            special.add(new Loc(3,1));
            special.add(new Loc(2,2));


            if(diagMove(special, fromX, fromY) && diagMove(special, toX, toY)){
                throw new IllegalMoveException("TodoService.playGame: cannot move in this direction");
            }


            // update x and y
            int res = 0;
            for(int i=0; i<playBoard.size(); i++){
                if(playBoard.get(i).getPieceType().equals(play.getPlayerId()) &&
                   playBoard.get(i).getX() == play.getFromX() && playBoard.get(i).getY() == play.getFromY()){

                    playBoard.get(i).setX(play.getToX());
                    playBoard.get(i).setY(play.getToY());

                    res = i;
                }
            }

           //update position of hare and hound
            String sqlUpdate = "UPDATE GameBoard SET x = :x, y = :y " +
                    "           WHERE gameId = :gameId AND pieceType = :pieceType AND x = :x1 AND y = :y1";

            conn.createQuery(sqlUpdate)
                    .addParameter("x", play.getToX())
                    .addParameter("y", play.getToY())
                    .addParameter("gameId", play.getGameId())
                    .addParameter("pieceType", play.getPlayerId())
                    .addParameter("x1", play.getFromX())
                    .addParameter("y1", play.getFromY())
                    .executeUpdate();

            //record the board position of hounds and hare

            List<Loc> houndPos = new ArrayList<>();
            int xNow = 0;
            int yNow = 0;
            for(GameBoard p : playBoard){
                if(p.getPieceType().equals("HARE")){
                    xNow = p.getX();
                    yNow = p.getY();
                }else{
                    houndPos.add(new Loc(p.getX(), p.getY()));
                }
            }

            List<Loc> houndSort = sortLoc(houndPos);

            String houndTotal = "";
            String harePos = String.valueOf(xNow) + String.valueOf(yNow);

            for(Loc l : houndSort){
                houndTotal = houndTotal + String.valueOf(l.x) + String.valueOf(l.y);
            }

            conn.createQuery(sqlRecord)
                .addParameter("gameId", play.getGameId())
                .addParameter("hound", houndTotal)
                .addParameter("hare", harePos)
                .executeUpdate();


            //update the state
            String sqlUpdateState;
            if(play.getPlayerId().equals("HOUND")) {
                sqlUpdateState = "UPDATE GameState SET state = 'TURN_HARE' WHERE gameId = :gameId";
            }else{
                sqlUpdateState = "UPDATE GameState SET state = 'TURN_HOUND' WHERE gameId = :gameId";
            }
            conn.createQuery(sqlUpdateState)
                    .addParameter("gameId", play.getGameId())
                    .executeUpdate();


            //WIN_HARE_BY_ESCAPE
            int[] hound = new int[3];
            int hare = 0;
            int n=0;
            for(int i=0; i<playBoard.size(); i++){
                if(playBoard.get(i).getPieceType().equals("HOUND")){
                    hound[n] = playBoard.get(i).getX();
                    n++;
                }else{
                    hare = playBoard.get(i).getX();
                }
            }
            int min = Math.min(Math.min(hound[0],hound[1]),hound[2]);

            if(hare < min){
                String sqlWin = "UPDATE GameState SET state = 'WIN_HARE_BY_ESCAPE' WHERE gameId = :gameId";
                conn.createQuery(sqlWin)
                        .addParameter("gameId", play.getGameId())
                        .executeUpdate();
                conn.createQuery(sqlDelete)
                        .addParameter("gameId", play.getGameId())
                        .executeUpdate();
            }

            // WIN_HOUND

            List<Loc> nextMoves = validNextMove(xNow, yNow);

            if(!freeMove(nextMoves,houndPos)) {

                String sqlWin = "UPDATE GameState SET state = 'WIN_HOUND' WHERE gameId = :gameId";
                conn.createQuery(sqlWin)
                        .addParameter("gameId", play.getGameId())
                        .executeUpdate();
                conn.createQuery(sqlDelete)
                        .addParameter("gameId", play.getGameId())
                        .executeUpdate();
            }

            //WIN_HARE_BY_STALLING
            String sqlFetchRecord = "SELECT * FROM GameRecord WHERE gameId = :gameId";

            List<Record> records = conn.createQuery(sqlFetchRecord)
                                       .addParameter("gameId", play.getGameId())
                                       .executeAndFetch(Record.class);
            Map<String,Integer> map = new HashMap<>();
            for(Record r : records){
                String key = r.getHound() + r.getHare();

                if(map.containsKey(key)){
                    map.put(key, map.get(key) + 1 );
                }else{
                    map.put(key,1);
                }

                if(map.get(key) > 2){
                    String sqlWin = "UPDATE GameState SET state = 'WIN_HARE_BY_STALLING' WHERE gameId = :gameId";
                    conn.createQuery(sqlWin)
                            .addParameter("gameId", play.getGameId())
                            .executeUpdate();

                    conn.createQuery(sqlDelete)
                            .addParameter("gameId", play.getGameId())
                            .executeUpdate();
                }

            }

            return playBoard.get(res);
        }catch(Sql2oException ex) {
            logger.error("TodoService.playGame: Failed to play", ex);
            throw new TodoServiceException("TodoService.playGame: Failed to play", ex);
        }
    }

    //-----------------------------------------------------------------------------//
    // Helper Classes and Methods
    //-----------------------------------------------------------------------------//

    public static class Loc{
        int x;
        int y;
        public Loc(int x, int y){
            this.x = x;
            this.y = y;
        }

        public boolean equal(Loc i){
            return i.x == this.x && i.y == this.y;
        }

        public boolean small(Loc n){
            if(this.y < n.y){
                return true;
            }else if(this.y > n.y){
                return false;
            }else{
                return this.x < n.x;
            }
        }

    }

    // sort the hound loc by y then x
    public List<Loc> sortLoc(List<Loc> hound){
        List<Loc> res = new ArrayList<>();
        Loc h1 = hound.get(0);
        Loc h2 = hound.get(1);
        Loc h3 = hound.get(2);

        if(h1.small(h2)){
            if(h2.small(h3)){
                res.add(h1);
                res.add(h2);
                res.add(h3);
            }else{
                if(h1.small(h3)){
                    res.add(h1);
                    res.add(h3);
                    res.add(h2);
                }else{
                    res.add(h3);
                    res.add(h1);
                    res.add(h2);
                }
            }
        }else{
            if(h1.small(h3)){
                res.add(h2);
                res.add(h3);
                res.add(h1);
            }else{
                if(h2.small(h3)){
                    res.add(h2);
                    res.add(h3);
                    res.add(h1);
                }else{
                    res.add(h3);
                    res.add(h2);
                    res.add(h1);
                }
            }
        }

        return res;

    }

    // form valid next move
    public List<Loc> validNextMove(int fromX, int fromY){
        List<Loc> nextMoves = new ArrayList<>();

        if((fromX == 1 && fromY == 1) || (fromX == 3 && fromY == 1) ){
            nextMoves.add(new Loc(fromX-1, fromY));
            nextMoves.add(new Loc(fromX+1, fromY));
            nextMoves.add(new Loc(fromX, fromY-1));
            nextMoves.add(new Loc(fromX, fromY+1));
        }else if(fromX == 2 && fromY == 0){
            nextMoves.add(new Loc(1,0));
            nextMoves.add(new Loc(2,1));
            nextMoves.add(new Loc(3,0));
        }else if(fromX == 2 && fromY == 2){
            nextMoves.add(new Loc(1,2));
            nextMoves.add(new Loc(2,1));
            nextMoves.add(new Loc(3,2));
        }else{
            for(int x=0; x<5; x++){
                for(int y=0; y<3; y++) {
                    if (legalMove(fromX, fromY, x, y)){
                        nextMoves.add(new Loc(x,y));
                    }
                }
            }
        }

        return nextMoves;
    }

    // judge whether the hare's next moves are all occupied by the hounds.
    public boolean freeMove(List<Loc> nextMove, List<Loc> houndPos){
        int n1 = nextMove.size();
        int n2 = houndPos.size();

        boolean[] eq = new boolean[n1];
        if(n1 > n2){
            return true;
        }else{
            for(int i=0; i < n1; i++){
                for(Loc j : houndPos){
                    if(j.equal(nextMove.get(i))){
                        eq[i] = true;
                    }
                }
            }

        }

        for(int i=0; i < n1; i++){
            if(!eq[i]){
                return true;
            }
        }
        return false;
    }

    // judge whether legal move or not
    public boolean legalMove(int fromX, int fromY, int toX, int toY){
        if( (toX == 0 || toX == 4 ) && toY != 1){ return false; }

        if(toY < 0 || toY > 3 || toX < 0 || toX > 4){ return false; }

        if(Math.abs(fromX - toX) > 1 || Math.abs(fromY - toY) > 1){ return false; }

        if(fromX == toX && fromY == toY){ return false;}

        return true;
    }

    // the to position cannot in the current occupied position
    public boolean samePosition(List<GameBoard> playBoard, int toX, int toY){
        for(GameBoard g : playBoard){
            if(g.getX() == toX && g.getY() == toY){
                return true;
            }
        }
        return false;
    }

    // cannot move in the diagonal line
    public boolean diagMove(ArrayList<Loc> special, int x, int y){
        Loc cur = new Loc(x,y);
        if(special.contains(cur)){
            return true;
        }

        return false;
    }

    // Exception function
    public static class TodoServiceException extends Exception {
        public TodoServiceException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class PieceTypeException extends Exception {
        public PieceTypeException(String message) {super(message);}
    }

    public static class InvalidGameIdException extends Exception {
        public InvalidGameIdException(String message) {
            super(message);
        }
    }

    public static class InvalidPlayerIdException extends Exception {
        public InvalidPlayerIdException(String message) {
            super(message);
        }
    }

    public static class PlayerFullException extends Exception {
        public PlayerFullException(String message) {
            super(message);
        }
    }

    public static class IncorrectTurnException extends Exception {
        public IncorrectTurnException(String message) {
            super(message);
        }
    }

    public static class IllegalMoveException extends Exception {
        public IllegalMoveException(String message) {
            super(message);
        }
    }

    /**
     * This Sqlite specific method returns the number of rows changed by the most recent
     * INSERT, UPDATE, DELETE operation. Note that you MUST use the same connection to get
     * this information
     */
    private int getChangedRows(Connection conn) throws Sql2oException {
        return conn.createQuery("SELECT changes()").executeScalar(Integer.class);
    }
}
