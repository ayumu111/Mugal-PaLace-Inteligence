package p25x01;

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
  static final float[][] M_LATE = {
      { 50,  20, 20, 20,  20,  50},
      { 20,   0,  5,  5,   0,  20},
      { 20,   5,  5,  5,   5,  20},
      { 20,   5,  5,  5,   5,  20},
      { 20,   0,  5,  5,   0,  20},
      { 50,  20, 20, 20,  20,  50},
  };

  // 評価関数：ゲームが終了していればスコア×1000000、そうでなければ各マスごとの合計
  public float value(OurBitBoard board) {
    if (board.isEnd()) return 1000000 * board.score();

    // 黒と白の石の数に応じてスコアを調整
    int nb = board.count(Color.BLACK);
    int nw = board.count(Color.WHITE);

    float psi = (float) IntStream.range(0, LENGTH)
    .mapToDouble(k -> score(board, k, nb + nw))
    .reduce(Double::sum).orElse(0);

    int lb = board.findLegalMoves(BLACK).size();
    int lw = board.findLegalMoves(WHITE).size();

    float w1 = getw1(nb + nw); 
    float w2 = getw2(nb + nw); 
    float w3 = getw3(nb + nw);
    float w4 = getw4(nb + nw);
    float w5 = getw5(nb + nw);

    float value = w1*psi + w2*nb + w3*nw + w4*lb + w5*lw;
    // デバッグ用出力
    System.out.println("[MyEval.value] psi=" + psi + ", nb=" + nb + ", nw=" + nw + ", lb=" + lb + ", lw=" + lw +"\n"+
                       "w1=" + w1 + ", w2=" + w2 + ", w3=" + w3 + ", w4=" + w4 + ", w5=" + w5 + "\n" +
                       "value=" + value);
    return value; // 黒番ならプラス、白番ならマイナス
  }

  float getw1(int stoneCount) {
    if (stoneCount < 12) return 100; // 序盤
    if (stoneCount < 24) return 50; // 中盤
    return 10; // 終盤
  }

  float getw2(int stoneCount) {
    if (stoneCount < 12) return 10; // 序盤
    if (stoneCount < 24) return 20; // 中盤
    return 1; // 終盤
  }
  float getw3(int stoneCount) {
    if (stoneCount < 12) return -10; // 序盤
    if (stoneCount < 24) return -20; // 中盤
    return -1; // 終盤
  }
  float getw4(int stoneCount) {
    if (stoneCount < 12) return 1; // 序盤
    if (stoneCount < 24) return 5; // 中盤
    return 100; // 終盤
  }
  float getw5(int stoneCount) {
    if (stoneCount < 12) return -1; // 序盤
    if (stoneCount < 24) return -5; // 中盤
    return -100; // 終盤
  }
  // 進行状況に応じて重み配列を返す
  float[][] getM(int stoneCount) {
    if (stoneCount < 12) return M_EARLY; // 序盤
    if (stoneCount < 24) return M_MIDDLE; // 中盤
    return M_LATE; // 終盤
  }

  // 特定のマス（k）のスコア計算：盤面の色値×重み
  float score(Board board, int k, int stoneCount) {
    float[][] M = getM(stoneCount);
    return M[k / SIZE][k % SIZE] * board.get(k).getValue();
  }

    // OurBitBoardとintを引数に取るscore関数
float score(OurBitBoard bitBoard, int k, int stoneCount) {
    float[][] M = getM(stoneCount); // 既存のgetMを流用
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
  static final String MY_NAME = "NotBit";
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
    float bestValue = (float)best[1];

    // 選択した手・評価値・盤面を表示
    System.out.println("[選択手] " + this.move);
    System.out.println("[評価値] " + bestValue);
    System.out.println("[盤面]\n" + this.board);


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