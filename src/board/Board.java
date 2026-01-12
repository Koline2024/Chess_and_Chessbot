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
    private Piece[][] lastGrid = new Piece[8][8];

    public Board() {
        initialise();
    }

    public void doMove(Move move) {
        // Clear the old square
        grid[move.getMoveFrom().getRow()][move.getMoveFrom().getCol()] = null;
        // Set the piece on the new square
        setPiece(move.getMoveTo(), move.piece);
        // Update the lastMove
        this.lastMove = move;
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
        // Geometric movement logic
        if (!move.piece.isValidMove(move.getMoveTo(), this)) {
            return false;
        }

        // Castling cannot occur if king is in check
        if (move.isCastling()) {
            if (isSquareAttacked(move.getMoveFrom(), move.piece.getColour())) {
                return false;
                // TODO: implement middle square checking
            }
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
                    // Case 1: Pawns
                    if (p.getType() == pieceType.PAWN) {
                        if (((Pawn) p).canAttack(coords, this)) {
                            return true;
                        }
                    }
                    // Case 2: All other pieces
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
}
