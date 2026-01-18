package board;

import pieces.Piece;
import pieces.King;
import enums.*;
import java.io.*;

public class Game {
    private Board board;
    private boolean isWhiteTurn;

    public Game() {
        this.board = new Board();
        this.isWhiteTurn = true;
    }

    public void start() {
        BufferedReader keyboard = new BufferedReader(new InputStreamReader(System.in));
        String gameStateWhite = board.getGameState(pieceColour.WHITE);
        String gameStateBlack = board.getGameState(pieceColour.BLACK);
        while (gameStateWhite.equals("PLAYING") && gameStateBlack.equals("PLAYING")) {
            board.printBoard();
            System.out.println((isWhiteTurn ? "White" : "Black") + "'s Turn");
            System.out.print("Enter Move: ");
            try {
                String input = keyboard.readLine();
                if (input.equals("exit")) {
                    break;
                }
                // Throw an IOexception if input is invalid
                processInput(input);
                gameStateWhite = board.getGameState(pieceColour.WHITE);
                gameStateBlack = board.getGameState(pieceColour.BLACK);
            } catch (IOException e) {
                System.out.println("Invalid move! ");
            }
        }
        board.printBoard();
        if(gameStateWhite.equals("CHECKMATE")){
            System.out.println("Black wins by checkmate! ");
        }else if(gameStateBlack.equals("CHECKMATE")){
            System.out.println("White wins by checkmate! ");
        }else if(gameStateBlack.equals("STALEMATE")){
            System.out.println("Draw by stalemate! ");
        }
    }

    private void processInput(String input) {
        // Hard code castling
        if (input.equals("O-O") || input.equals("O-O-O")) {
            handleCastling(input);
            return;
        }

        if (input.length() != 5) {
            System.out.println("Format error. ");
            return;
        }
        try {
            // For e2 e4:
            char startFile = input.charAt(0); // e
            int startRank = Character.getNumericValue(input.charAt(1)); // 2

            char endFile = input.charAt(3); // e
            int endRank = Character.getNumericValue(input.charAt(4)); // 4
            Coordinates initCoords = new Coordinates(startRank, startFile);
            Coordinates finalCoords = new Coordinates(endRank, endFile);
            Piece p = board.getPiece(initCoords);
            if (p == null || p.getColour() != (isWhiteTurn ? pieceColour.WHITE : pieceColour.BLACK)) {
                System.out.println("That's not your piece!");
                return;
            }
            Move move = new Move(p, initCoords, finalCoords);
            if (board.isMoveLegal(move)) {
                board.doMove(move);
                isWhiteTurn = !isWhiteTurn;
            } else {
                System.out.println("Move is illegal!");
            }
        } catch (IllegalArgumentException e) {
            System.out.println("Invalid coordinates. ");
        }

    }

    private void handleCastling(String type) {
        int rank = isWhiteTurn ? 1 : 8; // White is Rank 1, Black is Rank 8
        Coordinates start = new Coordinates(rank, 'e');
        Coordinates end;

        if (type.equals("O-O")) {
            end = new Coordinates(rank, 'g'); // Kingside
        } else {
            end = new Coordinates(rank, 'c'); // Queenside
        }

        Piece test = board.getPiece(start);

        // Safety check: is it actually a King?
        if (test == null || test.getType() != pieceType.KING) {
            System.out.println("Illegal: King has moved or is not there.");
            return;
        }
        King king = (King) board.getPiece(start);
        Move move = new Move(king, start, end);
        move.setIsCastling(true);

        if (board.isMoveLegal(move)) {
            board.doMove(move);
            isWhiteTurn = !isWhiteTurn;
        } else {
            System.out.println("Castling is illegal!");
        }
    }
}
