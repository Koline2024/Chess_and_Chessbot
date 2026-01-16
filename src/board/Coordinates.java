package board;

public class Coordinates{ 

  // Chess variables
  private final int rank; // 1-8 ONLY
  private final char file; // a-h ONLY

  public Coordinates(int rank, char file){
    if(rank < 1 || rank > 8){
      throw new IllegalArgumentException("Rank out of bounds.");
    }else{
      this.rank = rank;
    }

    if(file < 'a' || file > 'h'){
      throw new IllegalArgumentException("File out of bounds.");
    }else{
      this.file = file;
    }

  }

  public int getRank(){
    return rank;
  }

  public char getFile(){
    return file;
  }

  // Array based indices here
  
  public int getRow(){
    return 8 - rank;
  }

  public int getCol(){
    return file - 'a';
  }

  public String toString(){
    return ""+file+rank;
  }


} 