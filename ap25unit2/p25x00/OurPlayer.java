package p25x00;

import static ap25.Board.LENGTH;
import static ap25.Board.SIZE;
import static ap25.Color.BLACK;
import static ap25.Color.WHITE;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;
import ap25.Board;
import ap25.Color;
import ap25.Move;

// 評価関数クラス
class MyEval {
  // 盤面の重み
  static float[][] M = {
      { 10,  10, 10, 10,  10,  10},
      { 10,  -5,  1,  1,  -5,  10},
      { 10,   1,  1,  1,   1,  10},
      { 10,   1,  1,  1,   1,  10},
      { 10,  -5,  1,  1,  -5,  10},
      { 10,  10, 10, 10,  10,  10},
  };

  // 評価値を計算
  public float value(Board board) {
    if (board.isEnd()) return 1000000 * board.score();

    return (float) IntStream.range(0, LENGTH)
      .mapToDouble(k -> score(board, k))
      .reduce(Double::sum).orElse(0);
  }

  // 1マスごとの評価値
  float score(Board board, int k) {
    return M[k / SIZE][k % SIZE] * board.get(k).getValue();
  }
}

// プレイヤークラス
public class OurPlayer extends ap25.Player {
  static final String MY_NAME = "2400"; // プレイヤー名
  MyEval eval;         // 評価関数
  int depthLimit;      // 探索の最大深さ
  Move move;           // 選んだ手
  OurBoard board;      // 内部的に使う盤面

  // コンストラクタ（色のみ指定）
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

  // Boardの状態をOurBoardにコピー
  public void setBoard(Board board) {
    for (var i = 0; i < LENGTH; i++) {
      this.board.set(i, board.get(i));
    }
  }

  // 黒番か判定
  boolean isBlack() { return getColor() == BLACK; }

  // 思考メソッド
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

      // 最上位の合法手リスト
      var moves = order(newBoard.findLegalMoves(BLACK));

      // 並列で各手の評価値を計算
      var results = moves.parallelStream()
        .map(move -> {
          var nextBoard = newBoard.placed(move);
          float value = minSearch(nextBoard, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY, 1);
          return new Object[]{move, value};
        })
        .toList();

      // 最大値を持つ手を選ぶ
      var best = results.stream().max((a, b) -> Float.compare((float)a[1], (float)b[1])).orElse(null);
      this.move = ((Move)best[0]).colored(getColor());
    }

    // 自分の着手を盤面に反映
    this.board = this.board.placed(this.move);
    return this.move;
  }

  // αβ法（最大化側）
  float maxSearch(Board board, float alpha, float beta, int depth) {
    if (isTerminal(board, depth)) return this.eval.value(board);

    var moves = board.findLegalMoves(BLACK);
    moves = order(moves);

    if (depth == 0)
      this.move = moves.get(0);

    for (var move: moves) {
      var newBoard = board.placed(move);
      float v = minSearch(newBoard, alpha, beta, depth + 1);

      if (v > alpha) {
        alpha = v;
        if (depth == 0)
          this.move = move;
      }

      if (alpha >= beta)
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

  // 終端判定（終了 or 深さ制限）
  boolean isTerminal(Board board, int depth) {
    return board.isEnd() || depth > this.depthLimit;
  }

  // 手をランダムに並び替える（move ordering）
  List<Move> order(List<Move> moves) {
    var shuffled = new ArrayList<Move>(moves);
    Collections.shuffle(shuffled);
    return shuffled;
  }
}
