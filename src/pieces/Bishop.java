package pieces;

import board.Coordinates;
import board.Board;
import enums.pieceColour;
import enums.pieceType;

public class Bishop extends Piece {

    public Bishop(pieceColour colour, Coordinates coordinates) {
        super(pieceType.BISHOP, colour, coordinates);
    }

    @Override
    public boolean isValidMove(Coordinates target, Board board) {
        // Reject moves to the same square
        if (coordinates.getRow() == target.getRow() && coordinates.getCol() == target.getCol()) {
            return false;
        }

        // Use array-based indices for movement deltas 
        int dRow = target.getRow() - coordinates.getRow();
        int dCol = target.getCol() - coordinates.getCol();

        // Must move along a diagonal: absolute row and col deltas equal
        if (Math.abs(dRow) != Math.abs(dCol)) {
            return false;
        }

        // Ray tracing logic here:

        int stepRow = dRow / Math.abs(dRow); // -1 or 1
        int stepCol = dCol / Math.abs(dCol); // -1 or 1

        int r = coordinates.getRow() + stepRow;
        int c = coordinates.getCol() + stepCol;

        // Walk square by square until reaching the target square
        while (r != target.getRow() || c != target.getCol()) {
            if (board.getPieceAt(r, c) != null) {
                return false; // blocked by a piece before target
            }
            r += stepRow;
            c += stepCol;
        }

        // At target square: either empty (valid) or occupied by opponent (capture)
        var targetPiece = board.getPieceAt(target.getRow(), target.getCol());
        if (targetPiece == null || targetPiece.getColour() != this.getColour()){
            return true;
        }else{
            return false;
        }

    }

    @Override
    public String getSymbol(){
        return "B";
    }
 
}
