import java.sql.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * クイズの問題を管理するクラス。
 * 
 * - データベースからクイズの問題を取得し、ユーザーに出題する。
 * - 全問出題モードと、間違えた問題のみを出題するモードをサポート。
 * - ユーザーの入力を受け付け、正誤判定を行う。
 * - データベースアクセス時のエラーハンドリングを強化し、ロギングを追加。
 */
public class QuestionManager {
    private static final Logger LOGGER = Logger.getLogger(QuestionManager.class.getName());

    /**
     * クイズを実施するメソッド。
     * 
     * - ユーザーID に基づいて問題を取得し、出題する。
     * - `allQuestions` が `true` の場合、全問題を対象にする。
     * - `allQuestions` が `false` の場合、間違えた問題のみを対象にする。
     * - ユーザーの入力を受け付け、正誤判定を行う。
     * 
     * @param userId       ユーザーID
     * @param allQuestions `true`なら全問出題、`false`なら間違えた問題のみ出題
     * @param scanner      ユーザー入力を受け付ける `Scanner` オブジェクト
     */
    public static void takeQuiz(String userId, boolean allQuestions, Scanner scanner) {
        System.out.println("現在のモード: " + (allQuestions ? "全問解答" : "間違えた問題のみ"));
        List<Integer> questionsToAsk = getQuestionsToAsk(userId, allQuestions);

        if (questionsToAsk.isEmpty()) {
            System.out.println("出題する問題がありません。メニューに戻ります");
            return;
        }

        int totalQuestions = questionsToAsk.size();
        int correctCount = 0, incorrectCount = 0;

        for (int i = 0; i < totalQuestions; i++) {
            int questionId = questionsToAsk.get(i);
            Map<String, List<String>> questionData = getQuestionData(questionId);

            if (questionData.isEmpty()) {
                System.out.println("エラー: 問題データが見つかりませんでした。");
                continue;
            }

            // 問題文の取得
            String questionText = questionData.keySet().stream()
                    .filter(key -> !key.equals("correctAnswer"))
                    .findFirst()
                    .orElse("エラー:問題データなし");

            List<String> options = questionData.get(questionText);
            List<String> correctAnswerList = questionData.get("correctAnswer");

            String correctAnswer = correctAnswerList.get(0);

            System.out.println("\n(残り " + (totalQuestions - i) + " 問)");
            System.out.println("\n" + questionText);
            for (int j = 0; j < options.size(); j++) {
                System.out.println((j + 1) + ". " + options.get(j));
            }

            // ユーザー入力の取得とバリデーション
            System.out.print("番号を入力してください：");
            int userChoice;
            while (true) {
                if (scanner.hasNextInt()) {
                    userChoice = scanner.nextInt();
                    scanner.nextLine(); // 改行をクリア
                    if (userChoice >= 1 && userChoice <= options.size()) {
                        break;
                    }
                } else {
                    scanner.nextLine(); // 無効な入力をクリア
                }
                System.out.println("無効な選択です。もう一度入力してください。");
            }

            // ユーザーの選択肢と正解の比較
            String selectedAnswer = options.get(userChoice - 1);
            boolean isCorrect = selectedAnswer.equalsIgnoreCase(correctAnswer);
            AnswerManager.recordAnswer(userId, questionId, selectedAnswer, isCorrect);

            if (isCorrect) {
                System.out.println("正解です！");
                correctCount++;
            } else {
                System.out.println("不正解です...");
                incorrectCount++;
            }
        }

        // クイズ結果の表示
        System.out.println("\n=== クイズ結果 ===");
        System.out.println("総問題数: " + totalQuestions);
        System.out.println("正解数: " + correctCount);
        System.out.println("不正解数: " + incorrectCount);
    }

    /**
     * 出題する問題のIDを取得するメソッド。
     * 
     * - `allQuestions` が `true` の場合、全問題のIDを取得。
     * - `allQuestions` が `false` の場合、ユーザーが間違えた問題のIDのみを取得。
     * 
     * @param userId       ユーザーID
     * @param allQuestions `true`なら全問、`false`なら間違えた問題のみ
     * @return 出題する問題のIDリスト
     */
    public static List<Integer> getQuestionsToAsk(String userId, boolean allQuestions) {
        List<Integer> questionsToAsk = new ArrayList<>();
        String sql = allQuestions
                ? "SELECT question_id FROM questions"
                : "SELECT DISTINCT question_id FROM user_answers WHERE user_id = ? AND is_correct = 0";
    
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            if (!allQuestions) {
                pstmt.setString(1, userId);
            }
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    questionsToAsk.add(rs.getInt("question_id"));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "データベースエラー: クイズの問題取得に失敗", e);
            throw new RuntimeException("クイズの問題取得中にエラーが発生しました", e);
        }
        return questionsToAsk;
    }
    

    /**
     * 指定した問題IDに対応する問題データを取得するメソッド。
     * 
     * @param questionId 問題ID
     * @return 問題文、選択肢、正解を格納した `Map`
     */
    public static Map<String, List<String>> getQuestionData(int questionId) {
        Map<String, List<String>> questionData = new HashMap<>();
        List<String> correctAnswerList = new ArrayList<>();
        List<String> options = new ArrayList<>();

        String sql = "SELECT * FROM questions WHERE question_id = ?";

        try (Connection conn = DatabaseManager.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, questionId);
            ResultSet rs = pstmt.executeQuery();

            if (!rs.next()) {
                LOGGER.warning("問題ID " + questionId + " のデータが見つかりませんでした。");
                return questionData;
            }

            // 問題文と正解の取得
            String questionText = rs.getString("prefecture");
            String correctAnswer = rs.getString("correct_answer").trim();
            correctAnswerList.add(correctAnswer);
            options.add(correctAnswer);

            // 間違いの選択肢を取得し、リストに追加
            String wrongSql = "SELECT correct_answer FROM questions WHERE correct_answer != ? ORDER BY RANDOM() LIMIT 3";
            try (PreparedStatement pstmt2 = conn.prepareStatement(wrongSql)) {
                pstmt2.setString(1, correctAnswer);
                ResultSet wrongRs = pstmt2.executeQuery();
                while (wrongRs.next()) {
                    options.add(wrongRs.getString("correct_answer").trim());
                }
            }

            if (!options.isEmpty()) {
                Collections.shuffle(options);
            }

            questionData.put(questionText, options);
            questionData.put("correctAnswer", correctAnswerList);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "データベースエラー: 問題データの取得に失敗", e);
            throw new RuntimeException("問題データの取得中にエラーが発生しました", e);
        }
        return questionData;
    }
}
