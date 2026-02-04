package board;

public class TranspositionTable {
    public static final int exact = 0;
    public static final int upperBound = 1; // Beta cutoff
    public static final int lowerBound = 2; // Alpha cutoff

    public class Entry{
        long key; 
        int score; // Score reached by position
        int depth; // Depth searched
        int flag; // Exact, alpha, beta
        Move bestMove;  // Best move found
    }

    private final Entry[] entries;
    private final int size;

    public TranspositionTable(int size){
        int entryCount = size * 1024 * 1024 / 32;
        this.size = entryCount;
        this.entries = new Entry[this.size];
    }

    public Entry get(long zobristHash){
        int index = Math.abs((int)(zobristHash % size));
        return entries[index];
    }

    public void store(long zobristHash, int depth, int score, int flag, Move bestMove) {
        int index = Math.abs((int) (zobristHash % size));
        
        // Replacement Scheme: Always replace if the new search is deeper
        // or if the existing entry is from a different position (collision)
        Entry e = entries[index];
        if (e == null) {
            e = new Entry();
            entries[index] = e;
        }

        // Don't overwrite a deep search with a shallow one unless it's a different position
        if (e.key == 0 || e.key != zobristHash || depth >= e.depth) {
            e.key = zobristHash;
            e.score = score;
            e.depth = depth;
            e.flag = flag;
            e.bestMove = bestMove;
        }
    }


    public void clear(){
        for(int i = 0; i < entries.length; i++){
            entries[i] = null;
        }
    }

    /**
     * Returns how filled the transposition table is (percentage).
     * @return
     */
    public double filled(){
        int count = 0;
        for (int i = 0; i < entries.length; i++){
            if (entries[i] != null){
                count ++;
            }
        }

        return (double) count / entries.length * 100;
    }
    


}
