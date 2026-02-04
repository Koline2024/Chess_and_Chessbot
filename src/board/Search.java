package board;

import enums.pieceColour;
import enums.pieceType;
import pieces.Piece;

import java.time.Duration;
import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

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
    public final TranspositionTable tTable = new TranspositionTable(1000); // I have 16 gb ram lol

    public Move findBestMove(Board board, int maxDepth, boolean isWhiteTurn) {
        long startTime = System.currentTimeMillis();
        Move overallBestMove = null;   
        Instant start = Instant.now();
        Duration limit = Duration.ofMillis(1000);
        // Iterative deepening
        for (int depth = 1; depth < maxDepth; depth++) {
            int alpha = -inf;
            int beta = inf;
            minimax(board, depth, alpha, beta, isWhiteTurn);
            TranspositionTable.Entry rootEntry = tTable.get(board.zobristHash);
            if (rootEntry != null && rootEntry.bestMove != null) {
                overallBestMove = rootEntry.bestMove;
            }
            Instant elapsed = Instant.now();
            if (Duration.between(start, elapsed).toMillis() >= 0.8*limit.toMillis()){
                break;
            }
        }
        long time = System.currentTimeMillis();
        System.out.println("Move " + overallBestMove + " found in " + (time - startTime) / 1000 + " seconds");
        return overallBestMove;
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
    private int minimax(Board board, int depth, int alpha, int beta, boolean isWhiteTurn) {
        int originalAlpha = alpha;
        TranspositionTable.Entry ttEntry = tTable.get(board.zobristHash); // Get entry for this board
        if (ttEntry != null && ttEntry.key == board.zobristHash && ttEntry.depth >= depth) {
            if (ttEntry.flag == TranspositionTable.exact) {
                return ttEntry.score;
            } else if (ttEntry.flag == TranspositionTable.lowerBound) {
                alpha = Math.max(alpha, ttEntry.score);
            } else if (ttEntry.flag == TranspositionTable.upperBound) {
                beta = Math.min(beta, ttEntry.score);
            }

            if (alpha >= beta) {
                return ttEntry.score;
            }
        }

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

        Move ttBestMove = (ttEntry != null) ? ttEntry.bestMove : null;
        if (ttBestMove != null) {
            // moves.remove(ttBestMove);
            // moves.add(0, ttBestMove); // Add the best TT move to the front
        }

        int bestScore = isWhiteTurn ? -inf : inf;
        Move bestMove = null;
        int extension = 0;
        for (Move m : moves) {
            try {
                board.doMove(m);
                // If a king is in check (interesting position), extend search by 1
                for (Piece p : board.getPieceList(turn)) {
                    if (p.getType() == pieceType.KING) {
                        if (board.isSquareAttacked(p.getCoordinates(), p.getColour())) {
                            extension = 1;
                        }
                    }
                }

                int score = minimax(board, depth - 1 + extension, alpha, beta, !isWhiteTurn);
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
                if (alpha >= beta) {
                    break; // Snip
                }
            } finally {
                board.undoMove();
            }
        }

        int flag;
        if (bestScore <= originalAlpha) {
            flag = TranspositionTable.upperBound;
        } else if (bestScore >= beta) {
            flag = TranspositionTable.lowerBound;
        } else {
            flag = TranspositionTable.exact;
        }
        tTable.store(board.zobristHash, depth - 1 + extension, bestScore, flag, bestMove);
        return bestScore;
    }

    /**
     * Quiescence search is applied after standard minimax to
     * avoid the horizon effect problem.
     * 
     * @param board
     * @param alpha
     * @param beta
     * @param isWhiteTurn
     * @return
     */
    private int quiescenceSearch(Board board, int alpha, int beta, boolean isWhiteTurn) {
        int pat = evaluator.evalAll(board, isWhiteTurn);
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
        moves.removeIf(m -> m.getCapturedPiece() == null); // Filter for only captures
        sortMoves(moves, board);
        for (Move m : moves) {
            board.doMove(m);
            try {
                int score = quiescenceSearch(board, alpha, beta, !isWhiteTurn);

                if (isWhiteTurn) {
                    if (score >= beta) {
                        return beta;
                    }
                    if (score > alpha) {
                        alpha = score;
                    }
                } else {
                    if (score <= alpha) {
                        return alpha;
                    }
                    if (score < beta) {
                        beta = score;
                    }
                }
            } finally {
                board.undoMove();
            }

        }
        return isWhiteTurn ? alpha : beta;

    }

    /**
     * Scoremove gives each possible move on the board a score
     * based on how likely they are to be good, prioritising advantageous
     * captures and promotions to speed up alpha beta pruning.
     * 
     * @param move
     * @param board
     * @return
     */
    private int scoreMove(Move move, Board board) {
        int score = 0;
        Piece actor = move.piece;
        Piece victim = move.getCapturedPiece(); // MVV LVA
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
