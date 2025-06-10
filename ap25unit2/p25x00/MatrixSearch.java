// OurPlayerの重み配列（M）自動探索スクリプト
// 実行例: java MatrixSearch
// M_EARLY, M_MIDDLE, M_LATEの最適な値を探索

package p25x00;

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

public class MatrixSearch {
    static final int N = 6; // 6x6
    static final int TRIALS = 100000; // 試行回数
    static final int MATCHES = 10; // 先手5回＋後手5回
    static final float MIN = -100, MAX = 100; // 探索範囲
    

    // スレッドセーフな最良値とロック
    private static volatile float bestScoreBlack = Float.NEGATIVE_INFINITY;
    private static volatile float bestScoreWhite = Float.POSITIVE_INFINITY;
    private static float[][] bestEarlyBlack = new float[N][N];
    private static float[][] bestMiddleBlack = new float[N][N];
    private static float[][] bestLateBlack = new float[N][N];
    private static float[][] bestEarlyWhite = new float[N][N];
    private static float[][] bestMiddleWhite = new float[N][N];
    private static float[][] bestLateWhite = new float[N][N];
    private static final Object fileLock = new Object();

    public static void main(String[] args) throws Exception {
        Random rnd = new Random();
        StringBuilder log = new StringBuilder();

        ExecutorService executor = Executors.newFixedThreadPool(Math.min(TRIALS, Runtime.getRuntime().availableProcessors()));
        CountDownLatch latch = new CountDownLatch(TRIALS);

        

        for (int t = 0; t < TRIALS; t++) {
            executor.submit(() -> {
                try {
                    try {
                        int lose = 0; // 負けた回数
                        Random threadRnd = new Random();
                        float[][] mEarlyBlack = randomMatrix(threadRnd);
                        float[][] mMiddleBlack = randomMatrix(threadRnd);
                        float[][] mLateBlack = randomMatrix(threadRnd);
                        float[][] mEarlyWhite = randomMatrix(threadRnd);
                        float[][] mMiddleWhite = randomMatrix(threadRnd);
                        float[][] mLateWhite = randomMatrix(threadRnd);
                        float totalScoreBlack = 0;
                        float totalScoreWhite = 0;
                        // 対戦相手リスト（myplayer + enemy1〜enemy7）
                        ap25.Player[] blackEnemies = {
                            new enemy12.OurPlayer(ap25.Color.WHITE),
                            new enemy11.OurPlayer(ap25.Color.WHITE),
                            new enemy10.OurPlayer(ap25.Color.WHITE),
                            new enemy8.OurPlayer(ap25.Color.WHITE),
                            new enemy9.OurPlayer(ap25.Color.WHITE),
                            new myplayer.MyPlayer(ap25.Color.WHITE),
                            new myplayer.MyPlayer(ap25.Color.WHITE),
                            new enemy7.OurPlayer(ap25.Color.WHITE),
                            new enemy1.OurPlayer(ap25.Color.WHITE),
                            new enemy2.OurPlayer(ap25.Color.WHITE),
                            new enemy3.OurPlayer(ap25.Color.WHITE),
                            new enemy4.OurPlayer(ap25.Color.WHITE),
                            new enemy6.OurPlayer(ap25.Color.WHITE),
                        };
                        ap25.Player[] whiteEnemies = {
                            new enemy11.OurPlayer(ap25.Color.BLACK),
                            new enemy10.OurPlayer(ap25.Color.BLACK),
                            new enemy8.OurPlayer(ap25.Color.BLACK),
                            new enemy9.OurPlayer(ap25.Color.BLACK),
                            new myplayer.MyPlayer(ap25.Color.BLACK),
                            new myplayer.MyPlayer(ap25.Color.BLACK),
                            new enemy7.OurPlayer(ap25.Color.BLACK),
                            new enemy1.OurPlayer(ap25.Color.BLACK),
                            new enemy2.OurPlayer(ap25.Color.BLACK),
                            new enemy3.OurPlayer(ap25.Color.BLACK),
                            new enemy4.OurPlayer(ap25.Color.BLACK),
                            new enemy6.OurPlayer(ap25.Color.BLACK),
                        };
                        // 先手（黒）各敵と1回ずつ対戦
                        for (ap25.Player enemy : blackEnemies) {
                            MyEval evalBlack = new MyEval(ap25.Color.BLACK, mEarlyBlack, mMiddleBlack, mLateBlack);
                            float score = runMatch(new OurPlayer(ap25.Color.BLACK, evalBlack, 4), enemy);
                            if(score < 0) {
                                lose++;
                                totalScoreBlack = Float.NEGATIVE_INFINITY;
                                System.out.println(lose);
                                break;
                            }
                            System.out.println("Black vs " + enemy + " score: " + score);
                            totalScoreBlack += score;
                        }
                        // 後手（白）各敵と1回ずつ対戦
                        // for (ap25.Player enemy : whiteEnemies) {
                        //     MyEval evalWhite = new MyEval(ap25.Color.WHITE, mEarlyWhite, mMiddleWhite, mLateWhite);
                        //     float score = runMatch(enemy, new OurPlayer(ap25.Color.WHITE, evalWhite, 4));
                        //     if(score > 0) {
                        //         totalScoreWhite = Float.POSITIVE_INFINITY;
                        //         break;
                        //     }
                        //     System.out.println("White vs " + enemy + " score: " + score);
                        //     totalScoreWhite += score;
                        // }
                        float avgScoreBlack = totalScoreBlack / blackEnemies.length;
                        float avgScoreWhite = totalScoreWhite / whiteEnemies.length;
                        // 先手の最良判定
                        if (avgScoreBlack > bestScoreBlack) {
                            synchronized (fileLock) {
                                if (avgScoreBlack > bestScoreBlack) {
                                    System.out.println("New best for Black: " + avgScoreBlack );
                                    bestScoreBlack = avgScoreBlack;
                                    copyMatrix(mEarlyBlack, bestEarlyBlack);
                                    copyMatrix(mMiddleBlack, bestMiddleBlack);
                                    copyMatrix(mLateBlack, bestLateBlack);
                                    try (FileWriter fw = new FileWriter("matrix_search_result.txt", false)) {
                                        fw.write("Best for Black (先手):\n" + matrixToString(bestEarlyBlack));
                                        fw.write("Best Middle (Black):\n" + matrixToString(bestMiddleBlack));
                                        fw.write("Best Late (Black):\n" + matrixToString(bestLateBlack));
                                        fw.write("Best score (Black)=" + bestScoreBlack + "\n");
                                        fw.write("Best for White (後手):\n" + matrixToString(bestEarlyWhite));
                                        fw.write("Best Middle (White):\n" + matrixToString(bestMiddleWhite));
                                        fw.write("Best Late (White):\n" + matrixToString(bestLateWhite));
                                        fw.write("Best score (White)=" + bestScoreWhite + "\n");
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                        }
                        // 後手の最良判定
                        if (avgScoreWhite < bestScoreWhite) {
                            synchronized (fileLock) {
                                if (avgScoreWhite < bestScoreWhite) {
                                    System.out.println("New best for White: " + avgScoreWhite);
                                    bestScoreWhite = avgScoreWhite;
                                    copyMatrix(mEarlyWhite, bestEarlyWhite);
                                    copyMatrix(mMiddleWhite, bestMiddleWhite);
                                    copyMatrix(mLateWhite, bestLateWhite);
                                    try (FileWriter fw = new FileWriter("matrix_search_result.txt", false)) {
                                        fw.write("Best for Black (先手):\n" + matrixToString(bestEarlyBlack));
                                        fw.write("Best Middle (Black):\n" + matrixToString(bestMiddleBlack));
                                        fw.write("Best Late (Black):\n" + matrixToString(bestLateBlack));
                                        fw.write("Best score (Black)=" + bestScoreBlack + "\n");
                                        fw.write("Best for White (後手):\n" + matrixToString(bestEarlyWhite));
                                        fw.write("Best Middle (White):\n" + matrixToString(bestMiddleWhite));
                                        fw.write("Best Late (White):\n" + matrixToString(bestLateWhite));
                                        fw.write("Best score (White)=" + bestScoreWhite + "\n");
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();
        executor.shutdown();

        // 結果をファイルに保存
        try (FileWriter fw = new FileWriter("matrix_search_result.txt")) {
            fw.write("Best for Black (先手):\n" + matrixToString(bestEarlyBlack));
            fw.write("Best Middle (Black):\n" + matrixToString(bestMiddleBlack));
            fw.write("Best Late (Black):\n" + matrixToString(bestLateBlack));
            fw.write("Best score (Black)=" + bestScoreBlack + "\n");
            fw.write("Best for White (後手):\n" + matrixToString(bestEarlyWhite));
            fw.write("Best Middle (White):\n" + matrixToString(bestMiddleWhite));
            fw.write("Best Late (White):\n" + matrixToString(bestLateWhite));
            fw.write("Best score (White)=" + bestScoreWhite + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Best score (Black)=" + bestScoreBlack);
        System.out.println("Best Early (Black):\n" + matrixToString(bestEarlyBlack));
        System.out.println("Best Middle (Black):\n" + matrixToString(bestMiddleBlack));
        System.out.println("Best Late (Black):\n" + matrixToString(bestLateBlack));
        System.out.println("Best score (White)=" + bestScoreWhite);
        System.out.println("Best Early (White):\n" + matrixToString(bestEarlyWhite));
        System.out.println("Best Middle (White):\n" + matrixToString(bestMiddleWhite));
        System.out.println("Best Late (White):\n" + matrixToString(bestLateWhite));
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
