package board;

import pieces.Piece;

public class Board {

    private Piece[][] grid = new Piece[8][8];

    public Board() {
        initialise();
    }

    public void setPiece(Coordinates c, Piece p) {
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
     * @param x
     * @param y
     * @return
     */
    public Piece getPieceAt(int x, int y){
        // Assuming these are proper array indices
        return grid[x][y];
    }

    private void initialise() {
        // Read this as a2 (file, rank)
        // --- WHITE PIECES ---
        // Rooks, Knights, Bishops, Queen, King (Rank 1)
        /*
         * setPiece(new Coordinates(1, 'a'), new Rook(pieceColour.WHITE, new
         * Coordinates(1, 'a')));
         * setPiece(new Coordinates(1, 'b'), new Knight(pieceColour.WHITE, new
         * Coordinates(1, 'b')));
         * setPiece(new Coordinates(1, 'c'), new Bishop(pieceColour.WHITE, new
         * Coordinates(1, 'c')));
         * setPiece(new Coordinates(1, 'd'), new Queen(pieceColour.WHITE, new
         * Coordinates(1, 'd')));
         * setPiece(new Coordinates(1, 'e'), new King(pieceColour.WHITE, new
         * Coordinates(1, 'e')));
         * setPiece(new Coordinates(1, 'f'), new Bishop(pieceColour.WHITE, new
         * Coordinates(1, 'f')));
         * setPiece(new Coordinates(1, 'g'), new Knight(pieceColour.WHITE, new
         * Coordinates(1, 'g')));
         * setPiece(new Coordinates(1, 'h'), new Rook(pieceColour.WHITE, new
         * Coordinates(1, 'h')));
         * 
         * // White Pawns (Rank 2)
         * ['a', 'b', 'c', 'd', 'e', 'f', 'g', 'h'].forEach(file -> {
         * setPiece(new Coordinates(2, file), new Pawn(pieceColour.WHITE, new
         * Coordinates(2, file)));
         * });
         * 
         * // --- BLACK PIECES ---
         * // Black Pawns (Rank 7)
         * ['a', 'b', 'c', 'd', 'e', 'f', 'g', 'h'].forEach(file -> {
         * setPiece(new Coordinates(7, file), new Pawn(pieceColour.BLACK, new
         * Coordinates(7, file)));
         * });
         * 
         * // Rooks, Knights, Bishops, Queen, King (Rank 8)
         * setPiece(new Coordinates(8, 'a'), new Rook(pieceColour.BLACK, new
         * Coordinates(8, 'a')));
         * setPiece(new Coordinates(8, 'b'), new Knight(pieceColour.BLACK, new
         * Coordinates(8, 'b')));
         * setPiece(new Coordinates(8, 'c'), new Bishop(pieceColour.BLACK, new
         * Coordinates(8, 'c')));
         * setPiece(new Coordinates(8, 'd'), new Queen(pieceColour.BLACK, new
         * Coordinates(8, 'd')));
         * setPiece(new Coordinates(8, 'e'), new King(pieceColour.BLACK, new
         * Coordinates(8, 'e')));
         * setPiece(new Coordinates(8, 'f'), new Bishop(pieceColour.BLACK, new
         * Coordinates(8, 'f')));
         * setPiece(new Coordinates(8, 'g'), new Knight(pieceColour.BLACK, new
         * Coordinates(8, 'g')));
         * setPiece(new Coordinates(8, 'h'), new Rook(pieceColour.BLACK, new
         * Coordinates(8, 'h'))); }
         */
    }
}
