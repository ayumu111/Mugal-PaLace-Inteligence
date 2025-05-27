// マスの状態を表す列挙型。黒・白・空き・ブロックを定義
package ap25;

import java.util.Map;

public enum Color { //マスの状態を表す列挙型
  BLACK(1), // 黒の目
  WHITE(-1),  // 白の目
  NONE(0),  // 目がない
  BLOCK(3); // ブロック目(何かでふさがっている。特殊ルールのやつ？)

  // 各色に対応する記号
  static Map<Color, String> SYMBOLS =
      Map.of(BLACK, "o", WHITE, "x", NONE, " ", BLOCK, "#");  // 各色に対応する文字列を定義

  private int value;

  // コンストラクタ。色の値を設定
  private Color(int value) {  // コンストラクタ
    this.value = value;
  }

  // 色の値を取得する
  public int getValue() { // 色の値を取得する関数
    return this.value;
  }

  // 黒と白を反転させる
  public Color flipped() {  // 黒と白を反転させる関数
    switch (this) {
    case BLACK: return WHITE;
    case WHITE: return BLACK;
    default: return this;
    }
  }

  // 色を記号に変換
  public String toString() {  // 文字列に変換する関数
    return SYMBOLS.get(this);
  }

  // 記号からColorを取得
  public Color parse(String str) {  // 文字列をColorに変換する関数
    return Map.of("o", BLACK, "x" , WHITE).getOrDefault(str, NONE);
  }
}
