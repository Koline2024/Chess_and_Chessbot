package board;

import java.util.Hashtable;

public class TranspositionTable {
    public static final int exact = 0;
    public static final int upperBound = 1; // Beta cutoff
    public static final int lowerBound = 2; // Alpha cutoff
    private Hashtable<Integer, Entry> transpositionTable;

    public class Entry{
        long key;  // Key to Zobrist hash
        int score; // Score reached by position
        int depth; // Depth searched
        int flag; // Exact, alpha, beta
        Move bestMove;  // Best move found
    }

    private final Entry[] entries;
    private final int size;

    public TranspositionTable(int size){
        int entryCount = size;
        transpositionTable = new Hashtable<>(String)
    }
}
