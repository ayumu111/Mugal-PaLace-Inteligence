package enemy16;

import static ap25.Board.*;
import static ap25.Color.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;

import javax.swing.border.Border;

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
  public MyEval(Color myColor, float[][] w) {
    this.myColor = myColor;
    setEvalWeights(w);
  }
  // デフォルト重み
  public MyEval(Color myColor) {
    this.myColor = myColor;
    float[][] defEarlyBlack = {
{ -66, -56, 22, 22, -56, -66 },
{ -56, 14, 47, 47, 14, -56 },
{ 22, 47, 36, 36, 47, 22 },
{ 22, 47, 36, 36, 47, 22 },
{ -56, 14, 47, 47, 14, -56 },
{ -66, -56, 22, 22, -56, -66 },
    };
    float[][] defMiddleBlack = {
{ -15, -91, 43, 43, -91, -15 },
{ -91, -74, -39, -39, -74, -91 },
{ 43, -39, 67, 67, -39, 43 },
{ 43, -39, 67, 67, -39, 43 },
{ -91, -74, -39, -39, -74, -91 },
{ -15, -91, 43, 43, -91, -15 },
    };
    float[][] defLateBlack = {
{ 2, 43, 23, 23, 43, 2 },
{ 43, -35, -40, -40, -35, 43 },
{ 23, -40, 40, 40, -40, 23 },
{ 23, -40, 40, 40, -40, 23 },
{ 43, -35, -40, -40, -35, 43 },
{ 2, 43, 23, 23, 43, 2 },
    };
    float[][] defEarlyWhite = {
{ -46, 61, -67, -67, 61, -46 },
{ 61, 21, 38, 38, 21, 61 },
{ -67, 38, -28, -28, 38, -67 },
{ -67, 38, -28, -28, 38, -67 },
{ 61, 21, 38, 38, 21, 61 },
{ -46, 61, -67, -67, 61, -46 },
    };
    float[][] defMiddleWhite = {
{ 70, -43, 4, 4, -43, 70 },
{ -43, -1, -9, -9, -1, -43 },
{ 4, -9, -67, -67, -9, 4 },
{ 4, -9, -67, -67, -9, 4 },
{ -43, -1, -9, -9, -1, -43 },
{ 70, -43, 4, 4, -43, 70 },
    };
    float[][] defLateWhite = {
{ 47, -39, 86, 86, -39, 47 },
{ -39, 38, -94, -94, 38, -39 },
{ 86, -94, 13, 13, -94, 86 },
{ 86, -94, 13, 13, -94, 86 },
{ -39, 38, -94, -94, 38, -39 },
{ 47, -39, 86, 86, -39, 47 },
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

    // 合法手の数
    int lb = board.findLegalMoves(BLACK).size();
    int lw = board.findLegalMoves(WHITE).size();

    // 黒と白の石の数に応じてスコアを調整
    int nb = board.count(Color.BLACK);
    int nw = board.count(Color.WHITE);
    int nbDif = nb - nw; // 黒と白の石の数の差

    // 角に関するパラメータ
    int[] corner ={0, 5, 30, 35}; // 角のインデックス
    int CornerDif = 0, MyCorner = 0, EnemyCorner = 0, BlockCorner = 0;
    for (int i = 0; i < 4; i++) {

      if (board.get(corner[i]) == BLACK) {
        MyCorner++;
        CornerDif++;}
      else if (board.get(corner[i]) == WHITE) {
        EnemyCorner++;
        CornerDif--;}
      else if (board.get(corner[i]) == BLOCK) BlockCorner++;
    }

    // 辺に関するパラメータ
    int[] edge = {1, 2, 3, 4, 6, 11, 12, 17, 18, 23, 24, 29, 31, 32, 33, 34}; // 辺のインデックス
    int EdgeDif = 0, MyEdge = 0, EnemyEdge = 0, BlockEdge = 0;
    for (int i = 0; i < edge.length; i++) {
      if (board.get(edge[i]) == BLACK) {
        MyEdge++;
        EdgeDif++;}
      else if (board.get(edge[i]) == WHITE) {
        EnemyEdge++;
        EdgeDif--;}
      else if (board.get(edge[i]) == BLOCK) BlockEdge++;
    };    

    // ブロックの数
    int BlockCount = board.count(Color.BLOCK);
    
    // 安定石の数
    int MyStable = ((OurBoard)board).countSimpleStable(BLACK);
    int EnemyStable = ((OurBoard)board).countSimpleStable(WHITE);
    int StableDif = MyStable - EnemyStable;

    //重み探索用
    float[][] w = getW(board);

    float[] parameta = {psi, lb, lw, nb, nw, nbDif, CornerDif, MyCorner, EnemyCorner, BlockCorner,
      MyEdge, EnemyEdge, BlockEdge, EdgeDif, BlockCount, StableDif,
      MyStable, EnemyStable};
    float value = 0;
    int stoneCount = nb + nw; // 石の総数

    // 評価値計算
    for (int i = 0; i < w.length; i++) {
      if (stoneCount <= 12) {
        value += w[i][0] * parameta[i]; // 序盤
      } else if (stoneCount <= 24) {
        value += w[i][1] * parameta[i]; // 中盤
      } else {
        value += w[i][2] * parameta[i]; // 終盤
      }
      
    }

    return value; // 黒番ならプラス、白番ならマイナス
  }

  // 評価関数の重みを外部からセットできるようにする
  public void setEvalWeights(float[][] w) {
    for (int i = 0; i < customW.length; i++) {
        System.arraycopy(w[i], 0, customW[i], 0, w[i].length);
    }
  }
  // 重み配列を返すメソッド
  float[][] getW(Board board) {
    return customW;
  }

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

static float[][] customW = {
  {0.0f, 0.1f, 100.0f}, 
  {-0.001f, 10.0f, 0.0f}, 
  {0.0f, -1000.0f, 0.01f}, 
  {100.0f, 0.001f, 100.0f}, 
  {-1000.0f, 0.001f, 1.0f}, 
  {-0.1f, -0.001f, 1000.0f}, 
  {-1000.0f, 100.0f, -10.0f}, 
  {1.0f, 100.0f, 1.0f}, 
  {0.1f, 0.0f, -10.0f}, 
  {-0.01f, 0.1f, 0.001f}, 
  {1000.0f, 1000.0f, 100.0f}, 
  {0.001f, -0.01f, 1000.0f}, 
  {0.0f, 1000.0f, -0.01f}, 
  {-1.0f, -100.0f, 0.001f}, 
  {-10.0f, -0.001f, -0.1f}, 
  {-0.001f, 10.0f, -1.0f}, 
  {-1000.0f, -1000.0f, 0.01f}, 
  {10.0f, -1.0f, 1000.0f}
};
}

// プレイヤークラス
public class OurPlayer extends ap25.Player {
  static final String MY_NAME = "ememy16";
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
    this(MY_NAME, color, new MyEval(color), 6);
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
        float value = minSearch(nextBoard, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY, 1);
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
  boolean isTerminal(OurBitBoard board, int depth) {
    return board.isEnd() || depth > this.depthLimit;
  }

  List<Move> order(List<Move> moves) {
    var shuffled = new ArrayList<Move>(moves);
    Collections.shuffle(shuffled);
    return shuffled;
  }
}
