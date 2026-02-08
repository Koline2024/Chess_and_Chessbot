package board;

import pieces.*;
import enums.*;
import java.io.*;
import java.util.List;

public class Game {
    private Board board;
    private boolean isWhiteTurn;
    private Eval eval = new Eval();
    private Search AI;
    private List<Move> movesPlayer;

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
        int depth = 5; // 5 plys by default
        AI = new Search();
        board.initZobrist(isWhiteTurn);
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

        System.out.println("What depth should bot search to?");
        try {
            depth = Integer.parseInt(keyboard.readLine());
        } catch (Exception e) {
            System.out.println("Not a number!");
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
                    // List all moves
                    if (input.equals("list")) {
                        for (Move move : board.getLegalMoves(playerSide)) {
                            // System.out.print("\"" + move + "\", ");
                            System.out.println(move);
                        }
                        System.out.println(board.getLegalMoves(playerSide).size());
                        continue;
                    }
                    // Get history of moves
                    if (input.equals("history")) {
                        if (board.history.isEmpty()) {
                            continue;
                        }
                        board.historyToPGN();
                        continue;
                    }
                    // Get how full TTable is 
                    if (input.equals("filled")) {
                        System.out.println("The table is filled to: " + AI.tTable.filled() + "%");
                        continue;
                    }
                    // Recalibrate 
                    if (input.equals("recalibrate")){
                        AI.tTable.clear();
                        continue;
                    }
                    //Debug hash
                    if (input.equals("hash")){
                        System.out.println(board.zobristHash);
                        continue;
                    }
                    // Help function
                    if (input.equals("help")) {
                        System.out.println("Here are the available commands: ");
                        System.out.println("To move: Enter start square and stop square.");
                        System.out.println("To exit: Type 'exit'.");
                        System.out.println("To undo: Type 'undo'.");
                        System.out.println("To get all previous moves: Type 'history'");
                        System.out.println("To recalibrate transposition table: Type 'recalibrate'");
                        System.out.println("To get how filled the transposition table is: Type 'filled'.");
                        System.out.println("To get the zobrist hash type 'hash'. ");
                        continue;
                    }
                    
                    // Throw an IOexception if input is invalid
                    processInput(input, playerSide);
                    gameStateWhite = board.getGameState(pieceColour.WHITE);
                    gameStateBlack = board.getGameState(pieceColour.BLACK);
                    System.out.println(eval.evalAll(board, isWhiteTurn));
                } catch (IOException e) {
                    System.out.println("Invalid command. Type 'help' to get a list of valid commands. ");
                }
            } else {
                handleAImove(depth);
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
        movesPlayer = board.getLegalMoves(side);
        // Hard code castling
        if (input.equals("O-O") || input.equals("O-O-O")) {
            handleCastling(input);
            return;
        }

        if (input.length() != 5) {
            System.out.println("Invalid command. Type 'help' to get a list of valid commands. ");
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

            // Set is en passant HERE
            if (!board.history.isEmpty()) {
                Move last = board.history.peek();
                if (last.piece.getType() == pieceType.PAWN
                        && Math.abs(last.getMoveFrom().getRow() - last.getMoveTo().getCol()) == 2) {
                    int passingRow = last.to.getRow();
                    int passingCol = last.to.getCol();
                    int targetRow = (p.getColour() == pieceColour.WHITE) ? passingRow - 1 : passingRow + 1;
                    if (finalCoords.getRow() == targetRow && finalCoords.getCol() == passingCol) {
                        move.setIsEnPassant(true);
                         Piece victim = board.getPieceAt(passingRow, passingCol);
                    // Sanity check: The victim must exist and be the enemy colour
                    if (victim != null && victim.getColour() != p.getColour() && victim == last.piece) {
                        move.setCapturedPiece(victim);
                    }
                    }
                }
            }

            if (movesPlayer.contains(move)) {
                board.doMove(move);
                if (move.piece.getType() == pieceType.PAWN) {
                    if (((Pawn) move.piece).getJustMovedTwo() == true) {
                        ((Pawn) move.piece).setCanMoveTwo(false);
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

    private void handleAImove(int depth) {
        System.out.println("Chessbot is thinking... ");
        Move bestMove = AI.findBestMove(board, depth, isWhiteTurn, false);
        if (bestMove != null) {
            board.doMove(bestMove);
            System.out.println("Chessbot played " + bestMove);
            isWhiteTurn = !isWhiteTurn;
        } else {
            System.out.println("Chessbot has no legal moves. ");
        }
    }
}