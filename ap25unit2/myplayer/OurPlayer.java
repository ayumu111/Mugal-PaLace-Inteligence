package Ourplayer;

import static ap25.Board.*;
import static ap25.Color.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

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
    static final float[][] M_MIDDLE = {
        { 30, 12, 12, 12, 12, 30 },
        { 12, -8, 2, 2, -8, 12 },
        { 12, 2, 2, 2, 2, 12 },
        { 12, 2, 2, 2, 2, 12 },
        { 12, -8, 2, 2, -8, 12 },
        { 30, 12, 12, 12, 12, 30 },
    };
    static final float[][] M_LATE = {
        { 50, 20, 20, 20, 20, 50 },
        { 20, 0, 5, 5, 0, 20 },
        { 20, 5, 5, 5, 5, 20 },
        { 20, 5, 5, 5, 5, 20 },
        { 20, 0, 5, 5, 0, 20 },
        { 50, 20, 20, 20, 20, 50 },
    };

    // 評価関数：ゲームが終了していればスコア×1000000、そうでなければ各マスごとの合計
    public float value(OurBitBoard board) {
        if (board.isEnd()) return 1000000 * board.score();
        float psi = (float) IntStream.range(0, LENGTH)
            .mapToDouble(k -> score(board, k))
            .reduce(Double::sum).orElse(0);

        int lb = board.findLegalMoves(BLACK).size();
        int lw = board.findLegalMoves(WHITE).size();

        // 黒と白の石の数に応じてスコアを調整
        int nb = board.count(BLACK);
        int nw = board.count(WHITE);

        float w1 = getw1(board);
        float w2 = getw2(board);
        float w3 = getw3(board);
        float w4 = getw4(board);
        float w5 = getw5(board);

        return w1 * psi + w2 * nb + w3 * nw + w4 * lb + w5 * lw; // 黒番ならプラス、白番ならマイナス
    }

    float getw1(OurBitBoard board) {
        int stoneCount = board.count(BLACK) + board.count(WHITE);
        if (stoneCount < 12) return 100; // 序盤
        if (stoneCount < 24) return 50; // 中盤
        return 10; // 終盤
    }

    float getw2(OurBitBoard board) {
        int stoneCount = board.count(BLACK) + board.count(WHITE);
        if (stoneCount < 12) return 10; // 序盤
        if (stoneCount < 24) return 20; // 中盤
        return 1; // 終盤
    }

    float getw3(OurBitBoard board) {
        int stoneCount = board.count(BLACK) + board.count(WHITE);
        if (stoneCount < 12) return -10; // 序盤
        if (stoneCount < 24) return -20; // 中盤
        return -1; // 終盤
    }

    float getw4(OurBitBoard board) {
        int stoneCount = board.count(BLACK) + board.count(WHITE);
        if (stoneCount < 12) return 1; // 序盤
        if (stoneCount < 24) return 5; // 中盤
        return 100; // 終盤
    }

    float getw5(OurBitBoard board) {
        int stoneCount = board.count(BLACK) + board.count(WHITE);
        if (stoneCount < 12) return -1; // 序盤
        if (stoneCount < 24) return -5; // 中盤
        return -100; // 終盤
    }

    // 進行状況に応じて重み配列を返す
    float[][] getM(OurBitBoard board) {
        int stoneCount = board.count(BLACK) + board.count(WHITE);
        if (stoneCount < 12) return M_EARLY; // 序盤
        if (stoneCount < 24) return M_MIDDLE; // 中盤
        return M_LATE; // 終盤
    }

    // 特定のマス（k）のスコア計算：盤面の色値×重み
    float score(OurBitBoard board, int k) {
        float[][] M = getM(board);
        return M[k / SIZE][k % SIZE] * board.get(k).getValue();
    }
}

public class OurPlayer extends ap25.Player {
    static final String Our_NAME = "Our25";
    MyEval eval; // 評価関数
    int depthLimit; // 探索の最大深さ
    Move move; // 選んだ手
    OurBitBoard board; // 内部的に使うボード状態（OurBitBoard型）

    public OurPlayer(Color color) {
        this(Our_NAME, color, new MyEval(), 4);
    }

    public OurPlayer(String name, Color color, MyEval eval, int depthLimit) {
        super(name, color);
        this.eval = eval;
        this.depthLimit = depthLimit;
        this.board = new OurBitBoard();
    }

    public OurPlayer(String name, Color color, int depthLimit) {
        this(name, color, new MyEval(), depthLimit);
    }

    public void setBoard(Board board) {
        this.board = new OurBitBoard(board);
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

            // 並列で各手の評価値を計算
            var results = moves.parallelStream()
                .map(move -> {
                    var nextBoard = this.board.placed(move);
                    float value = minSearch(nextBoard, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY, 1);
                    return new Object[] { move, value };
                })
                .toList();

            // 最大値を持つ手を選ぶ
            var best = results.stream().max((a, b) -> Float.compare((float) a[1], (float) b[1])).orElse(null);
            this.move = ((Move) best[0]).colored(getColor());
        }

        // 自分の着手を盤面に反映
        this.board = this.board.placed(this.move);
        return this.move;
    }

    ////////// ミニマックス法だよ！
    float maxSearch(OurBitBoard board, float alpha, float beta, int depth) {
        if (isTerminal(board, depth)) return this.eval.value(board);

        var moves = board.findLegalMoves(BLACK);
        moves = order(moves); // 手の順番をランダムにシャッフル（枝刈り効果向上）

        if (depth == 0 && !moves.isEmpty())
            this.move = moves.get(0); // 最上位では候補として仮に最初の手を選ぶ

        for (var move : moves) {
            var newBoard = board.placed(move);
            float v = minSearch(newBoard, alpha, beta, depth + 1);

            if (v > alpha) {
                alpha = v;
                if (depth == 0)
                    this.move = move; // 最良手を更新
            }

            if (alpha >= beta) // 枝刈り条件
                break;
        }

        return alpha;
    }

    float minSearch(OurBitBoard board, float alpha, float beta, int depth) {
        if (isTerminal(board, depth)) return this.eval.value(board);

        var moves = board.findLegalMoves(WHITE);
        moves = order(moves);

        for (var move : moves) {
            var newBoard = board.placed(move);
            float v = maxSearch(newBoard, alpha, beta, depth + 1);
            beta = Math.min(beta, v);
            if (alpha >= beta) break;
        }

        return beta;
    }
    ////////// ミニマックス法終わり

    // ゲーム終了または深さ制限
    boolean isTerminal(OurBitBoard board, int depth) {
        return board.isEnd() || depth > this.depthLimit;
    }

    // 手をランダムに並び替える（単純なmove ordering）
    List<Move> order(List<Move> moves) {
        var shuffled = new ArrayList<Move>(moves);
        Collections.shuffle(shuffled);
        return shuffled;
    }
}