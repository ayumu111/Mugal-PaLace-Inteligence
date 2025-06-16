package p25x01;

import static ap25.Board.LENGTH;
import static ap25.Board.SIZE;
import static ap25.Color.BLACK;
import static ap25.Color.WHITE;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import ap25.Board;
import ap25.Color;
import ap25.Move;

// 強化された評価関数
class EnhancedEval {
    static final float[][] M = {
            { 120, -40, 20, 20, -40, 120 },
            { -40, -60, -5, -5, -60, -40 },
            { 20, -5, 15, 15, -5, 20 },
            { 20, -5, 15, 15, -5, 20 },
            { -40, -60, -5, -5, -60, -40 },
            { 120, -40, 20, 20, -40, 120 },
    };

    public float value(Board board) {
        // 終局時のスコアを大幅に強調することで、勝利を優先する評価を実現
        if (board.isEnd()) {
            int score = board.score();
            return (score > 0) ? 10000 + score : -10000 + score;
        }

        // 盤面の価値を多角的に評価
        float sum = calculateBoardValue(board);
        sum += calculateMobilityValue(board);
        sum += calculateStabilityValue(board);
        sum += calculateCornerBonus(board);
        sum += calculateEdgeBonus(board);
        sum += calculateConfirmedStonesValue(board);

        return sum;
    }

    private float calculateBoardValue(Board board) {
        // 盤面の位置価値を評価
        float sum = 0;
        for (int k = 0; k < LENGTH; k++) {
            sum += M[k / SIZE][k % SIZE] * board.get(k).getValue();
        }
        return sum;
    }

    private float calculateMobilityValue(Board board) {
        // モビリティ（合法手の数）を評価
        int myMoves = board.findLegalMoves(BLACK).size();
        int oppMoves = board.findLegalMoves(WHITE).size();

        int empty = countEmptyCells(board);
        float mobilityWeight = (empty > 30) ? 8.0f : (empty > 20) ? 6.0f : 4.0f;
        return mobilityWeight * (myMoves - oppMoves);
    }

    private float calculateStabilityValue(Board board) {
        // 安定石の数を評価
        int myStable = countStableStones(board, BLACK);
        int oppStable = countStableStones(board, WHITE);
        int myBlockStable = countBlockAdjacentStableStones(board, BLACK);
        int oppBlockStable = countBlockAdjacentStableStones(board, WHITE);

        return 25 * (myStable - oppStable) + 15 * (myBlockStable - oppBlockStable);
    }

    private float calculateCornerBonus(Board board) {
        // 隅の位置を評価
        int[] corners = { 0, SIZE - 1, LENGTH - SIZE, LENGTH - 1 };
        float bonus = 0;
        for (int corner : corners) {
            Color c = board.get(corner);
            if (c == BLACK)
                bonus += 1000;
            if (c == WHITE)
                bonus -= 1000;
        }
        return bonus;
    }

    private float calculateEdgeBonus(Board board) {
        // 辺の位置を評価
        int[] edges = new int[4 * (SIZE - 2)];
        for (int i = 1; i < SIZE - 1; i++) {
            edges[i - 1] = i; // 上辺
            edges[SIZE - 2 + i - 1] = SIZE * (SIZE - 1) + i; // 下辺
            edges[2 * (SIZE - 2) + i - 1] = i * SIZE; // 左辺
            edges[3 * (SIZE - 2) + i - 1] = i * SIZE + SIZE - 1; // 右辺
        }
        float bonus = 0;
        for (int edge : edges) {
            Color c = board.get(edge);
            if (c == BLACK)
                bonus += (isStableEdge(edge, BLACK, board) ? 100 : 50);
            if (c == WHITE)
                bonus -= (isStableEdge(edge, WHITE, board) ? 100 : 50);
        }
        return bonus;
    }

    private boolean isStableEdge(int index, Color color, Board board) {
        // 辺の安定性を評価
        int[] directions = { -1, 1, -SIZE, SIZE };
        for (int dir : directions) {
            int adjacentIndex = index + dir;
            if (adjacentIndex >= 0 && adjacentIndex < LENGTH && board.get(adjacentIndex) != color) {
                return false;
            }
        }
        return true;
    }

    private float calculateConfirmedStonesValue(Board board) {
        // 確定石の数を評価
        int myConfirmedStones = countConfirmedStones(board, BLACK);
        int oppConfirmedStones = countConfirmedStones(board, WHITE);

        return 50 * (myConfirmedStones - oppConfirmedStones);
    }

    private int countConfirmedStones(Board board, Color color) {
        // 確定石を探索
        boolean[] visited = new boolean[LENGTH];
        int count = 0;

        int[] directions = { -1, 0, 1, -SIZE, SIZE, -SIZE - 1, -SIZE + 1, SIZE - 1, SIZE + 1 };
        int[] corners = { 0, SIZE - 1, LENGTH - SIZE, LENGTH - 1 };

        for (int corner : corners) {
            if (board.get(corner) == color) {
                count += exploreConfirmedStones(board, corner, color, visited, directions);
            }
        }

        for (int i = 0; i < LENGTH; i++) {
            if (board.get(i) == Color.BLOCK && !visited[i]) {
                count += exploreConfirmedStones(board, i, color, visited, directions);
            }
        }

        return count;
    }

    private int exploreConfirmedStones(Board board, int index, Color color, boolean[] visited, int[] directions) {
        // 確定石の探索を再帰的に実行
        if (visited[index] || board.get(index) != color) {
            return 0;
        }

        visited[index] = true;
        int count = 1;

        for (int dir : directions) {
            int adjacentIndex = index + dir;
            if (adjacentIndex >= 0 && adjacentIndex < LENGTH) {
                count += exploreConfirmedStones(board, adjacentIndex, color, visited, directions);
            }
        }

        return count;
    }

    private int countEmptyCells(Board board) {
        // 空きマスの数をカウント
        int empty = 0;
        for (int i = 0; i < LENGTH; i++) {
            if (board.get(i) == Color.NONE)
                empty++;
        }
        return empty;
    }

    private int countStableStones(Board board, Color color) {
        // 安定石の数をカウント
        int count = 0;
        int[] corners = { 0, SIZE - 1, LENGTH - SIZE, LENGTH - 1 };
        for (int corner : corners) {
            if (board.get(corner) == color)
                count++;
        }
        return count;
    }

    private int countBlockAdjacentStableStones(Board board, Color color) {
        // ブロックに隣接する安定石の数をカウント
        int count = 0;
        for (int i = 0; i < LENGTH; i++) {
            if (board.get(i) == Color.BLOCK) {
                count += countAdjacentStableStones(board, i, color);
            }
        }
        return count;
    }

    private int countAdjacentStableStones(Board board, int blockIndex, Color color) {
        // ブロックに隣接する安定石を探索
        int count = 0;
        int[] directions = { -1, 0, 1, -SIZE, SIZE, -SIZE - 1, -SIZE + 1, SIZE - 1, SIZE + 1 };
        for (int dir : directions) {
            int adjacentIndex = blockIndex + dir;
            if (adjacentIndex >= 0 && adjacentIndex < LENGTH && board.get(adjacentIndex) == color) {
                count++;
            }
        }
        return count;
    }
}

// OurPlayer3の改善点:
// 1. 評価関数の強化: EnhancedEvalを使用し、隅、辺、安定石、モビリティなどの評価を詳細化。
// 2. 探索深度の動的調整: determineSearchLimitメソッドで盤面の空きマス数に応じて探索深度を調整。
// 3. トランスポジションテーブルの導入: transTableを使用して探索結果をキャッシュし、計算効率を向上。
// 4. 合法手の優先度付け: prepareForSearchメソッドで合法手を優先度に基づいてソート。
// 5. ビットボードの使用: BitBoard3を使用して盤面を管理し、メモリ効率と操作速度を向上。
// 6. 探索アルゴリズムの改善: maxSearchとminSearchにトランスポジションテーブルを組み込み、探索結果をキャッシュ。
public class OurPlayer3 extends ap25.Player {
    static final String MY_NAME = "0028";
    private final EnhancedEval eval;
    private final int depthLimit;
    private Move move;
    private BitBoard3 board;
    private final Map<Long, Float> transTable;

    public OurPlayer3(Color color) {
        this(MY_NAME, color, new EnhancedEval(), 10);
    }

    public OurPlayer3(String name, Color color, EnhancedEval eval, int depthLimit) {
        super(name, color);
        this.eval = eval;
        this.depthLimit = depthLimit;
        this.board = new BitBoard3();
        this.transTable = new HashMap<>();
    }

    public void setBoard(Board board) {
        this.board.setFromBoard(board);
    }

    public Move think(Board board) {
        this.board = this.board.placed(board.getMove());

        int emptyCells = countEmptyCells();
        int searchLimit = determineSearchLimit(emptyCells);

        List<Move> legalMoves = this.board.findLegalMoves(getColor());
        if (legalMoves.isEmpty()) {
            this.move = Move.ofPass(getColor());
        } else {
            prepareForSearch(legalMoves);
            Board searchBoard = isBlack() ? this.board : this.board.flipped();
            maxSearch(searchBoard, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY, 0, searchLimit);
            this.move = this.move.colored(getColor());
        }

        this.board = this.board.placed(this.move);
        return this.move;
    }

    private int countEmptyCells() {
        int empty = 0;
        for (int i = 0; i < LENGTH; i++) {
            if (this.board.get(i) == Color.NONE)
                empty++;
        }
        return empty;
    }

    private int determineSearchLimit(int emptyCells) {
        // 探索深度を盤面の空きマス数に応じて調整
        if (emptyCells <= 15) {
            return emptyCells;
        } else if (emptyCells <= 20) {
            return this.depthLimit + 2;
        } else {
            return this.depthLimit;
        }
    }

    private void prepareForSearch(List<Move> legalMoves) {
        // トランスポジションテーブルをクリア
        transTable.clear();

        // 優先度に基づいて合法手をソート
        legalMoves.sort((a, b) -> {
            int priorityA = movePriority(a);
            int priorityB = movePriority(b);
            return Integer.compare(priorityB, priorityA);
        });
    }

    private int movePriority(Move move) {
        int position = move.getIndex();
        if (isCorner(position)) {
            return 100; // 角の位置は最優先
        } else if (isEdge(position)) {
            return isStableEdge(position, getColor(), board) ? 80 : 60; // 安定した辺の位置は高評価
        } else {
            return 20; // その他の位置
        }
    }

    private boolean isCorner(int position) {
        return position == 0 || position == SIZE - 1 || position == LENGTH - SIZE || position == LENGTH - 1;
    }

    private boolean isEdge(int position) {
        return position / SIZE == 0 || position / SIZE == SIZE - 1 || position % SIZE == 0
                || position % SIZE == SIZE - 1;
    }

    private float maxSearch(Board board, float alpha, float beta, int depth, int searchLimit) {
        long hash = board.hashCode();
        if (transTable.containsKey(hash)) {
            return transTable.get(hash);
        }

        if (board.isEnd() || depth >= searchLimit) {
            float value = this.eval.value(board);
            transTable.put(hash, value);
            return value;
        }

        List<Move> moves = board.findLegalMoves(BLACK);
        if (moves.isEmpty()) {
            float value = minSearch(board, alpha, beta, depth + 1, searchLimit);
            transTable.put(hash, value);
            return value;
        }

        moves.sort((a, b) -> Integer.compare(movePriority(b), movePriority(a)));

        float best = alpha;
        for (Move move : moves) {
            Board newBoard = board.placed(move);
            float value = minSearch(newBoard, best, beta, depth + 1, searchLimit);

            if (value > best) {
                best = value;
                if (depth == 0) {
                    this.move = move;
                }
            }

            if (best >= beta) {
                break;
            }
        }
        transTable.put(hash, best);
        return best;
    }

    private float minSearch(Board board, float alpha, float beta, int depth, int searchLimit) {
        long hash = board.hashCode() ^ 0x7FFFFFFFL;
        if (transTable.containsKey(hash)) {
            return transTable.get(hash);
        }

        if (board.isEnd() || depth >= searchLimit) {
            float value = this.eval.value(board);
            transTable.put(hash, value);
            return value;
        }

        List<Move> moves = board.findLegalMoves(WHITE);
        if (moves.isEmpty()) {
            float value = maxSearch(board, alpha, beta, depth + 1, searchLimit);
            transTable.put(hash, value);
            return value;
        }

        moves.sort((a, b) -> Integer.compare(movePriority(b), movePriority(a)));

        float best = beta;
        for (Move move : moves) {
            Board newBoard = board.placed(move);
            float value = maxSearch(newBoard, alpha, best, depth + 1, searchLimit);
            best = Math.min(best, value);

            if (alpha >= best) {
                break;
            }
        }
        transTable.put(hash, best);
        return best;
    }

    private boolean isBlack() {
        return getColor() == BLACK;
    }

    private boolean isStableEdge(int index, Color color, BitBoard3 board) {
        int[] directions = { -1, 1, -SIZE, SIZE };
        for (int dir : directions) {
            int adjacentIndex = index + dir;
            if (adjacentIndex >= 0 && adjacentIndex < LENGTH && board.get(adjacentIndex) != color) {
                return false;
            }
        }
        return true;
    }
}

// ビットボード実装
class BitBoard3 implements ap25.Board, Cloneable {
    private long blackBits;
    private long whiteBits;
    private long blockBits;
    private int lastMoveIndex = -1;
    private Color lastMoveColor = Color.NONE;

    public BitBoard3() {
        int mid = SIZE / 2;
        set(mid * SIZE + mid - 1, Color.WHITE);
        set((mid - 1) * SIZE + mid, Color.WHITE);
        set((mid - 1) * SIZE + mid - 1, Color.BLACK);
        set(mid * SIZE + mid, Color.BLACK);
    }

    public BitBoard3(long blackBits, long whiteBits, long blockBits, int lastMoveIndex, Color lastMoveColor) {
        this.blackBits = blackBits;
        this.whiteBits = whiteBits;
        this.blockBits = blockBits;
        this.lastMoveIndex = lastMoveIndex;
        this.lastMoveColor = lastMoveColor;
    }

    @Override
    public BitBoard3 clone() {
        return new BitBoard3(blackBits, whiteBits, blockBits, lastMoveIndex, lastMoveColor);
    }

    public void set(int index, Color color) {
        long mask = 1L << index;
        if (color == Color.BLACK) {
            blackBits |= mask;
            whiteBits &= ~mask;
            blockBits &= ~mask;
        } else if (color == Color.WHITE) {
            whiteBits |= mask;
            blackBits &= ~mask;
            blockBits &= ~mask;
        } else if (color == Color.BLOCK) {
            blockBits |= mask;
            blackBits &= ~mask;
            whiteBits &= ~mask;
        } else {
            blackBits &= ~mask;
            whiteBits &= ~mask;
            blockBits &= ~mask;
        }
    }

    @Override
    public Color get(int index) {
        long mask = 1L << index;
        if ((blockBits & mask) != 0)
            return Color.BLOCK;
        if ((blackBits & mask) != 0)
            return Color.BLACK;
        if ((whiteBits & mask) != 0)
            return Color.WHITE;
        return Color.NONE;
    }

    @Override
    public BitBoard3 placed(Move move) {
        if (move.isPass()) {
            return new BitBoard3(blackBits, whiteBits, blockBits, -1, move.getColor());
        }
        int index = move.getIndex();
        Color color = move.getColor();
        long myBits = (color == Color.BLACK) ? blackBits : whiteBits;
        long oppBits = (color == Color.BLACK) ? whiteBits : blackBits;
        long mask = 1L << index;
        myBits |= mask;
        for (int d = 0; d < 8; d++) {
            int dx = new int[] { -1, 0, 1, -1, 1, -1, 0, 1 }[d];
            int dy = new int[] { -1, -1, -1, 0, 0, 1, 1, 1 }[d];
            int x = index % SIZE, y = index / SIZE;
            int nx = x + dx, ny = y + dy;
            int nIndex = ny * SIZE + nx;
            long flipBits = 0L;
            boolean found = false;
            while (0 <= nx && nx < SIZE && 0 <= ny && ny < SIZE) {
                long nMask = 1L << nIndex;
                if ((blockBits & nMask) != 0)
                    break;
                if ((oppBits & nMask) != 0) {
                    flipBits |= nMask;
                } else if ((myBits & nMask) != 0) {
                    found = true;
                    break;
                } else {
                    break;
                }
                nx += dx;
                ny += dy;
                nIndex = ny * SIZE + nx;
            }
            if (found) {
                myBits |= flipBits;
                oppBits &= ~flipBits;
            }
        }
        if (color == Color.BLACK) {
            return new BitBoard3(myBits, oppBits, blockBits, index, color);
        } else {
            return new BitBoard3(oppBits, myBits, blockBits, index, color);
        }
    }

    @Override
    public BitBoard3 flipped() {
        return new BitBoard3(whiteBits, blackBits, blockBits, lastMoveIndex, lastMoveColor.flipped());
    }

    @Override
    public int score() {
        return Long.bitCount(blackBits) - Long.bitCount(whiteBits);
    }

    @Override
    public boolean isEnd() {
        return findLegalMoves(Color.BLACK).isEmpty() && findLegalMoves(Color.WHITE).isEmpty();
    }

    @Override
    public Move getMove() {
        if (lastMoveIndex < 0)
            return Move.ofPass(lastMoveColor);
        return Move.of(lastMoveIndex, lastMoveColor);
    }

    @Override
    public Color getTurn() {
        return (lastMoveColor == null || lastMoveColor == Color.NONE) ? Color.BLACK : lastMoveColor.flipped();
    }

    @Override
    public int count(Color color) {
        if (color == Color.BLACK)
            return Long.bitCount(blackBits);
        if (color == Color.WHITE)
            return Long.bitCount(whiteBits);
        if (color == Color.BLOCK)
            return Long.bitCount(blockBits);
        return LENGTH - Long.bitCount(blackBits | whiteBits | blockBits);
    }

    @Override
    public Color winner() {
        int score = score();
        if (!isEnd() || score == 0)
            return Color.NONE;
        return score > 0 ? Color.BLACK : Color.WHITE;
    }

    @Override
    public void foul(Color color) {
        Color winner = color.flipped();
        blackBits = winner == Color.BLACK ? ~0L >>> (64 - LENGTH) : 0L;
        whiteBits = winner == Color.WHITE ? ~0L >>> (64 - LENGTH) : 0L;
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

    private boolean canPut(int index, Color color) {
        long myBits = (color == Color.BLACK) ? blackBits : whiteBits;
        long oppBits = (color == Color.BLACK) ? whiteBits : blackBits;
        long mask = 1L << index;
        if ((blockBits & mask) != 0)
            return false;
        int x = index % SIZE, y = index / SIZE;
        for (int d = 0; d < 8; d++) {
            int dx = new int[] { -1, 0, 1, -1, 1, -1, 0, 1 }[d];
            int dy = new int[] { -1, -1, -1, 0, 0, 1, 1, 1 }[d];
            int nx = x + dx, ny = y + dy;
            int nIndex = ny * SIZE + nx;
            boolean found = false;
            boolean hasOpp = false;
            while (0 <= nx && nx < SIZE && 0 <= ny && ny < SIZE) {
                long nMask = 1L << nIndex;
                if ((blockBits & nMask) != 0)
                    break;
                if ((oppBits & nMask) != 0) {
                    hasOpp = true;
                } else if ((myBits & nMask) != 0) {
                    found = hasOpp;
                    break;
                } else {
                    break;
                }
                nx += dx;
                ny += dy;
                nIndex = ny * SIZE + nx;
            }
            if (found)
                return true;
        }
        return false;
    }

    // Boardからブロック情報もコピーする
    public void setFromBoard(Board board) {
        this.blackBits = 0L;
        this.whiteBits = 0L;
        this.blockBits = 0L;
        for (int i = 0; i < LENGTH; i++) {
            set(i, board.get(i));
        }
    }
}