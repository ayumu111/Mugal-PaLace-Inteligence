// OurPlayerの重み配列（M）自動探索スクリプト
// 実行例: java MatrixSearch
// M_EARLY, M_MIDDLE, M_LATEの最適な値を探索

package p25x11;

import static ap25.Color.BLACK;
import static ap25.Color.WHITE;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import ap25.league.RandomPlayer;
import ap25.Color;
import ap25.Player;
import myplayer.MyPlayer;
import ap25.Board;

public class TournamentSearch {
    static final int N = 6; // 6x6
    static final int TRIALS = 100000; // 試行回数
    static final int MATCHES = 10; // 先手5回＋後手5回
    static final float MIN = -100, MAX = 100; // 探索範囲
    

    public static void main(String[] args) throws Exception {
        Random rnd = new Random();
        StringBuilder log = new StringBuilder();

        // 先手・後手それぞれの重み配列（親・挑戦者）
        float[][] mEarlyBlack1 = {
        { -20, 63, 51, 51, 63, -20 },
        { 63, -31, -70, -70, -31, 63 },
        { 51, -70, -12, -12, -70, 51 },
        { 51, -70, -12, -12, -70, 51 },
        { 63, -31, -70, -70, -31, 63 },
        { -20, 63, 51, 51, 63, -20 },
    };
        float[][] mMiddleBlack1 = {
      { 41, -7, 25, 25, -7, 41 },
      { -7, 57, 24, 24, 57, -7 },
      { 25, 24, -51, -51, 24, 25 },
      { 25, 24, -51, -51, 24, 25 },
      { -7, 57, 24, 24, 57, -7 },
      { 41, -7, 25, 25, -7, 41 },
    };
        float[][] mLateBlack1 = {
      { 59, 55, -68, -68, 55, 59 },
      { 55, 23, -8, -8, 23, 55 },
      { -68, -8, -93, -93, -8, -68 },
      { -68, -8, -93, -93, -8, -68 },
      { 55, 23, -8, -8, 23, 55 },
      { 59, 55, -68, -68, 55, 59 },
    };
        float[][] mEarlyBlack2 = randomMatrix(rnd);
        float[][] mMiddleBlack2 = randomMatrix(rnd);
        float[][] mLateBlack2 = randomMatrix(rnd);
        float[][] mEarlyWhite1 = {
      { -2, 60, 62, 62, 60, -2 },
      { 60, -59, 87, 87, -59, 60 },
      { 62, 87, -60, -60, 87, 62 },
      { 62, 87, -60, -60, 87, 62 },
      { 60, -59, 87, 87, -59, 60 },
      { -2, 60, 62, 62, 60, -2 },
    };
        float[][] mMiddleWhite1 = {
      { 68, 1, 25, 25, 1, 68 },
      { 1, 91, -23, -23, 91, 1 },
      { 25, -23, -87, -87, -23, 25 },
      { 25, -23, -87, -87, -23, 25 },
      { 1, 91, -23, -23, 91, 1 },
      { 68, 1, 25, 25, 1, 68 },
    };
        float[][] mLateWhite1 = {
      { -64, 54, 69, 69, 54, -64 },
      { 54, -44, -2, -2, -44, 54 },
      { 69, -2, 19, 19, -2, 69 },
      { 69, -2, 19, 19, -2, 69 },
      { 54, -44, -2, -2, -44, 54 },
      { -64, 54, 69, 69, 54, -64 },
    };
        float[][] mEarlyWhite2 = randomMatrix(rnd);
        float[][] mMiddleWhite2 = randomMatrix(rnd);
        float[][] mLateWhite2 = randomMatrix(rnd);

        float[][] bestEarlyBlack = new float[N][N], bestMiddleBlack = new float[N][N], bestLateBlack = new float[N][N];
        float[][] bestEarlyWhite = new float[N][N], bestMiddleWhite = new float[N][N], bestLateWhite = new float[N][N];
        float bestScoreBlack = Float.NEGATIVE_INFINITY;
        float bestScoreWhite = Float.NEGATIVE_INFINITY;

        for (int trial = 1; trial <= TRIALS; trial++) {
            // 先手（黒）親 vs 後手（白）挑戦者
            MyEval evalBlack1 = new MyEval(BLACK, mEarlyBlack1, mMiddleBlack1, mLateBlack1);
            MyEval evalWhite2 = new MyEval(WHITE, mEarlyWhite2, mMiddleWhite2, mLateWhite2);
            OurPlayer pBlack1 = new OurPlayer(BLACK, evalBlack1, 4);
            OurPlayer pWhite2 = new OurPlayer(WHITE, evalWhite2, 4);
            float score1 = runMatch(pBlack1, pWhite2);
            System.out.println("Trial " + trial + " 正で親の勝ち→ " + score1);
            // 逆（白親 vs 黒挑戦者）
            MyEval evalWhite1 = new MyEval(WHITE, mEarlyWhite1, mMiddleWhite1, mLateWhite1);
            MyEval evalBlack2 = new MyEval(BLACK, mEarlyBlack2, mMiddleBlack2, mLateBlack2);
            OurPlayer pWhite1 = new OurPlayer(WHITE, evalWhite1, 4);
            OurPlayer pBlack2 = new OurPlayer(BLACK, evalBlack2, 4);
            float score2 = runMatch(pBlack2, pWhite1);
            System.out.println("Trial " + trial + " 負で親の勝ち→ " + score2);

            // // 黒親 vs 白挑戦者の勝敗
            // if (score1 >= 0) {
            //     // 黒親が勝ち or 引き分け → 白挑戦者をランダム生成
            //     System.out.println("Black parent wins or draws, generating new White challenger.");
            //     mEarlyWhite2 = randomMatrix(rnd);
            //     mMiddleWhite2 = randomMatrix(rnd);
            //     mLateWhite2 = randomMatrix(rnd);
            // } else {
            //     // 白挑戦者が勝ち → 黒親をランダム生成
            //     System.out.println("White challenger wins, generating new Black parent.");
            //     mEarlyBlack1 = randomMatrix(rnd);
            //     mMiddleBlack1 = randomMatrix(rnd);
            //     mLateBlack1 = randomMatrix(rnd);
            //     //白挑戦者を白親に昇格
            //     mEarlyWhite1 = Arrays.copyOf(mEarlyWhite2, N);
            //     mMiddleWhite1 = Arrays.copyOf(mMiddleWhite2, N);
            //     mLateWhite1 = Arrays.copyOf(mLateWhite2, N);
            // }
            // // 白親 vs 黒挑戦者の勝敗
            // if (score2 <= 0) {
            //     // 白親が勝ち or 引き分け → 黒挑戦者をランダム生成
            //     System.out.println("White parent wins or draws, generating new Black challenger.");
            //     mEarlyBlack2 = randomMatrix(rnd);
            //     mMiddleBlack2 = randomMatrix(rnd);
            //     mLateBlack2 = randomMatrix(rnd);
            // } else {
            //     // 黒挑戦者が勝ち → 白親をランダム生成
            //     System.out.println("Black challenger wins, generating new White parent.");
            //     mEarlyWhite1 = randomMatrix(rnd);
            //     mMiddleWhite1 = randomMatrix(rnd);
            //     mLateWhite1 = randomMatrix(rnd);
            //     //黒挑戦者を黒親に昇格
            //     mEarlyBlack1 = Arrays.copyOf(mEarlyBlack2, N);
            //     mMiddleBlack1 = Arrays.copyOf(mMiddleBlack2, N);
            //     mLateBlack1 = Arrays.copyOf(mLateBlack2, N);
            // }

            
            if (score1 - score2 >= 0){
                //親の勝ち
                mEarlyBlack2 = randomMatrix(rnd);
                mMiddleBlack2 = randomMatrix(rnd);
                mLateBlack2 = randomMatrix(rnd);
                mEarlyWhite2 = randomMatrix(rnd);
                mMiddleWhite2 = randomMatrix(rnd);
                mLateWhite2 = randomMatrix(rnd);
            } else {
                //挑戦者の勝ち(親に昇格)
                System.out.println("挑戦者勝利、親に昇格");
                mEarlyBlack1 = Arrays.copyOf(mEarlyBlack2, N);
                mMiddleBlack1 = Arrays.copyOf(mMiddleBlack2, N);
                mLateBlack1 = Arrays.copyOf(mLateBlack2, N);
                mEarlyWhite1 = Arrays.copyOf(mEarlyWhite2, N);
                mMiddleWhite1 = Arrays.copyOf(mMiddleWhite2, N);
                mLateWhite1 = Arrays.copyOf(mLateWhite2, N);
                mEarlyBlack2 = randomMatrix(rnd);
                mMiddleBlack2 = randomMatrix(rnd);
                mLateBlack2 = randomMatrix(rnd);
                mEarlyWhite2 = randomMatrix(rnd);
                mMiddleWhite2 = randomMatrix(rnd);
                mLateWhite2 = randomMatrix(rnd);
            }

            // 進捗・最良をファイル保存（例：100回ごと）
            if (trial % 100 == 0) {
                try (FileWriter fw = new FileWriter("tournament_search_result.txt", false)) {
                    fw.write("Trial: " + trial + "\n");
                    fw.write("Best for Black (先手):\n" + matrixToString(mEarlyBlack1));
                    fw.write("Best Middle (先手\n" + matrixToString(mMiddleBlack1));
                    fw.write("Best Late (先手\n" + matrixToString(mLateBlack1));
                    fw.write("Best for White (後手):\n" + matrixToString(mEarlyWhite1));
                    fw.write("Best Middle (後手):\n" + matrixToString(mMiddleWhite1));
                    fw.write("Best Late (後手):\n" + matrixToString(mLateWhite1));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    static float[][] randomMatrix(Random rnd) {
        float[][] m = new float[N][N];
        // 左上3x3のみランダム生成（整数値のみ）
        for (int i = 0; i < (N+1)/2; i++) {
            for (int j = 0; j < (N+1)/2; j++) {
                int v = (int)MIN + rnd.nextInt((int)(MAX - MIN + 1)); // 整数値
                // 4回転＋鏡映で対称位置にコピー
                m[i][j] = v;
                m[N-1-i][j] = v;
                m[i][N-1-j] = v;
                m[N-1-i][N-1-j] = v;
                m[j][i] = v;
                m[N-1-j][i] = v;
                m[j][N-1-i] = v;
                m[N-1-j][N-1-i] = v;
            }
        }
        return m;
    }
    static void copyMatrix(float[][] src, float[][] dst) {
        for (int i = 0; i < N; i++)
            System.arraycopy(src[i], 0, dst[i], 0, N);
    }
    static String matrixToString(float[][] m) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < N; i++) {
            sb.append("{ ");
            for (int j = 0; j < N; j++) {
                sb.append(String.format("%d", Math.round(m[i][j])));
                if (j < N - 1) sb.append(", ");
            }
            sb.append(" },\n");
        }
        return sb.toString();
    }

    // OurPlayerとMyPlayerで1回対局し、OurPlayerのスコアを返す
    public static float runMatch(Player black, Player white) {
        ap25.Board board = new ap25.league.OfficialBoard();
        ap25.league.Game game = new ap25.league.Game(board, black, white, 75);
        game.play();
        Board finalBoard = getBoardReflect(game);
        return finalBoard.score();
    }
    // Gameクラスのprivateなboardフィールドをリフレクションで取得
    private static Board getBoardReflect(ap25.league.Game game) {
        try {
            java.lang.reflect.Field f = game.getClass().getDeclaredField("board");
            f.setAccessible(true);
            return (Board) f.get(game);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
