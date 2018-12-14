package com.codingame.game;

import com.codingame.gameengine.core.AbstractPlayer.TimeoutException;
import com.codingame.gameengine.core.AbstractReferee;
import com.codingame.gameengine.core.GameManager;
import com.codingame.gameengine.core.MultiplayerGameManager;
import com.codingame.gameengine.module.entities.GraphicEntityModule;
import com.codingame.gameengine.module.entities.Circle;
import com.google.inject.Inject;

import java.util.Arrays;
import java.util.HashMap;

public class Referee extends AbstractReferee {
    @Inject private MultiplayerGameManager<Player> gameManager;
    @Inject private GraphicEntityModule graphicEntityModule;

    private static final int WIDTH = 6;
    private static final int HEIGHT = 6;
    private static final String[][] GRID = new String[HEIGHT][WIDTH];
    private static final Circle[][] GRAPHICS = new Circle[HEIGHT][WIDTH];
    private static final int CELL_SIZE = 100;
    private static final int LINE_WIDTH = 5;
    private static final int LINE_COLOR = 0x2ecc71;
    private static final int[] PLAYER_COLOR= {0xf88379, 0x3498db};
    private static final int PAWN_RADIUS = 30;
    private static final int CANVAS_WIDTH = 1920;
    private static final int CANVAS_HEIGHT = 1080;
    private static  final String[] DIRECTIONS = {"UP", "RIGHT", "DOWN", "LEFT"};
    private static final HashMap<String, int[]> NEXT_POS;
    static
    {
        NEXT_POS = new HashMap<String, int[]>();
        int[] up_next_pos = {-1, 0};
        NEXT_POS.put("UP", up_next_pos);

        int[] down_next_pos = {1, 0};
        NEXT_POS.put("DOWN", down_next_pos);

        int[] right_next_pos = {0, 1};
        NEXT_POS.put("RIGHT", right_next_pos);

        int[] left_next_pos = {0, -1};
        NEXT_POS.put("LEFT", left_next_pos);
    }

    @Override
    public void init() {
        addPawns();
        drawGrid();
    }

    private void drawGrid() {
        int start_y = Math.round((CANVAS_HEIGHT - (HEIGHT * CELL_SIZE)) / 2);
        int start_x = Math.round((CANVAS_WIDTH - (WIDTH * CELL_SIZE)) / 2);

        for(int i=0; i<HEIGHT; i++){
            for(int j=0; j<WIDTH; j++){
                // Draw Tile
                graphicEntityModule.createRectangle()
                        .setX(start_x +(CELL_SIZE * j))
                        .setY(start_y)
                        .setLineWidth(LINE_WIDTH)
                        .setLineColor(LINE_COLOR)
                        .setHeight(CELL_SIZE)
                        .setWidth(CELL_SIZE)
                        .setZIndex(1);
                String cell_value = GRID[i][j];
                // Draw Pawn
                if(cell_has_player(cell_value)){
                    GRAPHICS[i][j] = drawPawn(
                            start_x +(CELL_SIZE * j) + (CELL_SIZE / 2),
                            start_y + (CELL_SIZE / 2),
                            Integer.parseInt(cell_value)
                    );
                }
            }
            start_y += CELL_SIZE;
        }
    }

    private Circle drawPawn(int x, int y, int player_id) {
        return graphicEntityModule.createCircle()
                .setRadius(PAWN_RADIUS)
                .setFillColor(PLAYER_COLOR[player_id])
                .setX(x)
                .setY(y)
                .setZIndex(2);
    }

    private void addPawns() {
        int pawns_per_player = WIDTH * HEIGHT / 2;
        int[] pawns = {pawns_per_player, pawns_per_player};

        for(int i=0; i<HEIGHT; i++){
            for(int j=0; j<WIDTH; j++){
                int player_index;
                if(pawns[0] == 0){
                    player_index = 1;
                } else if (pawns[1] == 0){
                    player_index = 0;
                } else {
                    player_index = (int) Math.round(Math.random());
                }
                pawns[player_index] -= 1;
                GRID[i][j] = Integer.toString(player_index);
            }
        }
    }

    @Override
    public void gameTurn(int turn) {
        String player_id = Integer.toString(turn % gameManager.getPlayerCount());
        Player player = gameManager.getPlayer(Integer.parseInt(player_id));
        String output = "";

        sendInputs(player);
        player.execute();

        // Read player output
        try {
            output = player.getOutputs().get(0);
            if(!Arrays.asList(DIRECTIONS).contains(output)){ // invalid ouput
                gameManager.addToGameSummary(String.format("Player %s played invalid output %s",
                        player.getNicknameToken(), output));
                deactivate_player(player, "Invalid action.");
                return;
            } else {
                handlePlayerOutput(output, player_id);
            }
        } catch (IndexOutOfBoundsException e) {
            gameManager.addToGameSummary(String.format("Player %s did not output anything",
                    player.getNicknameToken()));
            deactivate_player(player, "No output");
            return;
        } catch (TimeoutException e) {
            gameManager.addToGameSummary(GameManager.formatErrorMessage(
                    player.getNicknameToken() + " timeout!"));
            deactivate_player(player, "Timeout");
            return;
        }

        gameManager.addToGameSummary(String.format("Player %s played %s",
                player.getNicknameToken(), output));

        checkWinner();
    }

    private void handlePlayerOutput(String output, String player_id) {
        switch(output) {
            case "UP" :
                move_up(player_id);
                break;
            case "DOWN":
                move_down(player_id);
                break;
            case "RIGHT":
                move_right(player_id);
                break;
            case "LEFT":
                move_left(player_id);
                break;
        }
    }

    private void sendInputs(Player player) {
        for(int i=0; i<HEIGHT; i++){
            player.sendInputLine(String.join(" ", Arrays.asList(GRID[i])));
        }
    }

    private void move_up(String player_id) {
        for(int col=0; col<WIDTH; col++) {
            for (int y = 0; y < HEIGHT; y++) {
                if (GRID[y][col].equals(player_id)) {
                    single_move_up(player_id, y, col);
                }
            }
        }
    }

    private boolean single_move_up(String player_id, int y, int x) {
        int next_y = y - 1;
        if(next_y < 0) { // player moves out of the map
            move_circle(player_id, y, x, "UP", true);
            return true;
        }

        String above_cell_state = GRID[next_y][x];
        if(above_cell_state.equals("-")) { // case empty
            move_circle(player_id, y, x, "UP", false);
        }
        else if(cell_has_player(above_cell_state)){
            single_move_up(above_cell_state, y - 1, x);
            single_move_up(player_id, y, x);
        }

        return true;
    }

    private void move_down(String player_id) {
        for(int col=0; col<WIDTH; col++) {
            for (int y = HEIGHT-1; y >= 0; y--) {
                if (GRID[y][col].equals(player_id)) {
                    single_move_down(player_id, y, col);
                }
            }
        }
    }

    private boolean single_move_down(String player_id, int y, int x) {
        int next_y = y + 1;
        if(next_y > HEIGHT - 1) { // player moves out of the map
            move_circle(player_id, y, x, "DOWN", true);
            return true;
        }

        String below_cell_state = GRID[next_y][x];
        if(below_cell_state.equals("-")) { // case empty
            move_circle(player_id, y, x, "DOWN", false);
        }
        else if(cell_has_player(below_cell_state)){
            single_move_down(below_cell_state, y + 1, x);
            single_move_down(player_id, y, x);
        }

        return true;
    }

    private void move_right(String player_id) {
        for (int y = 0; y < HEIGHT; y++) {
            for(int col = WIDTH - 1; col>=0; col--) {
                if(GRID[y][col].equals(player_id)){
                    single_move_right(player_id, y, col);
                }
            }
        }
    }

    private boolean single_move_right(String player_id, int y, int x) {
        int next_x = x + 1;
        if(next_x > WIDTH - 1) { // player moves out of the map
            move_circle(player_id, y, x, "RIGHT", true);
            return true;
        }

        String right_cell_state = GRID[y][next_x];
        if(right_cell_state.equals("-")) { // case empty
            move_circle(player_id, y, x, "RIGHT", false);
        }
        else if(cell_has_player(right_cell_state)){
            single_move_right(right_cell_state, y, x + 1);
            single_move_right(player_id, y, x);
        }

        return true;
    }

    private void move_left(String player_id) {
        for (int y = 0; y < HEIGHT; y++) {
            for(int col = 0; col<WIDTH; col++) {
                if(GRID[y][col].equals(player_id)){
                    single_move_left(player_id, y, col);
                }
            }
        }
    }

    private boolean single_move_left(String player_id, int y, int x) {
        int next_x = x - 1;
        if(next_x < 0) { // player moves out of the map
            move_circle(player_id, y, x, "LEFT", true);
            return true;
        }

        String left_cell_state = GRID[y][next_x];
        if(left_cell_state.equals("-")) { // case empty
            move_circle(player_id, y, x, "LEFT", false);
        }
        else if(cell_has_player(left_cell_state)){
            single_move_left(left_cell_state, y, x - 1);
            single_move_left(player_id, y, x);
        }

        return true;
    }

    private boolean checkWinner() {
        int player_a = 0;
        int player_b = 0;

        for (int y = 0; y < HEIGHT; y++) {
            for(int col = 0; col < WIDTH; col++) {
                String cell_state = GRID[y][col];
                if(cell_state.equals("0")){
                    player_a++;
                } else if(cell_state.equals("1")){
                    player_b++;
                }

                if(player_a > 0 && player_b > 0) { // Game still in progress
                    return false;
                }
            }
        }

        Player winner;
        if(player_a == 0){
            winner = gameManager.getPlayer(1);
        } else {
            winner = gameManager.getPlayer(0);
        }

        gameManager.addToGameSummary(GameManager.formatSuccessMessage(
                winner.getNicknameToken() + " won!"));
        winner.setScore(1);
        gameManager.endGame();
        return true;
    }

    private boolean cell_has_player(String cell_value) {
        // Check if a grid cell has a player on it
        return cell_value.equals("0") || cell_value.equals("1");
    }

    private void move_circle(String player_id, int y, int x, String direction, boolean remove_after) {
        Circle pawn = GRAPHICS[y][x];

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

        if(remove_after) {
            pawn.setVisible(false);
        } else {
            int[] dir_next_pos = NEXT_POS.get(direction);
            int[] next_pos_coord = {y+dir_next_pos[0], x+dir_next_pos[1]};
            GRID[next_pos_coord[0]][next_pos_coord[1]] = player_id;
            GRAPHICS[next_pos_coord[0]][next_pos_coord[1]] = pawn;
        }
        GRID[y][x] = "-";
        GRAPHICS[y][x] = null;
    }

    private void deactivate_player(Player player, String reason) {
        player.deactivate(reason);
        player.setScore(-1);
        gameManager.endGame();
    }
}
