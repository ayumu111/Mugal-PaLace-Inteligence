package enemy3;

import static ap25.Board.SIZE;
import static ap25.Color.BLACK;
import static ap25.Color.NONE;
import static ap25.Color.WHITE;
import ap25.Move;
import java.util.List;
import java.util.Map;

// 盤面を整形して文字列として返すクラス
public class OurBoardFormatter {
  // OurBoardの状態を文字列として整形して返すメソッド
  public static String format(OurBoard board) {
    var turn = board.getTurn(); // 現在の手番
    var move = board.getMove(); // 直前の手
    var blacks = board.findNoPassLegalIndexes(BLACK); // 黒の合法手（パス以外）
    var whites = board.findNoPassLegalIndexes(WHITE); // 白の合法手（パス以外）
    var legals = Map.of(BLACK, blacks, WHITE, whites); // 色ごとの合法手リスト

    var buf = new StringBuilder("  ");
    // 列ラベル（a〜f）を追加
    for (int k = 0; k < SIZE; k++) buf.append(Move.toColString(k));
    buf.append("\n");

    // 盤面を1マスずつ文字列化
    for (int k = 0; k < SIZE * SIZE; k++) {
      int col = k % SIZE;
      int row = k / SIZE;

      if (col == 0) buf.append((row + 1) + "|"); // 行ラベル

      if (board.get(k) == NONE) {
        // 空きマスの場合、合法手なら'.'、そうでなければ空白
        boolean legal = false;
        var b = blacks.contains(k);
        var w = whites.contains(k);
        if (turn == BLACK && b) legal = true;
        if (turn == WHITE && w) legal = true;
        buf.append(legal ? '.' : ' ');
      } else {
        // 石がある場合は記号を表示（直前の手は大文字）
        var s = board.get(k).toString();
        if (move != null && k == move.getIndex()) s = s.toUpperCase();
        buf.append(s);
      }

      if (col == SIZE - 1) {
        buf.append("| ");
        // 1行目に直前の手、2行目に合法手リストを表示
        if (row == 0 && move != null) {
          buf.append(move);
        } else if (row == 1) {
          buf.append(turn + ": " + toString(legals.get(turn)));
        }
        buf.append("\n");
      }
    }

    buf.setLength(buf.length() - 1); // 最後の改行を削除
    return buf.toString();
  }

  // インデックスリストを座標文字列リストに変換
  static List<String> toString(List<Integer> moves) {
    return moves.stream().map(k -> Move.toIndexString(k)).toList();
  }
}
