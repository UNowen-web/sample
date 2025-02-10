import java.sql.*;

public class DatabaseManager {
    private static final String DB_URL = "jdbc:sqlite:data/quiz.db";
    
    public static Connection connect(){
        try{
            Class.forName("org.sqlite.JDBC");
            return DriverManager.getConnection(DB_URL);
        }catch(ClassNotFoundException e){
            System.out.println("Sqlite JDBC ドライバが見つかりません。");
            e.printStackTrace();
            return null;
        }catch(SQLException e){
            e.printStackTrace();
            return null;
        }
    }
}
