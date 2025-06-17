package p25x00ad;

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
    
        long own = (currentTurn == BLACK) ? bitBoardBlack : bitBoardWhite;
        long opp = (currentTurn == BLACK) ? bitBoardWhite : bitBoardBlack;
        long flip = 0L;
        int idx = move.getIndex();
    
        // 方向定義（{dir, mask}）
        int[] dirs = {1, -1, 6, -6, 7, -7, 5, -5};
        long[] masks = {
            0b011111011111011111011111011111011111L, // →
            0b111110111110111110111110111110111110L, // ←
            0xFFFFFFFFFL,                            // ↓
            0xFFFFFFFFFL,                            // ↑
            0b011110111101111011110111101111011000L, // ↘︎
            0b000110111101111011110111101111011110L, // ↖︎
            0b111101111011110111101111011110111110L, // ↙︎
            0b111101111011110111101111011110011111L  // ↗︎
        };
    
        for (int i = 0; i < 8; i++) {
            int dir = dirs[i];
            long mask = masks[i];
    
            long flipped = 0L;
            int pos = idx + dir;
    
            while (0 <= pos && pos < 36 && ((mask >>> pos) & 1L) != 0) {
                long bit = 1L << pos;
                if ((opp & bit) != 0) {
                    flipped |= bit;
                } else if ((own & bit) != 0) {
                    flip |= flipped; // はさめた → 反転確定
                    break;
                } else {
                    break;
                }
                pos += dir;
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
}
