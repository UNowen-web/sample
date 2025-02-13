import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * クイズの解答をデータベースに記録するクラス。
 */
public class AnswerManager {
    private static final Logger LOGGER = Logger.getLogger(AnswerManager.class.getName());

    /**
     * ユーザーの解答結果をデータベースに記録するメソッド。
     * `ON CONFLICT` を使用せず、データの存在を確認して `INSERT` または `UPDATE` を適用。
     * 
     * @param userId ユーザーID
     * @param questionId 問題ID
     * @param selectedAnswer ユーザーの選択した解答
     * @param isCorrect 正誤判定（true = 正解、false = 不正解）
     */
    public static void recordAnswer(String userId, int questionId, String selectedAnswer, boolean isCorrect) {
        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false); // ✅ 明示的にトランザクションを開始

            // ✅ まず、解答データが既に存在するか確認
            boolean exists = checkIfAnswerExists(conn, userId, questionId);

            if (exists) {
                // ✅ すでにデータがある場合は UPDATE
                updateAnswer(conn, userId, questionId, selectedAnswer, isCorrect);
            } else {
                // ✅ データがない場合は INSERT
                insertAnswer(conn, userId, questionId, selectedAnswer, isCorrect);
            }

            conn.commit(); // ✅ 成功したらコミット
            LOGGER.info("解答が記録されました: " + userId + " -> Q" + questionId);

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "データベースエラー: 解答の記録に失敗", e);
            try (Connection conn = DatabaseManager.getConnection()) {
                if (conn != null && !conn.getAutoCommit()) {
                    conn.rollback(); // ❌ エラー発生時にロールバック
                }
            } catch (SQLException rollbackError) {
                LOGGER.log(Level.SEVERE, "ロールバックの実行に失敗しました", rollbackError);
            }
            throw new RuntimeException("解答の記録中にエラーが発生しました", e);
        }
    }

    /**
     * 指定した `userId` と `questionId` の解答データが存在するか確認。
     * 
     * @param conn データベース接続
     * @param userId ユーザーID
     * @param questionId 問題ID
     * @return すでに解答がある場合 `true`、ない場合 `false`
     */
    private static boolean checkIfAnswerExists(Connection conn, String userId, int questionId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM user_answers WHERE user_id = ? AND question_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, userId);
            pstmt.setInt(2, questionId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    /**
     * すでに存在する解答を更新する。
     * 
     * @param conn データベース接続
     * @param userId ユーザーID
     * @param questionId 問題ID
     * @param selectedAnswer ユーザーの選択した解答
     * @param isCorrect 正誤判定
     */
    private static void updateAnswer(Connection conn, String userId, int questionId, String selectedAnswer, boolean isCorrect) throws SQLException {
        String sql = "UPDATE user_answers SET selected_answer = ?, is_correct = ? WHERE user_id = ? AND question_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, selectedAnswer);
            pstmt.setBoolean(2, isCorrect);
            pstmt.setString(3, userId);
            pstmt.setInt(4, questionId);
            pstmt.executeUpdate();
        }
    }

    /**
     * 新しい解答データを挿入する。
     * 
     * @param conn データベース接続
     * @param userId ユーザーID
     * @param questionId 問題ID
     * @param selectedAnswer ユーザーの選択した解答
     * @param isCorrect 正誤判定
     */
    private static void insertAnswer(Connection conn, String userId, int questionId, String selectedAnswer, boolean isCorrect) throws SQLException {
        String sql = "INSERT INTO user_answers (user_id, question_id, selected_answer, is_correct) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, userId);
            pstmt.setInt(2, questionId);
            pstmt.setString(3, selectedAnswer);
            pstmt.setBoolean(4, isCorrect);
            pstmt.executeUpdate();
        }
    }
}
