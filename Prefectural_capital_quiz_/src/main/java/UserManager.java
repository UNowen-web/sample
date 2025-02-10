import java.sql.*;
import java.util.Scanner;

public class UserManager {
    private static final Scanner scanner = new Scanner(System.in);

    public static String loginOrRegister(){
        System.out.println("1:ログイン 2:新規登録");
        int choice = scanner.nextInt();
        scanner.nextLine();

        if(choice == 1){
            return loginUser();
        }else{
            return registerUser();
        }
    }
    


public static String registerUser(){
    System.out.print("新しいユーザーIDを入力してください(半角英数字&記号8文字以下):");
    String userId = scanner.nextLine();

    System.out.print("パスワードを入力してください(半角英数字&記号8文字以下):");
    String password = scanner.nextLine();

    String sql = "INSERT INTO users(user_id, password) VALUES (?, ?)";
    try (Connection conn = DatabaseManager.connect();
        PreparedStatement pstmt = conn.prepareStatement(sql)){
            pstmt.setString(1, userId);
            pstmt.setString(2, password);
            pstmt.executeUpdate();
            System.out.println("ユーザー登録が完了しました！");
            return userId;
    }catch(SQLException e){
        System.out.println("登録に失敗しました。別のユーザーIDを試してください。");
        return loginOrRegister();
    }
}

public static String loginUser(){
    System.out.print("ユーザーIDを入力してください:");
    String userId = scanner.nextLine();

    System.out.print("パスワードを入力してください:");
    String password = scanner.nextLine();

    String sql = "SELECT * FROM users WHERE user_id = ? AND password = ?";
    try (Connection conn = DatabaseManager.connect();
        PreparedStatement pstmt = conn.prepareStatement(sql)){
            pstmt.setString(1, userId);
            pstmt.setString(2, password);
            ResultSet rs = pstmt.executeQuery();

            if(rs.next()){
                System.out.println("ログイン成功！");
                return userId;
            }else{
                System.out.println("ログインに失敗しました。もう一度試してください。");
                return loginOrRegister();
            }
        }catch(SQLException e){
            e.printStackTrace();
            return null;
        }
    
    }

}