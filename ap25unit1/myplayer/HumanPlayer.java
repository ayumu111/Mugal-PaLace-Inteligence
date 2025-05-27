package myplayer;

import static ap25.Board.*;
import static ap25.Color.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import java.util.stream.IntStream;

import ap25.*;

public class HumanPlayer extends ap25.Player {
  static final String MY_NAME = "human24";
  Move move;            // 選んだ手
  MyBoard board;        // 内部的に使うボード状態（MyBoard型）

  public HumanPlayer(Color color) {
    this(MY_NAME, color);
  }

  public HumanPlayer(String name, Color color) {
    super(name, color);
    this.board = new MyBoard();
  }

  // Boardの状態をMyBoardに代入
  public void setBoard(Board board) {
    for (var i = 0; i < LENGTH; i++) {
      this.board.set(i, board.get(i));
    }
  }

  // 黒番か判定
  boolean isBlack() {
    return getColor() == BLACK;
  }

  public Move think(Board board) {
  // 相手の着手を反映
  this.board = this.board.placed(board.getMove());

  // パスの場合（合法手なし）
  if (this.board.findNoPassLegalIndexes(getColor()).size() == 0) {
    this.move = Move.ofPass(getColor());
  } else {
    // 黒番ならそのまま、白番なら反転（白→黒にする）
    var newBoard = isBlack() ? this.board.clone() : this.board.flipped();
    this.move = null;

    // αβ法で最大探索を開始
    // maxSearch(newBoard, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY, 0);

    var moves = newBoard.findLegalMoves(BLACK);
    System.out.println("合法手を選んでください（例: 0, 1, 2...）:");
    Scanner scanner = new Scanner(System.in);
    Boolean end = false;
    while (end) {
      int number = scanner.nextInt();  // 整数の入力
      for (var move : moves) {
        if (move.getIndex() == number) {
          this.move = move;
          end = true;  // 合法手が見つかったらループを抜ける  
        }
      }
      System.out.println("無効な手です");
    }
    scanner.close();

    // 選んだ手に色を付ける
    this.move = this.move.colored(getColor());
  }

  // 自分の着手を盤面に反映
  this.board = this.board.placed(this.move);
  return this.move;
  }

  ////////// ミニマックス法だよ！
  float maxSearch(Board board, float alpha, float beta, int depth) {
    if (isTerminal(board, depth)) return this.eval.value(board);

    var moves = board.findLegalMoves(BLACK);
    moves = order(moves);  // 手の順番をランダムにシャッフル（枝刈り効果向上）

    if (depth == 0)
      this.move = moves.get(0);  // 最上位では候補として仮に最初の手を選ぶ

    for (var move: moves) {
      var newBoard = board.placed(move);
      float v = minSearch(newBoard, alpha, beta, depth + 1);

      if (v > alpha) {
        alpha = v;
        if (depth == 0)
          this.move = move;  // 最良手を更新
      }

      if (alpha >= beta)  // 枝刈り条件
        break;
    }

    return alpha;
  }

  float minSearch(Board board, float alpha, float beta, int depth) {
    if (isTerminal(board, depth)) return this.eval.value(board);

    var moves = board.findLegalMoves(WHITE);
    moves = order(moves);

    for (var move: moves) {
      var newBoard = board.placed(move);
      float v = maxSearch(newBoard, alpha, beta, depth + 1);
      beta = Math.min(beta, v);
      if (alpha >= beta) break;
    }

    return beta;
  }
  ////////// ミニマックス法終わり
  /// 
  // ゲーム終了または深さ制限
  boolean isTerminal(Board board, int depth) {
    return board.isEnd() || depth > this.depthLimit;
  }

  // 手をランダムに並び替える（単純なmove ordering）
  List<Move> order(List<Move> moves) {
    var shuffled = new ArrayList<Move>(moves);
    Collections.shuffle(shuffled);
    return shuffled;
  }
}
