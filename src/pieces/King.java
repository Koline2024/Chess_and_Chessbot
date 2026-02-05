package pieces;

import board.Coordinates;
import board.Board;
import enums.pieceColour;
import enums.pieceType;

public class King extends Piece{
    boolean hasMoved = false;

    public King(pieceColour colour, Coordinates coordinates){
        super(pieceType.KING, colour, coordinates);
    }

    @Override
    public boolean isValidMove(Coordinates target, Board board){
        // Reject moves to the same square
        if (coordinates.getRow() == target.getRow() && coordinates.getCol() == target.getCol()) {
            return false;
        }

        // Use array-based indices for movement deltas 
        int dRow = target.getRow() - coordinates.getRow();
        int dCol = target.getCol() - coordinates.getCol();

        // King can only move one square in any direction
        if(Math.abs(dRow) > 1 || Math.abs(dCol) > 1){
            return false;
        }

        var targetPiece = board.getPieceAt(target.getRow(), target.getCol());
        if (targetPiece == null || targetPiece.getColour() != this.getColour()){
            return true;
        }else{
            return false;
        }        


    }

    @Override
    public String getSymbol(){
        return "K";
    }

}
