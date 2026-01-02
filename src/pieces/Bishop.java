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
    public boolean isValidMove(Coordinates target, Board board){

        boolean pieceBlocking = false;
        int dUpDown = target.getRank() - coordinates.getRank();
        int dLeftRight = target.getFile() - coordinates.getFile();

        // Diagonal movement condition: absolute of each vector component must be same
        // Walk square by square 
        if(Math.abs(dUpDown) == Math.abs(dLeftRight)){
            while(coordinates.getCol() != target.getCol() && coordinates.getRow() != target.getCol()){
                
            }

        }
    }
}
