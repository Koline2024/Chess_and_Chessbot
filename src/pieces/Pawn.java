package pieces;

import board.Board;
import board.Coordinates;
import enums.pieceColour;
import enums.pieceType;

// TODO: Add en passant

public class Pawn extends Piece {

    boolean canMoveTwo = true;
    private boolean justMovedTwice = false;

    public Pawn(pieceColour colour, Coordinates coordinates) {
        super(pieceType.PAWN, colour, coordinates);
    }

    @Override
    public boolean isValidMove(Coordinates target, Board board) {
        // Takes care of both black and white pawn move direction
        int moveDir = (colour == pieceColour.WHITE) ? 1 : -1;
        int dUpDown = target.getRank() - coordinates.getRank();
        int dLeftRight = target.getFile() - coordinates.getFile();

        // Single square movement
        // Conditions: upDown must be in proper direction, no piece, and cannot move
        // left right
        if (dUpDown == moveDir && board.getPiece(target) == null && dLeftRight == 0) {
            canMoveTwo = false;
            justMovedTwice = false;
            return true;
        }

        // Diagonal capture movement
        // Conditions: upDown = moveDir, abs(leftRight) = 1 (both left and right), piece
        // present
        if (dUpDown == moveDir && board.getPiece(target) != null && Math.abs(dLeftRight) == 1) {
            Piece toBeCaptured = board.getPiece(target);
            // Only allow opposite colour capturing lol
            if (toBeCaptured.getColour() != colour) {
                canMoveTwo = false;
                justMovedTwice = false;
                return true;
            } else {
                return false;
            }
        }

        // Move two squares if it hasn't moved before
        // Conditions: dUpDown/2 is in moveDir, target square is empty, left right
        // motion zero
        if (dUpDown / 2 == moveDir && board.getPiece(target) == null && dLeftRight == 0 && canMoveTwo) {
            justMovedTwice = true;
            return true;
        }

        return false;

    }

    // Special case: Pawns attack differently and must be treated differently
    public boolean canAttack(Coordinates target, Board board){
        int moveDir = (colour == pieceColour.WHITE) ? 1 : -1;
        int dUpDown = target.getRank() - coordinates.getRank();
        int dLeftRight = target.getFile() - coordinates.getFile();

        if (dUpDown == moveDir && board.getPiece(target) != null && Math.abs(dLeftRight) == 1) {
            Piece toBeCaptured = board.getPiece(target);
            if (toBeCaptured.getColour() != colour) {
                return true;
            } else {
                return false;
            }
        }
        return false;
    }

    public boolean getCanMoveTwo() {
        return canMoveTwo;
    }

    public boolean hasJustMovedTwice(){
        return justMovedTwice;
    }

    @Override
    public String getSymbol(){
        if(colour == pieceColour.WHITE){
            return "Pw";
        }else{
            return "Pb";
        }
    }

}
