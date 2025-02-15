import java.sql.*;
import java.util.Scanner;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * ユーザー管理を行うクラス。
 * - ユーザーのログイン、新規登録を管理。
 * - パスワードのハッシュ化を行い、安全にデータベースへ保存。
 * - `Scanner` の管理を適切に行い、リソースリークを防ぐ。
 * - 入力チェックを強化し、不正なデータ入力を防ぐ。
 */
public class UserManager {
    private static final Scanner scanner = new Scanner(System.in);
    private static final Logger LOGGER = Logger.getLogger(UserManager.class.getName()); // ログ管理用
    private static final int MAX_LOGIN_ATTEMPTS = 3; // 最大ログイン試行回数
    private static final String VALID_PATTERN = "^[a-zA-Z0-9!@#$%^&*()_+=-]{1,8}$"; // 半角英数字記号 1~8文字

    /**
     * ユーザーにログインまたは新規登録を選択させるメソッド。
     * 
     * @return ログイン成功したユーザーID
     */
    public static String loginOrRegister() {
        System.out.println("1:ログイン 2:新規登録");
        int choice = getValidChoice(1, 2); // 1 または 2 のみ許可

        if (choice == 1) {
            return loginUser();
        } else {
            return registerUser();
        }
    }

    /**
     * ユーザーを新規登録するメソッド。
     * 
     * @return 登録したユーザーID
     */
    public static String registerUser() {
        String userId;
        do {
            System.out.print("新しいユーザーIDを入力してください(半角英数字&記号8文字以下): ");
            userId = scanner.nextLine();
            if (!isValidInput(userId)) {
                System.out.println("エラー: ユーザーIDは半角英数字と記号のみ、8文字以下で入力してください。");
            }
        } while (!isValidInput(userId));
        
        String password;
        do {
            System.out.print("パスワードを入力してください(半角英数字&記号8文字以下): ");
            password = scanner.nextLine();
            if (!isValidInput(password)) {
                System.out.println("エラー: パスワードは半角英数字と記号のみ、8文字以下で入力してください。");
            }
        } while (!isValidInput(password));
        String hashedPassword = hashPassword(password); // パスワードをハッシュ化

        // 既存のユーザーIDと重複していないかチェック
        if (isUserExists(userId)) {
            System.out.println("このユーザーIDは既に存在します。別のIDを試してください。");
            return loginOrRegister(); // `Scanner` を渡す
        }

        // データベースにユーザー情報を登録
        String sql = "INSERT INTO users(user_id, password) VALUES (?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, userId);
            pstmt.setString(2, hashedPassword);
            pstmt.executeUpdate();
            System.out.println("ユーザー登録が完了しました！");
            return userId;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "ユーザー登録エラー", e);
            System.out.println("登録に失敗しました。もう一度試してください。");
            return loginOrRegister();
        }
    }

    /**
     * ユーザーをログインさせるメソッド。
     * - 最大 `MAX_LOGIN_ATTEMPTS` 回の試行が可能。
     * - ログイン成功するとユーザーIDを返す。
     * 
     * @return ログイン成功したユーザーID
     */
    public static String loginUser() {
        int attempts = 0;

        while (attempts < MAX_LOGIN_ATTEMPTS) {
            System.out.print("ユーザーIDを入力してください: ");
            String userId = scanner.nextLine();

            System.out.print("パスワードを入力してください: ");
            String password = scanner.nextLine();
            String hashedPassword = hashPassword(password);

            String sql = "SELECT user_id FROM users WHERE user_id = ? AND password = ?";
            try (Connection conn = DatabaseManager.getConnection();
                    PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, userId);
                pstmt.setString(2, hashedPassword);
                ResultSet rs = pstmt.executeQuery();

                if (rs.next()) {
                    System.out.println("ログイン成功！");
                    return userId;
                } else {
                    attempts++;
                    System.out.println("ログインに失敗しました。残り試行回数: " + (MAX_LOGIN_ATTEMPTS - attempts));
                }
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "ログイン処理エラー", e);
                System.err.println("システムエラーが発生しました。ログインを終了します。");
                System.exit(1); // システム異常時は強制終了
                return null;
            }
        }

        System.out.println("ログイン試行回数が上限に達しました。しばらくしてから再試行してください。");
        System.exit(1); // 明示的にプログラムを終了
        return null;
    }

    /**
     * 指定したユーザーIDが既に存在するかをデータベースで確認するメソッド。
     * 
     * @param userId 確認するユーザーID
     * @return 存在する場合は `true`、存在しない場合は `false`
     */
    private static boolean isUserExists(String userId) {
        String sql = "SELECT user_id FROM users WHERE user_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, userId);
            ResultSet rs = pstmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "データベースエラー: ユーザーの存在確認に失敗", e);
            return false;
        }
    }

    /**
     * 数値の入力を検証し、適切な選択肢を取得するメソッド。
     * 
     * - 数値以外の入力がされた場合、再入力を求める。
     * - `nextInt()` の後に `nextLine()` を呼び、バッファをクリアする。
     *
     * @param min     選択肢の最小値
     * @param max     選択肢の最大値
     * @return 入力された有効な選択肢
     */
    private static int getValidChoice(int min, int max) {
        int choice;
        while (true) {
            System.out.print("選択肢を入力してください");
            String input = scanner.nextLine();
            try {
                choice = Integer.parseInt(input);
                if (choice >= min && choice <= max) {
                    return choice;
                } else {
                    System.out.println("エラー: " + min + " から " + max + " の間の数字を入力してください。");
                }
            } catch (NumberFormatException e) {
                System.out.println("エラー: 数字を入力してください。");
            }
        }
    }

    /**
     * パスワードをSHA-256でハッシュ化するメソッド。
     * 
     * - SHA-256 を使用してパスワードをハッシュ化。
     * - `bcrypt` などのより安全なハッシュ関数に移行することが推奨される。
     * 
     * @param password ハッシュ化するパスワード
     * @return ハッシュ化されたパスワード
     */
    private static String hashPassword(String password) {
        try {
            // SHA-256 の MessageDigest インスタンスを取得
            MessageDigest md = MessageDigest.getInstance("SHA-256");

            // パスワードのバイト配列をハッシュ化
            byte[] hashBytes = md.digest(password.getBytes());

            // ハッシュ値を16進数文字列に変換
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                hexString.append(String.format("%02x", b));
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("パスワードのハッシュ化に失敗しました。", e);
        }
    }

    /**
     * 入力値が半角英数字記号で8文字以下かどうかを判定するメソッド
     * @param input ユーザーが入力したIDまたはパスワード
     * @return 有効な入力ならtrue、無効ならfalse
     */
    private static boolean isValidInput(String input) {
        return Pattern.matches(VALID_PATTERN, input);
    }
}
