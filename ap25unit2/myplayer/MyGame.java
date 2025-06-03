package myplayer;

import ap25.*;
import static ap25.Color.*;
import java.util.*;
import java.util.stream.*;

public class MyGame {
  public static void main(String args[]) {
    int MyPlayerWins = 0; // MyPlayerの勝利数
    int RandomPlayerWins = 0; // RandomPlayerの勝利数
    int[] results = new int[2]; // 勝利数を格納する配列 

    // 1回目（黒:MyPlayer, 白:RandomPlayer）
    var myPlayer1 = new myplayer.MyPlayer(BLACK);
    var randomPlayer1 = new myplayer.RandomPlayer(WHITE);
    var board1 = new MyBoard();
    var game1 = new MyGame(board1, myPlayer1, randomPlayer1);
    results = game1.play(1);//1回目の対戦
    MyPlayerWins = results[0]; // MyPlayerの勝利数を更新
    RandomPlayerWins = results[1]; // RandomPlayerの勝利数を更新

    // 2回目（黒:RandomPlayer, 白:MyPlayer）
    var randomPlayer2 = new myplayer.RandomPlayer(BLACK);
    var myPlayer2 = new myplayer.MyPlayer(WHITE);
    var board2 = new MyBoard();
    var game2 = new MyGame(board2, randomPlayer2, myPlayer2);
    results = game2.play(2);//2回目の対戦
    MyPlayerWins += results[0]; // MyPlayerの勝利数を更新
    RandomPlayerWins += results[1]; // RandomPlayerの勝利数を更新
    // 最終結果を表示
    System.out.printf("Final Results: MyPlayer Wins: %d, RandomPlayer Wins: %d%n", MyPlayerWins, RandomPlayerWins);
    //勝率
    float winRate = (float) MyPlayerWins / (MyPlayerWins + RandomPlayerWins) * 100;
    System.out.printf("Win Rate: %.2f%%\n", winRate); // 勝率を表示
}

  static final float TIME_LIMIT_SECONDS = 60; // 持ち時間（秒）

  Board board; // 現在の盤面
  Player black; // 黒プレイヤー
  Player white; // 白プレイヤー
  int WinBlack = 0; // 黒の勝利数
  int WinWhite = 0; // 白の勝利数
  Map<Color, Player> players; // 色ごとのプレイヤー
  List<Move> moves = new ArrayList<>(); // 手の履歴
  Map<Color, Float> times = new HashMap<>(Map.of(BLACK, 0f, WHITE, 0f)); // 色ごとの消費時間

  public MyGame(Board board, Player black, Player white) {
    this.board = board.clone(); // 盤面を複製
    this.black = black;
    this.white = white;
    this.players = Map.of(BLACK, black, WHITE, white); // プレイヤーマップ
  }

  public int[] play(int gameNumber) {
    int MyPlayerWins = 0; // MyPlayerの勝利数
    int RandomPlayerWins = 0; // RandomPlayerの勝利数

    for(int i = 0;  i < 10; i++) { // 100回対戦
      
      this.players.values().forEach(p -> p.setBoard(this.board.clone())); // 各プレイヤーに盤面をセット

      while (this.board.isEnd() == false) { // ゲーム終了までループ
        var turn = this.board.getTurn(); // 現在の手番
        var player = this.players.get(turn); // 手番のプレイヤー

        Error error = null;
        long t0 = System.currentTimeMillis(); // 開始時刻
        Move move;

        // プレイヤーに手を考えさせる
        try {
          move = player.think(board.clone()).colored(turn);
        } catch (Error e) {
          error = e;
          move = Move.ofError(turn);
        }

        // 時間計測
        long t1 = System.currentTimeMillis();
        final var t = (float) Math.max(t1 - t0, 1) / 1000.f;
        this.times.compute(turn, (k, v) -> v + t);

        // 手のチェック
        move = check(turn, move, error);
        moves.add(move);

        // 盤面更新
        if (move.isLegal()) {
          board = board.placed(move);
        } else {
          board.foul(turn); // 反則時
          break;
        }

        //System.out.println(board); // 盤面表示
      }

      //printResult(board, moves); // 結果表示
      if (board.winner() == BLACK && gameNumber == 1) {
        MyPlayerWins++; // MyPlayer勝利
      } else if (board.winner() == WHITE && gameNumber == 1) {
        RandomPlayerWins++; // RandomPlayer勝利
      } else if (board.winner() == BLACK && gameNumber == 2) {
        RandomPlayerWins++; // RandomPlayer勝利
      } else if (board.winner() == WHITE && gameNumber == 2) {
        MyPlayerWins++; // MyPlayer勝利
      }
      board = new MyBoard(); // 盤面をリセット
      moves.clear(); // 手の履歴をクリア
      this.times.put(BLACK, 0f); // 黒の時間をリセット
      this.times.put(WHITE, 0f); // 白の時間をリセット
    }

    int[] results = new int[2];
    results[0] = MyPlayerWins; // MyPlayerの勝利数
    results[1] = RandomPlayerWins; // RandomPlayerの勝利数
    System.out.printf("Game %d: MyPlayer Wins: %d, RandomPlayer Wins: %d%n", gameNumber, MyPlayerWins, RandomPlayerWins);
    return results; // 勝利数を返す
    
  }

  // 手の妥当性チェック
  Move check(Color turn, Move move, Error error) {
    if (move.isError()) {
      System.err.printf("error: %s %s", turn, error);
      System.err.println(board);
      return move;
    }

    if (this.times.get(turn) > TIME_LIMIT_SECONDS) {
      System.err.printf("timeout: %s %.2f", turn, this.times.get(turn));
      System.err.println(board);
      return Move.ofTimeout(turn);
    }

    var legals = board.findLegalMoves(turn);
    if (move == null || legals.contains(move) == false) {
      System.err.printf("illegal move: %s %s", turn, move);
      System.err.println(board);
      return Move.ofIllegal(turn);
    }

    return move;
  }

  // 勝者プレイヤーを取得
  public Player getWinner(Board board) {
    return this.players.get(board.winner());
  }

  // 結果を表示
  public void printResult(Board board, List<Move> moves) {
    var result = String.format("%5s%-9s", "", "draw"); // デフォルトは引き分け
    var score = Math.abs(board.score());
    if (score > 0)
      result = String.format("%-4s won by %-2d", getWinner(board), score); // 勝者とスコア

    var s = toString() + " -> " + result + "\t| " + toString(moves);
    System.out.println(s);
  }

  // 対戦カードの文字列
  public String toString() {
    return String.format("%4s vs %4s", this.black, this.white);
  }

  // 手の履歴を文字列化
  public static String toString(List<Move> moves) {
    return moves.stream().map(x -> x.toString()).collect(Collectors.joining());
  }
}
