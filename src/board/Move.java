// Note: Move is a dumb data class. It is only implemented because of
// special rules requiring memory in chess.

package board;

import pieces.Piece;

public class Move {
    public final Piece piece;
    public final Coordinates from;
    public final Coordinates to;

    // Move identifiers
    private Piece capturedPiece;
    private boolean isEnPassant;
    private boolean isCastling;
    private boolean isPromotion;
    private boolean wasFirstMove;

    // Memory
    private boolean pieceWasMovedBefore;

    public Move(Piece piece, Coordinates from, Coordinates to){
        this.piece = piece;
        this.from = from;
        this.to = to;
    }

    public Coordinates getMoveTo(){
        return to;
    }

    public Coordinates getMoveFrom(){
        return from;
    }

    public boolean isCastling(){
        return isCastling;
    }

    public void setIsCastling(boolean x){
        isCastling = x;
    }

    public boolean isPromotion(){
        return isPromotion;
    }

    public boolean isEnPassant(){
        return isEnPassant;
    }
    
    public void setIsEnPassant(boolean x){
        isEnPassant = x;
    }

    public Piece getCapturePiece(){
        return capturedPiece;
    }

    public void setCapturedPiece(Piece p){
        capturedPiece = p;
    }
    
    public boolean wasFirstMove(){
        return wasFirstMove;
    }

    public void setWasFirstMove(boolean x){
        wasFirstMove = x;
    }

    public void setPieceWasMovedBefore(boolean val) { this.pieceWasMovedBefore = val; }
    
    public boolean getPieceWasMovedBefore() { return pieceWasMovedBefore; }


    @Override
    public String toString(){
        return piece.getSymbol() + ": " + from + " to " + to; 
    }
}
