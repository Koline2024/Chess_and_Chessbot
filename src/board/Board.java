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

public class Board {

    private Move lastMove;
    private Piece[][] grid = new Piece[8][8];

    public Board() {
        initialise();
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
                        if (targetPiece != null && targetPiece.getColour() == colour) continue;

                        Coordinates to = new Coordinates(8 - targetRow, (char) ('a' + targetCol));
                        Move testMove = new Move(p, p.getCoordinates(), to);

                        if (isMoveLegal(testMove)) {
                            System.out.println("You can move " + testMove.getMoveFrom().toString() + " to " + testMove.getMoveTo().toString());
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
        // Clear the old square
        grid[move.getMoveFrom().getRow()][move.getMoveFrom().getCol()] = null;
        // Set the piece on the new square
        setPiece(move.getMoveTo(), move.piece);
        // Tell the piece where its new square is
        move.piece.setCoordinates(move.getMoveTo());
        // Update lastMove
        this.lastMove = move;

        // 2. The Rook Swap (Only for castling)
        if (move.isCastling()) {
            int row = move.getMoveFrom().getRow();
            int rookStartCol = (move.getMoveTo().getCol() == 6) ? 7 : 0;
            int rookEndCol = (move.getMoveTo().getCol() == 6) ? 5 : 3;

            Rook rook = (Rook) grid[row][rookStartCol];
            grid[row][rookStartCol] = null; // Clear corner
            setPiece(new Coordinates(move.getMoveTo().getRank(), (char) ('a' + rookEndCol)), rook);
            rook.setHasMoved(true);
        }
        // Hard code castling hasMovedBefore booleans
        if (move.piece.getType() == pieceType.ROOK) {
            // Casting because I am bad at structuring and also lazy
            Rook p = (Rook) move.piece;
            if (p.getHasMoved() == false) {
                p.setHasMoved(true);
            }
        }

        if (move.piece.getType() == pieceType.KING) {
            // Casting because I am bad at structuring and also lazy
            King p = (King) move.piece;
            if (p.getHasMoved() == false) {
                p.setHasMoved(true);
            }
        }
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
            if (k.getHasMoved() == true) {
                return false;
            }
            // Check the rook
            int rookCol = (endCol == 6) ? 7 : 0;
            // Guard clause
            if (grid[row][rookCol].getType() != pieceType.ROOK) {
                return false;
            }
            Rook rook = (Rook) grid[row][rookCol]; // Again casting is fine since rook is ensured
            if (rook == null || rook.getColour() != k.getColour() || rook.getHasMoved()) {
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
        for (Piece[] pRow : grid) {
            for (Piece p : pRow) {
                if (p != null && p.getColour() != colour) {
                    if (p.getType() == pieceType.PAWN) {
                        if (((Pawn) p).canAttack(coords, this)) {
                            return true;
                        }
                    }
                    else {
                        if (p.isValidMove(coords, this)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private Coordinates findKing(pieceColour colour) {
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                Piece p = grid[i][j];
                if (p != null) {
                    if (p.getType() == pieceType.KING && p.getColour() == colour) {
                        return p.getCoordinates();
                    }
                }
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

    public void promote(Coordinates coords, Piece toPiece){
        setPiece(coords, toPiece);
        System.out.println("Pawn promoted to " + toPiece.getSymbol());
    }

}
