package board;

import java.security.SecureRandom;

public class Zobrist {
    public static final long[][][] pieces = new long[2][6][64];
    public static final long[] castlingRights = new long[16];
    public static final long[] passantFiles = new long[9];
    public static final long turn;

    static{
        SecureRandom random = new SecureRandom();
        for(int c = 0; c < 2; c++){
            for(int t = 0; t < 6; t++){
                for(int s = 0; s < 64; s++){
                    pieces[c][t][s] = random.nextLong();
                }
            }
        }

        for(int i = 0; i < castlingRights.length; i++){
            castlingRights[i] = random.nextLong();
        }

        for(int i = 0; i < passantFiles.length; i++){
            passantFiles[i] = random.nextLong();
        }

        turn = random.nextLong();
    }


}
