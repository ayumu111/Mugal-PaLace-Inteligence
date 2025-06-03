package p25x00;

import static ap25.Color.BLACK;
import static ap25.Color.BLOCK;
import static ap25.Color.NONE;
import static ap25.Color.WHITE;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import ap25.Board;
import ap25.Color;
import ap25.Move;

// 盤面クラス（OurBoard）: オセロの盤面状態を管理
public class OurBoard implements Board, Cloneable {
  Color board[]; // 盤面の状態を保持する配列
  Move move = Move.ofPass(NONE); // 直前の手

  // コンストラクタ（新規盤面を初期化）
  public OurBoard() {
    this.board = Stream.generate(() -> NONE).limit(LENGTH).toArray(Color[]::new);
    init(); // 初期配置
  }

  // コンストラクタ（盤面配列と直前の手をコピー）
  OurBoard(Color board[], Move move) {
    this.board = Arrays.copyOf(board, board.length);
    this.move = move;
  }

  // ディープコピーを返す
  public OurBoard clone() {
    return new OurBoard(this.board, this.move);
  }

  // 初期配置（中央4マスに石を置く）
  void init() {
    set(Move.parseIndex("c3"), BLACK);
    set(Move.parseIndex("d4"), BLACK);
    set(Move.parseIndex("d3"), WHITE);
    set(Move.parseIndex("c4"), WHITE);
  }

  // 指定インデックスの色を取得
  public Color get(int k) { return this.board[k]; }
  // 直前の手を取得
  public Move getMove() { return this.move; }

  // 現在の手番の色を取得
  public Color getTurn() {
    return this.move.isNone() ? BLACK : this.move.getColor().flipped();
  }

  // 指定インデックスに色をセット
  public void set(int k, Color color) {
    this.board[k] = color;
  }

  // 盤面の等価判定
  public boolean equals(Object otherObj) {
    if (otherObj instanceof OurBoard) {
      var other = (OurBoard) otherObj;
      return Arrays.equals(this.board, other.board);
    }
    return false;
  }

  // 盤面を文字列化
  public String toString() {
    return OurBoardFormatter.format(this);
  }

  // 指定色の石の数をカウント
  public int count(Color color) {
    return countAll().getOrDefault(color, 0L).intValue();
  }

  // ゲーム終了判定（両者合法手なし）
  public boolean isEnd() {
    var lbs = findNoPassLegalIndexes(BLACK);
    var lws = findNoPassLegalIndexes(WHITE);
    return lbs.size() == 0 && lws.size() == 0;
  }

  // 勝者の色を返す（引き分けはNONE）
  public Color winner() {
    var v = score();
    if (isEnd() == false || v == 0 ) return NONE;
    return v > 0 ? BLACK : WHITE;
  }

  // 反則時の処理（相手の勝ちにする）
  public void foul(Color color) {
    var winner = color.flipped();
    IntStream.range(0, LENGTH).forEach(k -> this.board[k] = winner);
  }

  // スコア計算（黒-白、全滅時は空きマス分加算）
  public int score() {
    var cs = countAll();
    var bs = cs.getOrDefault(BLACK, 0L);
    var ws = cs.getOrDefault(WHITE, 0L);
    var ns = LENGTH - bs - ws;
    int score = (int) (bs - ws);

    if (bs == 0 || ws == 0)
        score += Integer.signum(score) * ns;

    return score;
  }

  // 全色の石の数をカウント
  Map<Color, Long> countAll() {
    return Arrays.stream(this.board).collect(
        Collectors.groupingBy(Function.identity(), Collectors.counting()));
  }

  // 合法手（Move型）のリストを返す
  public List<Move> findLegalMoves(Color color) {
    return findLegalIndexes(color).stream()
        .map(k -> new Move(k, color)).toList();
  }

  // 合法手（パス含む）のインデックスリスト
  List<Integer> findLegalIndexes(Color color) {
    var moves = findNoPassLegalIndexes(color);
    if (moves.size() == 0) moves.add(Move.PASS);
    return moves;
  }

  // 合法手（パス以外）のインデックスリスト
  List<Integer> findNoPassLegalIndexes(Color color) {
    var moves = new ArrayList<Integer>();
    for (int k = 0; k < LENGTH; k++) {
      var c = this.board[k];
      if (c != NONE) continue;
      for (var line : lines(k)) {
        var outflanking = outflanked(line, color);
        if (outflanking.size() > 0) moves.add(k);
      }
    }
    return moves;
  }

  // 指定位置から8方向のラインを取得
  List<List<Integer>> lines(int k) {
    var lines = new ArrayList<List<Integer>>();
    for (int dir = 0; dir < 8; dir++) {
      var line = Move.line(k, dir);
      lines.add(line);
    }
    return lines;
  }

  // 挟んでひっくり返せる石のリストを返す
  List<Move> outflanked(List<Integer> line, Color color) {
    if (line.size() <= 1) return new ArrayList<>();
    var flippables = new ArrayList<Move>();
    for (int k: line) {
      var c = get(k);
      if (c == NONE || c == BLOCK) break;
      if (c == color) return flippables;
      flippables.add(new Move(k, color));
    }
    return new ArrayList<>();
  }

  // 指定手を打った後の盤面を返す
  public OurBoard placed(Move move) {
    var b = clone();
    b.move = move;

    if (move.isPass() | move.isNone())
      return b;

    var k = move.getIndex();
    var color = move.getColor();
    var lines = b.lines(k);
    for (var line: lines) {
      for (var p: outflanked(line, color)) {
        b.board[p.getIndex()] = color;
      }
    }
    b.set(k, color);

    return b;
  }

  // 盤面と手番を反転した盤面を返す
  public OurBoard flipped() {
    var b = clone();
    IntStream.range(0, LENGTH).forEach(k -> b.board[k] = b.board[k].flipped());
    b.move = this.move.flipped();
    return b;
  }

  public long getBitBoard(Color color) {
    long bitBoard = 0L;
    // for (int i = 0; i < LENGTH; i++) {
    //   if (this.board[i] == color) {
    //     bitBoard |= (1L << i);
    //   }
    // }
    return bitBoard;
  }
}
