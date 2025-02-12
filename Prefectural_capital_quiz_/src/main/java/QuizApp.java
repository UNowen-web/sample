import java.sql.*;
import java.util.*;

public class QuizApp {
    private static final Scanner scanner = new Scanner(System.in);
    private static String userId;

    public static void main(String[] args){
        System.out.println("クイズアプリへようこそ！");
        userId = UserManager.loginOrRegister();

        while(true){
            System.out.println("\nメニュー:");
            System.out.println("1. 全問回答");
            System.out.println("2. 間違えた問題を回答");
            System.out.println("3. 終了");
            System.out.print("選択肢を入力してください:");
            int choice = scanner.nextInt();

            switch(choice){
                case 1:
                    takeQuiz(false);
                    break;
                case 2:
                    takeQuiz(true);
                    break;
                case 3:
                    System.out.println("終了します。");
                    return;
                default:
                    System.out.println("無効な選択です。再度入力してください。");

            }
        }
    }
    
    private static void takeQuiz(boolean incorrectMode){

        //DEBUG
        System.out.println("現在のモード:" + (incorrectMode ? "間違えた問題のみ" : "全問解答"));

        List<Integer> questionsToAsk = getQuestionsToAsk(incorrectMode);

        for(int questionId : questionsToAsk){
            Map<String, List<String>> questionData = getQuestionData(questionId);
            String questionText = questionData.keySet().stream()
                .filter(key -> !key.equals("correctAnswer")) //"correctAnswer" キーを除外
                .findFirst()
                .orElse("エラー:問題データなし");
            List<String> options = questionData.get(questionText);
            List<String> correctAnswerList = questionData.get("correctAnswer");

            //debug
            //System.out.println("取得した questionData: " + questionData);
            //System.out.println("取得した correctAnswerList: " + correctAnswerList);


            if(correctAnswerList == null){
                System.out.println("エラー: 正解データが見つかりませんでした。データベースを確認してください。");
                continue;
            }

            String correctAnswerShuffled = correctAnswerList.get(0); //シャッフル後の正解


            System.out.println("\n" + questionText);
            for(int i = 0; i < options.size(); i++){
                System.out.println((i + 1) + "." + options.get(i));
            }

            System.out.print("番号を入力してください：");
            int userChoice = scanner.nextInt();
            String selectedAnswer = options.get(userChoice - 1);

            //デバッグ
            //System.out.println("選択肢:["+ selectedAnswer +"]");  //デバッグ用後で消す
            //System.out.println("正解:["+correctAnswer+"]"); //デバッグ用後で消す

            boolean isCorrect = selectedAnswer.trim().equalsIgnoreCase(correctAnswerShuffled.trim());
            
            //デバッグ..
            //System.out.println("ユーザーの選択: [" + selectedAnswer + "] 正解: [" + correctAnswer + "] 判定: " + isCorrect);

            recordAnswer(questionId, selectedAnswer, isCorrect);
            

            if(isCorrect){
                System.out.println("正解です！");
            }else{
                System.out.println("不正解です...");
            }
        }
    }
    
    private static List<Integer> getQuestionsToAsk(boolean incorrectMode){
        List<Integer> questionsToAsk = new ArrayList<>();
        String sql = incorrectMode
        ? "SELECT DISTINCT question_id FROM user_answers WHERE user_id = ? AND is_correct = 0"
        : "SELECT question_id FROM questions";

        //debug
        System.out.println("実行するクエリ" + sql);

        try(Connection conn = DatabaseManager.connect();
            PreparedStatement pstmt = conn.prepareStatement(sql)){
                if(incorrectMode){
                    pstmt.setString(1,userId);
                }
                ResultSet rs = pstmt.executeQuery();
                while(rs.next()){
                    questionsToAsk.add(rs.getInt("question_id"));
                }

                // debug
                System.out.println("取得した問題リスト (" + (incorrectMode ? "間違えた問題のみ" : "全問題") + "): " + questionsToAsk);

            }catch(SQLException e){
                e.printStackTrace();
            }
            return questionsToAsk;
    }

    private static Map<String, List<String>> getQuestionData(int questionId){
        Map<String, List<String>> questionData = new HashMap<>();
        List<String> correctAnswerList = new ArrayList<>();
        List<String> options = new ArrayList<>();
        String correctAnswer = "";

        String sql = "SELECT * FROM questions WHERE question_id = ?";

        try(Connection conn = DatabaseManager.connect();
            PreparedStatement pstmt = conn.prepareStatement(sql)){
                pstmt.setInt(1, questionId);
                ResultSet rs = pstmt.executeQuery();

                if(rs.next()){
                    String questionText = rs.getString("prefecture");
                    correctAnswer = rs.getString("correct_answer").trim();
                   

                    correctAnswerList.add(correctAnswer.trim());

                    
                    options.add(correctAnswer.trim());

                    String wrongSql = "SELECT correct_answer FROM questions WHERE correct_answer != ? ORDER BY RANDOM() LIMIT 3";
                    try(PreparedStatement pstmt2 = conn.prepareStatement(wrongSql)){
                        pstmt2.setString(1, correctAnswer);
                        ResultSet wrongRs = pstmt2.executeQuery();
                        while(wrongRs.next()){
                            options.add(wrongRs.getString("correct_answer").trim());
                        }
                    }
              
                    //debug
                    //System.out.println("問題:[" + questionText +"]");
                    //System.out.println("正解: [" + correctAnswer +"]");
                    //System.out.println("選択肢リスト:" + options);

                    //debug
                    //System.out.println("シャッフル前の選択肢" + options);
                    //System.out.println("シャッフル前の正解: [" + correctAnswer +"]");

                    int correctIndexBeforeShuffle = options.indexOf(correctAnswer);
                    if (correctIndexBeforeShuffle == -1){
                        System.out.println("警告: シャッフル前に正解が見つかりませんでした。データを確認してください。");
                        return null;
                    }

                    Collections.shuffle(options);

                    //new code シャッフル後の正解の位置を再取得する
                    int correctIndexAfterShuffle = options.indexOf(correctAnswer);
                    if (correctIndexAfterShuffle == -1){
                        System.out.println("警告: シャッフル後に正解が見つかりませんでした。データを確認してください。");
                        return null;
                    }

                    //debug
                    //System.out.println("シャッフル後の選択肢" + options);
                    //System.out.println("シャッフル後の正解: [" + correctAnswer +"]");

                    //new code 正解の選択肢を正しく比較する。
                    questionData.put(questionText, options);
                    questionData.put("correctAnswer", correctAnswerList); //正解のインデックスを渡す
                    
                }
            }catch(SQLException e){
                e.printStackTrace();
            }
            //DEBUG
            System.out.println("取得した questionData: [" + questionData +"]");

            return questionData;
    }

    private static void recordAnswer(int questionId, String selectedAnswer, boolean isCorrect){
        String sql = "INSERT INTO user_answers (user_id, question_id, selected_answer, is_correct) VALUES (?, ?, ?, ?)";
        
        try (Connection conn = DatabaseManager.connect()){
            if(isCorrect){
                //間違えた問題を正解したらレコードを１に更新

                sql = "UPDATE user_answers SET is_correct = 1 WHERE user_id = ? AND question_id = ?";
                try(PreparedStatement pstmt = conn.prepareStatement(sql)){
                    pstmt.setString(1, userId);
                    pstmt.setInt(2, questionId);
                    pstmt.executeUpdate();
                }
            }else{
                //新しい間違いを記録
                sql = "INSERT INTO user_answers (user_id, question_id, selected_answer, is_correct) VALUES(?, ?, ?, ?)";
                try(PreparedStatement pstmt = conn.prepareStatement(sql)){
                    pstmt.setString(1,userId);
                    pstmt.setInt(2, questionId);
                    pstmt.setString(3, selectedAnswer);
                    pstmt.setBoolean(4, isCorrect);
                    pstmt.executeUpdate();}
                }
        }   catch (SQLException e){
                e.printStackTrace();
        }
    }

}

