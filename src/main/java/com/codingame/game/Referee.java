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
    private int[][] grid = new int[WIDTH][HEIGHT];
    private static final int CELL_SIZE = 100;
    private static final int LINE_WIDTH = 5;
    private static final int LINE_COLOR = 0xff0000;
    private static final int CANVAS_WIDTH = 1920;
    private static final int CANVAS_HEIGHT = 1080;
    private static final int GRID_ORIGIN_X = (int) Math.round(CANVAS_WIDTH / 2 - CELL_SIZE);
    private static final int GRID_ORIGIN_Y = (int) Math.round(CANVAS_HEIGHT / 2 - CELL_SIZE);

    @Override
    public void init() {
        // Display the background image. The asset image must be in the directory src/main/resources/view/assets
        /*
        graphicEntityModule.createSprite()
                .setImage("Background.jpg")
                .setAnchor(0);
        */
        drawGrid();
        addPawns();
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
            }
            start_y += CELL_SIZE;
        }
    }

    private void addPawns() {
        int pawns_per_player= WIDTH * HEIGHT / 2;
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
                grid[i][j] = player_index;
            }
            System.out.println(Arrays.toString(grid[i]));
        }
    }

    private int convertX(double unit) {
        return (int) (GRID_ORIGIN_X + unit * CELL_SIZE);
    }

    private int convertY(double unit) {
        return (int) (GRID_ORIGIN_Y + unit * CELL_SIZE);
    }

    @Override
    public void gameTurn(int turn) {
        Player player = gameManager.getPlayer(turn % gameManager.getPlayerCount());
        String output = "";

        player.execute();

        // Read player inputs (output) ?
        try {
            output = player.getOutputs().get(0);
        } catch (NumberFormatException e) {
            player.deactivate();
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
