package board;

import enums.pieceColour;
import enums.pieceType;
import pieces.Piece;
import pieces.Queen;
import pieces.Rook;
import pieces.Bishop;
import pieces.King;
import pieces.Knight;
import pieces.Pawn;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import javax.management.RuntimeErrorException;

public class Board {

    private Stack<Move> history = new Stack<>();
    private Move lastMove;
    private Piece[][] grid = new Piece[8][8];
    private ArrayList<Piece> whitePieces = new ArrayList<>();
    private ArrayList<Piece> blackPieces = new ArrayList<>();

    public Board() {
        initialise("");
        syncPieceLists();
    }

    public String getGameState(pieceColour colour) {
        // First, check if there are ANY legal moves
        for (int startRow = 0; startRow < 8; startRow++) {
            for (int startCol = 0; startCol < 8; startCol++) {
                Piece p = grid[startRow][startCol];

                if (p != null && p.getColour() == colour) {
                    for (int targetRow = 0; targetRow < 8; targetRow++) {
                        for (int targetCol = 0; targetCol < 8; targetCol++) {

                            // Optimization: Don't check moving onto your own pieces
                            Piece targetPiece = grid[targetRow][targetCol];
                            if (targetPiece != null && targetPiece.getColour() == colour)
                                continue;

                            Coordinates to = new Coordinates(8 - targetRow, (char) ('a' + targetCol));
                            Move testMove = new Move(p, p.getCoordinates(), to);

                            if (isMoveLegal(testMove)) {
                                return "PLAYING"; // Found a move
                            }
                        }
                    }
                }
            }
        }

        // No moves found? Check for check vs stalemate
        if (isSquareAttacked(findKing(colour), colour)) {
            return "CHECKMATE";
        } else {
            return "STALEMATE";
        }
    }

    public void doMove(Move move) {
        // Update history
        move.setWasFirstMove(!move.piece.hasMoved());
        
        if (move.piece.getType() == pieceType.PAWN) {
            Pawn p = (Pawn) move.piece; 
            move.setWasFirstMove(p.getCanMoveTwo());
            if(Math.abs(move.getMoveTo().getRow() - move.getMoveFrom().getRow()) == 2){
                p.setJustMovedTwo(true);
            }else{
                p.setJustMovedTwo(false);
            }
        }

        move.setCapturedPiece(getPiece(move.getMoveTo()));
        if (move.getCapturePiece() != null) {
            removePieceFromSystem(move.getCapturePiece());
        }

        grid[move.getMoveFrom().getRow()][move.getMoveFrom().getCol()] = null;
        setPiece(move.getMoveTo(), move.piece);
        move.piece.setCoordinates(move.getMoveTo());
        move.piece.setMoved(true);

        // Castling rook swap
        if (move.isCastling()) {
            int row = move.getMoveFrom().getRow();
            // Kingside vs Queenside ternary
            int rookStartCol = (move.getMoveTo().getCol() == 6) ? 7 : 0;
            int rookEndCol = (move.getMoveTo().getCol() == 6) ? 5 : 3;

            Rook rook = (Rook) grid[row][rookStartCol];
            grid[row][rookStartCol] = null; // Clear corner
            setPiece(new Coordinates(move.getMoveTo().getRank(), (char) ('a' + rookEndCol)), rook);
            rook.setMoved(true);
        }
        // Hard code castling hasMovedBefore booleans
        if (move.piece.getType() == pieceType.ROOK) {
            // Casting because I am bad at structuring and also lazy
            Rook p = (Rook) move.piece;
            if (p.hasMoved() == false) {
                p.setMoved(true);
            }
        }

        if (move.piece.getType() == pieceType.KING) {
            // Casting because I am bad at structuring and also lazy
            King p = (King) move.piece;
            if (p.hasMoved() == false) {
                p.setMoved(true);
            }
        }
        lastMove = move;
        history.push(move);
    }

    private void setPiece(Coordinates c, Piece p) {
        grid[c.getRow()][c.getCol()] = p;
        if (p != null) {
            p.setCoordinates(c);
        }
    }

    public Piece getPiece(Coordinates c) {
        return grid[c.getRow()][c.getCol()];
    }

    /**
     * Row, Col
     * 
     * @param x
     * @param y
     * @return
     */
    public Piece getPieceAt(int x, int y) {
        // Assuming these are proper array indices
        return grid[x][y];
    }

    public boolean isMoveLegal(Move move) {
        // Before normal operation: Check if is castling
        if (move.isCastling()) {
            int row = move.getMoveFrom().getRow();
            int startCol = 4; // King is always on e
            int endCol = move.getMoveTo().getCol();
            King k = (King) move.piece; // Casting is lazy but fine because king is ensured prior
            if (k.hasMoved() == true) {
                System.out.println("Cannot castle because king has moved prior. ");
                return false;
            }
            // Check the rook
            int rookCol = (endCol == 6) ? 7 : 0;
            // Guard clause
            if (grid[row][rookCol] != null) {
                if (grid[row][rookCol].getType() != pieceType.ROOK) {
                    System.out.println("Not a rook you are trying to castle with. ");
                    return false;
                }
            }
            Rook rook = (Rook) grid[row][rookCol]; // Again casting is fine since rook is ensured
            if (rook == null || rook.getColour() != k.getColour() || rook.hasMoved()) {
                System.out.println("Error in castling with the rook. ");
                return false;
            }

            int step = (endCol > startCol) ? 1 : -1;
            for (int c = startCol + step; c != rookCol; c += step) {
                if (grid[row][c] != null) {
                    System.out.println("Castling middle square blocked");
                    return false; // Square is blocked
                }
            }

            // Square 1: Where the king starts
            if (isSquareAttacked(new Coordinates(move.getMoveFrom().getRank(), 'e'), move.piece.getColour()))
                return false;

            // Square 2: The square the king passes through
            char middleFile = (endCol == 6) ? 'f' : 'd';
            if (isSquareAttacked(new Coordinates(move.getMoveFrom().getRank(), middleFile), move.piece.getColour()))
                return false;
        } else if (!move.piece.isValidMove(move.getMoveTo(), this)) {
            return false;
        }

        // King safety
        if (!isMoveSafe(move)) {
            return false;
        }

        return true;
    }

public void initialise(String fen) {

    // Default starting position if FEN is empty
    if (fen == null || fen.isEmpty()) {
        fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
    }

    String[] parts = fen.split(" ");
    String boardPart = parts[0]; // The piece placement section

    String[] ranks = boardPart.split("/");
    int currentRank = 8; // FEN starts at Rank 8

    for (String rankContent : ranks) {
        int fileIndex = 0; // 0 = 'a', 1 = 'b', etc.

        for (int i = 0; i < rankContent.length(); i++) {
            char c = rankContent.charAt(i);

            if (Character.isDigit(c)) {
                // If it's a number, skip those squares
                fileIndex += Character.getNumericValue(c);
            } else {
                // If it's a letter, it's a piece
                char file = (char) ('a' + fileIndex);
                Coordinates coords = new Coordinates(currentRank, file);
                
                pieceColour color = Character.isUpperCase(c) ? pieceColour.WHITE : pieceColour.BLACK;
                Piece piece = createPieceFromChar(c, color, coords);
                
                setPiece(coords, piece);
                fileIndex++;
            }
        }
        currentRank--;
    }

    // Update Game State (Whose turn is it?)
    if (parts.length > 1) {
        //this.isWhiteTurn = parts[1].equals("w");
    }
}

// Helper to clean up the logic
private Piece createPieceFromChar(char c, pieceColour color, Coordinates coords) {
    char type = Character.toLowerCase(c);
    return switch (type) {
        case 'p' -> new Pawn(color, coords);
        case 'n' -> new Knight(color, coords);
        case 'b' -> new Bishop(color, coords);
        case 'r' -> new Rook(color, coords);
        case 'q' -> new Queen(color, coords);
        case 'k' -> new King(color, coords);
        default -> throw new IllegalArgumentException("Unknown FEN piece: " + c);
    };
}
    private boolean isMoveSafe(Move move) {
        // In the case of a capture

        // Try using domove and undomove instead of this code
        // Piece capturedPiece = getPiece(move.getMoveTo());
        // Coordinates originalCoords = move.getMoveFrom();

        // grid[originalCoords.getRow()][originalCoords.getCol()] = null;
        // grid[move.getMoveTo().getRow()][move.getMoveTo().getCol()] = move.piece;
        // if (capturedPiece != null) {
        //     removePieceFromSystem(capturedPiece);
        // }
        // move.piece.setCoordinates(move.getMoveTo());

        doMove(move);
        // Find where the king is
        Coordinates kingPos = findKing(move.piece.getColour());
        boolean safe = !isSquareAttacked(kingPos, move.piece.getColour());

        // Undo simulated move
        // grid[originalCoords.getRow()][originalCoords.getCol()] = move.piece;
        // grid[move.getMoveTo().getRow()][move.getMoveTo().getCol()] = capturedPiece;
        // move.piece.setCoordinates(originalCoords);
        // if(capturedPiece != null){
        //     addPieceToSystem(capturedPiece);
        // }
        undoMove();
        return safe;
    }

    /**
     * Takes as parameters the coordinates of the square and the colour of
     * ALLIED pieces. Internally computes arraylist of enemy pieces.
     * 
     * @param coords
     * @param colour
     * @return
     */
    public boolean isSquareAttacked(Coordinates coords, pieceColour colour) {
        ArrayList<Piece> enemyPieces = (colour == pieceColour.WHITE) ? blackPieces : whitePieces;
        for (Piece p : enemyPieces) {
            if (p.getType() == pieceType.PAWN) {
                if (((Pawn) p).canAttack(coords, this)) {
                    return true;
                }
            } else {
                if (p.isValidMove(coords, this)) {
                    return true;
                }
            }
        }

        return false;
    }

    public Coordinates findKing(pieceColour colour) throws RuntimeException {
        ArrayList<Piece> pieceList = (colour == pieceColour.WHITE) ? whitePieces : blackPieces;

        for (Piece p : pieceList) {
            if (p.getType() == pieceType.KING) {
                return p.getCoordinates();
            }
        }
        // If we reach here, something went wrong. Sync and retry.
        syncPieceLists();
        for (Piece p : pieceList) {
            if (p.getType() == pieceType.KING) {
                return p.getCoordinates();
            }
        }
        System.out.println("Even after a sync the king could not be found!");
        throw new RuntimeErrorException(null);
    }

    public void printBoard(pieceColour side) {

        String top = "  ╔═══╤═══╤═══╤═══╤═══╤═══╤═══╤═══╗";
        String middle = "  ╟───┼───┼───┼───┼───┼───┼───┼───╢";
        String bottom = "  ╚═══╧═══╧═══╧═══╧═══╧═══╧═══╧═══╝";
        String labels = "    a   b   c   d   e   f   g   h";

        if (side == pieceColour.WHITE) {
            System.out.println(top);
            for (int r = 0; r < 8; r++) {
                System.out.print((8 - r) + " ║"); // Rank number
                for (int c = 0; c < 8; c++) {
                    Piece p = grid[r][c];
                    String symbol = (p == null) ? " " : getUnicodeSymbol(p);
                    System.out.print(" " + symbol + " │");
                }
                // Replace last │ with ║
                System.out.print("\b║\n");
                if (r < 7) {
                    System.out.println(middle);
                }
            }
        } else {
            System.out.println(top);
            // Invert boardstate
            for (int r = 0; r < 8; r++) {
                System.out.print((1 + r) + " ║");
                for (int c = 0; c < 8; c++) {
                    Piece p = grid[7 - r][c];
                    String symbol = (p == null) ? " " : getUnicodeSymbol(p);
                    System.out.print(" " + symbol + " │");
                }
                System.out.print("\b║\n");
                if (r < 7) {
                    System.out.println(middle);
                }
            }
        }
        System.out.println(bottom);
        System.out.println(labels);
    }

    /**
     * Not actually unicode
     * 
     * @param p
     * @return
     */
    private String getUnicodeSymbol(Piece p) {
        String type = "";
        switch (p.getType()) {
            case PAWN:
                type = "P";
                break;
            case ROOK:
                type = "R";
                break;
            case KNIGHT:
                type = "N";
                break;
            case BISHOP:
                type = "B";
                break;
            case QUEEN:
                type = "Q";
                break;
            case KING:
                type = "K";
                break;
        }

        // White = Cyan, Black = Red (Better contrast than White/Black)
        if (p.getColour() == pieceColour.WHITE) {
            return "\u001b[36;1m" + type + "\u001b[0m";
        } else {
            return "\u001b[31;1m" + type + "\u001b[0m";
        }
    }

    public void promote(Coordinates coords, Piece toPiece) {
        Piece oldPawn = getPiece(coords);
        removePieceFromSystem(oldPawn); // Take the pawn out of the list

        setPiece(coords, toPiece); // Put queen on grid
        addPieceToSystem(toPiece); // Put queen in the list;
        System.out.println("Pawn promoted to " + toPiece.getSymbol());
    }

    public void undoMove() {
        if (history.isEmpty()) {
            return;
        }
        Move last = history.pop();
        if (last.getCapturePiece() != null) {
            addPieceToSystem(last.getCapturePiece());
        }

        // Put the piece back
        grid[last.from.getRow()][last.from.getCol()] = last.piece;
        last.piece.setCoordinates(last.from);

        // Restore the captured piece (or null)
        grid[last.to.getRow()][last.to.getCol()] = last.getCapturePiece();
        if (last.getCapturePiece() != null) {
            last.getCapturePiece().setCoordinates(last.to);
        }

        if (last.wasFirstMove()) {
            last.piece.setMoved(false);
            if (last.piece.getType() == pieceType.PAWN) {
                ((Pawn) last.piece).setCanMoveTwo(true);
            }
        }

        // Handle Special Cases (Castling)
        if (last.isCastling()) {
            int row = last.getMoveFrom().getRow();
            int rookStartCol = (last.getMoveTo().getCol() == 6) ? 7 : 0;
            int rookEndCol = (last.getMoveTo().getCol() == 6) ? 5 : 3;

            Rook rook = (Rook) grid[row][rookEndCol];
            grid[row][rookEndCol] = null; // Clear square
            setPiece(new Coordinates(last.getMoveFrom().getRank(), (char) ('a' + rookStartCol)), rook);
            rook.setMoved(false);
        }

        // if(last.piece.getType() == pieceType.PAWN){
        //     ((Pawn) last.piece).setCanMoveTwo(true);
        // }

    }

    public List<Piece> getPieceList(pieceColour colour) {
        return (colour == pieceColour.WHITE) ? whitePieces : blackPieces;
    }

    public void syncPieceLists() {
        whitePieces.clear();
        blackPieces.clear();
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                Piece p = grid[i][j];
                if (p != null) {
                    addPieceToSystem(p);
                }
            }
        }
    }

    private void addPieceToSystem(Piece p) {
        if (p == null) {
            return;
        }
        getPieceList(p.getColour()).add(p);
    }

    private void removePieceFromSystem(Piece p) {
        if (p == null) {
            return;
        }
        grid[p.getCoordinates().getRow()][p.getCoordinates().getCol()] = null;
        getPieceList(p.getColour()).remove(p);
    }

    public List<Move> getLegalMoves(pieceColour colour) {
        //TODO: Optimise by checking only valid squares using some sort
        List<Move> legalMoves = new ArrayList<>();
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                Piece p = grid[row][col];
                // If piece belongs to the active player
                if (p != null && p.getColour() == colour) {
                    // Check all squares for this piece
                    for (int tRow = 0; tRow < 8; tRow++) {
                        for (int tCol = 0; tCol < 8; tCol++) {
                            // Skip own pieces
                            Piece target = grid[tRow][tCol];
                            if (target != null && target.getColour() == colour) {
                                continue;
                            }
                            Coordinates to = new Coordinates(8 - tRow, (char) ('a' + tCol));
                            Move move = new Move(p, p.getCoordinates(), to);

                            // This is the expensive check:
                            if (isMoveLegal(move)) {
                                legalMoves.add(move);
                            }
                        }
                    }
                }
            }
        }
        return legalMoves;
    }
}
