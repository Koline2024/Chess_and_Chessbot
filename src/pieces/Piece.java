package pieces;

import enums.pieceColour;
import enums.pieceType;
import board.Coordinates;

import java.util.Objects;

import board.Board;

public abstract class Piece {

    // Base class for the piece which every piece inherits.
    pieceColour colour;
    pieceType type;
    Coordinates coordinates;
    boolean hasMoved;

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

    public boolean hasMoved() {
        return hasMoved;
    }

    public void setMoved(boolean x) {
        hasMoved = x;
    }

    // @Override
    // public boolean equals(Object o){
    //     if (o == null){
    //         return false;
    //     }
    //     if (o == this){
    //         return true;
    //     }

    //     if (o.getClass() != this.getClass()){
    //         return false;
    //     }

    //     Piece p = (Piece) o;
    //     if (p.getType() == this.getType() && p.getColour() == this.getColour() && p.getCoordinates().toString().equals(this.getCoordinates().toString())){
    //         return true;
    //     }else{
    //         return false;
    //     }
    // }

    // @Override
    // public int hashCode(){
    //     return Objects.hash(type, colour, coordinates);
    // }

    public abstract boolean isValidMove(Coordinates target, Board board);

    public abstract String getSymbol();

}
