package p25x00;

import static ap25.Board.*;
import static ap25.Color.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

import ap25.*;

// 評価関数クラス
class MyEval {
  // 序盤・中盤・終盤用の重み配列
  static final float[][] M_EARLY = {
      { 20,  10, 10, 10,  10,  20},
      { 10,  -5,  1,  1,  -5,  10},
      { 10,   1,  1,  1,   1,  10},
      { 10,   1,  1,  1,   1,  10},
      { 10,  -5,  1,  1,  -5,  10},
      { 20,  10, 10, 10,  10,  20},
  };
  static final float[][] M_MIDDLE = {
      { 30,  12, 12, 12,  12,  30},
      { 12,  -8,  2,  2,  -8,  12},
      { 12,   2,  2,  2,   2,  12},
      { 12,   2,  2,  2,   2,  12},
      { 12,  -8,  2,  2,  -8,  12},
      { 30,  12, 12, 12,  12,  30},
  };

  public float value(Board board) {
    if (board.isEnd()) return 1000000 * board.score();

    return (float) IntStream.range(0, LENGTH)
      .mapToDouble(k -> score(board, k))
      .reduce(Double::sum).orElse(0);
  }

  float score(Board board, int k) {
    float[][] M = getM(board);
    return M[k / SIZE][k % SIZE] * board.get(k).getValue();
  }

  // 進行状況に応じて重み配列を返す
  float[][] getM(Board board) {
    int stoneCount = board.count(BLACK) + board.count(WHITE);
    if (stoneCount < 12) return M_EARLY;   // 序盤
    if (stoneCount < 24) return M_MIDDLE;  // 中盤
    return M_MIDDLE;                       // 終盤（必要ならM_LATEに変更可）
  }
}

// プレイヤークラス
public class OurPlayer extends ap25.Player {
  static final String MY_NAME = "2511";
  MyEval eval;
  int depthLimit;
  Move move;
  OurBoard board;

  public OurPlayer(Color color) {
    this(MY_NAME, color, new MyEval(), 4);
  }

  // コンストラクタ（詳細指定）
  public OurPlayer(String name, Color color, MyEval eval, int depthLimit) {
    super(name, color);
    this.eval = eval;
    this.depthLimit = depthLimit;
    this.board = new OurBoard();
  }

  // コンストラクタ（評価関数省略）
  public OurPlayer(String name, Color color, int depthLimit) {
    this(name, color, new MyEval(), depthLimit);
  }

  public void setBoard(Board board) {
    for (var i = 0; i < LENGTH; i++) {
      ((OurBoard)this.board).set(i, board.get(i));
    }
  }

  boolean isBlack() { return getColor() == BLACK; }

  // 思考メソッド
  public Move think(Board board) {
    // 相手の着手を反映
    this.board = this.board.placed(board.getMove());

    if (this.board.findNoPassLegalIndexes(getColor()).isEmpty()) {
      this.move = Move.ofPass(getColor());
    } else {
      // 黒番ならそのまま、白番なら反転（白→黒にする）
      var newBoard = isBlack() ? this.board.clone() : this.board.flipped();
      this.move = null;

      var legals = this.board.findNoPassLegalIndexes(getColor());

      maxSearch(newBoard, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY, 0);

      this.move = this.move.colored(getColor());

      if (legals.contains(this.move.getIndex()) == false) {
        System.out.println("**************");
        System.out.println(legals);
        System.out.println(this.move);
        System.out.println(this.move.getIndex());
        System.out.println(this.board);
        System.out.println(newBoard);
        System.exit(0);
        maxSearch(newBoard, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY, 0);
      }
    }

    this.board = this.board.placed(this.move);
    return this.move;
  }

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

  // αβ法（最小化側）
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

  boolean isTerminal(Board board, int depth) {
    return board.isEnd() || depth > this.depthLimit;
  }

  List<Move> order(List<Move> moves) {
    var shuffled = new ArrayList<Move>(moves);
    Collections.shuffle(shuffled);
    return shuffled;
  }
}
