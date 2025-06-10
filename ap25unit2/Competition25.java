import ap25.*;
import static ap25.Color.*;
import ap25.league.*;
import java.util.function.*;
import myplayer.MyPlayer.*;



class Competition25 {
  final static long TIME_LIMIT_SECONDS = 60;

  public static void main(String args[]) {
    Function<Color, Player[]> builder = (Color color) -> {
      return new Player[] {
          //new myplayer.MyPlayer(color),
          new p25x00.OurPlayer(color),
          // new enemy1.OurPlayer(color),
          // new enemy2.OurPlayer(color),
          // new enemy3.OurPlayer(color),
          // new enemy4.OurPlayer(color),
          // new enemy5.OurPlayer(color),
          // new enemy6.OurPlayer(color),
          // new enemy7.OurPlayer(color),
          // new enemy8.OurPlayer(color),
          // new enemy9.OurPlayer(color),
          // new enemy10.OurPlayer(color),
          // new enemy11.OurPlayer(color),
          new enemy12.OurPlayer(color),
          // new ap25.league.RandomPlayer(color),
          // new ap25.league.RandomPlayer(color),

      };
    };

    var league = new League(3, builder, TIME_LIMIT_SECONDS);
    league.run();
  }

  public static void singleGame(String args[]) {
    var player1 = new p25x00.OurPlayer(BLACK);
    var player2 = new p25x00.OurPlayer(WHITE);
    var board = new OfficialBoard();
    var game = new Game(board, player1, player2, TIME_LIMIT_SECONDS);
    game.play();
  }
}
