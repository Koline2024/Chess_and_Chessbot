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
    private Stack<Move> history = new Stack<>();
    private Move lastMove;
    private Piece[][] grid = new Piece[8][8];
    private ArrayList<Piece> whitePieces = new ArrayList<>();
    private ArrayList<Piece> blackPieces = new ArrayList<>();

    public Board() {
        initialise("");
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
        Coordinates from = move.getMoveFrom();
        Coordinates to = move.getMoveTo();
        int colourIdx = (p.getColour() == pieceColour.WHITE) ? 0 : 1;

        zobristHash ^= Zobrist.turn;
        zobristHash ^= Zobrist.castlingRights[getCastlingMask()];
        if (move.isEnPassant()) {
            zobristHash ^= Zobrist.passantFiles[p.getCoordinates().getCol()];
        }

        Piece captured = null;
        if (move.isEnPassant()) {
            int offset = (p.getColour() == pieceColour.WHITE) ? 1 : -1;
            captured = grid[to.getRow() + offset][to.getCol()];
            // Manually remove since it's not on the 'to' square
            if (captured != null) {
                removePieceFromSystem(captured);
                zobristHash ^= Zobrist.pieces[1 - colourIdx][pieceType.PAWN.ordinal()][captured.getCoordinates()
                        .getIndex()];
            } else {
                captured = grid[to.getRow()][to.getCol()];
                if (captured != null) {
                    zobristHash ^= Zobrist.pieces[1 - colourIdx][captured.getType().ordinal()][to.getIndex()];
                    removePieceFromSystem(captured);
                }
            }
        }
            move.setCapturedPiece(captured);
            zobristHash ^= Zobrist.pieces[colourIdx][p.getType().ordinal()][from.getIndex()]; // XOR Out
            grid[from.getRow()][from.getCol()] = null;
            setPiece(to, p); // This puts it in the grid and updates coordinates
            zobristHash ^= Zobrist.pieces[colourIdx][p.getType().ordinal()][to.getIndex()]; // XOR In

            if (move.isCastling()) {
                int row = from.getRow();
                int rookStartCol = (to.getCol() == 6) ? 7 : 0;
                int rookEndCol = (to.getCol() == 6) ? 5 : 3;
                Piece rook = grid[row][rookStartCol];
                grid[row][rookStartCol] = null;
                setPiece(new Coordinates(8 - row, (char) ('a' + rookEndCol)), rook);
                rook.setMoved(true);
                // XOR the rook moves here too!
            }

            move.setPieceWasMovedBefore(p.hasMoved());
            p.setMoved(true);

            // Final Zobrist Update
            zobristHash ^= Zobrist.castlingRights[getCastlingMask()];
            history.push(move);
            this.lastMove = move;
        }
    

    private void setPiece(Coordinates c, Piece p) {
        if(p==null){
            grid[c.getRow()][c.getCol()] = null;
            return;
        }

        // If there was a piece there, it's being replaced/captured
        Piece oldPiece = grid[c.getRow()][c.getCol()];
        if (oldPiece != null) {
            getPieceList(oldPiece.getColour()).remove(oldPiece);
        }

        grid[c.getRow()][c.getCol()] = p;

        if (p != null) {
            p.setCoordinates(c);
            // Only add if it's not already in the list (prevents duplicates)
            List<Piece> list = getPieceList(p.getColour());
            if (!list.contains(p)) {
                list.add(p);
            }
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

    public void initialise(String fen) {

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
            // this.isWhiteTurn = parts[1].equals("w");
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
        System.out.println("Pawn promoted to " + toPiece.getSymbol());
    }

    public void undoMove() {
    if (history.isEmpty()) return;

    Move last = history.pop();
    Piece p = last.piece;
    int colourIdx = (p.getColour() == pieceColour.WHITE) ? 0 : 1;

    // 1. Remove CURRENT state from Zobrist (Turn and Castling)
    zobristHash ^= Zobrist.turn;
    zobristHash ^= Zobrist.castlingRights[getCastlingMask()];

    // 2. Move the main piece back
    // XOR out of the 'to' square
    zobristHash ^= Zobrist.pieces[colourIdx][p.getType().ordinal()][last.getMoveTo().getIndex()];
    grid[last.getMoveTo().getRow()][last.getMoveTo().getCol()] = null;

    // Restore to 'from' square (setPiece updates internal coords and PieceLists)
    setPiece(last.getMoveFrom(), p);
    p.setMoved(last.getPieceWasMovedBefore());
    
    // XOR into the 'from' square
    zobristHash ^= Zobrist.pieces[colourIdx][p.getType().ordinal()][last.getMoveFrom().getIndex()];

    // 3. Restore Captured Piece
    Piece captured = last.getCapturePiece();
    if (captured != null) {
        // IMPORTANT: For En Passant, the captured pawn is NOT at last.getMoveTo()
        // We use its own stored coordinates to put it back exactly where it died.
        setPiece(captured.getCoordinates(), captured);
        
        // XOR the captured piece back in
        int enemyIdx = 1 - colourIdx;
        zobristHash ^= Zobrist.pieces[enemyIdx][captured.getType().ordinal()][captured.getCoordinates().getIndex()];
    }

    // 4. Reverse Castling (Move the Rook back)
    if (last.isCastling()) {
        int row = last.getMoveFrom().getRow();
        int rookStartCol = (last.getMoveTo().getCol() == 6) ? 7 : 0; // Where it started (h or a)
        int rookEndCol = (last.getMoveTo().getCol() == 6) ? 5 : 3;   // Where it ended (f or d)
        
        Piece rook = grid[row][rookEndCol];
        if (rook != null) {
            // XOR Rook out of its castled position
            zobristHash ^= Zobrist.pieces[colourIdx][pieceType.ROOK.ordinal()][rook.getCoordinates().getIndex()];
            grid[row][rookEndCol] = null;
            
            // Put Rook back in its corner
            Coordinates rookHome = new Coordinates(8 - row, (char) ('a' + rookStartCol));
            setPiece(rookHome, rook);
            rook.setMoved(false);
            
            // XOR Rook back into its home position
            zobristHash ^= Zobrist.pieces[colourIdx][pieceType.ROOK.ordinal()][rookHome.getIndex()];
        }
    }

    // 5. Apply the NEW (restored) Castling Rights to Zobrist
    zobristHash ^= Zobrist.castlingRights[getCastlingMask()];
    
    // Update the lastMove reference for UI/Zobrist tracking
    this.lastMove = history.isEmpty() ? null : history.peek();
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
        getPieceList(p.getColour()).add(p);
    }

    private void removePieceFromSystem(Piece p) {
        if (p == null) {
            return;
        }
        grid[p.getCoordinates().getRow()][p.getCoordinates().getCol()] = null;
        getPieceList(p.getColour()).remove(p);
    }

    public List<Move> getLegalMoves(pieceColour colour) {
        // TODO: use magic bitboards
        List<Move> legalMoves = new ArrayList<>();
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                Piece p = grid[row][col];
                // If piece belongs to the active player
                if (p != null && p.getColour() == colour) {
                    // Check all squares for this piece
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
            }
        }
        addCastlingMoves(legalMoves, colour);
        addEnPassantMoves(legalMoves, colour);
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
        if (history.isEmpty())
            return;
        Move lastMove = history.peek(); // Look at previous move

        // 1. Check if last move was a double pawn jump
        if (lastMove.piece.getType() == pieceType.PAWN &&
                Math.abs(lastMove.from.getRow() - lastMove.to.getRow()) == 2) {

            int passingRow = lastMove.to.getRow();
            int passingCol = lastMove.to.getCol();

            // 2. Check left and right of that pawn for OUR pawns
            // offsets: -1 (left), +1 (right)
            int[] offsets = { -1, 1 };
            for (int offset : offsets) {
                int myCol = passingCol + offset;
                if (myCol >= 0 && myCol <= 7) {
                    Piece myPawn = grid[passingRow][myCol];
                    if (myPawn != null && myPawn.getType() == pieceType.PAWN && myPawn.getColour() == colour) {

                        // 3. Target square is "behind" the enemy pawn
                        int targetRow = (colour == pieceColour.WHITE) ? passingRow - 1 : passingRow + 1; // -1 for white
                                                                                                         // (up array),
                                                                                                         // +1 for black
                        Coordinates target = new Coordinates(8 - targetRow, (char) ('a' + passingCol)); // Convert to
                                                                                                        // your Coords

                        Move epMove = new Move(myPawn, myPawn.getCoordinates(), target);
                        epMove.setIsEnPassant(true);
                        // Note: Capture piece is NULL here because the square we land on is empty!
                        // We handle the capture logic in doMove

                        // 4. Safety Check (En Passant can rarely reveal a check on your King)
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
        return history.getLast();
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
                zobristHash ^= Zobrist.passantFiles[file + 1];
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
}
