package board;

import enums.pieceColour;
import enums.pieceType;
import pieces.Piece;
import pieces.Queen;
import pieces.Rook;
import pieces.Bishop;
import pieces.King;
import pieces.Knight;
import pieces.Pawn;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class Board {

    public long zobristHash;
    public int whosInCheck = 0; // 0: No checks. 1: White in check. 2: Black in check.
    public Stack<Move> history = new Stack<>();
    private Move lastMove;
    private Piece[][] grid = new Piece[8][8];
    private ArrayList<Piece> whitePieces = new ArrayList<>();
    private ArrayList<Piece> blackPieces = new ArrayList<>();

    public Board() {
        //initialise("8/7P/8/3K4/8/1k6/p7/8 w - - 0 1");
        initialise(null);
        syncPieceLists();
    }

    public String getGameState(pieceColour colour) {
        // First, check if there are ANY legal moves
        for (int startRow = 0; startRow < 8; startRow++) {
            for (int startCol = 0; startCol < 8; startCol++) {
                Piece p = grid[startRow][startCol];

                if (p != null && p.getColour() == colour) {
                    for (int targetRow = 0; targetRow < 8; targetRow++) {
                        for (int targetCol = 0; targetCol < 8; targetCol++) {

                            // Optimization: Don't check moving onto your own pieces
                            Piece targetPiece = grid[targetRow][targetCol];
                            if (targetPiece != null && targetPiece.getColour() == colour) {
                                continue;
                            }
                            if (targetPiece != null && targetPiece.getType() == pieceType.KING) {
                                continue; // Don't allow capturing the king
                            }

                            Coordinates to = new Coordinates(8 - targetRow, (char) ('a' + targetCol));
                            Move testMove = new Move(p, p.getCoordinates(), to);

                            if (isMoveLegal(testMove)) {
                                return "PLAYING"; // Found a move
                            }
                        }
                    }
                }
            }
        }

        // No moves found? Check for check vs stalemate
        if (isSquareAttacked(findKing(colour), colour)) {
            return "CHECKMATE";
        } else {
            return "STALEMATE";
        }
    }

    public void doMove(Move move) {
        Piece p = move.piece;
        int colourIdx = (p.getColour() == pieceColour.WHITE) ? 0 : 1;
        int enemyIdx = 1 - colourIdx;

        // 1. XOR OUT OLD STATE
        zobristHash ^= Zobrist.turn;
        // Remove the OLD castling rights
        zobristHash ^= Zobrist.castlingRights[getCastlingMask()];

        // Remove OLD EP file (if one existed)
        if (!history.isEmpty()) {
            Move prev = history.peek();
            if (prev.piece.getType() == pieceType.PAWN && Math.abs(prev.from.getRow() - prev.to.getRow()) == 2) {
                zobristHash ^= Zobrist.passantFiles[prev.to.getCol()];
            }
        }

        // 2. MOVE PIECE
        zobristHash ^= Zobrist.pieces[colourIdx][p.getType().ordinal()][move.from.getIndex()];

        if (move.isCastling()) {
            handleCastling(move, colourIdx, true);
        } else if (move.isEnPassant()) {
            Piece victim = move.getCapturedPiece(); // Use the one pre-identified
            zobristHash ^= Zobrist.pieces[enemyIdx][pieceType.PAWN.ordinal()][victim.getCoordinates().getIndex()];
            grid[victim.getCoordinates().getRow()][victim.getCoordinates().getCol()] = null;
            removePieceFromSystem(victim);
        } else if (grid[move.to.getRow()][move.to.getCol()] != null) {
            Piece victim = grid[move.to.getRow()][move.to.getCol()];
            move.setCapturedPiece(victim);
            zobristHash ^= Zobrist.pieces[enemyIdx][victim.getType().ordinal()][move.to.getIndex()];
            removePieceFromSystem(victim);
        }

        grid[move.from.getRow()][move.from.getCol()] = null;
        setPiece(move.to, p);
        move.setPieceWasMovedBefore(p.hasMoved());
        p.setMoved(true);

        // 3. XOR IN NEW STATE
        //zobristHash ^= Zobrist.pieces[colourIdx][p.getType().ordinal()][move.to.getIndex()];

        // Handle Promotion Hash 
        // TODO: Comment back
        int promotionRow = (move.piece.getColour() == pieceColour.WHITE) ? 0 : 7;
        if (move.piece.getType() == pieceType.PAWN && move.getMoveTo().getRow() == promotionRow) {
            move.setPromotion(true);
            Queen q = new Queen(move.piece.getColour(), null);
            promote(move.getMoveTo(), q);
            int c = (move.piece.getColour() == pieceColour.WHITE) ? 0 : 1;
            //zobristHash ^= Zobrist.pieces[c][pieceType.PAWN.ordinal()][move.getMoveTo().getIndex()];
            zobristHash ^= Zobrist.pieces[c][q.getType().ordinal()][move.getMoveTo().getIndex()];

        }else{
            zobristHash ^= Zobrist.pieces[colourIdx][p.getType().ordinal()][move.to.getIndex()];
        }

        // NEW EP possibility?
        if (p.getType() == pieceType.PAWN && Math.abs(move.from.getRow() - move.to.getRow()) == 2) {
            zobristHash ^= Zobrist.passantFiles[move.to.getCol()];
        }

        // XOR IN the NEW castling rights
        zobristHash ^= Zobrist.castlingRights[getCastlingMask()];
        history.push(move);
    }

    public void undoMove() {
        if (history.isEmpty())
            return;

        Move last = history.pop();
        Piece p = last.piece;
        int colourIdx = (p.getColour() == pieceColour.WHITE) ? 0 : 1;
        int enemyIdx = 1 - colourIdx;

        // 1. XOR OUT CURRENT STATE
        // Toggle turn back to previous player
        zobristHash ^= Zobrist.turn;

        // Remove the current castling rights hash
        zobristHash ^= Zobrist.castlingRights[getCastlingMask()];

        // 2. REMOVE EP HASH CREATED BY THIS MOVE
        // If THIS move was a double push, it created an EP square. Remove it.
        if (p.getType() == pieceType.PAWN && Math.abs(last.from.getRow() - last.to.getRow()) == 2) {
            zobristHash ^= Zobrist.passantFiles[last.to.getCol()];
        }

        // 3. REVERSE PIECE MOVEMENT & HASHING
        if (last.wasPromotion()) {
            // Remove the promoted piece (e.g., Queen) from the 'to' square
            Piece promotedPiece = grid[last.to.getRow()][last.to.getCol()];
            zobristHash ^= Zobrist.pieces[colourIdx][promotedPiece.getType().ordinal()][last.to.getIndex()];
            removePieceFromSystem(promotedPiece);
            grid[last.to.getRow()][last.to.getCol()] = null;

            // Put the original Pawn back on the 'from' square
            setPiece(last.from, p);
            addPieceToSystem(p);
            zobristHash ^= Zobrist.pieces[colourIdx][pieceType.PAWN.ordinal()][last.from.getIndex()];
        } else {
            // Standard reverse: move piece from 'to' back to 'from'
            zobristHash ^= Zobrist.pieces[colourIdx][p.getType().ordinal()][last.to.getIndex()];
            grid[last.to.getRow()][last.to.getCol()] = null;

            setPiece(last.from, p);
            zobristHash ^= Zobrist.pieces[colourIdx][p.getType().ordinal()][last.from.getIndex()];
        }

        // Restore moved status
        p.setMoved(last.getPieceWasMovedBefore());

        // 4. RESTORE CAPTURED PIECES
        Piece victim = last.getCapturedPiece();
        if (victim != null) {
            addPieceToSystem(victim);
            if (last.isEnPassant()) {
                // Restore En Passant victim to their specific row
                grid[victim.getCoordinates().getRow()][victim.getCoordinates().getCol()] = victim;
                zobristHash ^= Zobrist.pieces[enemyIdx][pieceType.PAWN.ordinal()][victim.getCoordinates().getIndex()];
            } else {
                // Standard capture: put victim back on the 'to' square
                grid[last.to.getRow()][last.to.getCol()] = victim;
                zobristHash ^= Zobrist.pieces[enemyIdx][victim.getType().ordinal()][last.to.getIndex()];
            }
        }

        // 5. RESTORE CASTLING ROOKS
        if (last.isCastling()) {
            handleCastling(last, colourIdx, false); // false = undoing
        }

        // 6. RESTORE PREVIOUS EP HASH
        // If the move BEFORE this one was a double pawn push, we need to put that EP
        // square back in the hash
        if (!history.isEmpty()) {
            Move prev = history.peek();
            if (prev.piece.getType() == pieceType.PAWN && Math.abs(prev.from.getRow() - prev.to.getRow()) == 2) {
                zobristHash ^= Zobrist.passantFiles[prev.to.getCol()];
            }
        }

        // 7. XOR IN THE RESTORED CASTLING RIGHTS
        zobristHash ^= Zobrist.castlingRights[getCastlingMask()];
    }

    /**
     * Handles the Rook's movement and Zobrist hashing during castling.
     * 
     * @param move      The castling move
     * @param colourIdx 0 for White, 1 for Black
     * @param isDoing   True if executing doMove, False if executing undoMove
     */
    private void handleCastling(Move move, int colourIdx, boolean isDoing) {
        int row = move.from.getRow();
        boolean isKingside = (move.to.getCol() == 6);

        // Rook positions:
        // Kingside: starts at col 7, ends at col 5
        // Queenside: starts at col 0, ends at col 3
        int rookStartCol = isKingside ? 7 : 0;
        int rookEndCol = isKingside ? 5 : 3;

        // During doMove: move from start to end. During undoMove: move from end to
        // start.
        int fromCol = isDoing ? rookStartCol : rookEndCol;
        int toCol = isDoing ? rookEndCol : rookStartCol;

        Piece rook = grid[row][fromCol];

        // Safety check - if the rook is missing, your move generator is broken!
        if (rook == null || rook.getType() != pieceType.ROOK)
            return;

        // 1. Hash out the Rook from its current position
        zobristHash ^= Zobrist.pieces[colourIdx][pieceType.ROOK.ordinal()][rook.getCoordinates().getIndex()];

        // 2. Physically move the Rook
        grid[row][fromCol] = null;
        // We reuse the move's rank to calculate the new coordinate
        Coordinates newRookCoords = new Coordinates(move.from.getRank(), (char) ('a' + toCol));
        setPiece(newRookCoords, rook);

        // 3. Update Rook state (moved status)
        rook.setMoved(isDoing);

        // 4. Hash the Rook into its new position
        zobristHash ^= Zobrist.pieces[colourIdx][pieceType.ROOK.ordinal()][newRookCoords.getIndex()];
    }

    private void setPiece(Coordinates c, Piece p) {
        grid[c.getRow()][c.getCol()] = p;
        if (p != null) {
            p.setCoordinates(c);
        }
    }

    public Piece getPiece(Coordinates c) {
        return grid[c.getRow()][c.getCol()];
    }

    /**
     * Row, Col
     * 
     * @param x
     * @param y
     * @return
     */
    public Piece getPieceAt(int x, int y) {
        // Assuming these are proper array indices
        return grid[x][y];
    }

    public boolean isMoveLegal(Move move) {
        // Before normal operation: Check if is castling
        if (move.isCastling()) {
            int row = move.getMoveFrom().getRow();
            int startCol = 4; // King is always on e
            int endCol = move.getMoveTo().getCol();
            King k = (King) move.piece; // Casting is lazy but fine because king is ensured prior
            if (k.hasMoved() == true) {
                return false;
            }
            // Check the rook
            int rookCol = (endCol == 6) ? 7 : 0;

            // Guard against invalid indices to avoid ArrayIndexOutOfBoundsException
            if (row < 0 || row > 7 || rookCol < 0 || rookCol > 7) {
                return false;
            }

            // Guard clause
            if (grid[row][rookCol] != null) {
                if (grid[row][rookCol].getType() != pieceType.ROOK) {
                    return false;
                }
            }
            Rook rook = (Rook) grid[row][rookCol]; // Again casting is fine since rook is ensured
            if (rook == null || rook.getColour() != k.getColour() || rook.hasMoved()) {
                return false;
            }

            int step = (endCol > startCol) ? 1 : -1;
            for (int c = startCol + step; c != rookCol; c += step) {
                if (c < 0 || c > 7) {
                    return false;
                }
                if (grid[row][c] != null) {
                    return false; // Square is blocked
                }
            }

            // Square 1: Where the king starts
            if (isSquareAttacked(new Coordinates(move.getMoveFrom().getRank(), 'e'), move.piece.getColour()))
                return false;

            // Square 2: The square the king passes through
            char middleFile = (endCol == 6) ? 'f' : 'd';
            if (isSquareAttacked(new Coordinates(move.getMoveFrom().getRank(), middleFile), move.piece.getColour()))
                return false;
        } else if (!move.piece.isValidMove(move.getMoveTo(), this)) {
            return false;
        }

        // King safety
        if (!isMoveSafe(move)) {
            return false;
        }

        return true;
    }

    public void clearBoard(){
        if (!whitePieces.isEmpty()){
            whitePieces.clear();
        }
        if (!blackPieces.isEmpty()){
            blackPieces.clear();
        }
        if (!history.isEmpty()){
            history.clear();
        }

        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                grid[row][col] = null;
            }
        }
    }

    public void initialise(String fen) {
        // Fully clear board
        clearBoard();
       
        // Default starting position if FEN is empty
        if (fen == null || fen.isEmpty()) {
            fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
        }

        String[] parts = fen.split(" ");
        String boardPart = parts[0]; // The piece placement section

        String[] ranks = boardPart.split("/");
        int currentRank = 8; // FEN starts at Rank 8

        for (String rankContent : ranks) {
            int fileIndex = 0; // 0 = 'a', 1 = 'b', etc.

            for (int i = 0; i < rankContent.length(); i++) {
                char c = rankContent.charAt(i);

                if (Character.isDigit(c)) {
                    // If it's a number, skip those squares
                    fileIndex += Character.getNumericValue(c);
                } else {
                    // If it's a letter, it's a piece
                    char file = (char) ('a' + fileIndex);
                    Coordinates coords = new Coordinates(currentRank, file);

                    pieceColour color = Character.isUpperCase(c) ? pieceColour.WHITE : pieceColour.BLACK;
                    Piece piece = createPieceFromChar(c, color, coords);

                    setPiece(coords, piece);
                    fileIndex++;
                }
            }
            currentRank--;
        }

        // Update Game State (Whose turn is it?)
        if (parts.length > 1) {
            //this.isWhiteTurn = parts[1].equals("w");
        }
    }

    // Helper to clean up the logic
    private Piece createPieceFromChar(char c, pieceColour color, Coordinates coords) {
        char type = Character.toLowerCase(c);
        return switch (type) {
            case 'p' -> new Pawn(color, coords);
            case 'n' -> new Knight(color, coords);
            case 'b' -> new Bishop(color, coords);
            case 'r' -> new Rook(color, coords);
            case 'q' -> new Queen(color, coords);
            case 'k' -> new King(color, coords);
            default -> throw new IllegalArgumentException("Unknown FEN piece: " + c);
        };
    }

    private boolean isMoveSafe(Move move) {
        doMove(move);
        // Find where the king is
        Coordinates kingPos = findKing(move.piece.getColour());
        boolean safe = !isSquareAttacked(kingPos, move.piece.getColour());
        undoMove();
        return safe;
    }

    /**
     * Takes as parameters the coordinates of the square and the colour of
     * ALLIED pieces. Internally computes arraylist of enemy pieces.
     * 
     * @param coords
     * @param colour
     * @return
     */
    public boolean isSquareAttacked(Coordinates coords, pieceColour colour) {
        ArrayList<Piece> enemyPieces = (colour == pieceColour.WHITE) ? blackPieces : whitePieces;
        for (Piece p : enemyPieces) {
            if (p.getType() == pieceType.PAWN) {
                if (((Pawn) p).canAttack(coords, this)) {
                    return true;
                }
            } else {
                if (p.isValidMove(coords, this)) {
                    return true;
                }
            }
        }

        return false;
    }

    public Coordinates findKing(pieceColour colour) throws RuntimeException {
        ArrayList<Piece> pieceList = (colour == pieceColour.WHITE) ? whitePieces : blackPieces;

        for (Piece p : pieceList) {
            if (p.getType() == pieceType.KING) {
                return p.getCoordinates();
            }
        }
        // If we reach here, something went wrong. Sync and retry.
        syncPieceLists();
        pieceList = (colour == pieceColour.WHITE) ? whitePieces : blackPieces; // Re fetch lists
        for (Piece p : pieceList) {
            if (p.getType() == pieceType.KING) {
                return p.getCoordinates();
            }
        }
        throw new RuntimeException("Even after a sync the " + colour + " king could not be found!");
    }

    public void printBoard(pieceColour side) {

        String top = "  ╔═══╤═══╤═══╤═══╤═══╤═══╤═══╤═══╗";
        String middle = "  ╟───┼───┼───┼───┼───┼───┼───┼───╢";
        String bottom = "  ╚═══╧═══╧═══╧═══╧═══╧═══╧═══╧═══╝";
        String labels = "    a   b   c   d   e   f   g   h";
        String labelsBlack = "    h   g   f   e   d   c   b   a";

        if (side == pieceColour.WHITE) {
            System.out.println(top);
            for (int r = 0; r < 8; r++) {
                System.out.print((8 - r) + " ║"); // Rank number
                for (int c = 0; c < 8; c++) {
                    Piece p = grid[r][c];
                    String symbol = (p == null) ? " " : getUnicodeSymbol(p);
                    System.out.print(" " + symbol + " │");
                }
                // Replace last │ with ║
                System.out.print("\b║\n");
                if (r < 7) {
                    System.out.println(middle);
                }
            }
            System.out.println(bottom);
            System.out.println(labels);
        } else {
            System.out.println(top);
            // Invert boardstate
            for (int r = 0; r < 8; r++) {
                System.out.print((1 + r) + " ║");
                for (int c = 0; c < 8; c++) {
                    Piece p = grid[7 - r][7 - c];
                    String symbol = (p == null) ? " " : getUnicodeSymbol(p);
                    System.out.print(" " + symbol + " │");
                }
                System.out.print("\b║\n");
                if (r < 7) {
                    System.out.println(middle);
                }
            }
            System.out.println(bottom);
            System.out.println(labelsBlack);
        }
    }

    /**
     * Not actually unicode
     * 
     * @param p
     * @return
     */
    private String getUnicodeSymbol(Piece p) {
        String type = "";
        switch (p.getType()) {
            case PAWN:
                type = "P";
                break;
            case ROOK:
                type = "R";
                break;
            case KNIGHT:
                type = "N";
                break;
            case BISHOP:
                type = "B";
                break;
            case QUEEN:
                type = "Q";
                break;
            case KING:
                type = "K";
                break;
        }

        // White = Cyan, Black = Red (Better contrast than White/Black)
        if (p.getColour() == pieceColour.WHITE) {
            return "\u001b[36;1m" + type + "\u001b[0m";
        } else {
            return "\u001b[31;1m" + type + "\u001b[0m";
        }
    }

    public void promote(Coordinates coords, Piece toPiece) {
        Piece oldPawn = getPiece(coords);
        removePieceFromSystem(oldPawn); // Take the pawn out of the list

        setPiece(coords, toPiece); // Put queen on grid
        addPieceToSystem(toPiece); // Put queen in the list;
    }

    public List<Piece> getPieceList(pieceColour colour) {
        return (colour == pieceColour.WHITE) ? whitePieces : blackPieces;
    }

    public void syncPieceLists() {
        whitePieces.clear();
        blackPieces.clear();
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                Piece p = grid[i][j];
                if (p != null) {
                    addPieceToSystem(p);
                }
            }
        }
    }

    private void addPieceToSystem(Piece p) {
        if (p == null) {
            return;
        }
        // Avoid adding duplicates to the internal piece lists
        java.util.List<Piece> list = getPieceList(p.getColour());
            list.add(p);
        
    }

    private void removePieceFromSystem(Piece p) {
        if (p == null) {
            return;
        }
        // Only remove from the internal piece list; do not mutate the grid here.
        // Grid updates should be handled by move/undo logic to avoid accidental
        // disappearance/duplication.
        java.util.List<Piece> list = getPieceList(p.getColour());
        list.remove(p);
    }

    public List<Move> getLegalMoves(pieceColour colour) {
        // TODO: use magic bitboards
        List<Move> legalMoves = new ArrayList<>();
        List<Piece> pieceList = (colour == pieceColour.WHITE) ? whitePieces : blackPieces;
        for (Piece p : new ArrayList<>(pieceList)) {
            for (int tRow = 0; tRow < 8; tRow++) {
                for (int tCol = 0; tCol < 8; tCol++) {
                    // Skip own pieces
                    Piece target = grid[tRow][tCol];
                    if (target != null) {
                        if (target.getColour() == colour || target.getType() == pieceType.KING) {
                            continue;
                        }
                    }
                    Coordinates to = new Coordinates(8 - tRow, (char) ('a' + tCol));
                    // See if move is valid first before expensive check
                    if (p.isValidMove(to, this)) {
                        Move move = new Move(p, p.getCoordinates(), to);
                        if (isMoveLegal(move)) { // Expensive check
                            legalMoves.add(move);
                        }
                    }
                }
            }
        }
        addEnPassantMoves(legalMoves, colour); // This broken fucking code doesn't work
        addCastlingMoves(legalMoves, colour);
        return legalMoves;
    }

    /**
     * Helper method to help the AI recognise it can castle
     * 
     * @param moves
     * @param colour
     */
    private void addCastlingMoves(List<Move> moves, pieceColour colour) {
        // White rank is 1, Black rank is 8
        int rank = (colour == pieceColour.WHITE) ? 1 : 8;
        int row = (colour == pieceColour.WHITE) ? 7 : 0; // For grid access

        Coordinates kingPos = new Coordinates(rank, 'e');
        Piece king = grid[row][4]; // 'e' is index 4

        if (king == null || king.getType() != pieceType.KING || king.getColour() != colour || king.hasMoved()) {
            return;
        }

        if (isSquareAttacked(kingPos, colour)) {
            return;
        }

        // --- KINGSIDE CASTLING ---
        Piece kRook = grid[row][7]; // 'h' is index 7
        if (kRook != null && kRook.getType() == pieceType.ROOK && !kRook.hasMoved()) {
            // Path empty: f (5) and g (6)
            if (grid[row][5] == null && grid[row][6] == null) {
                Coordinates fSq = new Coordinates(rank, 'f');
                Coordinates gSq = new Coordinates(rank, 'g');

                if (!isSquareAttacked(fSq, colour) && !isSquareAttacked(gSq, colour)) {
                    Move m = new Move(king, kingPos, gSq);
                    m.setIsCastling(true);
                    moves.add(m);
                }
            }
        }

        // --- QUEENSIDE CASTLING ---
        Piece qRook = grid[row][0]; // 'a' is index 0
        if (qRook != null && qRook.getType() == pieceType.ROOK && !qRook.hasMoved()) {
            // Path empty: b (1), c (2), d (3)
            if (grid[row][1] == null && grid[row][2] == null && grid[row][3] == null) {
                Coordinates cSq = new Coordinates(rank, 'c');
                Coordinates dSq = new Coordinates(rank, 'd');

                if (!isSquareAttacked(cSq, colour) && !isSquareAttacked(dSq, colour)) {
                    Move m = new Move(king, kingPos, cSq);
                    m.setIsCastling(true);
                    moves.add(m);
                }
            }
        }
    }

    private void addEnPassantMoves(List<Move> moves, pieceColour colour) {
        if (history.isEmpty()) {
            return;
        }
        Move lastMove = history.peek();

        // STRICT CHECK: The last piece moved MUST be a Pawn
        if (lastMove.piece.getType() != pieceType.PAWN) {
            return;
        }

        // STRICT CHECK: The last move MUST be a double push
        if (Math.abs(lastMove.from.getRow() - lastMove.to.getRow()) != 2) {
            return;
        }

        int passingRow = lastMove.to.getRow();
        int passingCol = lastMove.to.getCol();

        // Determine where our pawns would be (left and right of the enemy pawn)
        int[] offsets = { -1, 1 };

        for (int offset : offsets) {
            int myCol = passingCol + offset;

            // Bounds check
            if (myCol >= 0 && myCol < 8) {
                Piece myPawn = grid[passingRow][myCol];

                // Check if a piece exists, is a PAWN, and is OUR colour
                if (myPawn != null &&
                        myPawn.getType() == pieceType.PAWN &&
                        myPawn.getColour() == colour) {

                    // Determine target square (The empty square BEHIND the victim)
                    // White moves UP (row index decreases), Black moves DOWN (row index increases)
                    int targetRow = (colour == pieceColour.WHITE) ? passingRow - 1 : passingRow + 1;

                    Coordinates target = new Coordinates(8 - targetRow, (char) ('a' + passingCol));

                    // Create the move
                    Move epMove = new Move(myPawn, myPawn.getCoordinates(), target);
                    epMove.setIsEnPassant(true);

                    // PRE-CALCULATE CAPTURE to avoid doMove side-effect reliance logic quirks
                    // The piece being captured is the one that just moved (lastMove.piece)
                    // We must ensure this is actually the piece at grid[passingRow][passingCol]
                    Piece victim = grid[passingRow][passingCol];

                    // Sanity check: The victim must exist and be the enemy colour
                    if (victim != null && victim.getColour() != colour && victim == lastMove.piece) {
                        epMove.setCapturedPiece(victim);
                        // We safely simulate the move
                        if (isMoveSafe(epMove)) {
                            moves.add(epMove);
                        }
                    }
                }
            }
        }
    }

    /**
     * Gets last move played on the board
     * 
     * @return
     */
    public Move getLastMove() {
        if (history.isEmpty()) {
            return null;
        }
        // Stack#peek returns the top element without removing it
        return history.peek();
    }

    // Disclaimer: I have no clue how any of this works.
    public void initZobrist(boolean isWhiteTurn) {
        zobristHash = 0;
        for (Piece p : getPieceList(pieceColour.WHITE)) {
            int type = p.getType().ordinal(); // map to 0-5
            zobristHash ^= Zobrist.pieces[0][type][p.getCoordinates().getIndex()];
        }
        for (Piece p : getPieceList(pieceColour.BLACK)) {
            int type = p.getType().ordinal(); // map to 0-5
            zobristHash ^= Zobrist.pieces[1][type][p.getCoordinates().getIndex()];
        }
        if (!isWhiteTurn) {
            zobristHash ^= Zobrist.turn;
        }
        if (lastMove != null && lastMove.piece.getType() == pieceType.PAWN) {
            if (Math.abs(lastMove.to.getRow() - lastMove.from.getRow()) == 2) {
                int file = lastMove.to.getCol(); // 0-7
                // Use the same file index convention as doMove and guard against OOB
                if (file >= 0 && file < Zobrist.passantFiles.length) {
                    zobristHash ^= Zobrist.passantFiles[file];
                }
            }
        }
        zobristHash ^= Zobrist.castlingRights[getCastlingMask()];
    }

    // Nor do I know how this works.
    public int getCastlingMask() {
        int mask = 0;

        // --- WHITE RIGHTS ---
        Piece whiteKing = grid[7][4]; // e1
        if (whiteKing != null && whiteKing.getType() == pieceType.KING && !whiteKing.hasMoved()) {
            // Check Kingside Rook (h1)
            Piece wkRook = grid[7][7];
            if (wkRook != null && wkRook.getType() == pieceType.ROOK && !wkRook.hasMoved()) {
                mask |= 1;
            }
            // Check Queenside Rook (a1)
            Piece wqRook = grid[7][0];
            if (wqRook != null && wqRook.getType() == pieceType.ROOK && !wqRook.hasMoved()) {
                mask |= 2;
            }
        }

        // --- BLACK RIGHTS ---
        Piece blackKing = grid[0][4]; // e8
        if (blackKing != null && blackKing.getType() == pieceType.KING && !blackKing.hasMoved()) {
            // Check Kingside Rook (h8)
            Piece bkRook = grid[0][7];
            if (bkRook != null && bkRook.getType() == pieceType.ROOK && !bkRook.hasMoved()) {
                mask |= 4;
            }
            // Check Queenside Rook (a8)
            Piece bqRook = grid[0][0];
            if (bqRook != null && bqRook.getType() == pieceType.ROOK && !bqRook.hasMoved()) {
                mask |= 8;
            }
        }

        return mask;
    }

    public void historyToPGN() {
        int counter = 1;
        for (int i = 0; i < history.size(); i+=2) {
            Move whiteMove = history.get(i);
            Move blackMove = history.get(i + 1);
            System.out
                    .printf((counter) + ". " + whiteMove.piece.getSymbol() + whiteMove.to.getFile() + whiteMove.to.getRank());
            System.out.printf(" " + blackMove.piece.getSymbol() + blackMove.to.getFile() + blackMove.to.getRank()+" ");
            counter ++;
        }
        System.out.println("");
    }
}
