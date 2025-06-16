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

  // MyEvalクラス内に追加
  public float valueBitBoard(OurBitBoard bitBoard) {
    // 終了判定
    if (bitBoard.isEnd()) {
        return 1000000 * scoreBitBoard(bitBoard);
    }
    
    // BitBoardから直接重み付きスコアを計算
    float psi = scoreBitBoard(bitBoard);
    
    // 合法手数の計算（BitBoardから直接）
    int lb = bitBoard.findLegalMoves(BLACK).size();
    int lw = bitBoard.findLegalMoves(WHITE).size();
    
    // 石数の計算（BitBoardから直接）
    int nb = countStones(bitBoard.getBitBoardBlack());
    int nw = countStones(bitBoard.getBitBoardWhite());
    
    // 重み配列探索用
    float w1 = 1;
    float w2 = 0;
    float w3 = 0;
    float w4 = 0;
    float w5 = 0;
    
    return w1*psi + w2*nb + w3*nw + w4*lb + w5*lw;
  }

  // BitBoardから直接スコアを計算
  private float scoreBitBoard(OurBitBoard bitBoard) {
    float score = 0;
    long blackBoard = bitBoard.getBitBoardBlack();
    long whiteBoard = bitBoard.getBitBoardWhite();
    
    // 石数から進行状況を判定
    int stoneCount = countStones(blackBoard) + countStones(whiteBoard);
    float[][] weights = getWeightsByStoneCount(stoneCount);
    
    // 各マスの重み付きスコアを計算
    for (int i = 0; i < 36; i++) {
        long bit = 1L << i;
        int row = i / 6;
        int col = i % 6;
        
        if ((blackBoard & bit) != 0) {
            score += weights[row][col]; // 黒石
        } else if ((whiteBoard & bit) != 0) {
            score -= weights[row][col]; // 白石
        }
    }
    
    return score;
  }

  // BitBoardから石数をカウント
  private int countStones(long bitBoard) {
    return Long.bitCount(bitBoard);
  }

  // 石数から重み配列を取得
  private float[][] getWeightsByStoneCount(int stoneCount) {
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
        float v = this.eval.value(board.encode());
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
      return this.eval.value(board.encode());
    }

    var moves = board.findLegalMoves(WHITE);
    if (moves.isEmpty()) {
        return this.eval.value(board.encode());
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
          float v = this.eval.value(board.encode());
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
        float childEval = this.eval.value(childBoard.encode());
        
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
        float evaluation = this.eval.valueBitBoard(childBoard);
        
        children.add(new EvaluatedChildNode(move, childBoard, null, evaluation));
    }
    
    // 評価値でソート
    children.sort((a, b) -> Float.compare(b.evaluation, a.evaluation));
    return children;
}
}