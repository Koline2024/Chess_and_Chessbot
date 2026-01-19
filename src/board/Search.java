package board;

import enums.pieceColour;
import java.util.List;

public class Search {

    private Eval evaluator = new Eval();
    private static final double inf = 1000000;
    private static final double checkmate = 1000000;

    public Move findBestMove(Board board, int depth, boolean isWhiteTurn) {
        long startTime = System.currentTimeMillis();
        Move bestMove = null;
        double bestScore = (isWhiteTurn) ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY;
        double alpha = Double.NEGATIVE_INFINITY;
        double beta = Double.POSITIVE_INFINITY;
        pieceColour colour = (isWhiteTurn) ? pieceColour.WHITE : pieceColour.BLACK;
        List<Move> moves = board.getLegalMoves(colour);
        for (Move m : moves) {
            board.doMove(m);
            double score = minimax(board, depth - 1, alpha, beta, !isWhiteTurn);
            board.undoMove();

            if (isWhiteTurn) {
                if (score > bestScore) {
                    bestScore = score;
                    bestMove = m;
                }
                alpha = Math.max(alpha, score);
            } else {
                if (score < bestScore) {
                    bestScore = score;
                    bestMove = m;
                }
                beta = Math.min(beta, score);
            }

        }
        long time = System.currentTimeMillis() - startTime;
        System.out.println("Best move: " + bestMove + " was found in " + time + " ms.");
        return bestMove;
    }

    /**
     * Implements the classical minimax algorithm with alpha-beta pruning.
     * 
     * @param board
     * @param depth
     * @param alpha
     * @param beta
     * @param isWhiteTurn
     * @return
     */
    private double minimax(Board board, int depth, double alpha, double beta, boolean isWhiteTurn) {
        pieceColour turn = (isWhiteTurn) ? pieceColour.WHITE : pieceColour.BLACK;
        if (depth == 0) {
            return quiescenceSearch(board, alpha, beta, isWhiteTurn);
        }
        List<Move> moves = board.getLegalMoves(turn);
        // Check for mating moves first
        if (moves.isEmpty()) {
            Coordinates kingPos = board.findKing(turn);
            if (board.isSquareAttacked(kingPos, turn)) {
                // If white is checkmated return negative infinity plus depth, opposite with
                // black.
                return isWhiteTurn ? -checkmate - depth : checkmate + depth;
            } else {
                return 0; // Stalemate
            }
        }

        // Maximising player
        if (isWhiteTurn) {
            double maxEval = -inf;
            for (Move m : moves) {
                board.doMove(m);
                double score = minimax(board, depth - 1, alpha, beta, false);
                board.undoMove();
                maxEval = Math.max(maxEval, score);
                // Prune branch if unfavourable
                if (maxEval >= beta) {
                    break;
                }
                alpha = Math.max(alpha, score);
            }
            return maxEval;
        } else {
            // Minimising player
            double minEval = inf;
            for (Move m : moves) {
                board.doMove(m);
                double score = minimax(board, depth - 1, alpha, beta, true);
                board.undoMove();
                minEval = Math.min(minEval, score);
                if (minEval <= alpha) {
                    break;
                }
                beta = Math.min(beta, minEval);
            }
            return minEval;

        }

    }

    private double quiescenceSearch(Board board, double alpha, double beta, boolean isWhiteTurn) {
        double pat = evaluator.evalAll(board, isWhiteTurn);
        if (isWhiteTurn) {
            if (pat >= beta) {
                return beta;
            }
            if (alpha < pat) {
                alpha = pat;
            }
        } else {
            if (pat <= alpha) {
                return alpha;
            }
            if (beta > pat) {
                beta = pat;
            }
        }
        List<Move> moves = board.getLegalMoves(isWhiteTurn ? pieceColour.WHITE : pieceColour.BLACK);
        moves.removeIf(m -> m.getCapturePiece() == null); // Filter for only captures
        for (Move m : moves) {
            board.doMove(m);
            double score = quiescenceSearch(board, alpha, beta, !isWhiteTurn);
            board.undoMove();

            if (isWhiteTurn) {
                pat = Math.max(pat, score);
                alpha = Math.max(alpha, pat);
            } else {
                pat = Math.min(pat, score);
                beta = Math.min(beta, pat);
            }
            if (beta <= alpha) {
                break;
            }
        }
        return pat;

    }


    // TODO: move sort algorithm using captures with regard to least valuable/most valuable pieces
}
