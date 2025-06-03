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
//   private boolean gameEnded;
//   private Color foulColor;

  // コンストラクタ
  public OurBitBoard(long bitBoardBlack, long bitBoardWhite, long bitBoardBlock) {
      this.bitBoardBlack = bitBoardBlack;
      this.bitBoardWhite = bitBoardWhite;
      this.bitBoardBlock = bitBoardBlock;
      this.currentTurn = Color.BLACK; // デフォルトで黒の手番
      this.lastMove = null;
    //   this.gameEnded = false;
    //   this.foulColor = null;
  }



//   public Color get(int k) {
//       if (((bitBoardBlack >>> k) & 1L) != 0) return Color.BLACK;
//       if (((bitBoardWhite >>> k) & 1L) != 0) return Color.WHITE;
//       if (((bitBoardBlock >>> k) & 1L) != 0) return Color.BLOCK;
//       return Color.NONE;
//   }

//   public Move getMove() {
//       return lastMove;
//   }

//   public Color getTurn() {
//       return currentTurn;
//   }

//   public int count(Color color) {
//       long bitBoard = switch (color) {
//           case BLACK -> bitBoardBlack;
//           case WHITE -> bitBoardWhite;
//           case BLOCK -> bitBoardBlock;
//           default -> 0L;
//       };
//       return Long.bitCount(bitBoard);
//   }

    // public OurBoard clone() {
    //     OurBitBoard copy = new OurBitBoard(bitBoardBlack, bitBoardWhite, bitBoardBlock);
    //     copy.currentTurn = this.currentTurn;
    //     copy.lastMove = this.lastMove != null ? this.lastMove : Move.ofPass(Color.NONE);
    //     // copy.gameEnded = this.gameEnded;
    //     // copy.foulColor = this.foulColor;
    //     return copy;
    // }

  public boolean isEnd() {
      // 合法手がどちらの色にも存在しないかどうかを判定
      return findLegalMoves(Color.BLACK).isEmpty() && findLegalMoves(Color.WHITE).isEmpty();
  }

//   public Color winner() {
//       if (!isEnd()) return Color.NONE;
//       if (foulColor != null) return foulColor.flipped();
//       int diff = count(Color.BLACK) - count(Color.WHITE);
//       return diff > 0 ? Color.BLACK : diff < 0 ? Color.WHITE : Color.NONE;
//   }

//   public void foul(Color color) {
//       this.foulColor = color;
//       this.gameEnded = true;
//   }

//   public int score() {
//       int b = count(Color.BLACK);
//       int w = count(Color.WHITE);
//       int n = 64 - b - w;
//       int score = b - w;
//       if (b == 0 || w == 0) score += Integer.signum(score) * n;
//       return score;
//   }
// 000000000000000000000000000000000000
// 000000000000000000100000100000111000
// 001000001000001110011111001000000000
// 110111110111110001000000010111000111
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
            System.out.println(Long.toBinaryString(own));
            System.out.println(Long.toBinaryString(opp));
            System.out.println(Long.toBinaryString(empty));
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
    
  

//   public OurBitBoard placed(Move move) {
//       OurBitBoard next = (OurBitBoard) this.clone();
//       next.lastMove = move;
//       next.currentTurn = colorFlipped();

//       if (move.isPass() || move.isNone()) return next;

//       int index = move.getIndex();
//       Color color = move.getColor();
//       long own = (color == Color.BLACK) ? next.bitBoardBlack : next.bitBoardWhite;
//       long opp = (color == Color.BLACK) ? next.bitBoardWhite : next.bitBoardBlack;

//       long flipped = 0L;
//       int[] directions = {1, -1, 8, -8, 7, -7, 9, -9};

//       for (int d : directions) {
//           int i = index + d;
//           long line = 0L;
//           while (i >= 0 && i < 36 && isValidMove(index, i, d) && ((opp >>> i) & 1L) != 0) {
//               line |= (1L << i);
//               i += d;
//           }
//           if (i >= 0 && i < 36 && isValidMove(index, i, d) && ((own >>> i) & 1L) != 0) {
//               flipped |= line;
//           }
//       }

//       own |= flipped | (1L << index);
//       opp &= ~flipped;

//       if (color == Color.BLACK) {
//           next.bitBoardBlack = own;
//           next.bitBoardWhite = opp;
//       } else {
//           next.bitBoardWhite = own;
//           next.bitBoardBlack = opp;
//       }

//       return next;
//   }

//   public OurBitBoard flipped() {
//       OurBitBoard flipped = new OurBitBoard(bitBoardWhite, bitBoardBlack, bitBoardBlock);
//       flipped.lastMove = this.lastMove != null ? this.lastMove.flipped() : null;
//       flipped.currentTurn = this.currentTurn.flipped();
//       flipped.gameEnded = this.gameEnded;
//       flipped.foulColor = this.foulColor != null ? this.foulColor.flipped() : null;
//       return flipped;
//   }

//   public OurBitBoard clone() {
//       OurBitBoard copy = new OurBitBoard(bitBoardBlack, bitBoardWhite, bitBoardBlock);
//       copy.currentTurn = this.currentTurn;
//       copy.lastMove = this.lastMove;
//       copy.gameEnded = this.gameEnded;
//       copy.foulColor = this.foulColor;
//       return copy;
//   }

//   private Color colorFlipped() {
//       return currentTurn.flipped();
//   }

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
