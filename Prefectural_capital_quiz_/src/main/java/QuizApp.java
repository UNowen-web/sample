import java.util.Scanner;

/**
 * クイズアプリのメインクラス。
 * 
 * - ユーザーのログインまたは新規登録を管理。
 * - クイズの出題メニューを表示し、ユーザーの入力を受け付ける。
 * - ユーザー入力時のバリデーションを強化し、エラー処理を適切に行う。
 * - `Scanner` のリソース管理を適切に行い、リソースリークを防ぐ。
 */
public class QuizApp {
    private static String userId; // 現在ログインしているユーザーのIDを保存する変数

    public static void main(String[] args) {
        System.out.println("クイズアプリへようこそ！");

        // `try-with-resources` を使用し、Scannerのリソースリークを防ぐ
        try (Scanner scanner = new Scanner(System.in)) {
            userId = UserManager.loginOrRegister(scanner); // ユーザーのログインまたは新規登録処理

            while (true) {
                // メニュー表示
                System.out.println("\nメニュー:");
                System.out.println("1. 全問解答");
                System.out.println("2. 間違えた問題のみ解答");
                System.out.println("3. 終了");
                System.out.print("選択肢を入力してください: ");

                int choice = getValidMenuChoice(scanner); // 入力チェックを行い、選択肢を取得

                switch (choice) {
                    case 1:
                        QuestionManager.takeQuiz(userId, true, scanner);
                        break;
                    case 2:
                        QuestionManager.takeQuiz(userId, false, scanner);
                        break;
                    case 3:
                        System.out.println("アプリを終了します。");
                        return; // ループを抜け、`try-with-resources` により `scanner` が自動でクローズされる
                    default:
                        System.out.println("無効な選択です。再度入力してください。");
                }
            }
        } finally {
            DatabaseManager.closeConnection(); // プログラム終了時にデータベースを閉じる
        }
    }

    /**
     * メニューの選択肢を安全に取得するメソッド。
     * 
     * - 数値以外の入力がされた場合、再入力を求める。
     * - `nextInt()` の後に `nextLine()` を呼び、バッファをクリアすることで予期しない入力バグを防ぐ。
     * - 1〜3 以外の数値が入力された場合も再入力を促す。
     *
     * @param scanner ユーザー入力を受け付ける `Scanner` オブジェクト
     * @return ユーザーが選択したメニュー番号（1〜3）
     */
    private static int getValidMenuChoice(Scanner scanner) {
        int choice;
        while (true) {
            if (scanner.hasNextInt()) { // 数値が入力されているかチェック
                choice = scanner.nextInt();
                scanner.nextLine(); // バッファをクリア（nextInt() だけだと改行が残るため）

                // 有効な選択肢（1〜3）の場合のみループを抜ける
                if (choice >= 1 && choice <= 3) {
                    break;
                }
            } else {
                scanner.nextLine(); // 無効な入力をクリア
            }
            System.out.print("無効な入力です。1、2、3の数字を入力してください: ");
        }
        return choice;
    }
}
