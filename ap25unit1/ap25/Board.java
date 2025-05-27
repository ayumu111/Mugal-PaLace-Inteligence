// 盤面のインターフェース。オセロの盤面操作に必要なメソッドを定義
package ap25;

import java.util.List;

public interface Board {
  int SIZE = 6; // 盤の辺の長さ
  int LENGTH = SIZE * SIZE; // 盤のマスの数

  // 指定インデックスのマスの色を取得
  Color get(int k);
  // 直前の手を取得
  Move getMove();
  // 現在の手番の色を取得
  Color getTurn();
  // 指定色の石の数を数える
  int count(Color color);
  // ゲーム終了判定
  boolean isEnd();
  // 勝者の色を返す（引き分けはNONE）
  Color winner();
  // 反則を記録する
  void foul(Color color);
  // 現在のスコアを返す
  int score();
  // 指定色の合法手一覧を返す
  List<Move> findLegalMoves(Color color);
  // 指定手を打った後の盤面を返す
  Board placed(Move move);
  // 盤面を反転（色を入れ替え）した盤面を返す
  Board flipped();
  // 盤面のディープコピーを返す
  Board clone();
}
