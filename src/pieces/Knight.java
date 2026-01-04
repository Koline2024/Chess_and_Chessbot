package pieces;

import board.Board;
import board.Coordinates;
import enums.pieceColour;
import enums.pieceType;


public class Knight extends Piece{

    public Knight(pieceColour colour, Coordinates coordinates){
        super(pieceType.KNIGHT, colour, coordinates);
    }

    @Override
    public boolean isValidMove(Coordinates target, Board board){
        // Reject moves to the same square
        if (coordinates.getRow() == target.getRow() && coordinates.getCol() == target.getCol()) {
            return false;
        }

        int dRow = target.getRow() - coordinates.getRow();
        int dCol = target.getCol() - coordinates.getCol();
        int absRow = Math.abs(dRow);
        int absCol = Math.abs(dCol);

        // dRow = 2 AND dCol = 1 OR dRow = 1 AND dCol = 2 to be a valid knight move
        if(!(absRow == 2 && absCol == 1) && !(absRow == 1 && absCol == 2)){
            return false;
        }

        var targetPiece = board.getPieceAt(target.getRow(), target.getCol());
        if (targetPiece == null || targetPiece.getColour() != this.getColour()){
            return true;
        }else{
            return false;
        }
    }
}
