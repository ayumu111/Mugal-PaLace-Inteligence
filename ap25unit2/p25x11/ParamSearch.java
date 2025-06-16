// OurPlayerの評価関数パラメータ自動探索スクリプト
// 実行例: java ParamSearch
// 必要に応じてOurPlayerの重みを変更し、対局結果を記録します

package p25x11;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import ap25.league.RandomPlayer;
import ap25.Color;
import ap25.Player;
import myplayer.MyPlayer;
import ap25.Board;


// Removed unnecessary import for Competition25; assumed to be in the same package or already accessible

public class ParamSearch {
    public static void main(String[] args) throws Exception {
        // 探索範囲例（必要に応じて調整）
        float[] w1s = {10, 50};
        float[] w2s = {1, 10};
        float[] w3s = {-1, -10};
        float[] w4s = {1, 5};
        float[] w5s = {-1, -5};

        float bestScore = Float.NEGATIVE_INFINITY;
        float[] bestParams = new float[5];
        StringBuilder log = new StringBuilder();

        for (float w1 : w1s) for (float w2 : w2s) for (float w3 : w3s) for (float w4 : w4s) for (float w5 : w5s) {
            // パラメータをセット
            OurPlayer.setEvalWeights(w1, w2, w3, w4, w5);
            // 対局を実行（例: 10回）
            float totalScore = 0;
            for (int i = 0; i < 10; i++) {
                float score = runMatch(new OurPlayer(Color.BLACK), new MyPlayer(Color.WHITE));
                totalScore += score;
            }
            float avgScore = totalScore / 10;
            log.append(String.format("w1=%.1f w2=%.1f w3=%.1f w4=%.1f w5=%.1f avgScore=%.2f\n", w1, w2, w3, w4, w5, avgScore));
            if (avgScore > bestScore) {
                bestScore = avgScore;
                bestParams = new float[]{w1, w2, w3, w4, w5};
            }
        }
        // 結果をファイルに保存
        try (FileWriter fw = new FileWriter("param_search_result.txt")) {
            fw.write(log.toString());
            fw.write("Best: " + Arrays.toString(bestParams) + " score=" + bestScore + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Best params: " + Arrays.toString(bestParams) + " score=" + bestScore);
    }

    // OurPlayerとRandomPlayerで1回対局し、OurPlayerのスコアを返す
    public static float runMatch(Player black, Player white) {
        ap25.Board board = new ap25.league.OfficialBoard();
        ap25.league.Game game = new ap25.league.Game(board, black, white, 60);
        game.play();
        // boardは初期盤面のままなので、game.boardはprivateなのでscore()を取得できない
        // 代わりにGameクラスにgetBoard()を追加して取得する
        Board finalBoard = getBoardReflect(game);
        System.out.println(finalBoard.score());
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
