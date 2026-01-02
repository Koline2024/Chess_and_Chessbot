public class Pawn extends Piece {

    boolean captured;
    boolean canMoveTwo;
    boolean canEnPassant;

    public Pawn(pieceColour colour, Coordinates coordinates){
        super(colour, pieceType.PAWN, coordinates);
    }

    // Getters and Setters

    public boolean getCanMoveTwo(){
        return canMoveTwo;
    }

    public void setCanMoveTwo(boolean newCanMoveTwo){
        canMoveTwo = newCanMoveTwo;
    }

    public boolean getCanEnPassant(){
        return canEnPassant;
    }

    public void setCanEnPassant(boolean newCanEnPassant){
        canEnPassant = newCanEnPassant;
    }
}
