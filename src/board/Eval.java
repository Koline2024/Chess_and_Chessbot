package board;

import pieces.Piece;
import enums.*;

import java.util.EnumMap;
import java.util.Map;

public class Eval {

    // Implement pawn structure, pawn center, passed pawns
    // Implement decreasing value of knight the less pawns there are
    // Implement penalty for bad bishop based on mobility score
    // Implement bonus for bishop pair
    // Implement bonus for fianchetto if center is clear; penalty otherwise
    // Implement increasing value of rook the less pawns there are
    // Implement penalty for queen early movement (within first 10 moves)
    // Implement king safety algorithm based on king tropism
    // Implement minus/plus infinity penalty for any mate patterns

    // Map of piece types to material value
    private static Map<pieceType, Double> materialValues = new EnumMap<>(pieceType.class);
    static {
        materialValues.put(pieceType.PAWN, 1.0);
        materialValues.put(pieceType.KNIGHT, 3.0);
        materialValues.put(pieceType.BISHOP, 3.0);
        materialValues.put(pieceType.ROOK, 5.0);
        materialValues.put(pieceType.QUEEN, 9.0);
        materialValues.put(pieceType.KING, 200.0);
    }

    // Map of piece types to mobility value
    private static Map<pieceType, Double> mobilityValues = new EnumMap<>(pieceType.class);
    static {
        mobilityValues.put(pieceType.PAWN, 0.001);
        mobilityValues.put(pieceType.KNIGHT, 0.001); // Knights jump over other pieces anyway
        mobilityValues.put(pieceType.BISHOP, 0.01); // Bishop mobility is important
        mobilityValues.put(pieceType.ROOK, 0.01); 
        mobilityValues.put(pieceType.QUEEN, 0.01); 
        mobilityValues.put(pieceType.KING, 0.0); 
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
            { 0, 0, 0, -0.5, -0.5, 0, 0, 0 }, // Discourage idle centre
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
        totalScore += evalTropism(board);
        totalScore += evalMobility(board);
        // Tempo bonus
        // totalScore += (isWhiteTurn) ? 0.1 : -0.1;

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

        return materialScore;
    }

    private double evalPST(Board board) {
        double pstScore = 0;
        double phase = getGamePhase(board); // 1 is opening, 0 is endgame

        for (pieceColour color : pieceColour.values()) {
            double colorMultiplier = (color == pieceColour.WHITE) ? 1.0 : -1.0;

            for (Piece p : board.getPieceList(color)) {
                int row = p.getCoordinates().getRow();
                int col = p.getCoordinates().getCol();
                if (color == pieceColour.BLACK)
                    row = 7 - row;
                if (p.getType() == pieceType.KING) {
                    // Blend Early and End tables
                    double kingEarly = PSTKingEarly[row][col];
                    double kingEnd = PSTKingEnd[row][col];
                    pstScore += (phase * kingEarly + (1 - phase) * kingEnd) * colorMultiplier;
                } else {
                    pstScore += pstValues.get(p.getType())[row][col] * colorMultiplier;
                }
            }
        }
        return pstScore;
    }

    private double evalMobility(Board board) {
        double mobilityScore = 0;
        for(Move m : board.getLegalMoves(pieceColour.WHITE)){
            mobilityScore += mobilityValues.get(m.piece.getType());
        }
        for(Move m : board.getLegalMoves(pieceColour.BLACK)){
            mobilityScore -= mobilityValues.get(m.piece.getType());
        }
        return mobilityScore;
    }

    // Determine game stage from material score 
    // 1 is opening, 0 is endgame
    private double getGamePhase(Board board) {
        int totalPhase = 24; // Non pawn material
        int currentPhase = 0;
        for (pieceColour color : pieceColour.values()) {
            for (Piece p : board.getPieceList(color)) {
                if (p.getType() == pieceType.QUEEN) {
                    currentPhase += 4;
                } else if (p.getType() == pieceType.ROOK) {
                    currentPhase += 2;
                } else if (p.getType() == pieceType.BISHOP || p.getType() == pieceType.KNIGHT) {
                    currentPhase += 1;
                }
            }
        }
        return (double) currentPhase / totalPhase;
    }

    private double evalTropism(Board board) {
        double tropScore = 0;
        Coordinates whiteKing = board.findKing(pieceColour.WHITE);
        Coordinates blackKing = board.findKing(pieceColour.BLACK);

        for (Piece p : board.getPieceList(pieceColour.WHITE)) {
            tropScore += tropismBonus(p, blackKing);
        }
        for (Piece p : board.getPieceList(pieceColour.BLACK)) {
            tropScore -= tropismBonus(p, whiteKing);
        }
        return tropScore;
    }

    private double tropismBonus(Piece p, Coordinates kingPos) {
        int dist = Math.abs(p.getCoordinates().getCol() - kingPos.getCol())
                + Math.abs(p.getCoordinates().getRow() - kingPos.getRow());
        return 0.05 * (7 - dist); // Empirical, adjust as necessary
    }
}
