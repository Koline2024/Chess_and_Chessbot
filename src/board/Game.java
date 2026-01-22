package board;

import pieces.*;
import enums.*;
import java.io.*;

public class Game {
    private Board board;
    private boolean isWhiteTurn;
    private Eval eval = new Eval();
    private Search AI;

    public Game() {
        this.board = new Board();
        this.isWhiteTurn = true;
    }

    public void start() {
        BufferedReader keyboard = new BufferedReader(new InputStreamReader(System.in));
        String gameStateWhite = board.getGameState(pieceColour.WHITE);
        String gameStateBlack = board.getGameState(pieceColour.BLACK);
        String userInput = "";
        pieceColour playerSide = null;
        AI = new Search();
        while (playerSide == null) {
            System.out.println("Do you want to play white or black? ");
            try {
                userInput = keyboard.readLine().toLowerCase();
                if (userInput.equals("white")) {
                    playerSide = pieceColour.WHITE;
                } else if (userInput.equals("black")) {
                    playerSide = pieceColour.BLACK;
                } else {
                    System.out.println("Invalid side. Type white or black.");
                }
            } catch (Exception e) {
                System.out.println("Input error! ");
            }

        }

        while (gameStateWhite.equals("PLAYING") && gameStateBlack.equals("PLAYING")) {
            board.printBoard(playerSide);
            boolean isPlayerTurn = (isWhiteTurn && playerSide == pieceColour.WHITE) ||
                    (!isWhiteTurn && playerSide == pieceColour.BLACK);
            if (isPlayerTurn) {
                System.out.println("Your turn. Enter move: ");
                try {
                    String input = keyboard.readLine();
                    // Exit
                    if (input.equals("exit")) {
                        break;
                    }
                    // Undo shortcut
                    if (input.equals("undo")) {
                        board.undoMove();
                        board.undoMove();
                        continue;
                    }
                    if (input.equals("list")) {
                        System.out.println(board.getLegalMoves(playerSide));
                        continue;
                    }
                    // Throw an IOexception if input is invalid
                    processInput(input, playerSide);
                    gameStateWhite = board.getGameState(pieceColour.WHITE);
                    gameStateBlack = board.getGameState(pieceColour.BLACK);
                    System.out.println(eval.evalAll(board, isWhiteTurn));
                } catch (IOException e) {
                    System.out.println("Invalid move! ");
                }
            } else {
                handleAImove();
            }
            gameStateWhite = board.getGameState(pieceColour.WHITE);
            gameStateBlack = board.getGameState(pieceColour.BLACK);

        }

        board.printBoard(playerSide);
        if (gameStateWhite.equals("CHECKMATE")) {
            System.out.println("Black wins by checkmate! ");
        } else if (gameStateBlack.equals("CHECKMATE")) {
            System.out.println("White wins by checkmate! ");
        } else if (gameStateBlack.equals("STALEMATE")) {
            System.out.println("Draw by stalemate! ");
        }
    }

    private void processInput(String input, pieceColour side) {
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
            boolean playerSide = true;
            if (side == pieceColour.BLACK) {
                playerSide = false;
            }
            if (p == null || p.getColour() != (playerSide ? pieceColour.WHITE : pieceColour.BLACK)) {
                System.out.println("That's not your piece!");
                return;
            }
            Move move = new Move(p, initCoords, finalCoords);
            if (board.isMoveLegal(move)) {
                board.doMove(move);
                if (move.piece.getType() == pieceType.PAWN) {
                    if(((Pawn) move.piece).getJustMovedTwo() == true){
                        ((Pawn) move.piece).setCanMoveTwo(false);
                    }
                    int rank = move.getMoveTo().getRank();
                    if (rank == 1 || rank == 8) {
                        handlePromotion(move.getMoveTo(), move.piece.getColour());
                    }
                }
                isWhiteTurn = !isWhiteTurn;
            } else {
                System.out.println("Move is illegal!");
            }
        } catch (IllegalArgumentException e) {
            System.out.println("Invalid coordinates. ");
        }

    }

    private void handlePromotion(Coordinates coords, pieceColour colour) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("Choose promoted piece: Q, R, B, N");
        try {
            String choice = reader.readLine();
            Piece newPiece;
            switch (choice) {
                case "R":
                    newPiece = new Rook(colour, coords);
                    break;
                case "B":
                    newPiece = new Bishop(colour, coords);
                    break;
                case "N":
                    newPiece = new Knight(colour, coords);
                    break;
                default:
                    newPiece = new Queen(colour, coords);
            }
            board.promote(coords, newPiece);
        } catch (Exception e) {
            System.out.println("IO Exception! ");
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
            board.syncPieceLists();
            isWhiteTurn = !isWhiteTurn;
        } else {
            System.out.println("Castling is illegal!");
        }
    }

    private void handleAImove() {
        System.out.println("Chessbot is thinking... ");
        Move bestMove = AI.findBestMove(board, 4, isWhiteTurn);
        if (bestMove != null) {
            board.doMove(bestMove);
            System.out.println("Chessbot played " + bestMove);
            // Auto-queen promotion for chessbot
            if (bestMove.piece.getType() == pieceType.PAWN) {
                int r = bestMove.getMoveTo().getRank();
                if (r == 1 || r == 8) {
                    board.promote(bestMove.getMoveTo(), new Queen(bestMove.piece.getColour(), bestMove.getMoveTo()));
                }
            }
            isWhiteTurn = !isWhiteTurn;
        } else {
            System.out.println("Chessbot has no legal moves. ");
        }
    }
}