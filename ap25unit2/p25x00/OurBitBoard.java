package p25x00;

import static ap25.Color.BLACK;
import static ap25.Color.BLOCK;
import static ap25.Color.NONE;
import static ap25.Color.WHITE;
import java.util.ArrayList;
import java.util.List;
import ap25.Color;
import ap25.Move;

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
        long own = (color == BLACK) ? bitBoardBlack : bitBoardWhite;
        long opp = (color == BLACK) ? bitBoardWhite : bitBoardBlack;
        long empty = ~(own | opp | bitBoardBlock) & 0xFFFFFFFFFL; // 36bitだけ対象

        long legal = 0L;

        legal |= getMovesInDirection(own, opp, empty, 1, 0b011111011111011111011111011111011111L);   // →
        legal |= getMovesInDirection(own, opp, empty, -1, 0b111110111110111110111110111110111110L);  // ←
        legal |= getMovesInDirection(own, opp, empty, 6, 0xFFFFFFFFFL); // ↓
        legal |= getMovesInDirection(own, opp, empty, -6, 0xFFFFFFFFFL); // ↑
        legal |= getMovesInDirection(own, opp, empty, 7, 0b011110111101111011110111101111011000L);  // ↘︎
        legal |= getMovesInDirection(own, opp, empty, -7, 0b000110111101111011110111101111011110L); // ↖︎
        legal |= getMovesInDirection(own, opp, empty, 5, 0b111101111011110111101111011110111110L);  // ↙︎
        legal |= getMovesInDirection(own, opp, empty, -5, 0b111101111011110111101111011110011111L); // ↗︎

        if (legal == 0) {
            legalMoves.add(Move.ofPass(color));
            return legalMoves;
        }

        long mask = legal;
        while (mask != 0) {
            int idx = Long.numberOfTrailingZeros(mask);
            legalMoves.add(new Move(idx, color));
            mask &= mask - 1;
        }

        return legalMoves;
    }

    private long getMovesInDirection(long own, long opp, long empty, int dir, long mask) {
        long candidate = 0L;
        long tmp = own;

        for (int i = 0; i < 5; i++) { // 5回まで挟める（6x6なら十分）
            tmp = shift(tmp, dir) & opp & mask;
            candidate |= tmp;
        }

        return shift(candidate, dir) & empty & mask;
    }

    private long shift(long b, int d) {
        if (d > 0) return b << d;
        else return b >>> -d;
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
} 
