import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * データベース接続を管理するクラス。
 * - シングルトンパターンを使用して、データベース接続を1つだけ維持する。
 */
public class DatabaseManager {
    private static final String DB_URL = "jdbc:sqlite:data/quiz.db"; // SQLite データベースのパス
    private static Connection connection = null;
    private static final Logger LOGGER = Logger.getLogger(DatabaseManager.class.getName());

    /**
     * データベースへの接続を確立し、1つの `Connection` を維持する。
     *
     * @return `Connection` インスタンス（すでに開いている場合は同じものを返す）
     */
    public static Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                Class.forName("org.sqlite.JDBC"); // JDBC ドライバをロード
                connection = DriverManager.getConnection(DB_URL);
            }
        } catch (ClassNotFoundException e) {
            LOGGER.log(Level.SEVERE, "SQLite JDBC ドライバが見つかりません。", e);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "データベース接続に失敗しました。", e);
        }
        return connection;
    }

    /**
     * アプリ終了時にデータベース接続をクローズするメソッド。
     */
    public static void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
                connection = null;
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "データベース接続のクローズに失敗しました。", e);
            }
        }
    }
}
