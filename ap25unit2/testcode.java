import ap25.*;
import myplayer.MyPlayer;
import p25x11my.OurPlayer;
import p25x11my.OurBoard;
import p25x11my.OurBoardFormatter;

public class testcode {
    public static void main(String[] args) {
        // 任意の局面を作成（例：初期局面）
        OurBoard testBoard = new OurBoard();
        testBoard.init();
        // 必要ならここで盤面を編集して特定局面を作る

        // MyPlayer
        MyPlayer myPlayer = new MyPlayer(Color.BLACK);
        myPlayer.setBoard(testBoard);
        System.out.println("=== MyPlayer ===");
        Move myMove = myPlayer.think(testBoard);

        // OurPlayer
        OurPlayer ourPlayer = new OurPlayer(Color.BLACK);
        ourPlayer.setBoard(testBoard);

        System.out.println("=== OurPlayer ===");
        Move ourMove = ourPlayer.think(testBoard);

    }
}