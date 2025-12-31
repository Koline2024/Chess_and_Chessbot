public class Chessboard {

    public Chessboard() {
    }

    /**
     * This method prints a formatted terminal chessboard based on an input string
     * of the player's chosen side.
     * 
     * @param side
     */
    public void initialise(String side) {
        String[][] boardBlack = { { "Rw", "Nw", "Bw", "Kw", "Qw", "Bw", "Nw", "Rw" },
                { "Pw", "Pw", "Pw", "Pw", "Pw", "Pw", "Pw", "Pw" }, { "", "", "", "", "", "", "", "" },
                { "", "", "", "", "", "", "", "" }, { "", "", "", "", "", "", "", "" },
                { "", "", "", "", "", "", "", "" }, { "Pb", "Pb", "Pb", "Pb", "Pb", "Pb", "Pb", "Pb" },
                { "Rb", "Nb", "Bb", "Kb", "Qb", "Bb", "Nb", "Rb" } };
        String[][] boardWhite = { { "Rb", "Nb", "Bb", "Kb", "Qb", "Bb", "Nb", "Rb" },
                { "Pb", "Pb", "Pb", "Pb", "Pb", "Pb", "Pb", "Pb" }, { "", "", "", "", "", "", "", "" },
                { "", "", "", "", "", "", "", "" }, { "", "", "", "", "", "", "", "" },
                { "", "", "", "", "", "", "", "" }, { "Pw", "Pw", "Pw", "Pw", "Pw", "Pw", "Pw", "Pw" },
                { "Rw", "Nw", "Bw", "Kw", "Qw", "Bw", "Nw", "Rw" } };
        String[][] error = { {} };

        if (side.equals("White")) {
            printBoard(boardWhite, side);
        } else if (side.equals("Black")) {
            printBoard(boardBlack, side);
        } else {
            printBoard(error, side);
        }
    }

    private void printBoard(String[][] board, String side) {
        System.out.println("    +----+----+----+----+----+----+----+----+");
        if (side.equals("White")) {
            for (int i = 0; i < 8; i++) {
                int rank = 8 - i;
                System.out.printf(" %d  |", rank);
                for (int j = 0; j < 8; j++) {
                    String piece = board[i][j];
                    if (piece.equals("")) {
                        System.out.print("    |");
                    } else {
                        System.out.printf(" %s |", piece);
                    }
                }
                System.out.println("");
                System.out.println("    +----+----+----+----+----+----+----+----+");
            }

            System.out.print("      ");
            for (char file = 'a'; file <= 'h'; file++) {
                System.out.printf("%c    ", file);
            }
            System.out.println();
        } else {
            for (int i = 0; i < 8; i++) {
                int rank = i + 1;
                System.out.printf(" %d  |", rank);
                for (int j = 0; j < 8; j++) {
                    String piece = board[i][j];
                    if (piece.equals("")) {
                        System.out.print("    |");
                    } else {
                        System.out.printf(" %s |", piece);
                    }
                }
                System.out.println("");
                System.out.println("    +----+----+----+----+----+----+----+----+");
            }

            System.out.print("      ");
            for (char file = 'a'; file <= 'h'; file++) {
                System.out.printf("%c    ", file);
            }
            System.out.println();
        }
    }

}
