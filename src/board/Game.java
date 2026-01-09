package board;

import pieces.Piece;
import enums.*;
import java.io.*;
import java.util.regex.Pattern;

public class Game {
    private Board board;
    private boolean isWhiteTurn;

    public Game() {
        this.board = new Board();
        this.isWhiteTurn = true;
    }

    public void start() {
        BufferedReader keyboard = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            board.printBoard();
            System.out.println((isWhiteTurn ? "White" : "Black") + "'s Turn");
            System.out.print("Enter Move: ");
            try {
                String input = keyboard.readLine();
                if (input.equals("exit")) {
                    break;
                }
                processInput(input);
            } catch (IOException e) {
                System.out.println("Invalid move! ");
            }
        }
    }

    private void processInput(String input) {
        // Case 1: standard move
        

    }

}
