import Piece.pieceColour;

public class Board {

    private Piece[][] grid = new Piece[8][8];

    public Board() {
        initialise();
    }

    public void setPiece(Coordinates c, Piece p){
        grid[c.getRow()][c.getCol()] = p;
        if(p != null){
            p.setCoordinates(c);
        }
    }

    public Piece getPiece(Coordinates c){
        return grid[c.getRow()][c.getCol()];
    }

    private void initialise(){
        // Read this as a2 (file, rank)
        setPiece(new Coordinates(2, 'a'), new Pawn(pieceColour.WHITE, new Coordinates(2, 'a') ));
    }

}
