public abstract class Piece {

    // Base class for the piece which every piece inherits.
    pieceColour colour;
    pieceType type;
    Coordinates coordinates;

    public Piece(pieceType type, pieceColour colour, Coordinates coordinates) {
        this.colour = colour;
        this.coordinates = coordinates;
        this.type = type;
    }

    public pieceColour getColour() {
        return colour;
    }

    public void setColour(pieceColour newColour) {
        colour = newColour;
    }

    public pieceType getType() {
        return type;
    }

    public void setType(pieceType newType) {
        type = newType;
    }

    public Coordinates getCoordinates() {
        return coordinates;
    }

    public void setCoordinates(Coordinates newCoordinates) {
        coordinates = newCoordinates;
    }

    public abstract boolean isValidMove(Coordinates target, Board board);

    public enum pieceColour {
        WHITE, BLACK
    }

    public enum pieceType {
        PAWN, KNIGHT, BISHOP, ROOK, QUEEN, KING
    }

}
