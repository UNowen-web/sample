import java.sql.*; // JDBCを使用するためのインポート
import java.util.*;// ユーティリティクラス（List,Map,Scannerなど）を使用するためのインポート

public class QuizApp {
    private static final Scanner scanner = new Scanner(System.in); // ユーザーの入力を受け付けるScannerインスタンス
    private static String userId; // 現在ログインしているユーザーのIDを保存する変数

    public static void main(String[] args) {
        System.out.println("クイズアプリへようこそ！");
        userId = UserManager.loginOrRegister(); // ユーザーのログインまたは新規登録処理

        // メインメニューのループ
        while (true) {
            System.out.println("\nメニュー:");
            System.out.println("1. 全問解答");
            System.out.println("2. 間違えた問題を解答");
            System.out.println("3. 終了");
            System.out.print("選択肢を入力してください:");
            int choice = scanner.nextInt(); // ユーザーの選択を取得

            switch (choice) {
                case 1:
                    takeQuiz(true); // 全問解答モードを実行
                    break;
                case 2:
                    takeQuiz(false); // 間違えた問題のみ解答モードを実行
                    break;
                case 3:
                    System.out.println("終了します。");
                    return;
                default:
                    System.out.println("無効な選択です。再度入力してください。");

            }
        }
    }

    /**
     * クイズの出題を行うメソッド
     * 
     * @param incorrectMode 全問出題はtrue、間違えた問題のみを出題する場合はfalse
     */
    private static void takeQuiz(boolean incorrectMode) {
        System.out.println("現在のモード: " + (!incorrectMode ? "間違えた問題のみ" : "全問解答")); // モード変更
        List<Integer> questionsToAsk = getQuestionsToAsk(incorrectMode);// 出題する問題IDを取得

        if (questionsToAsk.isEmpty()) {
            System.out.println("出題する問題がありません。メニューに戻ります");
            return;
        }

        int totalQuestions = questionsToAsk.size(); //問題数
        int correctCount = 0; //正解数
        int incorrectCount = 0; //不正解数

        for (int i = 0; i < totalQuestions; i++) {
            int questionId = questionsToAsk.get(i);
            Map<String, List<String>> questionData = getQuestionData(questionId);// 問題データを取得
            String questionText = questionData.keySet().stream()
                    .filter(key -> !key.equals("correctAnswer")) // "correctAnswer" キーを除外
                    .findFirst()
                    .orElse("エラー:問題データなし");
            List<String> options = questionData.get(questionText); // 選択肢リストを取得
            List<String> correctAnswerList = questionData.get("correctAnswer"); // 正解リストを取得

            if (correctAnswerList == null) {
                System.out.println("エラー: 正解データが見つかりませんでした。データベースを確認してください。");
                continue;
            }

            String correctAnswerShuffled = correctAnswerList.get(0); // シャッフル後の正解

            //残りの問題数を表示
            System.out.println("\n(残り" + (totalQuestions - i) + "問)");

            // 出題
            System.out.println("\n" + questionText);
            for (int j = 0;  j < options.size(); j++) {
                System.out.println((j + 1) + "." + options.get(j));
            }

            // ユーザーの解答を取得
            System.out.print("番号を入力してください：");
            int userChoice = scanner.nextInt();

            // 入力が正しい範囲内か確認(範囲外なら再入力させる)
            if (userChoice < 1 || userChoice > options.size()) {
                System.out.println("無効な選択です。もう一度入力してください。");
                i--; //やりなおし
                continue;
            } 
                String selectedAnswer = options.get(userChoice - 1);

                // 正誤判定
                boolean isCorrect = selectedAnswer.trim().equalsIgnoreCase(correctAnswerShuffled.trim());

                recordAnswer(questionId, selectedAnswer, isCorrect); // 解答結果を記録

                if (isCorrect) {
                    System.out.println("正解です！");
                    correctCount++;
                } else {
                    System.out.println("不正解です...");
                    incorrectCount++;
                }
        }

        //最終結果を表示
        System.out.println("\n===クイズ結果===");
        System.out.println("総問題数:" + totalQuestions);
        System.out.println("正解数" + correctCount);
        System.out.println("不正解数" + incorrectCount);
    }

    /**
     * 出題する問題のIDを取得するメソッド
     * 
     * @param incorrectMode 全問対象はtrue、間違えた問題のみを対象にする場合はfalse
     * @return 出題する問題のIDのリスト
     */
    private static List<Integer> getQuestionsToAsk(boolean incorrectMode) {
        List<Integer> questionsToAsk = new ArrayList<>();
        String sql = incorrectMode
                ? "SELECT question_id FROM questions" // trueのとき全問解答
                : "SELECT DISTINCT question_id FROM user_answers WHERE user_id = ? AND is_correct = 0"; // falseのときのみ間違えた解答

        try (Connection conn = DatabaseManager.connect();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            if (!incorrectMode) {
                pstmt.setString(1, userId);
            }
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                questionsToAsk.add(rs.getInt("question_id"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        if (questionsToAsk.isEmpty()) {
            System.out.println("エラー:出題する問題がありません。");
            return questionsToAsk;
        }
        return questionsToAsk;
    }

    /*
     * 指定した問題IDに対応する問題データを取得
     * 
     * @param questionId 問題のID
     * 
     * @RETURN 問題文、選択肢、正解を格納した Map
     */

    private static Map<String, List<String>> getQuestionData(int questionId) {
        Map<String, List<String>> questionData = new HashMap<>();
        List<String> correctAnswerList = new ArrayList<>();
        List<String> options = new ArrayList<>();
        String correctAnswer = "";

        String sql = "SELECT * FROM questions WHERE question_id = ?";

        try (Connection conn = DatabaseManager.connect();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, questionId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                String questionText = rs.getString("prefecture");
                correctAnswer = rs.getString("correct_answer").trim();

                correctAnswerList.add(correctAnswer.trim());

                options.add(correctAnswer.trim());

                // 他の選択肢を取得
                String wrongSql = "SELECT correct_answer FROM questions WHERE correct_answer != ? ORDER BY RANDOM() LIMIT 3";
                try (PreparedStatement pstmt2 = conn.prepareStatement(wrongSql)) {
                    pstmt2.setString(1, correctAnswer);
                    ResultSet wrongRs = pstmt2.executeQuery();
                    while (wrongRs.next()) {
                        options.add(wrongRs.getString("correct_answer").trim());
                    }
                }

                Collections.shuffle(options); // 選択肢をシャッフル

                // 正解の選択肢を正しく比較する。
                questionData.put(questionText, options);
                questionData.put("correctAnswer", correctAnswerList); // 正解のインデックスを渡す

            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return questionData;
    }

    /*
     * ユーザーの解答結果をデータベースに記録
     * 
     * @param questionID 問題のID
     * 
     * @param selectedAnswer ユーザーの選択した解答
     * 
     * @param isCorrect 正誤判定(正解:true, 不正解:false)
     */
    private static void recordAnswer(int questionId, String selectedAnswer, boolean isCorrect) {
        String sql = "INSERT INTO user_answers (user_id, question_id, selected_answer, is_correct) VALUES (?, ?, ?, ?)";

        try (Connection conn = DatabaseManager.connect()) {
            if (isCorrect) {
                // 正解した場合、間違えた問題の記録を更新
                sql = "UPDATE user_answers SET is_correct = 1 WHERE user_id = ? AND question_id = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setString(1, userId);
                    pstmt.setInt(2, questionId);
                    pstmt.executeUpdate();
                }
            } else {
                // 不正解の場合、新たに間違えた問題として記録
                sql = "INSERT INTO user_answers (user_id, question_id, selected_answer, is_correct) VALUES(?, ?, ?, ?)";
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setString(1, userId);
                    pstmt.setInt(2, questionId);
                    pstmt.setString(3, selectedAnswer);
                    pstmt.setBoolean(4, isCorrect);
                    pstmt.executeUpdate();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

}
