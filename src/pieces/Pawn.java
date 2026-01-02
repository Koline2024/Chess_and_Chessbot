package pieces;

// if Board.java starts with "package chess;"
import board.Board;
import board.Coordinates;
import Piece;

public class Pawn extends Piece {

    boolean canMoveTwo = true;
    boolean canEnPassant;

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
                return true;
            } else {
                return false;
            }
        }

        // Move two squares if it hasn't moved before
        // Conditions: dUpDown/2 is in moveDir, target square is empty, left right
        // motion zero
        if (dUpDown / 2 == moveDir && board.getPiece(target) == null && dLeftRight == 0 && canMoveTwo) {
            return true;
        }

        return false;

    }

    public boolean getCanMoveTwo() {
        return canMoveTwo;
    }

    public void setCanMoveTwo(boolean newCanMoveTwo) {
        canMoveTwo = newCanMoveTwo;
    }

    public boolean getCanEnPassant() {
        return canEnPassant;
    }

    public void setCanEnPassant(boolean newCanEnPassant) {
        canEnPassant = newCanEnPassant;
    }
}
