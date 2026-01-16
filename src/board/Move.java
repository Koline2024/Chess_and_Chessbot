// Note: Move is a dumb data class. It is only implemented because of
// special rules requiring memory in chess.

package board;

import pieces.Piece;
import enums.pieceType;

public class Move {
    public final Piece piece;
    public final Coordinates from;
    public final Coordinates to;

    // Move identifiers
    private Piece capturedPiece;
    private boolean isEnPassant;
    private boolean isCastling;
    private boolean isPromotion;
    private pieceType promotionType;

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
        x = isCastling;
    }

    public boolean isPromotion(){
        return isPromotion;
    }

    public boolean isEnPassant(){
        return isEnPassant;
    }

}
