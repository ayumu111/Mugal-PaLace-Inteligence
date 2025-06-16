// OurPlayerの評価関数パラメータ自動探索スクリプト
// 実行例: java ParamSearch
// 必要に応じてOurPlayerの重みを変更し、対局結果を記録します

package p25x00;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import ap25.league.RandomPlayer;
import ap25.Color;
import ap25.Player;
import myplayer.MyPlayer;
import ap25.Board;
import p25x00.MyEval;


// Removed unnecessary import for Competition25; assumed to be in the same package or already accessible

public class ParamSearch {
    public static void main(String[] args) throws Exception {
        int paramNum = 18;
        float[] WChoices = {-1000, -100, -10, -1, -0.1f, -0.01f, -0.001f, 0, 0.001f, 0.01f, 0.1f, 1, 10, 100, 1000};
        float[][] ParentW, challengerW;
        // float[][] weights = generateWeights(WChoices);
        // ParentW = setW(weights, paramNum, 0);
        ParentW = randomW(WChoices, paramNum);

        // int total = (int)Math.pow(weights.length, paramNum);
        float bestScore = Float.NEGATIVE_INFINITY;
        float[][] bestW = null;
        for (int i = 0; i < 10000; i++) {
            // challengerW = setW(weights, paramNum, i+1);
            challengerW = randomW(WChoices, paramNum);

            // 評価関数を生成
            MyEval evalParent = new MyEval(ap25.Color.BLACK, ParentW);
            MyEval evalChallenger = new MyEval(ap25.Color.BLACK, challengerW);
            // 先手（ParentW）vs 後手（challengerW）
            OurPlayer blackP = new OurPlayer("parent",ap25.Color.BLACK, evalParent, 7);
            OurPlayer whiteC = new OurPlayer("challenger",ap25.Color.WHITE, evalChallenger, 7);
            float score1 = runMatch(blackP, whiteC);
            // 先手（challengerW）vs 後手（ParentW）
            OurPlayer blackC = new OurPlayer("challenger",ap25.Color.BLACK, evalChallenger, 7);
            OurPlayer whiteP = new OurPlayer("parent",ap25.Color.WHITE, evalParent, 7);
            float score2 = runMatch(blackC, whiteP);
            float totalScore = score1 - score2; // 黒のスコア合計

            // 合計スコアが高い方を親として更新
            if (totalScore > 0) {
                ParentW = challengerW;
                if (totalScore > bestScore) {
                    bestScore = totalScore;
                    bestW = challengerW;
                }
            }
        
            if(i % 100 == 0) {
                // 100回ごとに進捗をファイルに出力
                try (FileWriter writer = new FileWriter("progress4.txt", false)) {
                    writer.write("Iteration: " + (i + 1) + ", Score: " + totalScore + "\n");
                    writer.write("Current Best Score: " + bestScore + "\n");
                    if (bestW != null) {
                        writer.write("Best Weights: " + Arrays.deepToString(bestW) + "\n");
                    }
                    writer.write("Current ParentW: " + Arrays.deepToString(ParentW) + "\n");
                    writer.write("Current ChallengerW: " + Arrays.deepToString(challengerW) + "\n");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        try (FileWriter writer = new FileWriter("progress3.txt", false)) {
                    writer.write("Current Best Score: " + bestScore + "\n");
                    if (bestW != null) {
                        writer.write("Best Weights: " + Arrays.deepToString(bestW) + "\n");
                    }
                    writer.write("Current ParentW: " + Arrays.deepToString(ParentW) + "\n");
                } catch (IOException e) {
                    e.printStackTrace();
                }
        // 最良結果を出力
        System.out.println("Best score: " + bestScore);
        if (bestW != null) {
            for (float[] w : bestW) System.out.println(Arrays.toString(w));
        }
        // 最終的な親を出力
        System.out.println("Final ParentW:" + Arrays.deepToString(ParentW));
    }

    // OurPlayer同士で1回対局し、ParentWのスコアを返す
    public static float runMatch(Player black, Player white) {
        ap25.Board board = new ap25.league.OfficialBoard();
        ap25.league.Game game = new ap25.league.Game(board, black, white, 600);
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

    // すべての組み合わせを生成
    private static float[][] generateWeights(float[] choices) {
        int n = choices.length;
        int total = n * n * n;
        float[][] weights = new float[total][3];
        int idx = 0;
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                for (int k = 0; k < n; k++) {
                    weights[idx][0] = choices[i];
                    weights[idx][1] = choices[j];
                    weights[idx][2] = choices[k];
                    idx++;
                }
            }
        }
        return weights;
    }
    private static float[][] setW(float[][] weights, int paramNum, int tryCount) {
        int n = weights.length;
        float[][] W = new float[paramNum][3];
        int x = tryCount;
        for (int p = 0; p < paramNum; p++) {
            int idx = x % n;
            W[p] = Arrays.copyOf(weights[idx], 3);
            x /= n;
        }
        return W;
    }
    // WChoicesの中からランダムに要素を選び、paramNum×3の重み配列を返す
    private static float[][] randomW(float[] WChoices, int paramNum) {
        java.util.Random rand = new java.util.Random();
        float[][] W = new float[paramNum][3];
        for (int i = 0; i < paramNum; i++) {
            for (int j = 0; j < 3; j++) {
                int idx = rand.nextInt(WChoices.length);
                W[i][j] = WChoices[idx];
            }
        }
        return W;
    }
}
