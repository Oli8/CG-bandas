package com.codingame.game;

import com.codingame.gameengine.core.AbstractPlayer.TimeoutException;
import com.codingame.gameengine.core.AbstractReferee;
import com.codingame.gameengine.core.GameManager;
import com.codingame.gameengine.core.MultiplayerGameManager;
import com.codingame.gameengine.module.entities.GraphicEntityModule;
import com.google.inject.Inject;
import com.codingame.gameengine.module.entities.Circle;

public class Referee extends AbstractReferee {
    @Inject private MultiplayerGameManager<Player> gameManager;
    @Inject private GraphicEntityModule graphicEntityModule;

    private static final int WIDTH = 3;
    private static final int HEIGHT = 6;
    private int[][] grid = new int[WIDTH][HEIGHT];
    private static final int CELL_SIZE = 100;
    private static final int LINE_WIDTH = 10;
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
    }

    private void drawGrid() {
        int start_y = (int) Math.round((CANVAS_HEIGHT - (HEIGHT * CELL_SIZE)) / 2);
        int start_x = (int) Math.round((CANVAS_WIDTH - (WIDTH * CELL_SIZE)) / 2);
        System.out.println(start_y);
        int cell_y = start_y;
        int cell_x = 100; // TODO: dynamic start x
        // Horizontal lines
        for(int i=0; i<HEIGHT; i++){
            for(int j=0; j<WIDTH; j++){
                graphicEntityModule.createRectangle()
                        .setX(start_x +(CELL_SIZE * j))
                        .setY(cell_y)
                        .setLineWidth(5)
                        .setLineColor(LINE_COLOR)
                        .setHeight(CELL_SIZE)
                        .setWidth(CELL_SIZE);
            }
            cell_y += CELL_SIZE;
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
