package p25x01;

import ap25.Color;
import static ap25.Color.BLACK;
import ap25.Move;
import java.util.ArrayList;
import java.util.List;

public class OurBitBoard  {
  private long bitBoardBlack;
  private long bitBoardWhite;
  private long bitBoardBlock;
  private Color currentTurn;
  private Move lastMove;

  // コンストラクタ
  public OurBitBoard(long bitBoardBlack, long bitBoardWhite, long bitBoardBlock) {
      this.bitBoardBlack = bitBoardBlack;
      this.bitBoardWhite = bitBoardWhite;
      this.bitBoardBlock = bitBoardBlock;
      this.currentTurn = Color.BLACK; // デフォルトで黒の手番
      this.lastMove = null;
  }

  public boolean isEnd() {
      // 合法手がどちらの色にも存在しないかどうかを判定
      return findLegalMoves(Color.BLACK).isEmpty() && findLegalMoves(Color.WHITE).isEmpty();
  }

  public List<Move> findLegalMoves(Color color) {
        List<Move> legalMoves = new ArrayList<>();
        // long own   = 0b000000111011110001101101011001011111L;
        // long opp   = 0b010110000100001110010010100110100000L;

        /* 
         * b b b b b w
         * b w w b b w
         * b w b b w b
         * b w w w b b
         * b b w b b b
         * n w w n w n
         * 
         * 33 d6
        */
        long own = (color == BLACK) ? bitBoardBlack : bitBoardWhite;
        long opp = (color == BLACK) ? bitBoardWhite : bitBoardBlack;
        long empty = ~(own | opp | bitBoardBlock);
        int[] directions = {1, -1, 6, -6, 7, -7, 5, -5};
        // 盤面の各マスをチェック
        // System.out.println("Finding legal moves for color: " + color);
        
        for (int i = 0; i < 36; i++) {
            if (((empty >>> i) & 1L) == 0) continue;
            for (int d : directions) {
                int prej = i;
                int j = i + d;
                boolean foundOpponent = false;
                while (j >= 0 && j < 36 && isValidMove(prej, j, d)) {
                    if (((opp >>> j) & 1L) != 0) {
                        foundOpponent = true;
                        prej = j;
                        j += d;
                    } else if (((own >>> j) & 1L) != 0) {
                        if (foundOpponent) {
                            legalMoves.add(new Move(i, color));
                        }
                        break;
                    } else {
                        break;
                    }
                }
            }
        }
        
        if (legalMoves.isEmpty()) {
            legalMoves.add(Move.ofPass(color));
            // System.out.println(Long.toBinaryString(own));
            // System.out.println(Long.toBinaryString(opp));
            // System.out.println(Long.toBinaryString(empty));
        }
        return legalMoves;
    }
    

    private boolean isValidMove(int from, int to, int direction) {
        if (to < 0 || to >= 36) return false; // 盤面外チェック
    
        int fromRow = from / 6;
        int toRow = to / 6;
        int fromCol = from % 6;
        int toCol = to % 6;
    
        int rowDiff = toRow - fromRow;
        int colDiff = toCol - fromCol;
    
        switch (direction) {
            case 1:  // 右
                return fromRow == toRow && colDiff == 1;
            case -1: // 左
                return fromRow == toRow && colDiff == -1;
            case 6:  // 下
                return rowDiff == 1 && colDiff == 0;
            case -6: // 上
                return rowDiff == -1 && colDiff == 0;
            case 7:  // 右下
                return rowDiff == 1 && colDiff == 1;
            case -7: // 左上
                return rowDiff == -1 && colDiff == -1;
            case 5:  // 左下
                return rowDiff == 1 && colDiff == -1;
            case -5: // 右上
                return rowDiff == -1 && colDiff == 1;
            default:
                return false;
        }
    }
    
    
  

    public OurBitBoard placed(Move move) {
    if (move.isPass()) {
        OurBitBoard next = new OurBitBoard(bitBoardBlack, bitBoardWhite, bitBoardBlock);
        next.currentTurn = this.currentTurn.flipped();
        next.lastMove = move;
        return next;
    }

    int idx = move.getIndex();

    // ブロックマスや既に石があるマスには置けない
    long all = bitBoardBlack | bitBoardWhite | bitBoardBlock;
    if (((all >>> idx) & 1L) != 0) {
        throw new IllegalArgumentException("Invalid move: blocked or already occupied.");
    }

    long own = (currentTurn == BLACK) ? bitBoardBlack : bitBoardWhite;
    long opp = (currentTurn == BLACK) ? bitBoardWhite : bitBoardBlack;
    long flip = 0L;

    int[] dirs = {1, -1, 6, -6, 7, -7, 5, -5};

    for (int d : dirs) {
        int pos = idx + d;
        int prev = idx;
        long flipped = 0L;
        boolean foundOpponent = false;

        while (pos >= 0 && pos < 36 && isValidMove(prev, pos, d)) {
            long bit = 1L << pos;
            if ((opp & bit) != 0) {
                flipped |= bit;
                foundOpponent = true;
            } else if ((own & bit) != 0) {
                if (foundOpponent) {
                    flip |= flipped;
                }
                break;
            } else {
                break;
            }
            prev = pos;
            pos += d;
        }
    }

    long newOwn = own | flip | (1L << idx);
    long newOpp = opp & ~flip;

    OurBitBoard next;
    if (currentTurn == BLACK) {
        next = new OurBitBoard(newOwn, newOpp, bitBoardBlock);
    } else {
        next = new OurBitBoard(newOpp, newOwn, bitBoardBlock);
    }

    next.currentTurn = currentTurn.flipped();
    next.lastMove = move;
    return next;
}
    
    


  public OurBoard encode() {
    Color[] board = new Color[36];
    for (int i = 0; i < 36; i++) {
        if (((bitBoardBlack >>> i) & 1L) != 0) {
            board[i] = Color.BLACK;
        } else if (((bitBoardWhite >>> i) & 1L) != 0) {
            board[i] = Color.WHITE;
        } else if (((bitBoardBlock >>> i) & 1L) != 0) {
            board[i] = Color.BLOCK;
        } else {
            board[i] = Color.NONE;
        }
    }

    Move move = (this.lastMove != null) ? this.lastMove : Move.ofPass(Color.NONE);
    return new OurBoard(board, move);
    }

    public long hash() {
        return bitBoardBlack ^ (bitBoardWhite << 21) ^ (bitBoardBlock << 42);
    }

    
    
    // 潜在的モビリティ（指定色の石の隣にある空きマスの数、8近傍）
    public int findPotentialMobility(Color color) {
        long own;
        if (color == Color.BLACK) {
            own = this.bitBoardBlack;
        } else if (color == Color.WHITE) {
            own = this.bitBoardWhite;
        } else {
            return 0;
        }
        long empty = ~(this.bitBoardBlack | this.bitBoardWhite | this.bitBoardBlock) & 0xFFFFFFFFFL; // 36bitマスク
        long neighbor = 0L;
        // 8方向に1ビットずつシフトして隣接マスを集める
        // 右
        neighbor |= (own & 0xF7DEF7DEFL) << 1;
        // 左
        neighbor |= (own & 0xEF7BDEF7BL) >> 1;
        // 上
        neighbor |= (own & 0xFFFFFFFFFL) << 6;
        // 下
        neighbor |= (own & 0xFFFFFFFFFL) >> 6;
        // 右上
        neighbor |= (own & 0xF7DEF7DEF0L) << 7;
        // 左上
        neighbor |= (own & 0xEF7BDEF7B0L) << 5;
        // 右下
        neighbor |= (own & 0xF7DEF7DEFL) >> 5;
        // 左下
        neighbor |= (own & 0xEF7BDEF7BL) >> 7;
        // 自分自身の位置は除外
        neighbor &= ~own;
        // 空きマスのみ
        neighbor &= empty;
        // 1のビット数を数える
        return Long.bitCount(neighbor);
    }
    public long getBitBoardBlack() {
        return bitBoardBlack;
    }

    public long getBitBoardWhite() {
        return bitBoardWhite;
    }

    public long getBitBoardBlock() {
        return bitBoardBlock;
    }

    // 盤面全体のスコア計算（黒-白、全滅時は空きマス分加算）
    public int score() {
        int black = Long.bitCount(bitBoardBlack);
        int white = Long.bitCount(bitBoardWhite);
        int block = Long.bitCount(bitBoardBlock);
        if (black == 0 && white == 0) {
            // 全滅時は空きマス分加算
            return 36 - block;
        }
        return black - white;
    }

    public int count(Color color) {
        if (color == Color.BLACK) {
            return Long.bitCount(bitBoardBlack);
        } else if (color == Color.WHITE) {
            return Long.bitCount(bitBoardWhite);
        } else if (color == Color.BLOCK) {
            return Long.bitCount(bitBoardBlock);
        } else {
            // NONEやその他
            long all = bitBoardBlack | bitBoardWhite | bitBoardBlock;
            return 36 - Long.bitCount(all);
        }
    }
    // 指定色の簡易安定石数を返す(簡易版) for OurBitBoard
public int countSimpleStable(Color color) {
    int stable = 0;
    int[] cornerIdx = {0, 5, 30, 35};
    int[][] dir = {{1, 6}, {-1, 6}, {1, -6}, {-1, -6}};
    boolean[] isStable = new boolean[36];
    long colorBits = (color == Color.BLACK) ? bitBoardBlack : bitBoardWhite;
    long blockBits = bitBoardBlock;

    // 角とその直線上の安定石
    for (int c = 0; c < 4; c++) {
        int corner = cornerIdx[c];
        if (((colorBits >>> corner) & 1L) == 0) continue;
        isStable[corner] = true;
        stable++; // 角は安定
        for (int d = 0; d < 2; d++) {
            int pos = corner;
            while (true) {
                int nx = pos % 6 + dir[c][d] % 6;
                int ny = pos / 6 + dir[c][d] / 6;
                if (nx < 0 || nx >= 6 || ny < 0 || ny >= 6) break;
                int npos = ny * 6 + nx;
                if (((colorBits >>> npos) & 1L) != 0 && !isStable[npos]) {
                    isStable[npos] = true;
                    stable++;
                    pos = npos;
                } else {
                    break;
                }
            }
        }
    }
    if (Long.bitCount(blockBits) == 0) {
        return stable; // ブロックがない場合はここで終了
    }
    // 角以外の外周マスで、4近傍にブロックがある自分の石を追加で安定石とする
    int[] edgeIdx = {
        1,2,3,4,  // 1行目
        31,32,33,34, // 6行目
        6,12,18,24, // 1列目
        11,17,23,29 // 6列目
    };
    int[] dx = {1, -1, 0, 0};
    int[] dy = {0, 0, 1, -1};
    for (int idx : edgeIdx) {
        if (((colorBits >>> idx) & 1L) == 0) continue;
        if (isStable[idx]) continue; // すでに安定石ならスキップ
        int x = idx % 6, y = idx / 6;
        for (int d = 0; d < 4; d++) {
            int nx = x + dx[d];
            int ny = y + dy[d];
            if (nx < 0 || nx >= 6 || ny < 0 || ny >= 6) continue;
            int nidx = ny * 6 + nx;
            if (((blockBits >>> nidx) & 1L) != 0) {
                isStable[idx] = true;
                stable++;
                break;
            }
        }
    }
    return stable;
    }
}
