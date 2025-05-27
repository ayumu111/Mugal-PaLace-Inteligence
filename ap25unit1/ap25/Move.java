// オセロの手を表すクラス。位置・色・特殊手（パス等）を管理
package ap25;

import static ap25.Board.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Move {
  public final static int PASS = -1; // パス
  final static int TIMEOUT = -10;    // 時間切れ
  final static int ILLEGAL = -20;    // 反則
  final static int ERROR = -30;      // エラー

  int index; // マスのインデックス
  Color color; // 手の色

  // インデックスと色からMove生成
  public static Move of(int index, Color color) {
    return new Move(index, color);
  }

  // 文字列（例: "○"）と色からMove生成
  public static Move of(String pos, Color color) {
    return new Move(parseIndex(pos), color);
  }

  // パスのMove生成
  public static Move ofPass(Color color) {
    return new Move(PASS, color);
  }

  // 時間切れのMove生成
  public static Move ofTimeout(Color color) {
    return new Move(TIMEOUT, color);
  }

  // 反則のMove生成
  public static Move ofIllegal(Color color) {
    return new Move(ILLEGAL, color);
  }

  // エラーのMove生成
  public static Move ofError(Color color) {
    return new Move(ERROR, color);
  }

  // コンストラクタ。インデックスと色を設定
  public Move(int index, Color color) {
    this.index = index;
    this.color = color;
  }

  // インデックス取得
  public int getIndex() { return this.index; }
  // 行番号取得
  public int getRow() { return this.index / SIZE; }
  // 列番号取得
  public int getCol() { return this.index % SIZE; }
  // 色取得
  public Color getColor() { return this.color; }
  // ハッシュ値取得
  public int hashCode() { return Objects.hash(this.index, this.color); }

  // 等価判定
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    Move other = (Move) obj;
    return this.index == other.index && this.color == other.color;
  }

  // 色がNONEか判定
  public boolean isNone() { return this.color == Color.NONE; }

  // 合法手か判定
  public boolean isLegal() { return this.index >= PASS; }
  // パスか判定
  public boolean isPass() { return this.index == PASS; }

  // 反則手か判定
  public boolean isFoul() { return this.index < PASS; }
  // 時間切れか判定
  public boolean isTimeout() { return this.index == TIMEOUT; }
  // 反則か判定（スペルミス: isIllega→isIllegalが正）
  public boolean isIllega() { return this.index == ILLEGAL; }
  // エラーか判定
  public boolean isError() { return this.index == ERROR; }

  // 色を反転したMoveを返す
  public Move flipped() {
    return new Move(this.index, this.color.flipped());
  }

  // 指定色でMoveを返す
  public Move colored(Color color) {
    return new Move(this.index, color);
  }

  // 盤面内か判定
  public static boolean isValid(int col, int row) {
    return 0 <= col && col < SIZE && 0 <= row && row < SIZE;
  }

  // 指定距離の8方向オフセットを返す
  static int[][] offsets(int dist) {
    return new int[][] {
      { -dist, 0 }, { -dist, dist }, { 0, dist }, { dist, dist },
      { dist, 0 }, { dist, -dist }, { 0, -dist }, { -dist, -dist } };
  }

  // 隣接マスのインデックスリストを返す
  public static List<Integer> adjacent(int k) {
    var ps = new ArrayList<Integer>();
    int col0 = k % SIZE, row0 = k / SIZE;

    for (var o : offsets(1)) {
      int col = col0 + o[0], row = row0 + o[1];
      if (Move.isValid(col, row)) ps.add(index(col, row));
    }

    return ps;
  }

  // 指定方向の直線上のインデックスリストを返す
  public static List<Integer> line(int k, int dir) {
    var line = new ArrayList<Integer>();
    int col0 = k % SIZE, row0 = k / SIZE;

    for (int dist = 1; dist < SIZE; dist++) {
      var o = offsets(dist)[dir];
      int col = col0 + o[0], row = row0 + o[1];
      if (Move.isValid(col, row) == false)
        break;
      line.add(index(col, row));
    }

    return line;
  }

  // 列・行からインデックスを計算
  public static int index(int col, int row) {
    return SIZE * row + col;
  }

  // Moveを文字列化（例: "a1"）
  public String toString() {
    return toIndexString(this.index);
  }

  // 文字列（例: "a1"）からインデックスを計算
  public static int parseIndex(String pos) {
    return SIZE * (pos.charAt(1) - '1') + pos.charAt(0) - 'a';
  }

  // インデックスから文字列（例: "a1"）に変換
  public static String toIndexString(int index) {
    if (index == PASS) return "..";
    if (index == TIMEOUT) return "@";
    return toColString(index % SIZE) + toRowString(index / SIZE);
  }

  // 列番号から文字列（例: 0→"a"）
  public static String toColString(int col) {
    return Character.toString('a' + col);
  }

  // 行番号から文字列（例: 0→"1"）
  public static String toRowString(int row) {
    return Character.toString('1' + row);
  }

  // インデックスリストを文字列リストに変換
  public static List<String> toStringList(List<Integer> moves) {
    return moves.stream().map(k -> toIndexString(k)).toList();
  }
}
