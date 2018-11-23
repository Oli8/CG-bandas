import com.codingame.gameengine.runner.MultiplayerGameRunner;

public class Main {
    public static void main(String[] args) {
        // Sets a test case
        // gameRunner.setTestCase("test1.json");

        /* Multiplayer Game */
        MultiplayerGameRunner gameRunner = new MultiplayerGameRunner();

        // Adds as many player as you need to test your game
        gameRunner.addAgent(Agent1.class);
        gameRunner.addAgent(Agent1.class);

        // Another way to add a player
        // gameRunner.addAgent("python3 /home/user/player.py");

        gameRunner.start();
    }
}
