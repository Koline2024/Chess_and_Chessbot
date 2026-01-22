package board;

import enums.pieceColour;
import enums.pieceType;
import pieces.Piece;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class Search {

    private static Map<pieceType, Integer> materialValues = new EnumMap<>(pieceType.class);
    static {
        materialValues.put(pieceType.PAWN, 1);
        materialValues.put(pieceType.KNIGHT, 3);
        materialValues.put(pieceType.BISHOP, 3);
        materialValues.put(pieceType.ROOK, 5);
        materialValues.put(pieceType.QUEEN, 9);
        materialValues.put(pieceType.KING, 200);
    }

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
        // Sort moves here for the speedup
        sortMoves(moves, board);
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
        sortMoves(moves, board);
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

    /**
     * Quiescence search is applied after standard minimax to 
     * avoid the horizon effect problem.
     * @param board
     * @param alpha
     * @param beta
     * @param isWhiteTurn
     * @return
     */
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
        sortMoves(moves, board);
        for (Move m : moves) {
            board.doMove(m);
            double score = quiescenceSearch(board, alpha, beta, !isWhiteTurn);
            board.undoMove();

            if(isWhiteTurn){
                if(score >= beta){
                    return beta;
                }
                if(score > alpha){
                    alpha = score;
                }
            }else{
                if(score <= alpha){
                    return alpha;
                }
                if(score < beta){
                    beta = score;
                }
            }

        }
        return isWhiteTurn ? alpha : beta;

    }

    /**
     * Scoremove gives each possible move on the board a score
     * based on how likely they are to be good, prioritising advantageous
     * captures and promotions to speed up alpha beta pruning.
     * @param move
     * @param board
     * @return
     */
    private int scoreMove(Move move, Board board) {
        int score = 0;
        Piece actor = move.piece;
        Piece victim = move.getCapturePiece();
        if (victim != null) {
            score = (materialValues.get(victim.getType()) * 10) - materialValues.get(actor.getType());
            score += 10000;
        }
        if (move.isPromotion()) {
            score += 8000;
        }
        return score;
    }

    public void sortMoves(List<Move> moves, Board board) {
        moves.sort((a, b) -> Integer.compare(scoreMove(b, board), scoreMove(a, board)));
    }

}
