import java.sql.*;

public class DatabaseManager {
    private static final String DB_URL = "jdbc:sqlite:quiz.db";
    
    public static Connection connect(){
        try{
            return DriverManager.getConnection(DB_URL);
        }catch(SQLException e){
            e.printStackTrace();
            return null;
        }
    }
}
