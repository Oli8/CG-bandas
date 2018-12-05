import java.util.Random;
import java.util.Scanner;

public class Agent1 {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        while (true) {
            // String input = scanner.nextLine();
            String[] directions = {"UP", "RIGHT", "DOWN", "LEFT"};
            System.out.println(
                    directions[(new Random()).nextInt(directions.length)]);
        }
    }
}
