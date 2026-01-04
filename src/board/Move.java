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
    public Piece capturedPiece;
    public boolean isEnPassant;
    public boolean isCastling;
    public boolean isPromotion;
    public pieceType promotionType;

    public Move(Piece piece, Coordinates from, Coordinates to){
        this.piece = piece;
        this.from = from;
        this.to = to;
    }

}
