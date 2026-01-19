package board;

import pieces.Piece;
import enums.*;

import java.util.EnumMap;
import java.util.Map;

public class Eval {

    // Stage of the game: 0 is opening, 1 is endgame
    private double stage = 0;
    // Map of piece types to material value
    private static final Map<pieceType, Double> materialValues = new EnumMap<>(pieceType.class);
    static{
    materialValues.put(pieceType.PAWN, 1.0);
    materialValues.put(pieceType.KNIGHT, 3.0);
    materialValues.put(pieceType.BISHOP, 3.0); 
    materialValues.put(pieceType.ROOK, 5.0);
    materialValues.put(pieceType.QUEEN, 9.0);
    materialValues.put(pieceType.KING, 200.0);
    }

    public Eval() {
    }

    private double evalMaterial(Board board) {
        double materialScore = 0;
        // Material
        for(Piece p : board.getPieceList(pieceColour.WHITE)){
            materialScore += materialValues.get(p.getType());
        }
        for(Piece p : board.getPieceList(pieceColour.BLACK)){
            materialScore -= materialValues.get(p.getType());
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

    public double evalAll(Board board, boolean isWhiteTurn){
        double totalScore = 0;
        totalScore += evalMaterial(board);
        // Tempo bonus
        totalScore += (isWhiteTurn) ? 0.1 : -0.1;

        return totalScore;
    }
}
