package com.codingame.game;

import com.codingame.gameengine.core.AbstractPlayer.TimeoutException;
import com.codingame.gameengine.core.AbstractReferee;
import com.codingame.gameengine.core.GameManager;
import com.codingame.gameengine.core.MultiplayerGameManager;
import com.codingame.gameengine.module.entities.GraphicEntityModule;
import com.codingame.gameengine.module.entities.Sprite;
import com.codingame.gameengine.module.entities.Text;
import com.google.inject.Inject;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;

public class Referee extends AbstractReferee {
    @Inject private MultiplayerGameManager<Player> gameManager;
    @Inject private GraphicEntityModule graphicEntityModule;

    private static final int WIDTH = 8;
    private static final int HEIGHT = 8;
    private static final int MAX_TURNS = 200;
    private static final String DEAD_TILE_CHAR = "x";
    private static final String EMPTY_TILE_CHAR = "-";
    private static final String[][] GRID = new String[HEIGHT][WIDTH];
    private static final Sprite[][] PAWNS = new Sprite[HEIGHT][WIDTH];
    private static final Sprite[][] TILES = new Sprite[HEIGHT][WIDTH];
    private static Text[] PAWNS_COUNTERS = new Text[2];
    private static final int CELL_SIZE = 100;
    private static final int CANVAS_WIDTH = 1920;
    private static final int CANVAS_HEIGHT = 1080;
    private static  final String[] DIRECTIONS = {"UP", "RIGHT", "DOWN", "LEFT"};
    private static final HashMap<String, int[]> NEXT_POS;
    static
    {
        NEXT_POS = new HashMap<String, int[]>();
        NEXT_POS.put("UP", new int[] {-1, 0});
        NEXT_POS.put("DOWN", new int[] {1, 0});
        NEXT_POS.put("RIGHT", new int[] {0, 1});
        NEXT_POS.put("LEFT", new int[] {0, -1});
    }

    @Override
    public void init() {
        gameManager.setTurnMaxTime(100);
        gameManager.setMaxTurns(MAX_TURNS);
        addPawns(gameManager.getSeed());
        drawGrid();
        String panwsPerPlayer = Integer.toString(WIDTH * HEIGHT / 2);

        for (Player player : gameManager.getPlayers()) {
            // Send player id, height and width
            player.sendInputLine(String.format("%d", player.getIndex()));
            player.sendInputLine(String.format("%d", HEIGHT));
            player.sendInputLine(String.format("%d", WIDTH));
            int playerIndex = player.getIndex();
            graphicEntityModule.createText(player.getNicknameToken())
                    .setX(130 + playerIndex * 1400)
                    .setY(50 + 100 * (playerIndex / 2))
                    .setZIndex(20)
                    .setFontSize(90)
                    .setFillColor(player.getColorToken())
                    .setAnchor(0);
            graphicEntityModule.createSprite()
                    .setX(50 + playerIndex * 1400)
                    .setY(90 + 100 * (playerIndex / 2))
                    .setZIndex(20)
                    .setImage(player.getAvatarToken())
                    .setAnchor(0.5);
            PAWNS_COUNTERS[playerIndex] = graphicEntityModule.createText(panwsPerPlayer)
                    .setX(210 + playerIndex * 1400)
                    .setY(150 + 100 * (playerIndex / 2))
                    .setZIndex(20)
                    .setFontSize(90)
                    .setFillColor(player.getColorToken())
                    .setAnchor(0);
            graphicEntityModule.createText("WINNER")
                    .setX(60 + playerIndex * 1400)
                    .setY(210 + 100 * (playerIndex / 2))
                    .setZIndex(20)
                    .setFontSize(110)
                    .setFillColor(0xffff00)
                    .setRotation(-0.52)
                    .setAnchor(0);
        }
    }

    private void drawGrid() {
        int startY = Math.round((CANVAS_HEIGHT - (HEIGHT * CELL_SIZE)) / 2) + 30;
        int startX = Math.round((CANVAS_WIDTH - (WIDTH * CELL_SIZE)) / 2);

        for(int i=0; i<HEIGHT; i++){
            for(int j=0; j<WIDTH; j++){
                // Draw Tile
                TILES[i][j] = graphicEntityModule.createSprite()
                        .setX(startX +(CELL_SIZE * j))
                        .setY(startY)
                        .setImage("medievalTile_27.png")
                        .setScale(1.5625);
                String cellValue = GRID[i][j];
                // Draw Pawn
                if(cellHasPlayer(cellValue)){
                    PAWNS[i][j] = drawPawn(
                            startX +(CELL_SIZE * j) + (CELL_SIZE / 2),
                            startY + (CELL_SIZE / 2),
                            Integer.parseInt(cellValue)
                    );
                }
            }
            startY += CELL_SIZE;
        }
    }

    private Sprite drawPawn(int x, int y, int playerId) {
        return graphicEntityModule.createSprite()
                .setX(x-33)
                .setY(y-46)
                .setImage(String.format("pawn_%d.png", playerId))
                .setZIndex(2);
    }

    private void addPawns(long seed) {
        Random rng = new Random(seed);
        int pawnsPerPlayer = WIDTH * HEIGHT / 2;
        // Balance center 4*4 square
        for(int pawn = 0; pawn < 8; pawn++){
            int i, j;
            do {
                i = rng.nextInt(4) + 2;
                j = rng.nextInt(4) + 2;
            } while (GRID[i][j] != null);
            GRID[i][j] = "0";
        }

        for(int i=2; i<6; i++){
            for(int j=2; j<6; j++){
                if(GRID[i][j] == null){
                    GRID[i][j] = "1";
                }
            }
        }
        // Remainings tiles
        // Place all pawns for player 0 randomly
        for(int pawn=0; pawn<pawnsPerPlayer; pawn++){
            int i, j;
            do {
                i = rng.nextInt(HEIGHT);
                j = rng.nextInt(WIDTH);
            } while (GRID[i][j] != null);
            GRID[i][j] = "0";
        }
        // All empty cells are for player 1
        for(int i=0; i<HEIGHT; i++){
            for(int j=0; j<WIDTH; j++){
                if(GRID[i][j] == null){
                    GRID[i][j] = "1";
                }
            }
        }
    }

    @Override
    public void gameTurn(int turn) {
        String playerId = Integer.toString(turn % gameManager.getPlayerCount());
        Player player = gameManager.getPlayer(Integer.parseInt(playerId));
        String output = "";

        sendInputs(player);
        player.execute();

        // Read player output
        try {
            output = player.getOutputs().get(0);
            if(!Arrays.asList(DIRECTIONS).contains(output)){ // invalid ouput
                gameManager.addToGameSummary(String.format("Player %s played invalid output %s",
                        player.getNicknameToken(), output));
                deactivatePlayer(player, "Invalid action.");
                return;
            }
            handlePlayerOutput(output, playerId);
        } catch (IndexOutOfBoundsException e) {
            gameManager.addToGameSummary(String.format("Player %s did not output anything",
                    player.getNicknameToken()));
            deactivatePlayer(player, "No output");
            return;
        } catch (TimeoutException e) {
            gameManager.addToGameSummary(GameManager.formatErrorMessage(
                    player.getNicknameToken() + " timeout!"));
            deactivatePlayer(player, "Timeout");
            return;
        }

        gameManager.addToGameSummary(String.format("Player %s played %s",
                player.getNicknameToken(), output));

        checkWinner();
        findEmptyLines();
        findEmptyColumns();
        updatePawnCounter();
        // If we reach max turns, set the winner to the player with the most pawns left
        if(turn == MAX_TURNS - 1) {
            int[] playersPawnCount = countPlayersPawn(false);

            if(playersPawnCount[0] > playersPawnCount[1]){
                setWinner(0);
            } else if(playersPawnCount[1] > playersPawnCount[0]) {
                setWinner(1);
            } else {
                gameManager.addToGameSummary("It's a tie !");
                gameManager.endGame();
            }
        }
    }

    private void updatePawnCounter() {
        for (Player player : gameManager.getPlayers()) {
            int player_index = player.getIndex();
            PAWNS_COUNTERS[player_index]
                    .setText(Integer.toString(
                            countPlayersPawn(false)[player_index]
                    ));
        }
    }

    private void handlePlayerOutput(String output, String playerId) {
        switch(output) {
            case "UP" :
                moveUp(playerId);
                break;
            case "DOWN":
                moveDown(playerId);
                break;
            case "RIGHT":
                moveRight(playerId);
                break;
            case "LEFT":
                moveLeft(playerId);
                break;
        }
    }

    private void sendInputs(Player player) {
        for(int i=0; i<HEIGHT; i++){
            player.sendInputLine(String.join(" ", Arrays.asList(GRID[i])));
        }
    }

    private void moveUp(String playerId) {
        for(int col=0; col<WIDTH; col++) {
            for (int y = 0; y < HEIGHT; y++) {
                if (GRID[y][col].equals(playerId)) {
                    singleMoveUp(playerId, y, col);
                }
            }
        }
    }

    private boolean singleMoveUp(String playerId, int y, int x) {
        int nextY = y - 1;
        if(nextY < 0) { // player moves out of the map
            movePlayer(playerId, y, x, "UP", true);
            return true;
        }

        String aboveCellState = GRID[nextY][x];
        if(cellHasPlayer(aboveCellState)){
            singleMoveUp(aboveCellState, y - 1, x);
            singleMoveUp(playerId, y, x);
        } else {
            movePlayer(playerId, y, x, "UP", aboveCellState.equals("x"));
        }

        return true;
    }

    private void moveDown(String playerId) {
        for(int col=0; col<WIDTH; col++) {
            for (int y = HEIGHT-1; y >= 0; y--) {
                if (GRID[y][col].equals(playerId)) {
                    singleMoveDown(playerId, y, col);
                }
            }
        }
    }

    private boolean singleMoveDown(String playerId, int y, int x) {
        int nextY = y + 1;
        if(nextY > HEIGHT - 1) { // player moves out of the map
            movePlayer(playerId, y, x, "DOWN", true);
            return true;
        }

        String belowCellState = GRID[nextY][x];
        if(cellHasPlayer(belowCellState)){
            singleMoveDown(belowCellState, y + 1, x);
            singleMoveDown(playerId, y, x);
        } else {
            movePlayer(playerId, y, x, "DOWN", belowCellState.equals("x"));
        }

        return true;
    }

    private void moveRight(String playerId) {
        for (int y = 0; y < HEIGHT; y++) {
            for(int col = WIDTH - 1; col>=0; col--) {
                if(GRID[y][col].equals(playerId)){
                    singleMoveRight(playerId, y, col);
                }
            }
        }
    }

    private boolean singleMoveRight(String playerId, int y, int x) {
        int nextX = x + 1;
        if(nextX > WIDTH - 1) { // player moves out of the map
            movePlayer(playerId, y, x, "RIGHT", true);
            return true;
        }

        String rightCellState = GRID[y][nextX];
        if(cellHasPlayer(rightCellState)) {
            singleMoveRight(rightCellState, y, x + 1);
            singleMoveRight(playerId, y, x);
        } else {
            movePlayer(playerId, y, x, "RIGHT", rightCellState.equals("x"));
        }

        return true;
    }

    private void moveLeft(String playerId) {
        for (int y = 0; y < HEIGHT; y++) {
            for(int col = 0; col<WIDTH; col++) {
                if(GRID[y][col].equals(playerId)){
                    singleMoveLeft(playerId, y, col);
                }
            }
        }
    }

    private boolean singleMoveLeft(String playerId, int y, int x) {
        int nextX = x - 1;
        if(nextX < 0) { // player moves out of the map
            movePlayer(playerId, y, x, "LEFT", true);
            return true;
        }

        String leftCellState = GRID[y][nextX];
        if(cellHasPlayer(leftCellState)) {
            singleMoveLeft(leftCellState, y, x - 1);
            singleMoveLeft(playerId, y, x);
        } else {
            movePlayer(playerId, y, x, "LEFT", leftCellState.equals("x"));
        }

        return true;
    }

    private void findEmptyColumns() {
        // columns from the left
        for(int i=0; i<WIDTH; i++) {
            if(!checkColumn(i)) {
                break;
            }
        }
        // columns from the right
        for(int i=WIDTH-1; i>=0; i--) {
            if(!checkColumn(i)) {
                break;
            }
        }
    }

    private boolean checkColumn(int columnIndex) {
        boolean columnToRemove = true;
        boolean deadColumn = true;
        for(int j=0; j<HEIGHT; j++) {
            String cellValue = GRID[j][columnIndex];
            if(!cellValue.equals("x")) {
                deadColumn = false;
            }
            if(cellHasPlayer(cellValue)) {
                columnToRemove = false;
                break;
            }
        }
        if(columnToRemove) {
            removeColumn(columnIndex);
        } else if(!deadColumn) { // Player(s) found on the line / col
            return false;
        }

        return true;
    }

    private void removeColumn(int columnIndex) {
        // TODO: Animation
        for(int i=0; i<HEIGHT; i++) {
            Sprite tile = TILES[i][columnIndex];
            tile.setAlpha(0);
            removeTile(i, columnIndex);
        }
    }

    private void findEmptyLines() {
        // lines from the top
        for(int i=0; i<HEIGHT; i++) {
            if(!checkline(i)) {
                break;
            }
        }
        // lines from the bottom
        for(int i=HEIGHT-1; i>=0; i--) {
            if(!checkline(i)) {
                break;
            }
        }
    }

    private boolean checkline(int lineIndex) {
        boolean lineToRemove = true;
        boolean deadLine = true;
        for(int j=0; j<WIDTH; j++) {
            String cellValue = GRID[lineIndex][j];
            if(!cellValue.equals("x")) {
                deadLine = false;
            }
            if(cellHasPlayer(cellValue)) {
                lineToRemove = false;
                break;
            }
        }
        if(lineToRemove) {
            removeLine(lineIndex);
        } else if(!deadLine) { // Player(s) found on the line / col
            return false;
        }

        return true;
    }

    private void removeLine(int lineIndex) {
        // TODO: Animation
        for(int i=0; i<WIDTH; i++) {
            Sprite tile = TILES[lineIndex][i];
            tile.setAlpha(0);
            removeTile(lineIndex, i);
        }
    }

    private void removeTile(int y, int x) {
        GRID[y][x] = DEAD_TILE_CHAR;
    }

    private int[] countPlayersPawn(boolean breakIfInProgress) {
        int playerA = 0;
        int playerB = 0;

        for (int y = 0; y < HEIGHT; y++) {
            for(int col = 0; col < WIDTH; col++) {
                String cellState = GRID[y][col];
                if(cellState.equals("0")){
                    playerA++;
                } else if(cellState.equals("1")){
                    playerB++;
                }

                if(breakIfInProgress && playerA > 0 && playerB > 0) { // Game still in progress
                    break;
                }
            }
        }

        return new int[] {playerA, playerB};
    }

    private boolean checkWinner() {
        int[] playersPawnCount = countPlayersPawn(true);

        if(playersPawnCount[0] == 0){
            setWinner(1);
        } else if(playersPawnCount[1] == 0) {
            setWinner(0);
        } else {
            return false;
        }

        return true;
    }

    private void setWinner(int playerId) {
        Player winner = gameManager.getPlayer(playerId);
        gameManager.addToGameSummary(GameManager.formatSuccessMessage(
                winner.getNicknameToken() + " won!"));
        winner.setScore(1);
        gameManager.endGame();
    }

    private boolean cellHasPlayer(String cellValue) {
        // Check if a grid cell has a player on it
        return cellValue.equals("0") || cellValue.equals("1");
    }

    private void movePlayer(String playerId, int y, int x, String direction, boolean removeAfter) {
        Sprite pawn = PAWNS[y][x];

        switch (direction) {
            case "UP":
                pawn.setY(pawn.getY() - CELL_SIZE);
                break;
            case "DOWN":
                pawn.setY(pawn.getY() + CELL_SIZE);
                break;
            case "RIGHT":
                pawn.setX(pawn.getX() + CELL_SIZE);
                break;
            case "LEFT":
                pawn.setX(pawn.getX() - CELL_SIZE);
                break;
        }

        if(removeAfter) {
            pawn.setImage(String.format("pawn_%s_hurt.png", playerId));
            graphicEntityModule.commitEntityState(0.7, pawn);
            pawn.setAlpha(0);
        } else {
            int[] dirNextPos = NEXT_POS.get(direction);
            int[] nextPosCoord = {y+dirNextPos[0], x+dirNextPos[1]};
            GRID[nextPosCoord[0]][nextPosCoord[1]] = playerId;
            PAWNS[nextPosCoord[0]][nextPosCoord[1]] = pawn;
        }
        GRID[y][x] = EMPTY_TILE_CHAR;
        PAWNS[y][x] = null;
    }

    private void deactivatePlayer(Player player, String reason) {
        player.deactivate(reason);
        player.setScore(-1);
        gameManager.endGame();
    }
}
