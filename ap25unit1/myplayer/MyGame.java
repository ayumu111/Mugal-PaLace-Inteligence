package myplayer;

import ap25.*;
import static ap25.Color.*;
import java.util.*;
import java.util.stream.*;

public class MyGame {
  public static void main(String args[]) {
    var player1 = new myplayer.MyPlayer(BLACK);
    // Randomのプレイヤー設定（白色）
    var player2 = new myplayer.RandomPlayer(WHITE);
    var board = new MyBoard();
    var game = new MyGame(board, player1, player2);
    game.play();
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

  public void play() {

    for(int i = 0;  i < 100; i++) { // 100回対戦
      
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
      if (board.winner() == BLACK) {
        WinBlack++; // 黒勝利
      } else if (board.winner() == WHITE) {
        WinWhite++; // 白勝利
      }
    }
    System.out.printf("Black: %d, White: %d\n", WinBlack, WinWhite); // 勝敗数表示
    float totalGames = WinBlack + WinWhite;
    if (totalGames > 0) {
      float blackWinRate = (WinBlack / totalGames) * 100;
      float whiteWinRate = (WinWhite / totalGames) * 100;
      System.out.printf("Black win rate: %.2f%%, White win rate: %.2f%%\n", blackWinRate, whiteWinRate);
    }
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
