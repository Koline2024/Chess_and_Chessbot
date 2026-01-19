package board;

import pieces.Piece;
import enums.*;

public class Eval {
    public Eval() {
    }

    public double evalPosition(Board board) {
        double materialScore = 0;
        // Material
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                Piece p = board.getPieceAt(i, j);
                if (p != null) {
                    if (p.getColour() == pieceColour.WHITE) {
                        switch (p.getType()) {
                            case PAWN:
                                materialScore += 1;
                                break;
                            case KNIGHT:
                                materialScore += 3;
                                break;
                            case BISHOP:
                                materialScore += 3;
                                break;
                            case ROOK:
                                materialScore += 5;
                                break;
                            case QUEEN:
                                materialScore += 9;
                                break;
                            case KING:
                                materialScore += 200;
                                break;
                        }
                    }else{
                        switch (p.getType()) {
                            case PAWN:
                                materialScore -= 1;
                                break;
                            case KNIGHT:
                                materialScore -= 3;
                                break;
                            case BISHOP:
                                materialScore -= 3;
                                break;
                            case ROOK:
                                materialScore -= 5;
                                break;
                            case QUEEN:
                                materialScore -= 9;
                                break;
                            case KING:
                                materialScore -= 200;
                                break;
                        }
                    }
                }
            }
        }

        // Implement game phase recognition based on materialScore
        // Implement pawn structure, pawn center, passed pawns
        // Implement piece mobility score (areas controlled by enemy pawns do not count)
        // Implement decreasing value of knight the less pawns there are
        // Implement penalty for bad bishop based on mobility score
        // Implement bonus for bishop pair
        // Implement bonus for fianchetto if center is clear; penalty otherwise
        // Implement increasing value of rook the less pawns there are
        // Implement bonus for rook on enemy 7th/2nd rank; rook on open file
        // Implement penalty for queen early movement (within first 10 moves)
        // Implement king safety algorithm based on king tropism
        // Implement king centralisation bonus in endgame
        // Implement minus/plus infinity penalty for any mate patterns
        

        return materialScore;
    }
}
