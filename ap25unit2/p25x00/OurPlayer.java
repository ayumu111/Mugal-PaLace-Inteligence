package p25x00;

import static ap25.Board.*;
import static ap25.Color.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;

import ap25.*;

// 評価関数クラス
class MyEval {
  // 序盤・中盤・終盤用の重み配列（static→インスタンス変数に変更）
  float[][] M_EARLY = new float[6][6];
  float[][] M_MIDDLE = new float[6][6];
  float[][] M_LATE = new float[6][6];

  // 先手・後手用の重み配列
  float[][] earlyBlack = new float[6][6];
  float[][] middleBlack = new float[6][6];
  float[][] lateBlack = new float[6][6];
  float[][] earlyWhite = new float[6][6];
  float[][] middleWhite = new float[6][6];
  float[][] lateWhite = new float[6][6];

  Color myColor;

  // コンストラクタで重み配列と自分の色を受け取る
  public MyEval(Color myColor, float[][] early, float[][] middle, float[][] late) {
    this.myColor = myColor;
    for (int i = 0; i < 6; i++) {
      System.arraycopy(early[i], 0, earlyBlack[i], 0, 6);
      System.arraycopy(middle[i], 0, middleBlack[i], 0, 6);
      System.arraycopy(late[i], 0, lateBlack[i], 0, 6);
      System.arraycopy(early[i], 0, earlyWhite[i], 0, 6);
      System.arraycopy(middle[i], 0, middleWhite[i], 0, 6);
      System.arraycopy(late[i], 0, lateWhite[i], 0, 6);
    }
  }
  // デフォルト重み
  public MyEval(Color myColor) {
    this.myColor = myColor;
    float[][] defEarlyBlack = {
        { -20, 63, 51, 51, 63, -20 },
        { 63, -31, -70, -70, -31, 63 },
        { 51, -70, -12, -12, -70, 51 },
        { 51, -70, -12, -12, -70, 51 },
        { 63, -31, -70, -70, -31, 63 },
        { -20, 63, 51, 51, 63, -20 },
    };
    float[][] defMiddleBlack = {
      { 41, -7, 25, 25, -7, 41 },
      { -7, 57, 24, 24, 57, -7 },
      { 25, 24, -51, -51, 24, 25 },
      { 25, 24, -51, -51, 24, 25 },
      { -7, 57, 24, 24, 57, -7 },
      { 41, -7, 25, 25, -7, 41 },
    };
    float[][] defLateBlack = {
      { 59, 55, -68, -68, 55, 59 },
      { 55, 23, -8, -8, 23, 55 },
      { -68, -8, -93, -93, -8, -68 },
      { -68, -8, -93, -93, -8, -68 },
      { 55, 23, -8, -8, 23, 55 },
      { 59, 55, -68, -68, 55, 59 },
    };
    float[][] defEarlyWhite = {
      { -2, 60, 62, 62, 60, -2 },
      { 60, -59, 87, 87, -59, 60 },
      { 62, 87, -60, -60, 87, 62 },
      { 62, 87, -60, -60, 87, 62 },
      { 60, -59, 87, 87, -59, 60 },
      { -2, 60, 62, 62, 60, -2 },
    };
    float[][] defMiddleWhite = {
      { 68, 1, 25, 25, 1, 68 },
      { 1, 91, -23, -23, 91, 1 },
      { 25, -23, -87, -87, -23, 25 },
      { 25, -23, -87, -87, -23, 25 },
      { 1, 91, -23, -23, 91, 1 },
      { 68, 1, 25, 25, 1, 68 },
    };
    float[][] defLateWhite = {
      { -64, 54, 69, 69, 54, -64 },
      { 54, -44, -2, -2, -44, 54 },
      { 69, -2, 19, 19, -2, 69 },
      { 69, -2, 19, 19, -2, 69 },
      { 54, -44, -2, -2, -44, 54 },
      { -64, 54, 69, 69, 54, -64 },
    };
    for (int i = 0; i < 6; i++) {
      System.arraycopy(defEarlyBlack[i], 0, earlyBlack[i], 0, 6);
      System.arraycopy(defMiddleBlack[i], 0, middleBlack[i], 0, 6);
      System.arraycopy(defLateBlack[i], 0, lateBlack[i], 0, 6);
      System.arraycopy(defEarlyWhite[i], 0, earlyWhite[i], 0, 6);
      System.arraycopy(defMiddleWhite[i], 0, middleWhite[i], 0, 6);
      System.arraycopy(defLateWhite[i], 0, lateWhite[i], 0, 6);
    }
  }

  // 評価関数：ゲームが終了していればスコア×1000000、そうでなければ各マスごとの合計
  public float value(Board board) {
    if (board.isEnd()) return 1000000 * board.score();
    float psi = (float) IntStream.range(0, LENGTH)
    .mapToDouble(k -> score(board, k))
    .reduce(Double::sum).orElse(0);

    int lb = board.findLegalMoves(BLACK).size();
    int lw = board.findLegalMoves(WHITE).size();

    // 黒と白の石の数に応じてスコアを調整
    int nb = board.count(Color.BLACK);
    int nw = board.count(Color.WHITE);

    //重み探索用
    // float w1 = getw1(board);
    // float w2 = getw2(board);
    // float w3 = getw3(board);
    // float w4 = getw4(board);
    // float w5 = getw5(board);

    //重み配列探索用
    float w1 = 1;
    float w2 = 0;
    float w3 = 0;
    float w4 = 0;
    float w5 = 0;

    return w1*psi + w2*nb + w3*nw + w4*lb + w5*lw; // 黒番ならプラス、白番ならマイナス
  }

  // 評価関数の重みを外部からセットできるようにする
  static float customW1 = 100, customW2 = 10, customW3 = -10, customW4 = 1, customW5 = -1;
  public static void setEvalWeights(float w1, float w2, float w3, float w4, float w5) {
    customW1 = w1; customW2 = w2; customW3 = w3; customW4 = w4; customW5 = w5;
  }
  float getw1(Board board) { return customW1; }
  float getw2(Board board) { return customW2; }
  float getw3(Board board) { return customW3; }
  float getw4(Board board) { return customW4; }
  float getw5(Board board) { return customW5; }

  // 進行状況と「自分の色」に応じて重み配列を返す
  float[][] getM(Board board) {
    int stoneCount = board.count(Color.BLACK) + board.count(Color.WHITE);
    if (myColor == BLACK) {
      if (stoneCount < 12) return earlyBlack;
      if (stoneCount < 24) return middleBlack;
      return lateBlack;
    } else {
      if (stoneCount < 12) return earlyWhite;
      if (stoneCount < 24) return middleWhite;
      return lateWhite;
    }
  }

  // 特定のマス（k）のスコア計算：盤面の色値×重み
  float score(Board board, int k) {
    float[][] M = getM(board);
    return M[k / SIZE][k % SIZE] * board.get(k).getValue();
  }
}

// プレイヤークラス
public class OurPlayer extends ap25.Player {
  static final String MY_NAME = "2511";
  MyEval eval;
  int depthLimit;
  Move move;
  OurBoard board;

  // トランスポジションテーブル（盤面ハッシュ→ノード情報）
  static ConcurrentHashMap<Long, NodeInfo> transTable = new ConcurrentHashMap<>();

  static class NodeInfo {
    float value;
    int depth;
    // 必要ならMoveやαβ値も追加
    NodeInfo(float value, int depth) {
      this.value = value;
      this.depth = depth;
    }
  }

  public OurPlayer(Color color) {
    this(MY_NAME, color, new MyEval(color), 8);
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
    this(name, color, new MyEval(color), depthLimit);
  }

  // MyEvalを直接指定するコンストラクタ（MatrixSearch用）
  public OurPlayer(Color color, MyEval eval, int depthLimit) {
    this(MY_NAME, color, eval, depthLimit);
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
    // ここでOurBoardに渡る
    this.board = this.board.placed(board.getMove());

    // 合法手がなければパス
    var legalIndexes = this.board.findNoPassLegalIndexes(getColor());
    if (legalIndexes.isEmpty()) {
      this.move = Move.ofPass(getColor());
    } else {
      // 黒番ならそのまま、白番なら反転（白→黒にする）
      var newBoard = isBlack() ? this.board.clone() : this.board.flipped();

      long bitBoardBlack = newBoard.getBitBoard(BLACK);
      long bitBoardWhite = newBoard.getBitBoard(WHITE);
      long bitBoardBlock = newBoard.getBitBoard(BLOCK);
      OurBitBoard BitBoard = new OurBitBoard(bitBoardBlack, bitBoardWhite, bitBoardBlock);// bit化
      
      this.move = null;

      
      var legals = this.board.findNoPassLegalIndexes(getColor());
      maxSearch(BitBoard, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY, 0);// 探索 -> 結果


      
     
     
    // 合法手リストをMove型に変換
    var moves = BitBoard.findLegalMoves(BLACK);

    // 並列で各手の評価値を計算
    var results = moves.parallelStream()
      .map(move -> {
        var nextBoard = BitBoard.placed(move);
        // float value = minSearch(nextBoard, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY, 1);
        float value = iterativeDeepening(nextBoard, this.depthLimit);
        // float value = minSearch(nextBoard, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY, 1);
        return new Object[]{move, value};
      })
      .toList();

    // 最大値を持つ手を選ぶ
    var best = results.stream().max((a, b) -> Float.compare((float)a[1], (float)b[1])).orElse(null);
    this.move = ((Move)best[0]).colored(getColor());

     if (legals.contains(this.move.getIndex()) == false) {
        System.out.println("**************");
        System.out.println(legals);
        System.out.println(this.move);
        System.out.println(this.move.getIndex());
        System.out.println(this.board);
        System.exit(0);
      }

    
    

    }
    this.board = this.board.placed(this.move);

    return this.move;
  }

  ////////////////////////////////// αβ法開始
  float maxSearch(OurBitBoard board, float alpha, float beta, int depth) {
    long hash = board.hash();
    NodeInfo info = transTable.get(hash);
    if (info != null && info.depth >= this.depthLimit - depth) {
        return info.value;
    }

    if (isTerminal(board, depth)) {
      float v = this.eval.value(board.encode());
      transTable.put(hash, new NodeInfo(v, this.depthLimit - depth));
      return v;
    }

    var moves = board.findLegalMoves(BLACK);
    moves = order(moves);

    float best = Float.NEGATIVE_INFINITY;
    Move bestMove = null;
    for (var move: moves) {
      var newBoard = board.placed(move);
      float v = minSearch(newBoard, alpha, beta, depth + 1);
      if (v > best) {
        best = v;
        if (depth == 0) bestMove = move; // ←ここで最上位の手を保存
      }
      alpha = Math.max(alpha, v);
      if (alpha >= beta) break;
    }
    if (depth == 0 && bestMove != null) this.move = bestMove; // ←ここでセット
    transTable.put(hash, new NodeInfo(best, this.depthLimit - depth));

    return best;
  }

  // αβ法（最小化側）
  float minSearch(OurBitBoard board, float alpha, float beta, int depth) {
    if (isTerminal(board, depth)) {
      return this.eval.value(board.encode());
    }

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
  //////////////////////////////// αβ法終了
  /////////// MTD法
  
  float iterativeDeepening(OurBitBoard board, int maxDepth) {
    float guess = 0;
    for (int d = 1; d <= maxDepth; d++) {
      guess = mtd(board, guess, d); // 前回の結果を使う
    }
    return guess;
  }
  float mtd(OurBitBoard board, float firstGuess, int depth) {
    float g = firstGuess;
    float upperBound = Float.POSITIVE_INFINITY;// めっちゃプラス
    float lowerBound = Float.NEGATIVE_INFINITY;// めっちゃマイナス

    while (lowerBound < upperBound) {
        float beta = (g == lowerBound) ? g + 1 : g; // gがめっちゃマイナスならbeta = g+1 違うならbeta = g;
        g = alphaBeta(board, beta - 1, beta, depth);// 
        if (g < beta) {
            upperBound = g;
        } else {
            lowerBound = g;
        }
    }
    return g;
  }

  // null-window αβ探索
  float alphaBeta(OurBitBoard board, float alpha, float beta, int depth) {
      return maxSearch(board, alpha, beta, depth); // あなたのmaxSearchを再利用
  }

  ///////////////////////////////////
  
  
  /// /////////////////////////////// negaScout
  float negaScout(OurBitBoard board, float alpha, float beta, int depth) {
    long hash = board.hash();
    NodeInfo info = transTable.get(hash);
    if (info != null && info.depth >= this.depthLimit - depth) {
        return info.value;
    }

    if (isTerminal(board, depth)) {
        float v = this.eval.value(board.encode());
        transTable.put(hash, new NodeInfo(v, this.depthLimit - depth));
        return v;
    }

    var moves = board.findLegalMoves(BLACK);
    moves = order(moves);

    float b = beta;
    float best = Float.NEGATIVE_INFINITY;
    boolean first = true;
    for (var move : moves) {
        var newBoard = board.placed(move);
        float score;
        if (first) {
            score = -negaScoutMin(newBoard, -b, -alpha, depth + 1);
        } else {
            score = -negaScoutMin(newBoard, -alpha - 1, -alpha, depth + 1);
            if (alpha < score && score < beta) {
                // re-search if fail-soft
                score = -negaScoutMin(newBoard, -b, -alpha, depth + 1);
            }
        }

        best = Math.max(best, score);
        alpha = Math.max(alpha, score);
        if (alpha >= beta) break;

        b = alpha + 1;
        first = false;
    }

    transTable.put(hash, new NodeInfo(best, this.depthLimit - depth));
    return best;
  }

  // 最小化側（Negamax形式で）
  float negaScoutMin(OurBitBoard board, float alpha, float beta, int depth) {
    if (isTerminal(board, depth)) {
        return this.eval.value(board.encode());
    }

    var moves = board.findLegalMoves(WHITE);
    moves = order(moves);

    float b = beta;
    boolean first = true;
    float best = Float.POSITIVE_INFINITY;
    for (var move : moves) {
        var newBoard = board.placed(move);
        float score;
        if (first) {
            score = -negaScout(newBoard, -b, -alpha, depth + 1);
        } else {
            score = -negaScout(newBoard, -alpha - 1, -alpha, depth + 1);
            if (alpha < score && score < beta) {
                score = -negaScout(newBoard, -b, -alpha, depth + 1);
            }
        }
        best = Math.min(best, score);
        beta = Math.min(beta, score);
        if (alpha >= beta) break;

        b = beta;
        first = false;
    }

    return beta;
  }
//////////////
  boolean isTerminal(OurBitBoard board, int depth) {
    return board.isEnd() || depth > this.depthLimit;
  }

  List<Move> order(List<Move> moves) {
    var shuffled = new ArrayList<Move>(moves);
    Collections.shuffle(shuffled);
    return shuffled;
  }

  public static void setEvalWeights(float w1, float w2, float w3, float w4, float w5) {
    MyEval.setEvalWeights(w1, w2, w3, w4, w5);
  }
}
