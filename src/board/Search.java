package board;

import enums.pieceColour;
import enums.pieceType;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

public class Search {

    private static Map<pieceType, Integer> materialValues = new EnumMap<>(pieceType.class);
    static {
        materialValues.put(pieceType.PAWN, 100);
        materialValues.put(pieceType.KNIGHT, 300);
        materialValues.put(pieceType.BISHOP, 300);
        materialValues.put(pieceType.ROOK, 500);
        materialValues.put(pieceType.QUEEN, 900);
        materialValues.put(pieceType.KING, 20000);
    }

    private Eval evaluator = new Eval();
    private static final int inf = 1000000;
    private static final int checkmate = 900000;
    // Divisor 56 to account for Java object overhead
    public final TranspositionTable tTable = new TranspositionTable(516);

    public Move findBestMove(Board board, int maxDepth, boolean isWhiteTurn, boolean isTricky) {
        pieceColour side = (isWhiteTurn) ? pieceColour.WHITE : pieceColour.BLACK;
        List<Move> allRootMoves = board.getLegalMoves(side);

        // Map to store scores for each move at each depth
        Map<Move, int[]> historyMoves = new HashMap<>();
        if (allRootMoves.isEmpty()){
            return null;
        }
        for (Move m : allRootMoves) {
            historyMoves.put(m, new int[maxDepth + 1]);
            Arrays.fill(historyMoves.get(m), -inf - 7); // Placeholder for "unsearched"
        }

        Instant start = Instant.now();
        long limitMs = 3000;
        int lastCompletedDepth = 0;

        // Iterative deepening
        for (int depth = 1; depth <= maxDepth; depth++) {
            int alpha = -inf;
            int beta = inf;

            // Sort root moves based on previous depth results for better pruning
            sortRootMoves(allRootMoves, historyMoves, lastCompletedDepth, isWhiteTurn);

            for (Move m : allRootMoves) {
                board.doMove(m); 
                // Start search at ply 1 because we just made a move
                int score = minimax(board, depth - 1, alpha, beta, !isWhiteTurn, 1);
                board.undoMove();
                
                // VALIDATION CHECK TODO: delete
                // if (board.getPieceList(pieceColour.BLACK).size() != 2) {
                //     System.out.println("CRITICAL: Piece count desync after undoing " + m);
                //     System.out.println("Move info: Promo=" + m.isPromotion() + " Pim=" + m.piece.getSymbol());
                //     System.out.println("Black now has: ");
                //     for (Piece p : board.getPieceList(pieceColour.BLACK)){
                //         System.out.println(p.getSymbol());
                //     }
                // }

                historyMoves.get(m)[depth] = score;

                // Root Alpha-Beta Pruning
                if (isWhiteTurn) {
                    alpha = Math.max(alpha, score);
                } else {
                    beta = Math.min(beta, score);
                }

                // Time Check: Exit mid-depth if we are overtime
                if (Duration.between(start, Instant.now()).toMillis() > limitMs) {
                    return (isTricky) ? chooseTrickyMove(historyMoves, allRootMoves, lastCompletedDepth, isWhiteTurn)
                            : chooseMove(historyMoves, allRootMoves, lastCompletedDepth, isWhiteTurn);
                }
            }
            lastCompletedDepth = depth;
        }

        return (isTricky) ? chooseTrickyMove(historyMoves, allRootMoves, lastCompletedDepth, isWhiteTurn)
                            : chooseMove(historyMoves, allRootMoves, lastCompletedDepth, isWhiteTurn);
    }

    private int minimax(Board board, int depth, int alpha, int beta, boolean isWhiteTurn, int ply) {
        int originalAlpha = alpha;
        pieceColour turn = isWhiteTurn ? pieceColour.WHITE : pieceColour.BLACK;

        // verifyHash(board, depth, isWhiteTurn);
        // 1. TT Lookup (Normalizing mate scores with ply)
        TranspositionTable.Entry ttEntry = tTable.get(board.zobristHash, ply);
        if (ttEntry != null && ttEntry.depth >= depth) {
            int score = ttEntry.score;
            if (score > checkmate - 1000) {
                score -= ply;
            } else if (score < -checkmate + 1000) {
                score += ply;
            }
            if (ttEntry.flag == TranspositionTable.exact)
                return score;
            if (ttEntry.flag == TranspositionTable.lowerBound)
                alpha = Math.max(alpha, score);
            if (ttEntry.flag == TranspositionTable.upperBound)
                beta = Math.min(beta, score);
            if (alpha >= beta)
                return score;
        }

        // 2. Base Cases
        if (depth <= 0)
            return quiescenceSearch(board, alpha, beta, isWhiteTurn);

        List<Move> moves = board.getLegalMoves(turn);
        if (moves.isEmpty()) {
            Coordinates kingPos = board.findKing(turn);
            if (board.isSquareAttacked(kingPos, turn == pieceColour.WHITE ? pieceColour.WHITE : pieceColour.BLACK)) {
                return isWhiteTurn ? -checkmate + ply : checkmate - ply;
            }
            return isWhiteTurn ? -300 : 300; // Draw contempt factor: Don't draw unless 3 pawns down or equivalent
        }

        // 3. Move Ordering (Use TT move first)
        sortMoves(moves, board);
        if (ttEntry != null && ttEntry.bestMove != null) {
            Move best = ttEntry.bestMove;
            moves.removeIf(m -> m.toString().equals(best.toString()));
            moves.add(0, best);
        }

        int bestScore = isWhiteTurn ? -inf : inf;
        Move bestMove = null;

        for (Move m : moves) {
            board.doMove(m); // Make a copy

            // CHECK EXTENSION: If you put the opponent in check, search deeper
            int extension = 0;
            pieceColour nextToMove = isWhiteTurn ? pieceColour.BLACK : pieceColour.WHITE;
            if (board.isSquareAttacked(board.findKing(nextToMove), turn))
                extension = 1;

            int score = minimax(board, depth - 1 + extension, alpha, beta, !isWhiteTurn, ply + 1);
            board.undoMove();

            if (isWhiteTurn) {
                if (score > bestScore) {
                    bestScore = score;
                    bestMove = m;
                }
                alpha = Math.max(alpha, bestScore);
            } else {
                if (score < bestScore) {
                    bestScore = score;
                    bestMove = m;
                }
                beta = Math.min(beta, bestScore);
            }

            if (alpha >= beta)
                break;
        }

        // 4. TT Store (Normalizing mate scores with ply)
        int flag = (bestScore <= originalAlpha) ? TranspositionTable.upperBound
                : (bestScore >= beta) ? TranspositionTable.lowerBound : TranspositionTable.exact;

        tTable.store(board.zobristHash, depth, bestScore, flag, bestMove, ply);
        return bestScore;
    }

    private int quiescenceSearch(Board board, int alpha, int beta, boolean isWhiteTurn) {
        int standPat = evaluator.evalAll(board, isWhiteTurn);
        if (isWhiteTurn) {
            if (standPat >= beta)
                return beta;
            alpha = Math.max(alpha, standPat);
        } else {
            if (standPat <= alpha)
                return alpha;
            beta = Math.min(beta, standPat);
        }

        List<Move> captures = board.getLegalMoves(isWhiteTurn ? pieceColour.WHITE : pieceColour.BLACK);
        captures.removeIf(m -> m.getCapturedPiece() == null);
        sortMoves(captures, board);

        for (Move m : captures) {
            board.doMove(m); // Make a copy
            int score = quiescenceSearch(board, alpha, beta, !isWhiteTurn);
            board.undoMove();

            if (isWhiteTurn) {
                if (score >= beta)
                    return beta;
                alpha = Math.max(alpha, score);
            } else {
                if (score <= alpha)
                    return alpha;
                beta = Math.min(beta, score);
            }
        }
        return isWhiteTurn ? alpha : beta;
    }

    private void sortRootMoves(List<Move> moves, Map<Move, int[]> history, int depth, boolean isWhite) {
        if (depth == 0)
            return;
        moves.sort((a, b) -> Integer.compare(history.get(b)[depth], history.get(a)[depth]) * (isWhite ? 1 : -1));
    }

    public void sortMoves(List<Move> moves, Board board) {
        moves.sort((a, b) -> Integer.compare(scoreMove(b, board), scoreMove(a, board)));
    }

    private int scoreMove(Move move, Board board) {
        int score = 0;
        if (move.getCapturedPiece() != null) {
            score = (materialValues.get(move.getCapturedPiece().getType()) * 10)
                    - materialValues.get(move.piece.getType());
            score += 10000;
        }
        if (move.isPromotion())
            score += 8000;
        return score;
    }

    private Move chooseMove(Map<Move, int[]> history, List<Move> moves, int maxD, boolean isWhiteTurn) {
        int multiplier = isWhiteTurn ? 1 : -1;
        Move bestMove = moves.get(0);
        double relativeScore = -inf; // Make higher better regardless of side because side gave me a headache lol

        // First find objectively best move
        int objectiveBestScore = isWhiteTurn ? -inf : inf;
        for (Move m : moves) {
            int score = history.get(m)[maxD];
            if (score <= -inf - 7) {
                continue; // Skip unsearched moves
            }
            if (multiplier * score > multiplier * objectiveBestScore) {
                objectiveBestScore = score;
                bestMove = m;
            }
        }
        // Now we know the objectively best move
        // BUT WE DONT DO IT! Look for traps like a rat

        return bestMove;
    }

    private Move chooseTrickyMove(Map<Move, int[]> history, List<Move> moves, int maxD, boolean isWhiteTurn) {
        int multiplier = isWhiteTurn ? 1 : -1;
        Move bestMove = moves.get(0);
        double relativeScore = -inf; // Make higher better regardless of side because side gave me a headache lol

        // First find objectively best move
        int objectiveBestScore = isWhiteTurn ? -inf : inf;
        for (Move m : moves) {
            int score = history.get(m)[maxD];
            if (score <= -inf - 7) {
                continue; // Skip unsearched moves
            }
            if (multiplier * score > multiplier * objectiveBestScore) {
                objectiveBestScore = score;
                bestMove = m;
            }
        }
        // Now we know the objectively best move
        // BUT WE DONT DO IT! Look for traps like a rat

        for (Move m : moves) {
            int finalScore = history.get(m)[maxD]; // This is the final score of the move
            if (finalScore <= -inf - 7) {
                continue; // Skip unsearched moves
            }
            // TODO: Experimental! Worst objective move must be within 1 pawn of best
            if (multiplier * finalScore >= (multiplier * objectiveBestScore - 50)) {
                double shallowScore = history.get(m)[Math.min(maxD, 3)]; // Experimental depth of 3
                if (shallowScore <= -inf - 7) {
                    shallowScore = finalScore;
                }
                // Trap potential, or the difference between shallow and deep evaluations
                double trapPotential = multiplier * (finalScore - shallowScore);
                double trapWeight = 0.2; // TODO: experimental
                double currentRelativeScore = multiplier * finalScore + trapPotential * trapWeight;
                if (currentRelativeScore > relativeScore) {
                    relativeScore = currentRelativeScore;
                    bestMove = m;
                }

                if (finalScore < -500 && objectiveBestScore > -100) {
                    System.out.println("CRITICAL EVALUATION DROP DETECTED");
                    System.out.println("Move: " + m);
                    System.out.println("Deep Score: " + finalScore);
                    System.out.println("Shallow Score: " + shallowScore);
                }
            }

        }

        return bestMove;
    }

    public void verifyHash(Board board, int depth, boolean isWhiteTurn) {
        if (depth == 0)
            return;

        // Capture the hash before making any moves
        long originalHash = board.zobristHash;
        List<Move> moves = board.getLegalMoves(isWhiteTurn ? pieceColour.WHITE : pieceColour.BLACK);

        for (Move m : moves) {
            board.doMove(m); // Make a copy

            // 2. (Optional) Re-calculate hash from scratch to compare
            // long manualHash = board.calculateHashFromScratch();
            // if (board.zobristHash != manualHash) throw new RuntimeException("Incremental
            // update failed!");

            verifyHash(board, depth - 1, !isWhiteTurn);

            board.undoMove();

            // 3. The Ultimate Test
            if (board.zobristHash != originalHash) {
                System.out.println("HASH CORRUPTION DETECTED!");
                System.out.println("Move: " + m);
                System.out.println("Expected: " + Long.toHexString(originalHash));
                System.out.println("Actual:   " + Long.toHexString(board.zobristHash));
                System.out.println("Difference: " + Long.toHexString(originalHash ^ board.zobristHash));
                throw new RuntimeException("Zobrist Sync Lost");
            }
        }
    }

}