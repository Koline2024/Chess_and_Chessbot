package board;

import enums.*;
import java.io.*;

public class MatchManager {
    private Board board;
    private Search AI1;
    private Search AI2;

    private boolean isWhiteTurn;

    public MatchManager() {
        this.board = new Board();
        this.AI1 = new Search();
        this.AI2 = new Search();
        this.isWhiteTurn = true;
    }

    public void begin() {
        int matchesRemaining = 1; // Default one match
        int depth1 = 5; // Default depth 5
        int depth2 = 5;
        float score1 = 0;
        float score2 = 0;
        boolean tricky1 = false;
        boolean tricky2 = false;
        board.initZobrist(isWhiteTurn);
        BufferedReader keyboard = new BufferedReader(new InputStreamReader(System.in));
        String gameStateWhite = board.getGameState(pieceColour.WHITE);
        String gameStateBlack = board.getGameState(pieceColour.BLACK);

        try {
            System.out.println("BOT MATCH CONFIG");
            System.out.println("How many matches to be played? ");
            matchesRemaining = Integer.parseInt(keyboard.readLine());
            System.out.println("What depth should bot 1 search to? ");
            depth1 = Integer.parseInt(keyboard.readLine());
            System.out.println("Choose bot 1 personality. ");
            System.out.printf("1: Tricky\n2: Normal\n");
            switch (keyboard.readLine()) {
                case "1":
                    tricky1 = true;
                    break;
                case "2":
                    tricky1 = false;
                    break;
                default:
                    tricky1 = false;
                    break;
            }
            System.out.println("What depth should bot 2 search to? ");
            depth2 = Integer.parseInt(keyboard.readLine());
            System.out.println("Choose bot 2 personality. ");
            System.out.printf("1: Tricky\n2: Normal\n");
            switch (keyboard.readLine()) {
                case "1":
                    tricky2 = true;
                    break;
                case "2":
                    tricky2 = false;
                    break;
                default:
                    tricky2 = false;
                    break;
            }

            while (matchesRemaining > 0) {
                while (gameStateWhite.equals("PLAYING") && gameStateBlack.equals("PLAYING")) {
                    board.printBoard(pieceColour.WHITE);
                    gameStateWhite = board.getGameState(pieceColour.WHITE);
                    gameStateBlack = board.getGameState(pieceColour.BLACK);
                    handleAI1move(depth1, tricky1);
                    board.printBoard(pieceColour.WHITE);
                    gameStateWhite = board.getGameState(pieceColour.WHITE);
                    gameStateBlack = board.getGameState(pieceColour.BLACK);
                    handleAI2move(depth2, tricky2);
                    
                }
                if (gameStateWhite.equals("CHECKMATE")) {
                    System.out.println("Black wins by checkmate! ");
                    score2 ++;
                } else if (gameStateBlack.equals("CHECKMATE")) {
                    System.out.println("White wins by checkmate! ");
                    score1 ++;
                } else if (gameStateBlack.equals("STALEMATE")) {
                    System.out.println("Draw by stalemate! ");
                    score1 += 0.5;
                    score2 += 0.5;
                }
                matchesRemaining --;
                System.out.println("Current scores:");
                System.out.println("BOT 1: " + score1);
                System.out.println("BOT 2: " + score2);
            }

        } catch (Exception e) {
            System.out.println(e);

        }

    }

    private void handleAI1move(int depth, boolean isTricky) {
        System.out.println("Bot 1 is thinking... ");
        Move bestMove = AI1.findBestMove(board, depth, isWhiteTurn, isTricky);
        if (bestMove != null) {
            board.doMove(bestMove);
            System.out.println("Bot 1 played " + bestMove);
            isWhiteTurn = !isWhiteTurn;
        } else {
            System.out.println("Bot 1 has no legal moves. ");
        }
    }

    private void handleAI2move(int depth, boolean isTricky) {
        System.out.println("Bot 2 is thinking... ");
        Move bestMove = AI2.findBestMove(board, depth, isWhiteTurn, isTricky);
        if (bestMove != null) {
            board.doMove(bestMove);
            System.out.println("Bot 2 played " + bestMove);
            isWhiteTurn = !isWhiteTurn;
        } else {
            System.out.println("Bot 2 has no legal moves. ");
        }
    }

}
