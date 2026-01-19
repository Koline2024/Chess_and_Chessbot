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

public class Board {

    private Stack<Move> history = new Stack<>();
    private Move lastMove;
    private Piece[][] grid = new Piece[8][8];
    private ArrayList<Piece> whitePieces = new ArrayList<>();
    private ArrayList<Piece> blackPieces = new ArrayList<>();

    public Board() {
        initialise();
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
                                System.out.println("You can move " + testMove.getMoveFrom().toString() + " to "
                                        + testMove.getMoveTo().toString());
                                return "PLAYING"; // Found a move! No need to check any others.
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
            move.setWasFirstMove(((Pawn) move.piece).getCanMoveTwo());
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
                return false;
            }
            // Check the rook
            int rookCol = (endCol == 6) ? 7 : 0;
            // Guard clause
            if (grid[row][rookCol] != null) {
                if (grid[row][rookCol].getType() != pieceType.ROOK) {
                    return false;
                }
            }
            Rook rook = (Rook) grid[row][rookCol]; // Again casting is fine since rook is ensured
            if (rook == null || rook.getColour() != k.getColour() || rook.hasMoved()) {
                return false;
            }

            int step = (endCol > startCol) ? 1 : -1;
            for (int c = startCol + step; c != rookCol; c += step) {
                if (grid[row][c] != null)
                    return false; // Square is blocked
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

    private void initialise() {
        // --- WHITE PIECES ---
        // Rooks, Knights, Bishops, Queen, King (Rank 1)
        setPiece(new Coordinates(1, 'a'), new Rook(pieceColour.WHITE, new Coordinates(1, 'a')));
        setPiece(new Coordinates(1, 'b'), new Knight(pieceColour.WHITE, new Coordinates(1, 'b')));
        setPiece(new Coordinates(1, 'c'), new Bishop(pieceColour.WHITE, new Coordinates(1, 'c')));
        setPiece(new Coordinates(1, 'd'), new Queen(pieceColour.WHITE, new Coordinates(1, 'd')));
        setPiece(new Coordinates(1, 'e'), new King(pieceColour.WHITE, new Coordinates(1, 'e')));
        setPiece(new Coordinates(1, 'f'), new Bishop(pieceColour.WHITE, new Coordinates(1, 'f')));
        setPiece(new Coordinates(1, 'g'), new Knight(pieceColour.WHITE, new Coordinates(1, 'g')));
        setPiece(new Coordinates(1, 'h'), new Rook(pieceColour.WHITE, new Coordinates(1, 'h')));

        // White Pawns (Rank 2)
        char[] files = { 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h' };
        for (char file : files) {
            setPiece(new Coordinates(2, file), new Pawn(pieceColour.WHITE, new Coordinates(2, file)));
        }

        // --- BLACK PIECES ---
        // Black Pawns (Rank 7)
        for (char file : files) {
            setPiece(new Coordinates(7, file), new Pawn(pieceColour.BLACK, new Coordinates(7, file)));
        }

        // Rooks, Knights, Bishops, Queen, King (Rank 8)
        setPiece(new Coordinates(8, 'a'), new Rook(pieceColour.BLACK, new Coordinates(8, 'a')));
        setPiece(new Coordinates(8, 'b'), new Knight(pieceColour.BLACK, new Coordinates(8, 'b')));
        setPiece(new Coordinates(8, 'c'), new Bishop(pieceColour.BLACK, new Coordinates(8, 'c')));
        setPiece(new Coordinates(8, 'd'), new Queen(pieceColour.BLACK, new Coordinates(8, 'd')));
        setPiece(new Coordinates(8, 'e'), new King(pieceColour.BLACK, new Coordinates(8, 'e')));
        setPiece(new Coordinates(8, 'f'), new Bishop(pieceColour.BLACK, new Coordinates(8, 'f')));
        setPiece(new Coordinates(8, 'g'), new Knight(pieceColour.BLACK, new Coordinates(8, 'g')));
        setPiece(new Coordinates(8, 'h'), new Rook(pieceColour.BLACK, new Coordinates(8, 'h')));
    }

    private boolean isMoveSafe(Move move) {
        // In the case of a capture
        Piece capturedPiece = getPiece(move.getMoveTo());
        Coordinates originalCoords = move.getMoveFrom();

        grid[originalCoords.getRow()][originalCoords.getCol()] = null;
        grid[move.getMoveTo().getRow()][move.getMoveTo().getCol()] = move.piece;
        move.piece.setCoordinates(move.getMoveTo());
        // Find where the king is
        Coordinates kingPos = findKing(move.piece.getColour());
        boolean safe = !isSquareAttacked(kingPos, move.piece.getColour());

        // Undo simulated move
        grid[originalCoords.getRow()][originalCoords.getCol()] = move.piece;
        grid[move.getMoveTo().getRow()][move.getMoveTo().getCol()] = capturedPiece;
        move.piece.setCoordinates(originalCoords);
        return safe;
    }

    private boolean isSquareAttacked(Coordinates coords, pieceColour colour) {
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

    private Coordinates findKing(pieceColour colour) {
        ArrayList<Piece> pieceList = (colour == pieceColour.WHITE) ? whitePieces : blackPieces;

        for (Piece p : pieceList) {
            if (p.getType() == pieceType.KING) {
                return p.getCoordinates();
            }
        }
        return null;
    }

    public void printBoard() {
        for (int i = 0; i < 8; i++) {
            System.out.print((8 - i) + " "); // Rank headers
            for (int j = 0; j < 8; j++) {
                Piece p = grid[i][j];
                if (p == null) {
                    System.out.print("[  ]"); // Empty square
                } else {
                    System.out.print("[" + p.getSymbol() + "]");
                }
            }
            System.out.println(); // New line after each row
        }
        System.out.println("   a   b   c   d   e   f   g   h"); // File headers
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
        getPieceList(p.getColour()).remove(p);
    }
}
