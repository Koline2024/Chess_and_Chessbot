// Note: Move is a dumb data class. It is only implemented because of
// special rules requiring memory in chess.

package board;

import pieces.Piece;
import java.util.Objects;

public class Move {
    public final Piece piece;
    public final Coordinates from;
    public final Coordinates to;
    public static long[] oldCastlingRights = new long[16];
    

    // Move identifiers
    private Piece capturedPiece;
    private boolean isEnPassant;
    private boolean isCastling;
    private boolean isPromotion;
    private boolean wasFirstMove;
    private Piece promotedPiece;
    private long zobHash;

    // Memory
    private boolean pieceWasMovedBefore;

    public Move(Piece piece, Coordinates from, Coordinates to) {
        this.piece = piece;
        this.from = from;
        this.to = to;
    }

    public Coordinates getMoveTo() {
        return to;
    }

    public Coordinates getMoveFrom() {
        return from;
    }

    public boolean isCastling() {
        return isCastling;
    }

    public void setIsCastling(boolean x) {
        isCastling = x;
    }

    public boolean isPromotion() {
        return isPromotion;
    }

    public boolean isEnPassant() {
        return isEnPassant;
    }

    public void setIsEnPassant(boolean x) {
        isEnPassant = x;
    }

    public Piece getCapturedPiece() {
        return capturedPiece;
    }

    public void setCapturedPiece(Piece p) {
        capturedPiece = p;
    }

    public boolean wasFirstMove() {
        return wasFirstMove;
    }

    public void setWasFirstMove(boolean x) {
        wasFirstMove = x;
    }

    public long[] getOldCastlingRights() {
        return oldCastlingRights;
    }

    public void setOldCastlingRights(long[] a) {
        oldCastlingRights = a;
    }

    public boolean wasPromotion() {
        return isPromotion;
    }

    public void setPromotion(boolean x) {
        isPromotion = x;
    }

    public Piece getPromotedPiece(){
        return promotedPiece;
    }

    public void setPromotedPiece(Piece p){
        promotedPiece = p;
    }

    public long getZob(){
        return zobHash;
    }

    public void setZob(long x){
        zobHash = x;
    }

    @Override
    public boolean equals(Object m) {
        if (this == m) {
            return true;
        }

        if (m == null) {
            return false;
        }

        if (!(m instanceof Move)) {
            return false;
        }

        Move move = (Move) m;
        return (move.piece.getType() == this.piece.getType()
                && move.getMoveFrom().getIndex() == this.getMoveFrom().getIndex()
                && move.getMoveTo().getIndex() == this.getMoveTo().getIndex());
    }

    @Override
    public int hashCode(){
        return Objects.hash(piece.getType(), from.getIndex(), to.getIndex());
    }

    public void setPieceWasMovedBefore(boolean val) {
        this.pieceWasMovedBefore = val;
    }

    public boolean getPieceWasMovedBefore() {
        return pieceWasMovedBefore;
    }

    @Override
    public String toString() {
        return piece.getSymbol() + ": " + from + " to " + to;
        
    }
}
