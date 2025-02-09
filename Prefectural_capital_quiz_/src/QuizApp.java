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
        List<Integer> questionsToAsk = getQuestionsToAsk(incorrectMode);

        for(int questionId : questionsToAsk){
            Map<String, List<String>> questionData = getQuestionData(questionId);
            String questionText = questionData.keySet().iterator().next();
            List<String> options = questionData.get(questionText);
            String correctAnswer = options.get(0);


            System.out.println("\n" + questionText);
            for(int i = 0; i < options.size(); i++){
                System.out.println((i + 1) + "." + options.get(i));
            }

            System.out.print("番号を入力してください：");
            int userChoice = scanner.nextInt();
            String selectedAnswer = options.get(userChoice - 1);

            boolean isCorrect = selectedAnswer.equals(correctAnswer);
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

        try(Connection conn = DatabaseManager.connect();
            PreparedStatement pstmt = conn.prepareStatement(sql)){
                if(incorrectMode){
                    pstmt.setString(1,userId);
                }
                ResultSet rs = pstmt.executeQuery();
                while(rs.next()){
                    questionsToAsk.add(rs.getInt("question_id"));
                }
            }catch(SQLException e){
                e.printStackTrace();
            }
            return questionsToAsk;
    }

    private static Map<String, List<String>> getQuestionData(int questionId){
        Map<String, List<String>> questionfData = new HashMap<>();
        String sql = "SELECT * FROM questions WHERE question_id = ?";

        try(Connection conn = DatabaseManager.connect();
            PreparedStatement pstmt = conn.prepareStatement(sql)){
                pstmt.setInt(1, questionId);
                ResultSet rs = pstmt.executeQuery();

                if(rs.next()){
                    String questionText = rs.getString("prefecture");
                    String correctAnswer = rs.getString("correct_answer");

                    List<String> options = new ArrayList<>();
                    options.add(correctAnswer);

                    String wrongSql = "SELECT correct_answer FROM questions WHERE correct_answer != ? ORDER BY RANDOM() LIMIT 3";
                    try(PreparedStatement pstmt2 = conn.prepareStatement(wrongSql)){
                        pstmt2.setString(1, correctAnswer);
                        ResultSet wrongRs = pstmt2.executeQuery();
                        while(wrongRs.next()){
                            options.add(wrongRs.getString("correct_answer"));
                        }
                    }

                    Collections.shuffle(options);
                    questionfData.put(questionText, options);
                }
            }catch(SQLException e){
                e.printStackTrace();
            }
            return questionfData;
    }

    private static void recordAnswer(int questionId, String selectedAnswer, boolean isCorrect){
        String sql = "INSERT INTO user_answers (user_id, question_id, selected_answer, is_correct) VALUES (?, ?, ?, ?)";
        
        try (Connection conn = DatabaseManager.connect();
            PreparedStatement pstmt = conn.prepareStatement(sql)){
                pstmt.setString(1,userId);
                pstmt.setInt(2, questionId);
                pstmt.setString(3, selectedAnswer);
                pstmt.setInt(4, isCorrect ? 1:0);
                pstmt.executeUpdate();
            } catch (SQLException e){
                e.printStackTrace();
            }
    }

}

