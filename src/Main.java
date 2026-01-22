import board.Game;

public class Main {


    // Note: There's a weird bug where the game throws a runtime exception halfway due
    // To a ghost king in the findKing method. 

    // Bug related to capturing an object under pin 
    public static void main(String[] args) {

        Game game = new Game();
        game.start();
    }
}
