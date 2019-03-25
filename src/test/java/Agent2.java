import java.util.Random;
import java.util.Scanner;

public class Agent2 {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        final int MY_ID = scanner.nextInt();
        while (true) {
            String[] directions = {"UP", "RIGHT", "DOWN", "LEFT"};
            System.out.println(
                    directions[(new Random()).nextInt(directions.length)]);
        }
    }
}
