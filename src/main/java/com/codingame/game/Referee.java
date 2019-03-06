package com.codingame.game;

import com.codingame.gameengine.core.AbstractPlayer.TimeoutException;
import com.codingame.gameengine.core.AbstractReferee;
import com.codingame.gameengine.core.GameManager;
import com.codingame.gameengine.core.MultiplayerGameManager;
import com.codingame.gameengine.module.entities.GraphicEntityModule;
import com.codingame.gameengine.module.entities.Sprite;
import com.google.inject.Inject;

import java.util.Arrays;
import java.util.HashMap;

public class Referee extends AbstractReferee {
    @Inject private MultiplayerGameManager<Player> gameManager;
    @Inject private GraphicEntityModule graphicEntityModule;

    private static final int WIDTH = 6;
    private static final int HEIGHT = 6;
    private static final String[][] GRID = new String[HEIGHT][WIDTH];
    private static final Sprite[][] PAWNS = new Sprite[HEIGHT][WIDTH];
    private static final Sprite[][] TILES = new Sprite[HEIGHT][WIDTH];
    private static final int CELL_SIZE = 100;
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

        for (Player player : gameManager.getPlayers()) {
            // Send player id, height and width
            player.sendInputLine(String.format("%d", player.getIndex()));
            player.sendInputLine(String.format("%d", HEIGHT));
            player.sendInputLine(String.format("%d", WIDTH));
            graphicEntityModule.createText(player.getNicknameToken())
                    .setX(180 + (player.getIndex() % 2) * 1400)
                    .setY(50 + 100 * (player.getIndex() / 2))
                    .setZIndex(20)
                    .setFontSize(90)
                    .setFillColor(player.getColorToken())
                    .setAnchor(0);

            graphicEntityModule.createSprite()
                    .setX(100 + (player.getIndex() % 2) * 1400)
                    .setY(90 + 100 * (player.getIndex() / 2))
                    .setZIndex(20)
                    .setImage(player.getAvatarToken())
                    .setAnchor(0.5);
        }
    }

    private void drawGrid() {
        int start_y = Math.round((CANVAS_HEIGHT - (HEIGHT * CELL_SIZE)) / 2);
        int start_x = Math.round((CANVAS_WIDTH - (WIDTH * CELL_SIZE)) / 2);

        for(int i=0; i<HEIGHT; i++){
            for(int j=0; j<WIDTH; j++){
                // Draw Tile
                TILES[i][j] = graphicEntityModule.createSprite()
                        .setX(start_x +(CELL_SIZE * j))
                        .setY(start_y)
                        .setImage("medievalTile_27.png")
                        .setScale(1.5625);
                String cell_value = GRID[i][j];
                // Draw Pawn
                if(cell_has_player(cell_value)){
                    PAWNS[i][j] = drawPawn(
                            start_x +(CELL_SIZE * j) + (CELL_SIZE / 2),
                            start_y + (CELL_SIZE / 2),
                            Integer.parseInt(cell_value)
                    );
                }
            }
            start_y += CELL_SIZE;
        }
    }

    private Sprite drawPawn(int x, int y, int player_id) {
        return graphicEntityModule.createSprite()
                .setX(x-33)
                .setY(y-46)
                .setImage(String.format("pawn_%d.png", player_id))
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
            }
            handlePlayerOutput(output, player_id);
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
        find_empty_lines();
        find_empty_columns();
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
        if(cell_has_player(above_cell_state)){
            single_move_up(above_cell_state, y - 1, x);
            single_move_up(player_id, y, x);
        } else {
            move_circle(player_id, y, x, "UP", above_cell_state.equals("x"));
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
        if(cell_has_player(below_cell_state)){
            single_move_down(below_cell_state, y + 1, x);
            single_move_down(player_id, y, x);
        } else {
            move_circle(player_id, y, x, "DOWN", below_cell_state.equals("x"));
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
        if(cell_has_player(right_cell_state)) {
            single_move_right(right_cell_state, y, x + 1);
            single_move_right(player_id, y, x);
        } else {
            move_circle(player_id, y, x, "RIGHT", right_cell_state.equals("x"));
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
        if(cell_has_player(left_cell_state)) {
            single_move_left(left_cell_state, y, x - 1);
            single_move_left(player_id, y, x);
        } else {
            move_circle(player_id, y, x, "LEFT", left_cell_state.equals("x"));
        }

        return true;
    }

    private void find_empty_columns() {
        // columns from the top
        for(int i=0; i<WIDTH; i++) {
            if(!checkColumn(i)) {
                break;
            }
        }
        // columns from the bottom
        for(int i=WIDTH-1; i>=0; i--) {
            if(!checkColumn(i)) {
                break;
            }
        }
    }

    private boolean checkColumn(int column_index) {
        boolean column_to_remove = true;
        boolean dead_column = true;
        for(int j=0; j<HEIGHT; j++) {
            String cell_value = GRID[j][column_index];
            if(!cell_value.equals("x")) {
                dead_column = false;
            }
            if(cell_has_player(cell_value)) {
                column_to_remove = false;
                break;
            }
        }
        if(column_to_remove) {
            remove_column(column_index);
        } else if(!dead_column) { // Player(s) found on the line / col
            return false;
        }

        return true;
    }

    private void remove_column(int column_index) {
        // TODO: Animation
        for(int i=0; i<HEIGHT; i++) {
            Sprite tile = TILES[i][column_index];
            tile.setAlpha(0);
            GRID[i][column_index] = "x";
        }
    }

    private void find_empty_lines() {
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

    private boolean checkline(int line_index) {
        boolean line_to_remove = true;
        boolean dead_line = true;
        for(int j=0; j<WIDTH; j++) {
            String cell_value = GRID[line_index][j];
            if(!cell_value.equals("x")) {
                dead_line = false;
            }
            if(cell_has_player(cell_value)) {
                line_to_remove = false;
                break;
            }
        }
        if(line_to_remove) {
            remove_line(line_index);
        } else if(!dead_line) { // Player(s) found on the line / col
            return false;
        }

        return true;
    }

    private void remove_line(int line_index) {
        // TODO: Animation
        for(int i=0; i<WIDTH; i++) {
            Sprite tile = TILES[line_index][i];
            tile.setAlpha(0);
            GRID[line_index][i] = "x";
        }
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

        if(remove_after) {
            pawn.setImage(String.format("pawn_%s_hurt.png", player_id));
            graphicEntityModule.commitEntityState(0.7, pawn);
            pawn.setAlpha(0);
        } else {
            int[] dir_next_pos = NEXT_POS.get(direction);
            int[] next_pos_coord = {y+dir_next_pos[0], x+dir_next_pos[1]};
            GRID[next_pos_coord[0]][next_pos_coord[1]] = player_id;
            PAWNS[next_pos_coord[0]][next_pos_coord[1]] = pawn;
        }
        GRID[y][x] = "-";
        PAWNS[y][x] = null;
    }

    private void deactivate_player(Player player, String reason) {
        player.deactivate(reason);
        player.setScore(-1);
        gameManager.endGame();
    }
}
