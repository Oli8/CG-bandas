package com.codingame.game;

import com.codingame.gameengine.core.AbstractPlayer.TimeoutException;
import com.codingame.gameengine.core.AbstractReferee;
import com.codingame.gameengine.core.GameManager;
import com.codingame.gameengine.core.MultiplayerGameManager;
import com.codingame.gameengine.module.entities.GraphicEntityModule;
import com.google.inject.Inject;

import java.util.Arrays;

public class Referee extends AbstractReferee {
    @Inject private MultiplayerGameManager<Player> gameManager;
    @Inject private GraphicEntityModule graphicEntityModule;

    private static final int WIDTH = 6;
    private static final int HEIGHT = 6;
    private static final String[][] grid = new String[HEIGHT][WIDTH];
    private static final int CELL_SIZE = 100;
    private static final int LINE_WIDTH = 5;
    private static final int LINE_COLOR = 0x2ecc71;
    private static int[] PLAYER_COLOR= {0xf88379, 0x3498db};
    private static final int PAWN_RADIUS = 30;
    private static final int CANVAS_WIDTH = 1920;
    private static final int CANVAS_HEIGHT = 1080;
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
                graphicEntityModule.createRectangle()
                        .setX(start_x +(CELL_SIZE * j))
                        .setY(start_y)
                        .setLineWidth(LINE_WIDTH)
                        .setLineColor(LINE_COLOR)
                        .setHeight(CELL_SIZE)
                        .setWidth(CELL_SIZE);
                String cell_value = grid[i][j];
                if(cell_value.equals("0") || cell_value.equals("1")){
                    drawPawn(
                            start_x +(CELL_SIZE * j) + (CELL_SIZE / 2),
                            start_y + (CELL_SIZE / 2),
                            Integer.parseInt(cell_value)
                    );
                }
            }
            start_y += CELL_SIZE;
        }
    }

    private void drawPawn(int x, int y, int player_id) {
        graphicEntityModule.createCircle()
                .setRadius(PAWN_RADIUS)
                .setFillColor(PLAYER_COLOR[player_id])
                .setX(x)
                .setY(y);
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
                grid[i][j] = Integer.toString(player_index);
            }
            System.out.println(Arrays.toString(grid[i]));
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
            String[] directions = {"UP", "RIGHT", "DOWN", "LEFT"};
            if(!Arrays.asList(directions).contains(output)){ // invalid ouput
                gameManager.addToGameSummary(String.format("Player %s played invalid output %s",
                        player.getNicknameToken(), output));
                player.deactivate("Invalid action.");
                player.setScore(-1);
                gameManager.endGame();
            } else {
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
        } catch (IndexOutOfBoundsException e) {
            player.deactivate("No output");
            player.setScore(-1);
            gameManager.endGame();
        } catch (TimeoutException e) {
            gameManager.addToGameSummary(GameManager.formatErrorMessage(
                    player.getNicknameToken() + " timeout!"));
            player.deactivate();
            player.setScore(-1);
            gameManager.endGame();
        }

        gameManager.addToGameSummary(String.format("Player %s played %s",
                player.getNicknameToken(), output));
    }

    private void sendInputs(Player player) {
        for(int i=0; i<HEIGHT; i++){
            player.sendInputLine(String.join(" ", Arrays.asList(grid[i])));
        }
    }

    private void move_up(String player_id) {
        // TODO: DRY and stuff
        for(int col=0; col<WIDTH; col++) {
            for (int y = 0; y < HEIGHT; y++) {
                if (grid[y][col].equals(player_id)) {
                    single_move_up(player_id, y, col);
                }
            }
        }
        drawGrid();
    }

    private boolean single_move_up(String player_id, int y, int x) {
        int next_y = y - 1;
        if(next_y < 0) { // player moves out of the map
            grid[y][x] = "-";
            return true;
        }

        String above_cell_state = grid[next_y][x];
        if(above_cell_state.equals("-")) { // case empty
            grid[y-1][x] = player_id;
            grid[y][x] = "-";
        }
        else if(above_cell_state.equals("0") || above_cell_state.equals("1")){
            single_move_up(above_cell_state, y - 1, x);
            single_move_up(player_id, y, x);
        }

        return true;
    }

    private void move_down(String player_id) {
        for(int col=0; col<WIDTH; col++) {
            for (int y = HEIGHT-1; y >= 0; y--) {
                if (grid[y][col].equals(player_id)) {
                    single_move_down(player_id, y, col);
                }
            }
        }
        drawGrid();
    }

    private boolean single_move_down(String player_id, int y, int x) {
        int next_y = y + 1;
        if(next_y > HEIGHT - 1) { // player moves out of the map
            grid[y][x] = "-";
            return true;
        }

        String above_cell_state = grid[next_y][x];
        if(above_cell_state.equals("-")) { // case empty
            grid[y+1][x] = player_id;
            grid[y][x] = "-";
        }
        else if(above_cell_state.equals("0") || above_cell_state.equals("1")){
            single_move_down(above_cell_state, y + 1, x);
            single_move_down(player_id, y, x);
        }

        return true;
    }

    private void move_right(String player_id) {
        for (int y = 0; y < HEIGHT; y++) {
            for(int col = WIDTH - 1; col>=0; col--) {
                if(grid[y][col].equals(player_id)){
                    single_move_right(player_id, y, col);
                }
            }
        }
        drawGrid();
    }

    private boolean single_move_right(String player_id, int y, int x) {
        int next_x = x + 1;
        if(next_x > WIDTH - 1) { // player moves out of the map
            grid[y][x] = "-";
            return true;
        }

        String above_cell_state = grid[y][next_x];
        if(above_cell_state.equals("-")) { // case empty
            grid[y][x+1] = player_id;
            grid[y][x] = "-";
        }
        else if(above_cell_state.equals("0") || above_cell_state.equals("1")){
            single_move_right(above_cell_state, y, x + 1);
            single_move_right(player_id, y, x);
        }

        return true;
    }

    private void move_left(String player_id) {
        for (int y = 0; y < HEIGHT; y++) {
            for(int col = 0; col<WIDTH; col++) {
                if(grid[y][col].equals(player_id)){
                    single_move_left(player_id, y, col);
                }
            }
        }
        drawGrid();
    }

    private boolean single_move_left(String player_id, int y, int x) {
        int next_x = x - 1;
        if(next_x < 0) { // player moves out of the map
            grid[y][x] = "-";
            return true;
        }

        String above_cell_state = grid[y][next_x];
        if(above_cell_state.equals("-")) { // case empty
            grid[y][x-1] = player_id;
            grid[y][x] = "-";
        }
        else if(above_cell_state.equals("0") || above_cell_state.equals("1")){
            single_move_left(above_cell_state, y, x - 1);
            single_move_left(player_id, y, x);
        }

        return true;
    }

    private String get_opponent_id(String player_id) {
        return Integer.toString(1 - Integer.parseInt(player_id));
    }
}
