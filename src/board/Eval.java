package board;

import pieces.Piece;
import enums.*;

import java.util.EnumMap;
import java.util.Map;

public class Eval {


    
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

    // Map of piece types to material value
    private static final Map<pieceType, Double> materialValues = new EnumMap<>(pieceType.class);
    static {
        materialValues.put(pieceType.PAWN, 1.0);
        materialValues.put(pieceType.KNIGHT, 3.0);
        materialValues.put(pieceType.BISHOP, 3.0);
        materialValues.put(pieceType.ROOK, 5.0);
        materialValues.put(pieceType.QUEEN, 9.0);
        materialValues.put(pieceType.KING, 200.0);
    }

    // PSTs below
    private static double[][] PSTPawn = {
            // Encourage pawns to push & control centre
            { 0, 0, 0, 0, 0, 0, 0, 0 }, // Promotion rank
            { 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5 },
            { 0.1, 0.1, 0.2, 0.3, 0.3, 0.2, 0.1, 0.1 },
            { 0.05, 0.05, 0.1, 0.25, 0.25, 0.1, 0.05, 0.05 },
            { 0, 0, 0, 0.1, 0.1, 0, 0, 0 },
            { 0.05, 0.05, 0.1, 0, 0, 0.1, 0.05, 0.05 }, // Encourage middle two to advance and others to support
            { 0, 0, 0, -0.2, -0.2, 0, 0, 0 }, // Discourage idle centre
            { 0, 0, 0, 0, 0, 0, 0, 0 }
    };

private static double[][] PSTKnight = {
    // Encourage center control, knight on rim is dim
            { -0.5, -0.4, -0.3, -0.3, -0.3, -0.3, -0.4, -0.5 },
            { -0.4, -0.2, 0, 0, 0, 0, -0.2, -0.4 },
            { -0.3, 0, 0.1, 0.2, 0.2, 0.1, 0, -0.3 },
            { -0.3, 0.05, 0.15, 0.3, 0.3, 0.15, 0.05, -0.3 },
            { -0.3, 0, 0.1, 0.2, 0.2, 0.1, 0, -0.3 },
            { -0.3, 0, 0.1, 0.2, 0.2, 0.1, 0, -0.3 }, 
            { -0.4, -0.2, 0, 0, 0, 0, -0.2, -0.4 },
            { -0.5, -0.4, -0.3, -0.3, -0.3, -0.3, -0.4, -0.5 }
    };

    private static double[][] PSTBishop = {
            // Less penalty than knight but still prioritise more square control
            { -0.2, -0.1, -0.1, -0.1, -0.1, -0.1, -0.1, -0.2 },
            { -0.1, 0, 0, 0, 0, 0, 0, -0.1 },
            { -0.1, 0, 0.05, 0.1, 0.1, 0.05, 0, -0.1 },
            { -0.1, 0.05, 0.05, 0.1, 0.1, 0.05, 0.05, -0.1 },
            { -0.1, 0.05, 0.05, 0.1, 0.1, 0.05, 0.05, -0.1 },
            { -0.1, 0, 0.05, 0.1, 0.1, 0.05, 0, -0.1 },
            { -0.1, 0, 0, 0, 0, 0, 0, -0.1 },
            { -0.2, -0.1, -0.5, -0.1, -0.1, -0.5, -0.1, -0.2 } // Discourage laying eggs
    };

    private static double[][] PSTRook = {
            { 0, 0, 0, 0, 0, 0, 0, 0 }, // Rank 8
            { 0.2, 0.2, 0.2, 0.2, 0.2, 0.2, 0.2, 0.2 }, // 7th rank rook bonus
            { 0, 0, 0, 0, 0, 0, 0, 0 },
            { 0, 0, 0, 0, 0, 0, 0, 0 },
            { 0, 0, 0, 0, 0, 0, 0, 0 },
            { 0, 0, 0, 0, 0, 0, 0, 0 },
            { 0, 0, 0, 0, 0, 0, 0, 0 },
            { -0.1, 0, 0, 0.1, 0.1, 0, 0, -0.1 }, // Rank 1
    };

private static double[][] PSTQueen = {
            { -0.1, -0.05, -0.05, 0, 0, -0.05, -0.05, -0.1 },
            { -0.05, 0, 0, 0.05, 0.05, 0, 0, -0.05 },
            { -0.05, 0, 0.1, 0.1, 0.1, 0.1, 0, -0.05 },
            { -0.05, 0, 0.1, 0.1, 0.1, 0.1, 0, -0.05 },
            { -0.05, 0, 0.1, 0.1, 0.1, 0.1, 0, -0.05 },
            { -0.05, 0, 0.1, 0.1, 0.1, 0.1, 0, -0.05 }, 
            { -0.05, 0, 0, 0.05, 0.05, 0, 0, -0.05 },
            { -0.1, -0.05, -0.05, -0.1, 0, -0.05, -0.05, -0.1 }
    };

    public static double[][] PSTKingEarly = {
            { -0.5, -0.6, -0.6, -0.7, -0.7, -0.6, -0.6, -0.5 },
            { -0.5, -0.6, -0.6, -0.7, -0.7, -0.6, -0.6, -0.5 },
            { -0.5, -0.6, -0.6, -0.7, -0.7, -0.6, -0.6, -0.5 },
            { -0.5, -0.6, -0.6, -0.7, -0.7, -0.6, -0.6, -0.5 },
            { -0.4, -0.5, -0.5, -0.6, -0.6, -0.5, -0.5, -0.4 },
            { -0.4, -0.5, -0.5, -0.6, -0.6, -0.5, -0.5, -0.4 },
            { 0.2, 0.2, 0, 0, 0, 0, 0.2, 0.2 },
            { 0.2, 0.3, 0.1, 0, -0.2, 0.1, 0.3, 0.2 } // Encourage castling and hiding on the wing
    };

public static double[][] PSTKingEnd = {
            { -0.5, -0.4, -0.3, -0.3, -0.3, -0.3, -0.4, -0.5 },
            { -0.4, -0.2, 0, 0, 0, 0, -0.2, -0.4 },
            { -0.3, 0, 0.1, 0.2, 0.2, 0.1, 0, -0.3 },
            { -0.3, 0.05, 0.15, 0.3, 0.3, 0.15, 0.05, -0.3 },
            { -0.3, 0, 0.1, 0.2, 0.2, 0.1, 0, -0.3 },
            { -0.3, 0, 0.1, 0.2, 0.2, 0.1, 0, -0.3 }, 
            { -0.4, -0.2, 0, 0, 0, 0, -0.2, -0.4 },
            { -0.5, -0.4, -0.3, -0.3, -0.3, -0.3, -0.4, -0.5 }
    };

    private static Map<pieceType, double[][]> pstValues = new EnumMap<>(pieceType.class);
    static {
        pstValues.put(pieceType.PAWN, PSTPawn);
        pstValues.put(pieceType.KNIGHT, PSTKnight);
        pstValues.put(pieceType.BISHOP, PSTBishop);
        pstValues.put(pieceType.ROOK, PSTRook);
        pstValues.put(pieceType.QUEEN, PSTQueen);
        pstValues.put(pieceType.KING, PSTKingEarly);
    }

    public Eval() {
    }

    public double evalAll(Board board, boolean isWhiteTurn) {
        double totalScore = 0;
        totalScore += evalMaterial(board);
        totalScore += evalPST(board);
        // Tempo bonus
        totalScore += (isWhiteTurn) ? 0.1 : -0.1;

        return totalScore;
    }

    private double evalMaterial(Board board) {
        double materialScore = 0;
        // Material
        for (Piece p : board.getPieceList(pieceColour.WHITE)) {
            materialScore += materialValues.get(p.getType());
        }
        for (Piece p : board.getPieceList(pieceColour.BLACK)) {
            materialScore -= materialValues.get(p.getType());
        }

        // Replace middlegame king table with endgame king table if endgame is near
        if(gameStageScore(materialScore) <= 0.1){
            pstValues.put(pieceType.KING, PSTKingEnd);
        }
        
        return materialScore;
    }

    private double evalPST(Board board) {

        double pstScore = 0;
        for (Piece p : board.getPieceList(pieceColour.WHITE)) {
            int row = p.getCoordinates().getRow();
            int col = p.getCoordinates().getCol();
            pstScore += pstValues.get(p.getType())[row][col];
        }
        for (Piece p : board.getPieceList(pieceColour.BLACK)) {
            int row = p.getCoordinates().getRow();
            int col = p.getCoordinates().getCol();
            pstScore -= pstValues.get(p.getType())[7-row][col];
        }
        return pstScore;
    }

    // Determine game stage from material score x using interpolation
    // 1 is opening, 0.5 is middlegame, 0 is endgame
    private double gameStageScore(double x){
        return (-32*x*x + 8*x + 239);
    }
}
