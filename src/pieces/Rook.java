package pieces;

import board.Board;
import board.Coordinates;
import enums.pieceColour;
import enums.pieceType;

public class Rook extends Piece {

    boolean hasMoved = false;

    public Rook(pieceColour colour, Coordinates coordinates) {
        super(pieceType.ROOK, colour, coordinates);
    }

    @Override
    public boolean isValidMove(Coordinates target, Board board) {
        // Reject moves to the same square
        if (coordinates.getRow() == target.getRow() && coordinates.getCol() == target.getCol()) {
            return false;
        }

        int dRow = target.getRow() - coordinates.getRow();
        int dCol = target.getCol() - coordinates.getCol();

        // Rook moves in straight lines; one delta must be zero
        if (dRow != 0 && dCol != 0) {
            return false;
        }

        // Ray tracing logic here:

        // Only have to check for one case
        if (dCol != 0) {
            int stepCol = dCol / Math.abs(dCol); // -1 or 1
            int c = coordinates.getCol() + stepCol;
            while (c != target.getCol()) {
                if (board.getPieceAt(coordinates.getRow(), c) != null) {
                    return false;
                }
                c += stepCol;
            }

            // At target square: either empty (valid) or occupied by opponent (capture)
            var targetPiece = board.getPieceAt(target.getRow(), target.getCol());
            if (targetPiece == null || targetPiece.getColour() != this.getColour()) {
                return true;
            } else {
                return false;
            }

        } else {
            int stepRow = dRow / Math.abs(dRow); // -1 or 1
            int r = coordinates.getRow() + stepRow;
            while (r != target.getRow()) {
                if (board.getPieceAt(r, coordinates.getCol()) != null) {
                    return false;
                }
                r += stepRow;
            }

            // At target square: either empty (valid) or occupied by opponent (capture)
            var targetPiece = board.getPieceAt(target.getRow(), target.getCol());
            if (targetPiece == null || targetPiece.getColour() != this.getColour()) {
                return true;
            } else {
                return false;
            }
        }
    }

    @Override
    public String getSymbol(){
        return "R";
    }

}
