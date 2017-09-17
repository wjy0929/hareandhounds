//-------------------------------------------------------------------------------------------------------------//
// Code based on a tutorial by Shekhar Gulati of SparkJava at
// https://blog.openshift.com/developing-single-page-web-applications-using-java-8-spark-mongodb-and-angularjs/
//-------------------------------------------------------------------------------------------------------------//

package com.oose2017.jwu107.hareandhounds;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;

import static spark.Spark.*;

public class TodoController {

    private static final String API_CONTEXT = "/hareandhounds/api/games";

    private final TodoService todoService;

    private final Logger logger = LoggerFactory.getLogger(TodoController.class);

    public TodoController(TodoService todoService) {
        this.todoService = todoService;
        setupEndpoints();
    }

    private void setupEndpoints() {
        // new game
        post(API_CONTEXT, "application/json", (request, response) -> {
            try {
                logger.info("Create a new game");
                Game newGame = todoService.createNewGame(request.body());

                response.status(201);
                return newGame;
            } catch (TodoService.PieceTypeException ex) {
                logger.error("No this pieceType");
                response.status(400);
                return Collections.EMPTY_MAP;
            } catch (TodoService.TodoServiceException ex) {
                logger.error("Failed to create new game");
                response.status(400);
                return Collections.EMPTY_MAP;
            }
        }, new JsonTransformer());


        // join game
        put(API_CONTEXT + "/:gameId", "application/json", (request, response) -> {
            try {
                logger.info("Second Player join the game " + request.params(":gameId"));
                Game newPlayer = todoService.joinGame(request.params(":gameId"));
                response.status(200);
                return newPlayer;
            } catch (TodoService.InvalidGameIdException ex) {
                logger.error(String.format("Invalid game id ", request.params(":gameId")));
                response.status(404);
            }catch (TodoService.PlayerFullException ex) {
                logger.error(String.format("Second player already join the game ", request.params(":gameId")));
                response.status(410);
            }
            catch (TodoService.TodoServiceException ex) {
                logger.error(String.format("Failed to join the game: %s", request.params(":gameId")));
                response.status(400);
            }
            return Collections.EMPTY_MAP;
        }, new JsonTransformer());

        // get game state
        get(API_CONTEXT + "/:gameId/state", "application/json", (request, response) -> {
            try {
                response.status(200);
                return todoService.findState(request.params(":gameId"));
            } catch (TodoService.InvalidGameIdException ex) {
                logger.error("Cannot find the state of game " + request.params(":gameId"));
                response.status(404);
            } catch (TodoService.TodoServiceException ex) {
                logger.error("Failed to get game state");
                response.status(400);
            }
            return Collections.EMPTY_MAP;
        }, new JsonTransformer());

        // get game board
        get(API_CONTEXT + "/:gameId/board", "application/json", (request, response)-> {
            try {
                response.status(200);
                return todoService.findBoard(request.params(":gameId")) ;
            } catch  (TodoService.InvalidGameIdException ex) {
                logger.error("Cannot find the board of game " + request.params(":gameId"));
                response.status(404);
            } catch (TodoService.TodoServiceException ex) {
                logger.error("Failed to get the board");
                response.status(400);
            }
            return Collections.EMPTY_MAP;
        }, new JsonTransformer());

        // play game
        post(API_CONTEXT + "/:gameId/turns", "application/json", (request, response) -> {
            try {
                response.status(200);
                return todoService.playGame(request.body());

            } catch (TodoService.InvalidGameIdException ex) {
                logger.error("Invalid game id");
                response.status(404);
                return new ErrorReason("INVALID_GAME_ID");

            } catch (TodoService.InvalidPlayerIdException ex) {
                logger.error("Invalid player id");
                response.status(404);
                return new ErrorReason("INVALID_PLAYER_ID");

            } catch (TodoService.IllegalMoveException ex) {
                logger.error("Illegal move");
                response.status(422);
                return new ErrorReason("ILLEGAL_MOVE");

            } catch (TodoService.IncorrectTurnException ex) {
                logger.error("Incorrect Turn");
                response.status(422);
                return new ErrorReason("INCORRECT_TURN");
            }
            catch (TodoService.TodoServiceException ex) {
                logger.error("Failed to play");
                response.status(400);
                return new ErrorReason("FAILED_TO_PLAY");
            }
        }, new JsonTransformer());

    }
}
