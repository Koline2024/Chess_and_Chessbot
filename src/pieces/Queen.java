package pieces;

import board.Board;
import board.Coordinates;
import enums.pieceColour;
import enums.pieceType;

public class Queen extends Piece {

    public Queen(pieceColour colour, Coordinates coordinates) {
        super(pieceType.QUEEN, colour, coordinates);
    }

    @Override
    public boolean isValidMove(Coordinates target, Board board) {

        // Reject moves to the same square
        if (coordinates.getRow() == target.getRow() && coordinates.getCol() == target.getCol()) {
            return false;
        }

        int dRow = target.getRow() - coordinates.getRow();
        int dCol = target.getCol() - coordinates.getCol();

        boolean bishopMovement = (Math.abs(dRow) == Math.abs(dCol));
        boolean rookMovement = (dRow == 0 || dCol == 0);

        // Queen combines bishop and rook logic
        // Either it moves in a diagonal OR a straight line
        if (!(bishopMovement || rookMovement)) {
            return false;
        }

        if (bishopMovement) {
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
            if (targetPiece == null || targetPiece.getColour() != this.getColour()) {
                return true;
            } else {
                return false;
            }
        } else {
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
    }

    @Override
    public String getSymbol(){
        if(colour == pieceColour.WHITE){
            return "Qw";
        }else{
            return "Qb";
        }
    }
}
