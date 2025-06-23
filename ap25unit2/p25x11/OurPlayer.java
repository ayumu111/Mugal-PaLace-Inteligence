package p25x11;

import static ap25.Board.*;
import static ap25.Color.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ap25.*;

class MyEval {
    // 序盤・中盤・終盤用の重み配列
    static final float[][] M_EARLY = {
        { 20, 10, 10, 10, 10, 20 },
        { 10, -5, 1, 1, -5, 10 },
        { 10, 1, 1, 1, 1, 10 },
        { 10, 1, 1, 1, 1, 10 },
        { 10, -5, 1, 1, -5, 10 },
        { 20, 10, 10, 10, 10, 20 },
    };

    public float value(OurBitBoard board) {
        if (board.isEnd()) return 1000000 * board.score();
        return board.score(); // 簡易評価（詳細な評価関数は省略）
    }
}

public class OurPlayer extends ap25.Player {
    static final String MY_NAME = "OurPlayer";
    MyEval eval; // 評価関数
    int depthLimit; // 探索の最大深さ
    Move move; // 選んだ手
    OurBitBoard board; // 内部的に使うボード状態（OurBitBoard型）

    public OurPlayer(Color color) {
        this(MY_NAME, color, new MyEval(), 4);
    }

    public OurPlayer(String name, Color color, MyEval eval, int depthLimit) {
        super(name, color);
        this.eval = eval;
        this.depthLimit = depthLimit;
        this.board = new OurBitBoard(0L, 0L, 0L); // 初期化
    }

    public OurPlayer(String name, Color color, int depthLimit) {
        this(name, color, new MyEval(), depthLimit);
    }

    public void setBoard(Board board) {
        this.board = new OurBitBoard(board
    }

    boolean isBlack() {
        return getColor() == BLACK;
    }

    public Move think(Board board) {
        // 相手の着手を反映
        this.board = this.board.placed(board.getMove());

        // パスの場合（合法手なし）
        if (this.board.findLegalMoves(getColor()).isEmpty()) {
            this.move = Move.ofPass(getColor());
        } else {
            this.move = null;

            // 最上位の合法手リスト
            var moves = order(this.board.findLegalMoves(getColor()));

            // 各手の評価値を計算
            float bestValue = Float.NEGATIVE_INFINITY;
            for (var move : moves) {
                var nextBoard = this.board.placed(move);
                float value = minSearch(nextBoard, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY, 1);
                if (value > bestValue) {
                    bestValue = value;
                    this.move = move;
                }
            }
        }

        // 自分の着手を盤面に反映
        this.board = this.board.placed(this.move);
        return this.move;
    }

    ////////// ミニマックス法
    float maxSearch(OurBitBoard board, float alpha, float beta, int depth) {
        if (isTerminal(board, depth)) return this.eval.value(board);

        var moves = order(board.findLegalMoves(BLACK));

        for (var move : moves) {
            var newBoard = board.placed(move);
            float value = minSearch(newBoard, alpha, beta, depth + 1);
            alpha = Math.max(alpha, value);
            if (alpha >= beta) break; // 枝刈り
        }

        return alpha;
    }

    float minSearch(OurBitBoard board, float alpha, float beta, int depth) {
        if (isTerminal(board, depth)) return this.eval.value(board);

        var moves = order(board.findLegalMoves(WHITE));

        for (var move : moves) {
            var newBoard = board.placed(move);
            float value = maxSearch(newBoard, alpha, beta, depth + 1);
            beta = Math.min(beta, value);
            if (alpha >= beta) break; // 枝刈り
        }

        return beta;
    }
    ////////// ミニマックス法終わり

    // ゲーム終了または深さ制限
    boolean isTerminal(OurBitBoard board, int depth) {
        return board.isEnd() || depth >= this.depthLimit;
    }

    // 手をランダムに並び替える（単純なMove Ordering）
    List<Move> order(List<Move> moves) {
        var shuffled = new ArrayList<>(moves);
        Collections.shuffle(shuffled);
        return shuffled;
    }
}