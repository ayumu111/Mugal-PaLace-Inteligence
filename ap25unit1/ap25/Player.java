// プレイヤーの抽象クラス。名前・色・盤面を保持し、思考メソッドを持つ
package ap25;

public abstract class Player {
  String name; // プレイヤー名
  Color color; // プレイヤーの色
  Board board; // プレイヤーが操作する盤面

  // コンストラクタ。名前と色を設定
  public Player(String name, Color color) {
    this.name = name;
    this.color = color;
  }

  // 盤面を設定する
  public void setBoard(Board board) { this.board = board; }
  // プレイヤーの色を取得する
  public Color getColor() { return this.color; }
  // プレイヤー名を文字列で返す
  public String toString() { return this.name; }
  // プレイヤーが手を考える（デフォルトはnullを返す）
  public Move think(Board board) { return null; }
}
