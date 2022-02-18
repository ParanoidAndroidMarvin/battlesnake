package com.battlesnake.starter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import static spark.Spark.port;
import static spark.Spark.post;
import static spark.Spark.get;

/**
 * This is a simple Battlesnake server written in Java.
 * <p>
 * For instructions see
 * https://github.com/BattlesnakeOfficial/starter-snake-java/README.md
 */
public class Snake {
    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();
    private static final Handler HANDLER = new Handler();
    private static final Logger LOG = LoggerFactory.getLogger(Snake.class);

    /**
     * Main entry point.
     *
     * @param args are ignored.
     */
    public static void main(String[] args) {
        String port = System.getProperty("PORT");
        if (port == null) {
            LOG.info("Using default port: {}", port);
            port = "8080";
        } else {
            LOG.info("Found system provided port: {}", port);
        }
        port(Integer.parseInt(port));
        get("/", HANDLER::process, JSON_MAPPER::writeValueAsString);
        post("/start", HANDLER::process, JSON_MAPPER::writeValueAsString);
        post("/move", HANDLER::process, JSON_MAPPER::writeValueAsString);
        post("/end", HANDLER::process, JSON_MAPPER::writeValueAsString);
    }

    /**
     * Handler class for dealing with the routes set up in the main method.
     */
    public static class Handler {

        /**
         * For the start/end request
         */
        private static final Map<String, String> EMPTY = new HashMap<>();

        /**
         * Generic processor that prints out the request and response from the methods.
         *
         * @param req
         * @param res
         * @return
         */
        public Map<String, String> process(Request req, Response res) {
            try {
                JsonNode parsedRequest = JSON_MAPPER.readTree(req.body());
                String uri = req.uri();
                LOG.info("{} called with: {}", uri, req.body());
                Map<String, String> snakeResponse;
                if (uri.equals("/")) {
                    snakeResponse = index();
                } else if (uri.equals("/start")) {
                    snakeResponse = start(parsedRequest);
                } else if (uri.equals("/move")) {
                    snakeResponse = move(parsedRequest);
                } else if (uri.equals("/end")) {
                    snakeResponse = end(parsedRequest);
                } else {
                    throw new IllegalAccessError("Strange call made to the snake: " + uri);
                }

                LOG.info("Responding with: {}", JSON_MAPPER.writeValueAsString(snakeResponse));

                return snakeResponse;
            } catch (JsonProcessingException e) {
                LOG.warn("Something went wrong!", e);
                return null;
            }
        }

        /**
         * This method is called everytime your Battlesnake is entered into a game.
         * <p>
         * Use this method to decide how your Battlesnake is going to look on the board.
         *
         * @return a response back to the engine containing the Battlesnake setup
         * values.
         */
        public Map<String, String> index() {
            Map<String, String> response = new HashMap<>();
            response.put("apiversion", "1");
            response.put("author", "ParanoidBattlesnake"); // TODO: Your Battlesnake Username
            response.put("color", "#dddddd"); // TODO: Personalize
            response.put("head", "safe"); // TODO: Personalize
            response.put("tail", "freckled"); // TODO: Personalize
            return response;
        }

        /**
         * This method is called everytime your Battlesnake is entered into a game.
         * <p>
         * Use this method to decide how your Battlesnake is going to look on the board.
         *
         * @param startRequest a JSON data map containing the information about the game
         *                     that is about to be played.
         * @return responses back to the engine are ignored.
         */
        public Map<String, String> start(JsonNode startRequest) {
            LOG.info("START");
            return EMPTY;
        }

        /**
         * This method is called on every turn of a game. It's how your snake decides
         * where to move.
         * <p>
         * Use the information in 'moveRequest' to decide your next move. The
         * 'moveRequest' variable can be interacted with as
         * com.fasterxml.jackson.databind.JsonNode, and contains all of the information
         * about the Battlesnake board for each move of the game.
         * <p>
         * For a full example of 'json', see
         * https://docs.battlesnake.com/references/api/sample-move-request
         *
         * @param moveRequest JsonNode of all Game Board data as received from the
         *                    Battlesnake Engine.
         * @return a Map<String,String> response back to the engine the single move to
         * make. One of "up", "down", "left" or "right".
         */
        public Map<String, String> move(JsonNode moveRequest) {

            try {
                LOG.info("Data: {}", JSON_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(moveRequest));
            } catch (JsonProcessingException e) {
                LOG.error("Error parsing payload", e);
            }

            /*
             * Example how to retrieve data from the request payload:
             *
             * String gameId = moveRequest.get("game").get("id").asText();
             *
             * int height = moveRequest.get("board").get("height").asInt();
             *
             */

            JsonNode head = moveRequest.get("you").get("head");
            JsonNode body = moveRequest.get("you").get("body");
            JsonNode snakes = moveRequest.get("board").get("snakes");
            JsonNode food = moveRequest.get("board").get("food");

            int boardHeigth = moveRequest.get("board").get("height").asInt();
            int boardWidth = moveRequest.get("board").get("width").asInt();

            ArrayList<String> possibleMoves = new ArrayList<>(Arrays.asList("up", "down", "left", "right"));

            // Don't allow your Battlesnake to move back in on it's own neck
            avoidMyNeck(head, body, possibleMoves);

            // TODO: Using information from 'moveRequest', find the edges of the board and
            // don't
            // let your Battlesnake move beyond them board_height = ? board_width = ?
            avoidWalls(head, boardHeigth, boardWidth, possibleMoves);

            // TODO Using information from 'moveRequest', don't let your Battlesnake pick a
            // move
            // that would hit its own body
            avoidMyBody(head, body, possibleMoves);

            // TODO: Using information from 'moveRequest', don't let your Battlesnake pick a
            // move
            // that would collide with another Battlesnake
            avoidOtherSnakes(head, snakes, possibleMoves);

            // TODO: Using information from 'moveRequest', make your Battlesnake move
            // towards a
            // piece of food on the board
            findFood(head, food, possibleMoves);

            // Choose a random direction to move in
            final int choice = new Random().nextInt(possibleMoves.size());
            final String move = possibleMoves.get(choice);

            LOG.info("MOVE {}", move);

            Map<String, String> response = new HashMap<>();
            response.put("move", move);
            return response;
        }

        /**
         * Remove the 'neck' direction from the list of possible moves
         *
         * @param head          JsonNode of the head position e.g. {"x": 0, "y": 0}
         * @param body          JsonNode of x/y coordinates for every segment of a
         *                      Battlesnake. e.g. [ {"x": 0, "y": 0}, {"x": 1, "y": 0},
         *                      {"x": 2, "y": 0} ]
         * @param possibleMoves ArrayList of String. Moves to pick from.
         */
        public void avoidMyNeck(JsonNode head, JsonNode body, ArrayList<String> possibleMoves) {
            JsonNode neck = body.get(1);

            if (neck.get("x").asInt() < head.get("x").asInt()) {
                possibleMoves.remove("left");
            } else if (neck.get("x").asInt() > head.get("x").asInt()) {
                possibleMoves.remove("right");
            } else if (neck.get("y").asInt() < head.get("y").asInt()) {
                possibleMoves.remove("down");
            } else if (neck.get("y").asInt() > head.get("y").asInt()) {
                possibleMoves.remove("up");
            }
        }

        public void avoidWalls(JsonNode head, int boardHeight, int boardWidth, ArrayList<String> possibleMoves) {
            if (head.get("x").asInt() == 0) {
                possibleMoves.remove("left");
            } else if (head.get("x").asInt() == boardWidth - 1) {
                possibleMoves.remove("right");
            }
            if (head.get("y").asInt() == 0) {
                possibleMoves.remove("down");
            } else if (head.get("y").asInt() == boardHeight - 1) {
                possibleMoves.remove("up");
            }
        }

        public void avoidMyBody(JsonNode head, JsonNode body, ArrayList<String> possibleMoves) {
            for (int i = 1; i < body.size() - 1; i++) {
                JsonNode bodyPart = body.get(i);
                int headX = head.get("x").asInt();
                int headY = head.get("y").asInt();
                int bodyPartX = bodyPart.get("x").asInt();
                int bodyPartY = bodyPart.get("y").asInt();

                if (headY == bodyPartY) {
                    if (headX - 1 == bodyPartX) {
                        possibleMoves.remove("left");
                    } else if (headX + 1 == bodyPartX) {
                        possibleMoves.remove("right");
                    }
                } else if (headX == bodyPartX) {
                    if (headY - 1 == bodyPartY) {
                        possibleMoves.remove("down");
                    } else if (headY + 1 == bodyPartY) {
                        possibleMoves.remove("up");
                    }
                }
            }
        }

        public void avoidOtherSnakes(JsonNode head, JsonNode snakes, ArrayList<String> possibleMoves) {
            for (int i = 0; i < snakes.size(); i++) {
                JsonNode body = snakes.get(i).get("body");
                for (int j = 0; j < body.size(); j++) {
                    JsonNode bodyPart = body.get(j);
                    int headX = head.get("x").asInt();
                    int headY = head.get("y").asInt();
                    int bodyPartX = bodyPart.get("x").asInt();
                    int bodyPartY = bodyPart.get("y").asInt();

                    if (headY == bodyPartY) {
                        if (headX - 1 == bodyPartX) {
                            possibleMoves.remove("left");
                        } else if (headX + 1 == bodyPartX) {
                            possibleMoves.remove("right");
                        }
                    } else if (headX == bodyPartX) {
                        if (headY - 1 == bodyPartY) {
                            possibleMoves.remove("down");
                        } else if (headY + 1 == bodyPartY) {
                            possibleMoves.remove("up");
                        }
                    }
                }
            }
        }

        public void findFood(JsonNode head, JsonNode food, ArrayList<String> possibleMoves){
            // only look for food when multiple moves are available and there is food on the board
            if(food.size() == 0 || possibleMoves.size() <= 1){
                return;
            }

            int headX = head.get("x").asInt();
            int headY = head.get("y").asInt();

            // Get closest piece of food
            JsonNode closestFood = food.get(0);
            int closestFoodDistance = Integer.MAX_VALUE;
            for(int i = 0; i < food.size(); i++) {
                JsonNode foodPiece = food.get(i);
                int foodX = foodPiece.get("x").asInt();
                int foodY = foodPiece.get("y").asInt();
                int foodDistance = Math.abs(headX - foodX) + Math.abs(headY - foodY);

                if(foodDistance < closestFoodDistance) {
                    closestFood = foodPiece;
                    closestFoodDistance = foodDistance;
                }
            }

            // Remove directions leading away from food
            int foodX = closestFood.get("x").asInt();
            int foodY = closestFood.get("y").asInt();

            if (headX < foodX) {
                possibleMoves.remove("left");
            } else if (headX > foodX) {
                possibleMoves.remove("right");
            }
            if(possibleMoves.size() <= 1){
                return;
            }
            if (headY < foodY) {
                possibleMoves.remove("down");
            } else if (headY > foodY) {
                possibleMoves.remove("up");
            }
        }

        /**
         * This method is called when a game your Battlesnake was in ends.
         * <p>
         * It is purely for informational purposes, you don't have to make any decisions
         * here.
         *
         * @param endRequest a map containing the JSON sent to this snake. Use this data
         *                   to know which game has ended
         * @return responses back to the engine are ignored.
         */
        public Map<String, String> end(JsonNode endRequest) {
            LOG.info("END");
            return EMPTY;
        }
    }

}
