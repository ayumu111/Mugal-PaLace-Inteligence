package p25x11;

import static ap25.Board.*;
import static ap25.Color.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
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
  public MyEval(Color myColor, float[][] w) {
    this.myColor = myColor;
    setEvalWeights(w);
  }
  // デフォルト重み
  public MyEval(Color myColor) {
    this.myColor = myColor;
    float[][] defEarlyBlack = {
      { 20,  10, 10, 10,  10,  20},
      { 10,  -10,  1,  1,  -10,  10},
      { 10,   1,  1,  1,   1,  10},
      { 10,   1,  1,  1,   1,  10},
      { 10,  -10,  1,  1,  -10,  10},
      { 20,  10, 10, 10,  10,  20},
    };
    float[][] defMiddleBlack = {
      { 30,  12, 12, 12,  12,  30},
      { 12,  -8,  2,  2,  -8,  12},
      { 12,   2,  2,  2,   2,  12},
      { 12,   2,  2,  2,   2,  12},
      { 12,  -8,  2,  2,  -8,  12},
      { 30,  12, 12, 12,  12,  30},
    };
    float[][] defLateBlack = {
      { 50,  20, 20, 20,  20,  50},
      { 20,   0,  5,  5,   0,  20},
      { 20,   5,  5,  5,   5,  20},
      { 20,   5,  5,  5,   5,  20},
      { 20,   0,  5,  5,   0,  20},
      { 50,  20, 20, 20,  20,  50},
    };
    float[][] defEarlyWhite = {
      { 20,  10, 10, 10,  10,  20},
      { 10,  -10,  1,  1,  -10,  10},
      { 10,   1,  1,  1,   1,  10},
      { 10,   1,  1,  1,   1,  10},
      { 10,  -10,  1,  1,  -10,  10},
      { 20,  10, 10, 10,  10,  20},
    };
    float[][] defMiddleWhite = {
      { 30,  12, 12, 12,  12,  30},
      { 12,  -8,  2,  2,  -8,  12},
      { 12,   2,  2,  2,   2,  12},
      { 12,   2,  2,  2,   2,  12},
      { 12,  -8,  2,  2,  -8,  12},
      { 30,  12, 12, 12,  12,  30},
    };
    float[][] defLateWhite = {
      { 50,  20, 20, 20,  20,  50},
      { 20,   0,  5,  5,   0,  20},
      { 20,   5,  5,  5,   5,  20},
      { 20,   5,  5,  5,   5,  20},
      { 20,   0,  5,  5,   0,  20},
      { 50,  20, 20, 20,  20,  50},
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
  public float value(OurBitBoard board) {
 if (board.isEnd()) return 1000000 * board.score();
    float psi = (float) IntStream.range(0, LENGTH)
    .mapToDouble(k -> score(board, k))
    .reduce(Double::sum).orElse(0);

    // 合法手の数
    int lb = board.findLegalMoves(BLACK).size();
    int lw = board.findLegalMoves(WHITE).size();
    int mobility = lb - lw; // 黒と白の合法手の数の差

    // 角のモビリティ
    int[] corner = {0, 5, 30, 35}; // 角のインデックス
    int cMobility_black = 0;
    int cMobility_white = 0;
    List<Move> legalMovesBlack = board.findLegalMoves(BLACK);
    List<Move> legalMovesWhite = board.findLegalMoves(WHITE);
    for (int idx : corner) {
      for (Move m : legalMovesBlack) {
        if (m.getIndex() == idx) { cMobility_black++; break; }
      }
      for (Move m : legalMovesWhite) {
        if (m.getIndex() == idx) { cMobility_white++; break; }
      }
    }
    

    // 黒と白の石の数に応じてスコアを調整
    int nb = board.count(Color.BLACK);
    int nw = board.count(Color.WHITE);
    int nbDif = nb - nw; // 黒と白の石の数の差

    if(nw == 0 && nb > 0){
      return 1000000 * nb; // 黒が勝ち
    }

    // 角に関するパラメータ
    // int[] corner ={0, 5, 30, 35}; // 角のインデックス
    // int CornerDif = 0, MyCorner = 0, EnemyCorner = 0, BlockCorner = 0;
    // for (int i = 0; i < 4; i++) {

    //   if (board.get(corner[i]) == BLACK) {
    //     MyCorner++;
    //     CornerDif++;}
    //   else if (board.get(corner[i]) == WHITE) {
    //     EnemyCorner++;
    //     CornerDif--;}
    //   else if (board.get(corner[i]) == BLOCK) BlockCorner++;// そのままパラメータとして使ってもその試合の中でブロックの数は変わらないので、意味なし
    // }

    // 辺に関するパラメータ
    // int[] edge = {1, 2, 3, 4, 6, 11, 12, 17, 18, 23, 24, 29, 31, 32, 33, 34}; // 辺のインデックス
    // int EdgeDif = 0, MyEdge = 0, EnemyEdge = 0, BlockEdge = 0;
    // for (int i = 0; i < edge.length; i++) {
    //   if (board.get(edge[i]) == BLACK) {
    //     MyEdge++;
    //     EdgeDif++;}
    //   else if (board.get(edge[i]) == WHITE) {
    //     EnemyEdge++;
    //     EdgeDif--;}
    //   else if (board.get(edge[i]) == BLOCK) BlockEdge++;// そのままパラメータとして使ってもその試合の中でブロックの数は変わらないので、意味なし
    // };    

    // ブロックの数
    int BlockCount = board.count(Color.BLOCK);// そのままパラメータとして使ってもその試合の中でブロックの数は変わらないので、意味なし
    
    // 安定石の数
    int MyStable = board.countSimpleStable(BLACK);
    int EnemyStable = board.countSimpleStable(WHITE);
    int StableDif = MyStable - EnemyStable;
    // System.out.println("MyStable: " + MyStable + ", EnemyStable: " + EnemyStable);
    // System.out.println("StableDif: " + StableDif);

    // 潜在的モビリティ(潜在的合法手の数)
    int pmob_black = 0;
    int pmob_white = 0;
    if(nb + nw  < 28) {
      pmob_black = (board).findPotentialMobility(BLACK);
      pmob_white = (board).findPotentialMobility(WHITE);
    } 
    //重み探索用
    float[][] w = getW();

    // 角モビリティをパラメータに追加
    float[] parameta = {psi, mobility, nb, nw, MyStable,
                        EnemyStable, pmob_black, pmob_white,
                        cMobility_black, cMobility_white};
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
    
    // System.out.println(board);
    // System.out.println(cMobility_black + ", " + cMobility_white);
    // System.out.println(parameta[0] + ", " + parameta[1] + ", " + parameta[2] + ", " + parameta[3] + ", " +
    //                    parameta[4] + ", " + parameta[5] + ", " + parameta[6] + ", " + parameta[7]);
    // System.out.println("評価値: " + value);
    
    return value; // 黒番ならプラス、白番ならマイナス
  }

  static float[][] customW = {
    {1, 0.8f, 0.3f}, {1.24f, 1.24f, 1.24f}, {0.1f, 0.8f, 1.5f}, {-0.1f, -0.8f, 1.2f}, {2, 5, 8},
    {-2, -4, -6}, {0.5f, 0.5f, 0},{-0.5f, -0.5f, 0},
    {2.0f, 2.0f, 2.0f}, {-2, -2, -10} 
    };

  // 評価関数の重みを外部からセットできるようにする
  public void setEvalWeights(float[][] w) {
    for (int i = 0; i < customW.length; i++) {
        System.arraycopy(w[i], 0, customW[i], 0, w[i].length);
    }
  }
  // 重み配列を返すメソッド
  float[][] getW() {
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

  // OurBitBoardとintを引数に取るscore関数
float score(OurBitBoard bitBoard, int k) {
    float[][] M = getM(bitBoard.encode()); // 既存のgetMを流用
    int row = k / SIZE;
    int col = k % SIZE;
    // k番目の色をビット演算で取得
    int value;
    if (((bitBoard.getBitBoardBlack() >>> k) & 1L) != 0) {
        value = 1; // 黒
    } else if (((bitBoard.getBitBoardWhite() >>> k) & 1L) != 0) {
        value = -1; // 白
    } else {
        value = 0; // ブロックまたは空き
    }
    return M[row][col] * value;
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
    this(MY_NAME, color, new MyEval(color), 9);
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
    // 【重要】思考開始時に子ノードテーブルをクリア
    clearChildNodeTable();
    
    // 相手の着手を反映
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

      
     //var legals = this.board.findNoPassLegalIndexes(getColor());
     // maxSearch(BitBoard, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY, 0);// 探索 -> 結果
      
     
     
     // 合法手リストをMove型に変換
     var moves = BitBoard.findLegalMoves(BLACK);
     // int stoneCount = board.count(Color.BLACK) + board.count(Color.WHITE);

    // 各手の評価値を逐次的に計算
    ArrayList<Object[]> results;
    //if (stoneCount < 30) {
      results = new ArrayList<>();
      for (var move : moves) {
        var nextBoard = BitBoard.placed(move);
        // float value = minSearch(nextBoard, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY, 1);
        float value = iterativeDeepening(nextBoard, this.depthLimit);
        // float value = mtd(nextBoard, 0, 1);
        // float value = negaScout(nextBoard, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY, 1,1);

        // float value = minSearch(nextBoard, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY, 1);
        results.add(new Object[]{move, value});
      }
    // } else {
    //   // 終盤になったら並列処理を使用
    //   results = moves.parallelStream()
    //     .map(move -> {
    //       var nextBoard = BitBoard.placed(move);
    //       float value = iterativeDeepening(nextBoard, this.depthLimit);
    //       return new Object[]{move, value};
    //     })
    //     .collect(Collectors.toCollection(ArrayList::new));
    // }

    // 最大値を持つ手を選ぶ
    var best = results.stream().max((a, b) -> Float.compare((float)a[1], (float)b[1])).orElse(null);
    this.move = ((Move)best[0]).colored(getColor());

    // BitBoard版の合法手チェックとエラー表示
    var legalBitMoves = BitBoard.findLegalMoves(BLACK).stream()
      .map(Move::getIndex)
      .collect(Collectors.toSet());
    if (!legalBitMoves.contains(this.move.getIndex())) {
      System.out.println("**************");
      System.out.println("Legal moves: " + legalBitMoves);
      System.out.println("Selected move: " + this.move);
      System.out.println("Selected move index: " + this.move.getIndex());
      System.out.println("BitBoard: " + BitBoard);
      System.exit(0);
    }

    
    

    }
    this.board = this.board.placed(this.move);
    //System.out.println(this.move);
    return this.move;
  }

  ////////////////////////////////// αβ法開始
  // maxSearchの修正版（良い手ほど深く探索）
  float maxSearch(OurBitBoard board, float alpha, float beta, int depth) {
    long hash = board.hash();
    NodeInfo info = transTable.get(hash);
    if (info != null && info.depth >= this.depthLimit - depth) {
        return info.value;
    }

    if (isTerminal(board, depth)) {
        float v = this.eval.value(board);
        transTable.put(hash, new NodeInfo(v, this.depthLimit - depth));
        return v;
    }

    var moves = board.findLegalMoves(BLACK);
    
    // 子ノードを記録
    recordChildNodes(board, moves, depth);
    
    // ソート実行
    moves = order(moves, board);
    
    float best = Float.NEGATIVE_INFINITY;
    Move bestMove = null;
    
    for (int i = 0; i < moves.size(); i++) {
        var move = moves.get(i);
        
        // 追加深度の計算
        int extraDepth = 0;
        if (i == 0) extraDepth = 2;
        else if (i == 1) extraDepth = 1;
        else if (i == 2) extraDepth = 1;
        
        // 既に生成済みの盤面を使用（重複なし）
        OurBitBoard newBoard = board.placed(move);
        float v = minSearch(newBoard, alpha, beta, depth + 1 + extraDepth);
        
        if (v > best) {
            best = v;
            if (depth == 0) bestMove = move;
        }
        alpha = Math.max(alpha, v);
        if (alpha >= beta) break;
    }
    
    if (depth == 0 && bestMove != null) this.move = bestMove;
    transTable.put(hash, new NodeInfo(best, this.depthLimit - depth));
    return best;
  }

  // αβ法（最小化側）
  // minSearchも同様に修正
  float minSearch(OurBitBoard board, float alpha, float beta, int depth) {
    if (isTerminal(board, depth)) {
      return this.eval.value(board);
    }

    var moves = board.findLegalMoves(WHITE);
    if (moves.isEmpty()) {
        return this.eval.value(board);
    }
    
    // 子ノードを記録
    long hash = board.hash();
    NodeInfo info = transTable.get(hash);
    if (info == null) {
        recordChildNodes(board, moves, depth);
    }
    
    // 保存された子ノード情報を使ってソート（評価値の高い順）
    moves = order(moves, board);

    float best = Float.POSITIVE_INFINITY;
    
    for (int i = 0; i < moves.size(); i++) {
        var move = moves.get(i);
        var newBoard = board.placed(move);
        
        // 良い手ほど深く探索（上位3手に追加の深さを与える）
        int extraDepth = 0;
        if (i == 0) extraDepth = 2;      // 最良手: +3深く
        else if (i == 1) extraDepth = 1; // 2番目: +2深く
        else if (i == 2) extraDepth = 1; // 3番目: +1深く
        
        float v = maxSearch(newBoard, alpha, beta, depth + 1 + extraDepth);
        
        best = Math.min(best, v);
        beta = Math.min(beta, v);
        if (alpha >= beta) break; // αβ枝刈り
    }
    
    return best;
  }
  //////////////////////////////// αβ法終了
  /////////// MTD法
  
  float iterativeDeepening(OurBitBoard board, int maxDepth) {
    float guess = 0;
    for (int d = 1; d <= maxDepth; d++) {
      this.depthLimit = d;
        guess = mtd(board, guess, d);
    }
    return guess;
  }

  // MTD(f) メイン
  float mtd(OurBitBoard board, float firstGuess, int depth) {
      float g = firstGuess;
      float upperBound = Float.POSITIVE_INFINITY;
      float lowerBound = Float.NEGATIVE_INFINITY;

      int maxIterations = 50;
      int iteration = 0;

      while (lowerBound < upperBound && iteration < maxIterations) {
          float beta = (g == lowerBound) ? g + 1 : g;
          // NegaScoutを使用（colorは1で開始 = 黒番）
          g = negaScout(board, beta-1, beta, 1, false);;
          // g = minSearch(board, beta - 1, beta, depth);
          
          if (g < beta) {
              upperBound = g;
          } else {
              lowerBound = g;
          }
          // System.out.println("mtd" + lowerBound + "<" + upperBound + ";" + g);
          iteration++;
      }
      return g;
  }

// NegaScout統一版（colorパラメータ付き）
 // negaScout法実装
// 反復深化対応版（depthは減らしていく方針）

  float negaScout(OurBitBoard board, float alpha, float beta, int depth, boolean isBlack) {
      long hash = board.hash();
      NodeInfo info = transTable.get(hash);
      if (info != null && info.depth >= this.depthLimit - depth) {
          return isBlack ? info.value : -info.value;
      }
      
      if (isTerminal(board, depth)) {
          float v = this.eval.value(board);
          transTable.put(hash, new NodeInfo(v, this.depthLimit - depth));
          return isBlack ? v : -v;
      }
      
      var moves = board.findLegalMoves(isBlack ? BLACK : WHITE);
      moves = order(moves, board); // 手の順番を評価値でソート
      
      float best = Float.NEGATIVE_INFINITY;
      Move bestMove = null;
      boolean firstMove = true;
      
      for (var move : moves) {
          var newBoard = board.placed(move);
          float v;
          
          if (firstMove) {
              // 最初の手は通常の探索
              v = -negaScout(newBoard, -beta, -alpha, depth + 1, !isBlack);
              firstMove = false;
          } else {
              // null window searchを試行
              v = -negaScout(newBoard, -alpha - 0.001f, -alpha, depth + 1, !isBlack);
              
              // null window searchで上限を超えた場合、再探索
              if (v > alpha && v < beta) {
                  v = -negaScout(newBoard, -beta, -alpha, depth + 1, !isBlack);
              }
          }
          
          if (v > best) {
              best = v;
              if (depth == 0) bestMove = move;
          }
          
          alpha = Math.max(alpha, v);
          if (alpha >= beta) break; // βカット
      }
      
      if (depth == 0 && bestMove != null) this.move = bestMove;
      
      float storeValue = isBlack ? best : -best;
      transTable.put(hash, new NodeInfo(storeValue, this.depthLimit - depth));
      
      return best;
  }
//////////////
  boolean isTerminal(OurBitBoard board, int depth) {
    return board.isEnd() || depth > this.depthLimit;
  }

  // 評価関数の値によってMoveをソートする
  // 保存された子ノード情報を使ってMoveOrderingを行う
  List<Move> order(List<Move> moves, OurBitBoard board) {
    // movesが空または1個以下の場合はそのまま返す
    if (moves == null || moves.size() <= 1) {
        return moves;
    }
    
    // 保存された子ノード情報を取得
    long parentHash = board.hash();
    List<ChildNodeRecord> childNodes = childNodeTable.getOrDefault(parentHash, new ArrayList<>());
    
    // 子ノード情報がない場合は元の順序を返す
    if (childNodes.isEmpty()) {
      //System.out.println("[DEBUG] No child nodes found for parent hash: " + parentHash + ", moves: " + moves);
      return moves;
    }
    
    // 子ノード情報をMap化（Move -> 評価値）
    Map<Move, Float> evaluationMap = new HashMap<>();
    for (ChildNodeRecord child : childNodes) {
        evaluationMap.put(child.move, child.evaluation);
    }
    
    // 保存された評価値でソート（評価値が高い順）
    return moves.stream()
        .sorted((a, b) -> {
            Float evalA = evaluationMap.get(a);
            Float evalB = evaluationMap.get(b);
            
            // 評価値がない場合は0として扱う
            if (evalA == null) evalA = 0.0f;
            if (evalB == null) evalB = 0.0f;
            
            return Float.compare(evalB, evalA); // 降順ソート
        })
        .collect(Collectors.toList());
   }

// 子ノードの情報を記録するクラス
static class ChildNodeRecord {
    long bitBoardHash;  // 子ノードの盤面ハッシュ
    float evaluation;   // 子ノードの評価値
    Move move;          // その子ノードに至る手
    int depth;          // 探索した深さ
    
    ChildNodeRecord(long hash, float eval, Move move, int depth) {
        this.bitBoardHash = hash;
        this.evaluation = eval;
        this.move = move;
        this.depth = depth;
    }
}

// 親ノードのハッシュ → 子ノード記録のマップ
static ConcurrentHashMap<Long, List<ChildNodeRecord>> childNodeTable = new ConcurrentHashMap<>();

// 子ノードを記録するメソッド
private void recordChildNodes(OurBitBoard parentBoard, List<Move> moves, int depth) {
    long parentHash = parentBoard.hash();
    List<ChildNodeRecord> children = new ArrayList<>();
    
    for (Move move : moves) {
        OurBitBoard childBoard = parentBoard.placed(move);
        long childHash = childBoard.hash();
        
        // 子ノードの簡易評価（実際の探索前の予備評価）
        float childEval = this.eval.value(childBoard);
        
        children.add(new ChildNodeRecord(childHash, childEval, move, depth + 1));
    }
    
    childNodeTable.put(parentHash, children);
}

// 子ノード情報を取得するメソッド
public List<ChildNodeRecord> getChildNodes(OurBitBoard board) {
    long hash = board.hash();
    return childNodeTable.getOrDefault(hash, new ArrayList<>());
}

// 子ノード情報をクリアするメソッド（メモリ管理用）
public static void clearChildNodeTable() {
    childNodeTable.clear();
    System.gc(); // メモリ解放を促進（オプション）
}

// 特定の親ノードの子ノード情報のみを削除
public static void removeChildNodes(long parentHash) {
    childNodeTable.remove(parentHash);
}

// 子ノード情報を効率的に更新するメソッド
private void updateChildNodeEvaluation(OurBitBoard parentBoard, Move move, float newEvaluation) {
    long parentHash = parentBoard.hash();
    List<ChildNodeRecord> children = childNodeTable.get(parentHash);
    
    if (children != null) {
        for (ChildNodeRecord child : children) {
            if (child.move.equals(move)) {
                child.evaluation = newEvaluation;
                break;
            }
        }
    }
}

// 評価済み子ノード情報を保持するクラス
static class EvaluatedChildNode {
    Move move;
    OurBitBoard board;     // 既に生成済みの盤面
    OurBoard encodedBoard; // 既にエンコード済みの盤面
    float evaluation;      // 既に計算済みの評価値
    
    EvaluatedChildNode(Move move, OurBitBoard board, OurBoard encoded, float eval) {
        this.move = move;
        this.board = board;
        this.encodedBoard = encoded;
        this.evaluation = eval;
    }
}

// 子ノードの事前計算（1回だけ実行）
private List<EvaluatedChildNode> precomputeChildren(OurBitBoard parentBoard, List<Move> moves) {
    List<EvaluatedChildNode> children = new ArrayList<>();
    
    for (Move move : moves) {
        OurBitBoard childBoard = parentBoard.placed(move);
        // encode()を使わずに直接BitBoardで評価
        float evaluation = this.eval.value(childBoard);
        
        children.add(new EvaluatedChildNode(move, childBoard, null, evaluation));
    }
    
    // 評価値でソート
    children.sort((a, b) -> Float.compare(b.evaluation, a.evaluation));
    return children;
}
}