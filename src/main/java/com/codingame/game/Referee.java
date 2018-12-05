package com.codingame.game;

import com.codingame.gameengine.core.AbstractPlayer.TimeoutException;
import com.codingame.gameengine.core.AbstractReferee;
import com.codingame.gameengine.core.GameManager;
import com.codingame.gameengine.core.MultiplayerGameManager;
import com.codingame.gameengine.module.entities.GraphicEntityModule;
import com.google.inject.Inject;
import com.codingame.gameengine.module.entities.Circle;

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
    private static int[] PLAYER_COLOR= {0x3498db, 0xf88379};
    private static final int PAWN_RADIUS = 30;
    private static final int CANVAS_WIDTH = 1920;
    private static final int CANVAS_HEIGHT = 1080;
    private static final int GRID_ORIGIN_X = (int) Math.round(CANVAS_WIDTH / 2 - CELL_SIZE);
    private static final int GRID_ORIGIN_Y = (int) Math.round(CANVAS_HEIGHT / 2 - CELL_SIZE);

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
        Player player = gameManager.getPlayer(turn % gameManager.getPlayerCount());
        String output = "";

        player.execute();

        // Read player output
        try {
            output = player.getOutputs().get(0);
            String[] direction = {"UP", "RIGHT", "DOWN", "LEFT"};
            if(!Arrays.asList(direction).contains(output)){ // invalid ouput
                gameManager.addToGameSummary(String.format("Player %s played invalid output %s",
                        player.getNicknameToken(), output));
                player.deactivate("Invalid action.");
                player.setScore(-1);
                gameManager.endGame();
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
}
