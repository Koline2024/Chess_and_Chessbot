public class Coordinates{ 
  private int rank;
  private int file;

  public Coordinates(int rank, char file){
    this.rank = rank;
    this.file = file - 'a';
  }

  public int getRank(){
    return rank;
  }

  public int getFile(){
    return file;
  }

  public int getRow(){
    return 8 - rank;
  }

  public int getCol(){
    return file;
  }

  public void setRank(int newRank){
    rank = newRank;
  }

  public void setFile(int newFile){
    file = newFile; 
  }
} 