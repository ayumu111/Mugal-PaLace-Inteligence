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
        int[] directions = {1, -1, 6, -6, 7, -7, 5, -5};
        int index = move.getIndex();
    
        long flippedStones = 0L;
    
        for (int d : directions) {
            int prej = index;
            int j = index + d;
            long toFlip = 0L;
    
            while (j >= 0 && j < 36 && isValidMove(prej, j, d)) {
                if (((opp >>> j) & 1L) != 0) {
                    toFlip |= (1L << j);
                    prej = j;
                    j += d;
                } else if (((own >>> j) & 1L) != 0) {
                    flippedStones |= toFlip;
                    break;
                } else {
                    break;
                }
            }
        }
    
        // 自分の石を配置＋ひっくり返す
        long newOwn = own | flippedStones | (1L << index);
        long newOpp = opp & ~flippedStones;
    
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
