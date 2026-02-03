package board;

import pieces.Piece;
import enums.*;

import java.util.EnumMap;
import java.util.Map;
import java.util.List;

public class Eval {

    public double phase;

    // Map of piece types to material value
    private static Map<pieceType, Integer> materialValues = new EnumMap<>(pieceType.class);
    static {
        materialValues.put(pieceType.PAWN, 100);
        materialValues.put(pieceType.KNIGHT, 300);
        materialValues.put(pieceType.BISHOP, 300);
        materialValues.put(pieceType.ROOK, 500);
        materialValues.put(pieceType.QUEEN, 900);
        materialValues.put(pieceType.KING, 20000);
    }

    // Map of piece types to mobility value
    private static Map<pieceType, Integer> mobilityValues = new EnumMap<>(pieceType.class);
    static {
        mobilityValues.put(pieceType.PAWN, 10);
        mobilityValues.put(pieceType.KNIGHT, 50);
        mobilityValues.put(pieceType.BISHOP, 50);
        mobilityValues.put(pieceType.ROOK, 50);
        mobilityValues.put(pieceType.QUEEN, 50);
        mobilityValues.put(pieceType.KING, 0);
    }

    // PSTs below
    private static int[][] PSTPawn = {
            { 900, 900, 900, 900, 900, 900, 900, 900 }, // Encourage promotion
            { 50, 50, 50, 50, 50, 50, 50, 50 },
            { 10, 10, 20, 30, 30, 20, 10, 10 },
            { 5, 5, 10, 25, 25, 10, 5, 5 },
            { 0, 0, 0, 15, 15, 0, 0, 0 },
            { 5, 5, 10, -10, -10, 10, 5, 5 },
            { 0, 0, 0, -30, -30, 0, 0, 0 },
            { 0, 0, 0, 0, 0, 0, 0, 0 }
    };

    private static int[][] PSTKnight = {
            { -50, -40, -30, -30, -30, -30, -40, -50 },
            { -40, -20, 0, 0, 0, 0, -20, -40 },
            { -30, 0, 10, 20, 20, 10, 0, -30 },
            { -30, 5, 15, 30, 30, 15, 5, -30 },
            { -30, 0, 10, 20, 20, 10, 0, -30 },
            { -30, 0, 10, 20, 20, 10, 0, -30 },
            { -40, -20, 0, 0, 0, 0, -20, -40 },
            { -50, -40, -30, -30, -30, -30, -40, -50 }
    };

    private static int[][] PSTBishop = {
            { -20, -10, -10, -10, -10, -10, -10, -20 },
            { -10, 0, 0, 0, 0, 0, 0, -10 },
            { -10, 0, 5, 10, 10, 5, 0, -10 },
            { -10, 5, 5, 10, 10, 5, 5, -10 },
            { -10, 5, 5, 10, 10, 5, 5, -10 },
            { -10, 0, 5, 10, 10, 5, 0, -10 },
            { -10, 0, 0, 0, 0, 0, 0, -10 },
            { -20, -10, -50, -10, -10, -50, -10, -20 }
    };

    private static int[][] PSTRook = {
            { 0, 0, 0, 0, 0, 0, 0, 0 },
            { 20, 20, 20, 20, 20, 20, 20, 20 },
            { 0, 0, 0, 0, 0, 0, 0, 0 },
            { 0, 0, 0, 0, 0, 0, 0, 0 },
            { 0, 0, 0, 0, 0, 0, 0, 0 },
            { 0, 0, 0, 0, 0, 0, 0, 0 },
            { 0, 0, 0, 0, 0, 0, 0, 0 },
            { -10, 0, 0, 10, 10, 0, 0, -10 },
    };

    private static int[][] PSTQueen = {
            { -10, -5, -5, 0, 0, -5, -5, -10 },
            { -5, 0, 0, 5, 5, 0, 0, -5 },
            { -5, 0, 10, 10, 10, 10, 0, -5 },
            { -5, 0, 10, 10, 10, 10, 0, -5 },
            { -5, 0, 10, 10, 10, 10, 0, -5 },
            { -5, 0, 10, 10, 10, 10, 0, -5 },
            { -5, 0, 0, 5, 5, 0, 0, -5 },
            { -10, -5, -5, -10, 0, -5, -5, -10 }
    };

    public static int[][] PSTKingEarly = {
            { -50, -60, -60, -70, -70, -60, -60, -50 },
            { -50, -60, -60, -70, -70, -60, -60, -50 },
            { -50, -60, -60, -70, -70, -60, -60, -50 },
            { -50, -60, -60, -70, -70, -60, -60, -50 },
            { -40, -50, -50, -60, -60, -50, -50, -40 },
            { -40, -50, -50, -60, -60, -50, -50, -40 },
            { 20, 20, 0, 0, 0, 0, 20, 20 },
            { 20, 30, 20, 0, -20, -20, 30, 20 }
    };

    public static int[][] PSTKingEnd = {
            { -50, -40, -30, -30, -30, -30, -40, -50 },
            { -40, -20, 0, 0, 0, 0, -20, -40 },
            { -30, 0, 10, 20, 20, 10, 0, -30 },
            { -30, 5, 15, 30, 30, 15, 5, -30 },
            { -30, 0, 10, 20, 20, 10, 0, -30 },
            { -30, 0, 10, 20, 20, 10, 0, -30 },
            { -40, -20, 0, 0, 0, 0, -20, -40 },
            { -50, -40, -30, -30, -30, -30, -40, -50 }
    };

    private static Map<pieceType, int[][]> pstValues = new EnumMap<>(pieceType.class);
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

    // returns evaluation in original scale (integer centipawns)
    public int evalAll(Board board, boolean isWhiteTurn) {
        // cache commonly requested lists/positions to avoid repeated lookups
        List<Piece> whitePieces = board.getPieceList(pieceColour.WHITE);
        List<Piece> blackPieces = board.getPieceList(pieceColour.BLACK);
        //List<Move> whiteMoves = board.getLegalMoves(pieceColour.WHITE);
        //List<Move> blackMoves = board.getLegalMoves(pieceColour.BLACK);
        Coordinates whiteKing = board.findKing(pieceColour.WHITE);
        Coordinates blackKing = board.findKing(pieceColour.BLACK);
        phase = getGamePhase(whitePieces, blackPieces); // 1 is opening, 0 is endgame
        int deltaMaterial = evalMaterial(whitePieces, blackPieces); // Necessary for finale

        int totalScore = 0;
        totalScore += deltaMaterial;
        totalScore += evalPST(whitePieces, blackPieces);
        totalScore += evalTropism(whitePieces, blackPieces, whiteKing, blackKing);
        //totalScore += evalMobility(whiteMoves, blackMoves);
        totalScore += evalSpecialBonuses(whitePieces, blackPieces);
        totalScore += evalPositionals(board, whitePieces, blackPieces);
        totalScore += evalOpening(whitePieces, blackPieces);
        totalScore += evalFinale(whiteKing, blackKing, deltaMaterial);

        return totalScore;
    }

    private int evalMaterial(List<Piece> whitePieces, List<Piece> blackPieces) {
        int materialScore = 0;
        // use local reference to map to avoid repeated static lookups
        Map<pieceType, Integer> mat = materialValues;
        for (Piece p : whitePieces) {
            materialScore += mat.get(p.getType());
        }
        for (Piece p : blackPieces) {
            materialScore -= mat.get(p.getType());
        }
        return materialScore;
    }

    private int evalPST(List<Piece> whitePieces, List<Piece> blackPieces) {
        int pstScore = 0;
        // white pieces
        for (Piece p : whitePieces) {
            int row = p.getCoordinates().getRow();
            int col = p.getCoordinates().getCol();
            if (p.getType() == pieceType.KING) {
                int kingEarly = PSTKingEarly[row][col];
                int kingEnd = PSTKingEnd[row][col];
                double blended = phase * kingEarly + (1.0 - phase) * kingEnd;
                pstScore += (int) Math.round(blended);
            } else {
                pstScore += pstForPiece(p.getType(), row, col);
            }
        }
        // black pieces (mirror rows)
        for (Piece p : blackPieces) {
            int row = 7 - p.getCoordinates().getRow();
            int col = p.getCoordinates().getCol();
            if (p.getType() == pieceType.KING) {
                int kingEarly = PSTKingEarly[row][col];
                int kingEnd = PSTKingEnd[row][col];
                double blended = phase * kingEarly + (1.0 - phase) * kingEnd;
                pstScore -= (int) Math.round(blended);
            } else {
                pstScore -= pstForPiece(p.getType(), row, col);
            }
        }
        return pstScore;
    }

    // helper to avoid map lookups for PSTs - faster switch dispatch
    private int pstForPiece(pieceType t, int row, int col) {
        switch (t) {
            case PAWN:
                return PSTPawn[row][col];
            case KNIGHT:
                return PSTKnight[row][col];
            case BISHOP:
                return PSTBishop[row][col];
            case ROOK:
                return PSTRook[row][col];
            case QUEEN:
                return PSTQueen[row][col];
            default:
                return 0;
        }
    }

    private int evalMobility(List<Move> whiteMoves, List<Move> blackMoves) {
        int mobilityScore = 0;
        for (Move m : whiteMoves) {
            mobilityScore += mobilityForPiece(m.piece.getType());
        }
        for (Move m : blackMoves) {
            mobilityScore -= mobilityForPiece(m.piece.getType());
        }
        return mobilityScore;
    }

    // small switch to avoid map lookup overhead in hot loops
    private int mobilityForPiece(pieceType t) {
        switch (t) {
            case PAWN:
                return 1;
            case KNIGHT:
                return 2;
            case BISHOP:
                return 3;
            case ROOK:
                return 2;
            case QUEEN:
                return 2;
            default:
                return 0;
        }
    }

    // Determine game stage from material score
    // 1 is opening, 0 is endgame
    private double getGamePhase(List<Piece> whitePieces, List<Piece> blackPieces) {
        final int totalPhase = 24; // Non pawn material
        int currentPhase = 0;
        for (Piece p : whitePieces) {
            currentPhase += phaseWeight(p.getType());
        }
        for (Piece p : blackPieces) {
            currentPhase += phaseWeight(p.getType());
        }
        return (double) currentPhase / totalPhase;
    }

    private int phaseWeight(pieceType t) {
        switch (t) {
            case QUEEN:
                return 4;
            case ROOK:
                return 2;
            case BISHOP:
            case KNIGHT:
                return 1;
            default:
                return 0;
        }
    }

    private int evalTropism(List<Piece> whitePieces, List<Piece> blackPieces, Coordinates whiteKing,
            Coordinates blackKing) {
        int tropScore = 0;
        for (Piece p : whitePieces) {
            tropScore += tropismBonus(p, blackKing);
        }
        for (Piece p : blackPieces) {
            tropScore -= tropismBonus(p, whiteKing);
        }
        return tropScore;
    }

    private int tropismBonus(Piece p, Coordinates kingPos) {
        int dist = Math.abs(p.getCoordinates().getCol() - kingPos.getCol())
                + Math.abs(p.getCoordinates().getRow() - kingPos.getRow());
        return 5 * (7 - dist);
    }

    private int evalSpecialBonuses(List<Piece> whitePieces, List<Piece> blackPieces) {
        int bonus = 0;
        int bishopCountWhite = 0;
        int bishopCountBlack = 0;
        int rookCountWhite = 0;
        int rookCountBlack = 0;
        int knightCountWhite = 0;
        int knightCountBlack = 0;

        for (Piece p : whitePieces) {
            switch (p.getType()) {
                case PAWN:
                    break;
                case KNIGHT:
                    knightCountWhite++;
                    break;
                case BISHOP:
                    bishopCountWhite++;
                    break;
                case ROOK:
                    rookCountWhite++;
                    break;
                case QUEEN:
                    break;
                case KING:
                    break;
                default:
                    break;
            }
        }
        for (Piece p : blackPieces) {
            switch (p.getType()) {
                case PAWN:
                    break;
                case KNIGHT:
                    knightCountBlack++;
                    break;
                case BISHOP:
                    bishopCountBlack++;
                    break;
                case ROOK:
                    rookCountBlack++;
                    break;
                case QUEEN:
                    break;
                case KING:
                    break;
                default:
                    break;
            }
        }
        if (knightCountWhite == 2) {
            bonus -= 0;
        }
        if (knightCountBlack == 2) {
            bonus += 0;
        }
        if (bishopCountWhite == 2) {
            bonus += (1 - phase) * 30;
        }
        if (bishopCountBlack == 2) {
            bonus -= (1 - phase) * 30;
        }
        if (rookCountWhite == 2) {
            bonus += 10;
        }
        if (rookCountBlack == 2) {
            bonus -= 10;
        }
        return bonus;
    }

    // If in a winning endgame use this to checkmate opponent king
    private int evalFinale(Coordinates whiteKing, Coordinates blackKing, int deltaMaterial) {
        int finaleScore = 0;
        int dist = Math.abs(whiteKing.getCol() - blackKing.getCol())
                + Math.abs(whiteKing.getRow() - blackKing.getRow());
        int distWhiteCorner = Math.max(3 - whiteKing.getCol(), whiteKing.getCol() - 4)
                + Math.max(3 - whiteKing.getRow(), whiteKing.getRow() - 4);
        int distBlackCorner = Math.max(3 - blackKing.getCol(), blackKing.getCol() - 4)
                + Math.max(3 - blackKing.getRow(), blackKing.getRow() - 4);
        // Make sure it's end-endgame
        if (phase > 0.2) {
            return finaleScore;
        }
        // Only apply finale heuristics when one side has a decisive material advantage
        // (>= 2 pawns)
        if (deltaMaterial >= 200) { // White winning by >= 2 pawns
            finaleScore += 10 * distBlackCorner;
            finaleScore += 4 * (14 - dist);
        } else if (deltaMaterial <= -200) { // Black winning by >= 2 pawns
            finaleScore -= 10 * distWhiteCorner;
            finaleScore -= 4 * (14 - dist);
        }
        return finaleScore;
    }

    public int evalOpening(List<Piece> whitePieces, List<Piece> blackPieces) {
        int openingScore = 0;
        for (Piece p : whitePieces) {
            if (p.hasMoved()) {
                openingScore += 10;
            }
        }
        for (Piece p : blackPieces) {
            if (p.hasMoved()) {
                openingScore -= 10;
            }
        }
        return openingScore;
    }

    public int evalPositionals(Board board, List<Piece> whitePieces, List<Piece> blackPieces) {
        int positionalScore = 0;
        for (Piece p : whitePieces) {
            if (p.getType() == pieceType.PAWN) {
                for (int i = p.getCoordinates().getRow(); i < 8; i++) {
                    Piece p2 = (board.getPieceAt(i, p.getCoordinates().getCol()));
                    if (p2 != null) {
                        if (p2.getType() == pieceType.PAWN && p2.getColour() == p.getColour()) {
                            positionalScore -= 30; // Penalise doubled pawns or tripled pawns
                        }
                    }
                }
            }
        }
        for (Piece p : blackPieces) {
            if (p.getType() == pieceType.PAWN) {
                for (int i = p.getCoordinates().getRow(); i >= 0; i--) {
                    Piece p2 = board.getPieceAt(i, p.getCoordinates().getCol());
                    if (p2 != null) {
                        if (p2.getType() == pieceType.PAWN && p2.getColour() == p.getColour()) {
                            positionalScore += 30; // Penalise doubled pawns or tripled pawns
                        }
                    }
                }
            }
        }

        return positionalScore;
    }
}
