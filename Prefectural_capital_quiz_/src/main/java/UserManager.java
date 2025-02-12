import java.sql.*; //JDBCを使用するためのインポート
import java.util.Scanner; // ユーザー入力を受け付けるためのScannerをインポート
import java.security.MessageDigest; //パスワードハッシュ化のためのインポート
import java.security.NoSuchAlgorithmException; //ハッシュ化エラー処理

/*
 * ユーザーのログインおよび登録を管理するクラス
 */
public class UserManager {
    private static final Scanner scanner = new Scanner(System.in); // ユーザー入力用のScannerインスタンス
    private static final int MAX_LOGIN_ATTEMPTS = 3; // ログイン試行回数の制限
    private static final String VALID_PATTERN = "^[a-zA-Z0-9!@#$%^&*()_+=\\-\\[\\]{};:'\",.<>?/\\\\|]+$";

    /*
     * ユーザーにログインまたは新規登録を選択させるメソッド
     * 
     * @return ログイン成功したユーザーのID
     */
    public static String loginOrRegister() {
        System.out.println("1:ログイン 2:新規登録"); // 選択肢の表示
        int choice = scanner.nextInt(); // ユーザーの選択を取得
        scanner.nextLine(); // 改行

        if (choice == 1) {
            return loginUser(); // ログイン処理を実行
        } else {
            return registerUser(); // 新規登録処理を実行
        }
    }

    /*
     * 新規ユーザーを登録するメソッド
     * 
     * @return 登録成功したユーザーのID
     */
    public static String registerUser() {
        System.out.print("新しいユーザーIDを入力してください(半角英数字&記号8文字以下):");
        String userId = scanner.nextLine(); // ユーザーIDの入力

        // 入力チェック(半角英数字と記号のみ、1-8文字)
        if (!isValidInput(userId) || userId.length() < 1 || userId.length() > 8) {
            System.out.println("エラー:　ユーザーIDは半角英数字と記号のみ、1-8文字にしてください。");
            return loginOrRegister();
        }

        // ユーザーIDの重複チェック
        if (isUserExists(userId)) {
            System.out.println("このユーザーIDは既に存在します。別のIDを試してください。");
            return loginOrRegister();
        }

        System.out.print("パスワードを入力してください(半角英数字&記号8文字以下):");
        String password = scanner.nextLine(); // パスワードの入力

        // 入力チェック(半角英数字と記号のみ、1-8文字)
        if (!isValidInput(password) || password.length() < 1 || password.length() > 8) {
            System.out.println("エラー: パスワードは半角英数字と記号のみ、1-8文字にしてください。");
            return loginOrRegister();
        }

        String hashedPassword = hashPassword(password); // パスワードをハッシュ化

        // データベースにユーザー情報を登録
        String sql = "INSERT INTO users(user_id, password) VALUES (?, ?)";
        try (Connection conn = DatabaseManager.connect();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, userId);
            pstmt.setString(2, hashedPassword);
            pstmt.executeUpdate(); // SQLを実行してデータを挿入
            System.out.println("ユーザー登録が完了しました！");
            return userId; // 登録が成功したユーザーIDを返す
        } catch (SQLException e) {
            System.out.println("登録に失敗しました。別のユーザーIDを試してください。");
            return loginOrRegister(); // 再度ログインまたは登録を実行
        }
    }

    /**
     * 既存のユーザーをログインさせるメソッド
     * 
     * @return ログイン成功したユーザーのID
     */
    public static String loginUser() {
        int attempts = 0; // ログイン試行回数のカウント

        while (attempts < MAX_LOGIN_ATTEMPTS) {
            System.out.print("ユーザーIDを入力してください:");
            String userId = scanner.nextLine(); // ユーザーIDの入力

            // 入力チェック(半角英数字と記号のみ、1-8文字)
            if (!isValidInput(userId) || userId.length() < 1 || userId.length() > 8) {
                System.out.println("エラー:ユーザーIDは半角英数字と記号のみ、1-8文字にしてください。");
            }

            System.out.print("パスワードを入力してください:");
            String password = scanner.nextLine(); // パスワードの入力

            // 入力チェック（半角英数字と記号のみ、1〜8文字）
            if (!isValidInput(password) || password.length() < 1 || password.length() > 8) {
                System.out.println("エラー: パスワードは半角英数字と記号のみ、1〜8文字にしてください。");
            }

            String hashedPassword = hashPassword(password); // 入力されたパスワードをハッシュ化

            // データベースでユーザー情報を照合
            String sql = "SELECT * FROM users WHERE user_id = ? AND password = ?";
            try (Connection conn = DatabaseManager.connect();
                    PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, userId);
                pstmt.setString(2, hashedPassword);
                ResultSet rs = pstmt.executeQuery();// クエリを実行

                if (rs.next()) { // 一致するユーザーが存在する場合
                    System.out.println("ログイン成功！");
                    return userId; // ログイン成功したユーザーIDを返す
                } else {
                    attempts++;
                    System.out.println("ログインに失敗しました。もう一度試してください。残り:" + (MAX_LOGIN_ATTEMPTS - attempts));
                }
            } catch (SQLException e) {
                e.printStackTrace(); // デバッグ用のエラーログ
                return null; // エラー発生時はnullを返す
            }

        }

        System.out.println("ログイン試行回数が上限に達しました。しばらくしてから再試行してください。");
        System.exit(0); // プログラムを終了させる
        return null; // 文法上必要のため
    }

    /**
     * ユーザーIDが既に存在するかをチェックするメソッド
     * 
     * @param userId チェックするユーザーID
     * @return 存在する場合はtrue、存在しない場合はfalse
     */
    private static boolean isUserExists(String userId) {
        String sql = "SELECT user_id FROM users WHERE user_id = ?";
        try (Connection conn = DatabaseManager.connect();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, userId);
            ResultSet rs = pstmt.executeQuery();
            return rs.next(); // ユーザーが存在する場合trueを返す

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /*
     * パスワードをSHA-256でハッシュ化するメソッド
     * 
     * @param password ハッシュ化するパスワード
     * 
     * @return ハッシュ化されたパスワード
     */
    private static String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = md.digest(password.getBytes()); // バイト配列に変換してハッシュ化
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                hexString.append(String.format("%02x", b)); // 16進数文字列に変換
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("パスワードのハッシュ化に失敗しました。", e);
        }
    }

    /**
     * 入力が半角英数字および記号のみで構成されているかをチェックするメソッド
     * 
     * @param input ユーザーが入力した文字列(IDまたはパスワード)
     * @return 有効な場合はtrue、無効な場合はfalse
     */
    private static boolean isValidInput(String input) {
        return input.matches(VALID_PATTERN);
    }

}