package p25x01;

import static ap25.Board.LENGTH;
import static ap25.Board.SIZE;
import static ap25.Color.BLACK;
import static ap25.Color.WHITE;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import ap25.Board;
import ap25.Color;
import ap25.Move;

class MyEval_8128 {
  static float[][] M = {
      { 100, -50, 25, 25, -50, 100 },
      { -50, -25, 1, 1, -25, -50 },
      { 25, 1, 1, 1, 1, 25 },
      { 25, 1, 1, 1, 1, 25 },
      { -50, -25, 1, 1, -25, -50 },
      { 100, -50, 25, 25, -50, 100 },
  };

  public float value(Board board) {
    if (board.isEnd())
      return 1000000 * board.score();

    int piecesCount = IntStream.range(0, LENGTH)
        .map(i -> board.get(i).getValue() != 0 ? 1 : 0).sum();
    if (piecesCount > 50) {
      // 終盤は石数評価を重視し、全探索を促す
      return board.score() * 10000;
    } else if (piecesCount > 24) {
      return board.score() * 100;
    }

    // 通常評価 + 隅・安定石ボーナス
    float eval = 0;
    for (int k = 0; k < LENGTH; k++) {
      eval += score(board, k);
    }
    // 隅のボーナス
    int[] corners = { 0, SIZE - 1, LENGTH - SIZE, LENGTH - 1 };
    for (int idx : corners) {
      if (board.get(idx) == Color.BLACK)
        eval += 500;
      if (board.get(idx) == Color.WHITE)
        eval -= 500;
    }
    // 安定石ボーナス（簡易: 角とその隣）
    for (int idx : corners) {
      int[] dx = { 0, 1, 0, -1, 1, 1, -1, -1 };
      int[] dy = { 0, 0, 1, 1, -1, 1, -1, 1 };
      Color c = board.get(idx);
      if (c == Color.NONE || c == Color.BLOCK)
        continue;
      for (int d = 0; d < 8; d++) {
        int x = idx % SIZE + dx[d];
        int y = idx / SIZE + dy[d];
        int nidx = y * SIZE + x;
        if (0 <= x && x < SIZE && 0 <= y && y < SIZE && board.get(nidx) == c) {
          eval += (c == Color.BLACK ? 200 : -200);
        }
      }
    }
    return eval;
  }

  float score(Board board, int k) {
    return M[k / SIZE][k % SIZE] * board.get(k).getValue();
  }
}

public class OurPlayer extends ap25.Player {
  static final String MY_NAME = "8128";
  MyEval_8128 eval;
  int depthLimit;
  Move move;
  OurBoard board;

  public OurPlayer(Color color) {
    this(MY_NAME, color, new MyEval_8128(), 8);
  }

  public OurPlayer(String name, Color color, MyEval_8128 eval, int depthLimit) {
    super(name, color);
    this.eval = eval;
    this.depthLimit = depthLimit;
    this.board = new OurBoard();
  }

  public OurPlayer(String name, Color color, int depthLimit) {
    this(name, color, new MyEval_8128(), depthLimit);
  }

  public void setBoard(Board board) {
    this.board.setFromBoard(board);
  }

  boolean isBlack() {
    return getColor() == BLACK;
  }

  public Move think(Board board) {
    this.board = this.board.placed(board.getMove());

    List<Move> legalMoves = this.board.findLegalMoves(getColor());
    if (legalMoves.isEmpty()) {
      this.move = Move.ofPass(getColor());
    } else {
      var newBoard = isBlack() ? this.board.clone() : this.board.flipped();
      this.move = null;

      maxSearch(newBoard, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY, 0);

      this.move = this.move.colored(getColor());

      // 合法手でなければ最初の合法手を返す
      if (!legalMoves.contains(this.move)) {
        this.move = legalMoves.get(0);
      }
    }

    this.board = this.board.placed(this.move);
    return this.move;
  }

  float maxSearch(OurBoard board, float alpha, float beta, int depth) {
    if (isTerminal(board, depth))
      return this.eval.value(board);

    var moves = board.findLegalMoves(BLACK);
    moves = order(moves, board, BLACK);

    if (depth == 0)
      this.move = moves.get(0);

    for (var move : moves) {
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

  float minSearch(OurBoard board, float alpha, float beta, int depth) {
    if (isTerminal(board, depth))
      return this.eval.value(board);

    var moves = board.findLegalMoves(WHITE);
    moves = order(moves, board, WHITE);

    for (var move : moves) {
      var newBoard = board.placed(move);
      float v = maxSearch(newBoard, alpha, beta, depth + 1);
      beta = Math.min(beta, v);
      if (alpha >= beta)
        break;
    }

    return beta;
  }

  boolean isTerminal(Board board, int depth) {
    int piecesCount = IntStream.range(0, LENGTH)
        .map(i -> board.get(i).getValue() != 0 ? 1 : 0).sum();

    // 終盤は探索深さを大きくする
    int dynamicDepthLimit = piecesCount > 50 ? 20 : (piecesCount > 24 ? this.depthLimit + 3 : this.depthLimit);
    return board.isEnd() || depth > dynamicDepthLimit;
  }

  List<Move> order(List<Move> moves, OurBoard board, Color color) {
    // 評価値の高い順にソート
    moves.sort((a, b) -> {
      float va = eval.value(board.placed(a));
      float vb = eval.value(board.placed(b));
      // 黒番なら大きい順、白番なら小さい順
      return color == Color.BLACK ? Float.compare(vb, va) : Float.compare(va, vb);
    });
    return moves;
  }
}

// --- ここからビットボード実装 ---
class OurBoard implements ap25.Board, Cloneable {
  private long black;
  private long white;
  private long block; // ブロック用
  private int lastMove = -1;
  private Color lastColor = Color.NONE;

  public OurBoard() {
    // 初期配置
    int mid = SIZE / 2;
    set(mid * SIZE + mid - 1, Color.WHITE);
    set((mid - 1) * SIZE + mid, Color.WHITE);
    set((mid - 1) * SIZE + mid - 1, Color.BLACK);
    set(mid * SIZE + mid, Color.BLACK);
    // blockは初期化時は0
  }

  public OurBoard(long black, long white, long block, int lastMove, Color lastColor) {
    this.black = black;
    this.white = white;
    this.block = block;
    this.lastMove = lastMove;
    this.lastColor = lastColor;
  }

  @Override
  public OurBoard clone() {
    return new OurBoard(black, white, block, lastMove, lastColor);
  }

  public void set(int idx, Color c) {
    long mask = 1L << idx;
    if (c == Color.BLACK) {
      black |= mask;
      white &= ~mask;
      block &= ~mask;
    } else if (c == Color.WHITE) {
      white |= mask;
      black &= ~mask;
      block &= ~mask;
    } else if (c == Color.BLOCK) {
      block |= mask;
      black &= ~mask;
      white &= ~mask;
    } else {
      black &= ~mask;
      white &= ~mask;
      block &= ~mask;
    }
  }

  @Override
  public Color get(int idx) {
    long mask = 1L << idx;
    if ((block & mask) != 0)
      return Color.BLOCK;
    if ((black & mask) != 0)
      return Color.BLACK;
    if ((white & mask) != 0)
      return Color.WHITE;
    return Color.NONE;
  }

  @Override
  public OurBoard placed(Move move) {
    if (move.isPass()) {
      return new OurBoard(black, white, block, -1, move.getColor());
    }
    int idx = move.getIndex();
    Color color = move.getColor();
    long b = black, w = white, bl = block;
    long me = (color == Color.BLACK) ? b : w;
    long opp = (color == Color.BLACK) ? w : b;
    long mask = 1L << idx;
    // ブロック上には置けない
    if ((bl & mask) != 0) {
      return new OurBoard(b, w, bl, lastMove, lastColor); // 何も変化しない
    }
    me |= mask;
    for (int d = 0; d < 8; d++) {
      int dx = new int[] { -1, 0, 1, -1, 1, -1, 0, 1 }[d];
      int dy = new int[] { -1, -1, -1, 0, 0, 1, 1, 1 }[d];
      int x = idx % SIZE, y = idx / SIZE;
      int nx = x + dx, ny = y + dy;
      int nidx = ny * SIZE + nx;
      long flip = 0L;
      boolean found = false;
      while (0 <= nx && nx < SIZE && 0 <= ny && ny < SIZE) {
        long nmask = 1L << nidx;
        if ((bl & nmask) != 0)
          break; // ブロックで止まる
        if ((opp & nmask) != 0) {
          flip |= nmask;
        } else if ((me & nmask) != 0) {
          found = (flip != 0);
          break;
        } else {
          break;
        }
        nx += dx;
        ny += dy;
        nidx = ny * SIZE + nx;
      }
      if (found) {
        me |= flip;
        opp &= ~flip;
      }
    }
    if (color == Color.BLACK) {
      return new OurBoard(me, opp, bl, idx, color);
    } else {
      return new OurBoard(opp, me, bl, idx, color);
    }
  }

  @Override
  public OurBoard flipped() {
    return new OurBoard(white, black, block, lastMove, lastColor == Color.BLACK ? Color.WHITE : Color.BLACK);
  }

  @Override
  public int score() {
    return Long.bitCount(black) - Long.bitCount(white);
  }

  @Override
  public boolean isEnd() {
    // 両者合法手なし
    return findNoPassLegalIndexes(Color.BLACK).isEmpty() && findNoPassLegalIndexes(Color.WHITE).isEmpty();
  }

  @Override
  public Move getMove() {
    if (lastMove < 0)
      return Move.ofPass(lastColor);
    return Move.of(lastMove, lastColor);
  }

  @Override
  public Color getTurn() {
    return (lastColor == null || lastColor == Color.NONE) ? Color.BLACK : lastColor.flipped();
  }

  @Override
  public int count(Color color) {
    if (color == Color.BLACK)
      return Long.bitCount(black);
    if (color == Color.WHITE)
      return Long.bitCount(white);
    if (color == Color.BLOCK)
      return Long.bitCount(block);
    return LENGTH - Long.bitCount(black | white | block);
  }

  @Override
  public Color winner() {
    int v = score();
    if (!isEnd() || v == 0)
      return Color.NONE;
    return v > 0 ? Color.BLACK : Color.WHITE;
  }

  @Override
  public void foul(Color color) {
    // 反則時は全て相手色に
    Color winner = color.flipped();
    black = winner == Color.BLACK ? ~0L >>> (64 - LENGTH) : 0L;
    white = winner == Color.WHITE ? ~0L >>> (64 - LENGTH) : 0L;
    // blockはそのまま
  }

  @Override
  public List<Move> findLegalMoves(Color color) {
    List<Move> moves = new ArrayList<>();
    for (int i = 0; i < LENGTH; i++) {
      if (get(i) != Color.NONE)
        continue;
      if (canPut(i, color))
        moves.add(Move.of(i, color));
    }
    if (moves.isEmpty())
      moves.add(Move.ofPass(color));
    return moves;
  }

  public List<Integer> findNoPassLegalIndexes(Color color) {
    List<Integer> list = new ArrayList<>();
    for (int i = 0; i < LENGTH; i++) {
      if (get(i) != Color.NONE)
        continue;
      if (canPut(i, color))
        list.add(i);
    }
    return list;
  }

  private boolean canPut(int idx, Color color) {
    long b = black, w = white, bl = block;
    long me = (color == Color.BLACK) ? b : w;
    long opp = (color == Color.BLACK) ? w : b;
    long mask = 1L << idx;
    if ((bl & mask) != 0)
      return false; // ブロック上は不可
    int x = idx % SIZE, y = idx / SIZE;
    for (int d = 0; d < 8; d++) {
      int dx = new int[] { -1, 0, 1, -1, 1, -1, 0, 1 }[d];
      int dy = new int[] { -1, -1, -1, 0, 0, 1, 1, 1 }[d];
      int nx = x + dx, ny = y + dy;
      int nidx = ny * SIZE + nx;
      boolean found = false;
      boolean hasOpp = false;
      while (0 <= nx && nx < SIZE && 0 <= ny && ny < SIZE) {
        long nmask = 1L << nidx;
        if ((bl & nmask) != 0)
          break; // ブロックで止まる
        if ((opp & nmask) != 0) {
          hasOpp = true;
        } else if ((me & nmask) != 0) {
          found = hasOpp;
          break;
        } else {
          break;
        }
        nx += dx;
        ny += dy;
        nidx = ny * SIZE + nx;
      }
      if (found)
        return true;
    }
    return false;
  }

  // Boardからブロック情報もコピーする
  public void setFromBoard(Board board) {
    this.black = 0L;
    this.white = 0L;
    this.block = 0L;
    for (int i = 0; i < LENGTH; i++) {
      set(i, board.get(i));
    }
  }
}
// --- ここまでビットボード実装 ---