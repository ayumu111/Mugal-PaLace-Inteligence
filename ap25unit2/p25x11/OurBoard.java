package p25x11;

import static ap25.Color.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import ap25.*;

public class OurBoard implements Board, Cloneable {
  Color board[]; // 盤面の状態を保持する配列
  Move move = Move.ofPass(NONE); // 直前の手

  public OurBoard() {
    this.board = Stream.generate(() -> NONE).limit(LENGTH).toArray(Color[]::new);
    init(); // 新しい盤面を初期化
  } // コンストラクタ
  // 初期化時に盤面を生成する関数を持つ

  OurBoard(Color board[], Move move) {
    this.board = Arrays.copyOf(board, board.length);
    this.move = move;
  } // コンストラクタ（オーバーロード）

  public OurBoard clone() {
    return new OurBoard(this.board, this.move);
  } // クローンメソッド

  void init() {
    set(Move.parseIndex("c3"), BLACK);
    set(Move.parseIndex("d4"), BLACK);
    set(Move.parseIndex("d3"), WHITE);
    set(Move.parseIndex("c4"), WHITE);
  } // 起動時の盤面設定メソッド

  public Color get(int k) { return this.board[k]; } // 指定位置の色を取得
  public Move getMove() { return this.move; } // 直前の手を取得

  public Color getTurn() {
    return this.move.isNone() ? BLACK : this.move.getColor().flipped();
  } // 現在の手番を取得

  public void set(int k, Color color) {
    this.board[k] = color;
  } // 指定位置に色を設定

  public boolean equals(Object otherObj) {
    if (otherObj instanceof OurBoard) {
      var other = (OurBoard) otherObj;
      return Arrays.equals(this.board, other.board);
    }
    return false;
  } // 盤面の等価判定

  public String toString() {
    return OurBoardFormatter.format(this);
  } // 盤面を文字列化

  public int count(Color color) {
    return countAll().getOrDefault(color, 0L).intValue();
  } // 指定色の石の数をカウント

  public boolean isEnd() {
    var lbs = findNoPassLegalIndexes(BLACK);
    var lws = findNoPassLegalIndexes(WHITE);
    return lbs.size() == 0 && lws.size() == 0;
  } // ゲーム終了判定

  public Color winner() {
    var v = score();
    if (isEnd() == false || v == 0 ) return NONE;
    return v > 0 ? BLACK : WHITE;
  } // 勝者を返す

  public void foul(Color color) {
    var winner = color.flipped();
    IntStream.range(0, LENGTH).forEach(k -> this.board[k] = winner);
  } // 反則時の処理（相手の勝ちにする）

  public int score() {
    var cs = countAll();
    var bs = cs.getOrDefault(BLACK, 0L);
    var ws = cs.getOrDefault(WHITE, 0L);
    var ns = LENGTH - bs - ws;
    int score = (int) (bs - ws);

    if (bs == 0 || ws == 0)
        score += Integer.signum(score) * ns;

    return score;
  } // スコア計算

  Map<Color, Long> countAll() {
    return Arrays.stream(this.board).collect(
        Collectors.groupingBy(Function.identity(), Collectors.counting()));
  } // 全色の石の数をカウント

  public List<Move> findLegalMoves(Color color) {
    return findLegalIndexes(color).stream()
        .map(k -> new Move(k, color)).toList();
  } // 合法手のリストを返す

  List<Integer> findLegalIndexes(Color color) {
    var moves = findNoPassLegalIndexes(color);
    if (moves.size() == 0) moves.add(Move.PASS);
    return moves;
  } // 合法手（パス含む）のインデックスリスト

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
  } // 合法手（パス以外）のインデックスリスト

  List<List<Integer>> lines(int k) {
    var lines = new ArrayList<List<Integer>>();
    for (int dir = 0; dir < 8; dir++) {
      var line = Move.line(k, dir);
      lines.add(line);
    }
    return lines;
  } // 指定位置から8方向のラインを取得

  List<Move> outflanked(List<Integer> line, Color color) {
    if (line.size() <= 1) return new ArrayList<Move>();
    var flippables = new ArrayList<Move>();
    for (int k: line) {
      var c = get(k);
      if (c == NONE || c == BLOCK) break;
      if (c == color) return flippables;
      flippables.add(new Move(k, color));
    }
    return new ArrayList<Move>();
  } // 挟んでひっくり返せる石のリストを返す

    public long getBitBoard(Color color) {
    long bitBoard = 0L;
    for (int i = 0; i < LENGTH; i++) {
      if (this.board[i] == color) {
        bitBoard |= (1L << i);
      }
    }
    return bitBoard;
  }

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
  } // 指定手を打った後の盤面を返す

  public OurBoard flipped() {
    var b = clone();
    IntStream.range(0, LENGTH).forEach(k -> b.board[k] = b.board[k].flipped());
    b.move = this.move.flipped();
    return b;
  } // 盤面と手番を反転した盤面を返す
}
