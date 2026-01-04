package board;

import enums.pieceType;
import pieces.Piece;
import pieces.Pawn;

public class Board {

    private Move lastMove;

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
     * 
     * @param x
     * @param y
     * @return
     */
    public Piece getPieceAt(int x, int y) {
        // Assuming these are proper array indices
        return grid[x][y];
    }

    public boolean isLegalMove(Move move) {
        // General structural idea:
        // 1. Check if move.piece is a special piece (king, pawn)
        // 2. Check for move flags (en passant, castling, promotion)
        // 3a. If none of the above, run the isValidMove method of the piece class
        // 3b. If king: run the isValidMove, then iterate through every enemy piece and
        // see if they can
        // Attack the square the king is moving to.
        // 3c. If castling: DO NOT use isValidMove; instead use a specific isCastleValid
        // private method
        // That iterates through each enemy piece and see if they can attack any
        // inbetween squares the king
        // Hops through.
        // 3d. If en passant, use the private lastMove Move object stored in Board to
        // check if the last
        // moved piece was 1. a pawn, 2. moved 2 squares and 3. could have been captured
        // if it moved one

        // Check if move.piece is a special piece (king, pawn)
        if (move.piece.getType() != pieceType.KING || move.piece.getType() != pieceType.PAWN) {
            if (move.piece.isValidMove(move.getMoveTo(), this)) {
                return true;
            }
        }

        // Piece is a king. Oh god.
        if (move.piece.getType() == pieceType.KING) {
            // For not castling
            if (!move.isCastling()) {
                if (move.piece.isValidMove(move.getMoveTo(), this)) {
                    // Check every enemy piece to see if they can legally move to where the king
                    // wants to move
                    for (Piece[] pRow : grid) {
                        for (Piece p : pRow) {
                            if (p != null) {
                                if (p.isValidMove(move.getMoveTo(), this) && p.getColour() != move.piece.getColour()) {
                                    return false;
                                }
                            }
                        }
                    }
                    return true;
                }
            }
            // For castling
            if (move.isCastling()) {
                int dCol = move.getMoveTo().getCol() - move.getMoveFrom().getCol();
                int dRow = move.getMoveTo().getRow() - move.getMoveFrom().getRow();

                // Invalid if moving vertically or not kingside or queenside
                if (dCol != 0 || (dRow != 2 && dRow != -3)) {
                    return false;
                }
                int stepRow = dRow / Math.abs(dRow); // -1 or 1
                int r = move.getMoveFrom().getRow() + stepRow;
                while (r != move.getMoveTo().getRow()) {
                    if (this.getPieceAt(r, move.getMoveTo().getCol()) != null) {
                        return false;
                    }
                    for (Piece[] pRow : grid) {
                        for (Piece p : pRow) {
                            if (p != null) {
                                if (p.isValidMove(new Coordinates(r, move.getMoveFrom().getFile()), this)
                                        && p.getColour() != move.piece.getColour()) {
                                    return false;
                                }
                            }
                        }
                    }
                    r += stepRow;
                }
                if (this.getPiece(move.getMoveTo()) == null) {
                    // Check if final square is under attack by enemy piece
                    for (Piece[] pRow : grid) {
                        for (Piece p : pRow) {
                            if (p != null) {
                                if (p.isValidMove(move.getMoveTo(), this) && p.getColour() != move.piece.getColour()) {
                                    return false;
                                }
                            }
                        }
                    }
                    return true;
                }
            }

            // En passant

            if(move.isEnPassant() && move.piece.getType() != pieceType.PAWN){
                return false;
            }else{
                if(lastMove.piece.getType() == pieceType.PAWN){
                    // Cursed casting here
                    if(((Pawn)lastMove.piece).hasJustMovedTwice()){
                        return true;
                    }
                }
                return false;
            }




        }

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
