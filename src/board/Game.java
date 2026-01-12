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
        boolean isPawnMove = true;
        boolean isCaptureMove = false;
        // Hard code O-O and O-O-O here

        // If no uppercase in string, is pawn. Else is piece.
        for(int i = 0; i < input.length(); i++){
            if(Character.isUpperCase(input.charAt(i))){
                isPawnMove = false;
            }
            if(input.charAt(i) == 'x'){
                isCaptureMove = true;
            }
        }

        if(isPawnMove){
            
            }
        }




    }


