import java.sql.*; //JDBC　API を使用するためのインポート

public class DatabaseManager {
    // SQlite データベースのファイルパス（data/quiz.dbに保存される）
    private static final String DB_URL = "jdbc:sqlite:data/quiz.db";

    /**
     * データベースへの接続を確率するメソッド。
     * 
     * @return 成功時は Connection オブジェクト、失敗時はnullを返す。
     */
    public static Connection connect() {
        try {
            // SQlite JDBC ドライバをロード
            Class.forName("org.sqlite.JDBC");

            // データベースに接続
            return DriverManager.getConnection(DB_URL);
        } catch (ClassNotFoundException e) {

            // JDBC ドライバが見つからない場合のエラーハンドリング
            System.out.println("Sqlite JDBC ドライバが見つかりません。");
            e.printStackTrace();
            return null;
        } catch (SQLException e) {

            // データベース接続に失敗した場合のエラーハンドリング
            e.printStackTrace();
            return null;
        }
    }
}
