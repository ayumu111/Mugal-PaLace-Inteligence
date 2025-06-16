package p25x11;

import ap25.*;
import java.util.*;
import java.util.concurrent.*;

public class EvolutionSearch {
    static final int PARAMS = 10; // パラメータ数
    static final int PHASES = 3;  // 局面数
    static final int POP_SIZE = 10; // 個体数
    static final int GENERATIONS = 100; // 世代数
    static final float MUTATION_STDDEV = 0.5f; // 突然変異の標準偏差
    static final float INIT_MIN = -5, INIT_MAX = 5; // 初期値範囲

    // --- 追加: オプションフラグ ---
    static final boolean USE_CROSSOVER = true;
    static final boolean USE_ADAPTIVE_MUTATION = true;
    static final boolean USE_PARALLEL = true;
    static final boolean USE_INITIAL_PARENT = true; // trueでファイルから初期親を読み込む
    static final String INITIAL_PARENT_FILE = "initial_customW.txt"; // 初期親を指定するファイル

    public static void main(String[] args) throws Exception {
        Random rnd = new Random();
        float[][][] population = new float[POP_SIZE][PARAMS][PHASES];
        float[] fitness = new float[POP_SIZE];

        // --- 初期親指定機能 ---
        if (USE_INITIAL_PARENT) {
            float[][] initialParent = loadWeightsFromFile(INITIAL_PARENT_FILE);
            if (initialParent != null) {
                copyTo(initialParent, population[0]);
                for (int i = 1; i < POP_SIZE; i++) randomize(population[i], rnd);
            } else {
                // ファイルがなければ全個体ランダム
                for (int i = 0; i < POP_SIZE; i++) randomize(population[i], rnd);
            }
        } else {
            for (int i = 0; i < POP_SIZE; i++) randomize(population[i], rnd);
        }

        ExecutorService executor = USE_PARALLEL ? Executors.newFixedThreadPool(Math.min(POP_SIZE, Runtime.getRuntime().availableProcessors())) : null;

        for (int gen = 0; gen < GENERATIONS; gen++) {
            // 評価
            if (USE_PARALLEL) {
                CountDownLatch latch = new CountDownLatch(POP_SIZE);
                for (int i = 0; i < POP_SIZE; i++) {
                    final int idx = i;
                    executor.submit(() -> {
                        fitness[idx] = evaluate(population[idx]);
                        latch.countDown();
                    });
                }
                latch.await();
            } else {
                for (int i = 0; i < POP_SIZE; i++)
                    fitness[i] = evaluate(population[i]);
            }

            // エリート選択
            int bestIdx = 0;
            for (int i = 1; i < POP_SIZE; i++)
                if (fitness[i] > fitness[bestIdx]) bestIdx = i;

            float[][] elite = copy(population[bestIdx]);

            // 新世代生成
            for (int i = 0; i < POP_SIZE; i++) {
                if (i == 0) {
                    // エリート保存
                    copyTo(elite, population[i]);
                } else {
                    float[][] child;
                    if (USE_CROSSOVER) {
                        // 2親選択
                        int p1 = rnd.nextInt(POP_SIZE);
                        int p2 = rnd.nextInt(POP_SIZE);
                        child = crossover(population[p1], population[p2], rnd);
                    } else {
                        int p = rnd.nextInt(POP_SIZE);
                        child = copy(population[p]);
                    }
                    float mutationStd = MUTATION_STDDEV;
                    if (USE_ADAPTIVE_MUTATION) {
                        // 世代が進むほど突然変異を減らす
                        mutationStd = (float)(MUTATION_STDDEV * (1.0 - (double)gen / GENERATIONS));
                        mutationStd = Math.max(mutationStd, 0.05f); // 最小値
                    }
                    mutate(child, rnd, mutationStd);
                    copyTo(child, population[i]);
                }
            }
            System.out.println("Gen " + gen + " best fitness: " + fitness[bestIdx]);
        }
        if (executor != null) executor.shutdown();
        // 最良個体の出力
        System.out.println("Best customW:");
        print(population[0]);
        // 追加: 最良個体をファイルに保存
        saveWeightsToFile(population[0], "best_customW.txt");
    }

    static void randomize(float[][] w, Random rnd) {
        for (int i = 0; i < w.length; i++)
            for (int j = 0; j < w[0].length; j++)
                w[i][j] = INIT_MIN + rnd.nextFloat() * (INIT_MAX - INIT_MIN);
    }

    static void mutate(float[][] w, Random rnd, float stddev) {
        for (int i = 0; i < w.length; i++)
            for (int j = 0; j < w[0].length; j++)
                w[i][j] += rnd.nextGaussian() * stddev;
    }
    // 互換性のための旧mutate
    static void mutate(float[][] w, Random rnd) {
        mutate(w, rnd, MUTATION_STDDEV);
    }

    // 交叉: 2親からランダムに遺伝子を選ぶ
    static float[][] crossover(float[][] p1, float[][] p2, Random rnd) {
        float[][] child = new float[PARAMS][PHASES];
        for (int i = 0; i < PARAMS; i++)
            for (int j = 0; j < PHASES; j++)
                child[i][j] = rnd.nextBoolean() ? p1[i][j] : p2[i][j];
        return child;
    }

    static float[][] copy(float[][] src) {
        float[][] dst = new float[src.length][src[0].length];
        for (int i = 0; i < src.length; i++)
            System.arraycopy(src[i], 0, dst[i], 0, src[0].length);
        return dst;
    }

    static void copyTo(float[][] src, float[][] dst) {
        for (int i = 0; i < src.length; i++)
            System.arraycopy(src[i], 0, dst[i], 0, src[0].length);
    }

    static float evaluate(float[][] customW) {
        // 評価対象の重みでOurPlayerを生成
        int depth = 9; // 探索深さ（適宜調整）
        int games = 4; // 対戦回数（偶数にすると先後公平）
        int enemyKind = 2;
        float totalScore = 0;
        int winCount = 0;
        try {
            for (int i = 0; i < games; i++) {
                // 先手OurPlayer vs 後手MyPlayer
                ap25.Player black = new OurPlayer("evo", ap25.Color.BLACK, new MyEval(ap25.Color.BLACK, customW), depth);
                ap25.Player white = new p25x01.MyPlayer(ap25.Color.WHITE);
                float score1 = runMatch(black, white);
                totalScore += score1;
                if (score1 > 0) winCount++;
                white = new myplayer.MyPlayer(ap25.Color.WHITE);
                float score2 = runMatch(black, white);
                totalScore += score2;
                if (score2 > 0) winCount++;

                // 先手MyPlayer vs 後手OurPlayer
                black = new p25x01.MyPlayer(ap25.Color.BLACK);
                white = new OurPlayer("evo", ap25.Color.WHITE, new MyEval(ap25.Color.WHITE, customW), depth);
                float score3 = runMatch(black, white);
                totalScore -= score3; // 白番はスコアをマイナス
                if (score3 < 0) winCount++;
                black = new myplayer.MyPlayer(ap25.Color.BLACK);
                float score4 = runMatch(black, white);
                totalScore -= score4; // 白番はスコアをマイナス
                if (score4 < 0) winCount++;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        float avgScore = totalScore / (games * enemyKind);
        // 勝利数を優先し、同数なら平均スコアで比較
        return winCount + avgScore * 0.001f;
    }

    // OurPlayerとMyPlayerで1回対局し、OurPlayerのスコアを返す
    public static float runMatch(Player black, Player white) {
        ap25.Board board = new ap25.league.OfficialBoard();
        ap25.league.Game game = new ap25.league.Game(board, black, white, 60);
        game.play();
        Board finalBoard = getBoardReflect(game);
        return finalBoard.score();
    }

      private static Board getBoardReflect(ap25.league.Game game) {
        try {
            java.lang.reflect.Field f = game.getClass().getDeclaredField("board");
            f.setAccessible(true);
            return (Board) f.get(game);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static void print(float[][] w) {
        for (float[] row : w)
            System.out.println(Arrays.toString(row));
    }

    // 追加: 重みをファイルに保存
    static void saveWeightsToFile(float[][] w, String filename) {
        try (java.io.PrintWriter out = new java.io.PrintWriter(filename)) {
            for (float[] row : w) {
                out.println(Arrays.toString(row));
            }
        } catch (Exception e) {
            System.err.println("[saveWeightsToFile] " + e);
        }
    }

    // 追加: 重みをファイルから読み込む
    static float[][] loadWeightsFromFile(String filename) {
        try (java.util.Scanner sc = new java.util.Scanner(new java.io.File(filename))) {
            float[][] w = new float[PARAMS][PHASES];
            int i = 0;
            while (sc.hasNextLine() && i < PARAMS) {
                String line = sc.nextLine().replaceAll("[\\[\\]]", "");
                String[] tokens = line.split(",");
                for (int j = 0; j < PHASES && j < tokens.length; j++) {
                    w[i][j] = Float.parseFloat(tokens[j].trim());
                }
                i++;
            }
            return w;
        } catch (Exception e) {
            System.err.println("[loadWeightsFromFile] " + e);
            return null;
        }
    }
}