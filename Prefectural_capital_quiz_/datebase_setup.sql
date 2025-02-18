--データベースを作成（もし存在しない場合）
PRAGMA foreign_keys = ON;

--1. ユーザーテーブル
CREATE TABLE IF NOT EXISTS users(
    user_id TEXT PRIMARY KEY, --ユーザーID(半角英数字&記号8文字以下)
    password TEXT NOT NULL    --パスワード(半角英数字&記号8文字)
);

--2. 問題テーブル
CREATE TABLE IF NOT EXISTS questions(
    question_id INTEGER PRIMARY KEY AUTOINCREMENT, --問題ID
    prefecture TEXT NOT NULL,                      --県名(問題として表示)
    correct_answer TEXT NOT NULL                   --正解(県庁所在地)
);

--3. 解答履歴テーブル
CREATE TABLE IF NOT EXISTS user_answers(
    user_id TEXT,                                  --解答したユーザー
    question_id INTEGER,                           --問題ID
    selected_answer TEXT NOT NULL,                 --ユーザーが選んだ解答
    is_correct INTEGER NOT NULL CHECK(is_correct IN (0,1)), --0:不正解/1:正解
    FOREIGN KEY (user_id) REFERENCES users(user_id),
    FOREIGN KEY (question_id) REFERENCES questions(question_id)
);

--4. 初期データ: 問題テーブルのデータ(例)
INSERT INTO questions (prefecture, correct_answer) VALUES
('北海道', '札幌'),
('青森県', '青森'),
('岩手県', '盛岡'),
('宮城県', '仙台'),
('秋田県', '秋田'),
('山形県', '山形'),
('福島県', '福島'),
('茨城県', '水戸'),
('栃木県', '宇都宮'),
('群馬県', '前橋'),
('埼玉県', 'さいたま'),
('千葉県', '千葉'),
('東京都', '東京'),
('神奈川県', '横浜'),
('新潟県', '新潟'),
('富山県', '富山'),
('石川県', '金沢'),
('福井県', '福井'),
('山梨県', '甲府'),
('長野県', '長野'),
('岐阜県', '岐阜'),
('静岡県', '静岡'),
('愛知県', '名古屋'),
('三重県', '津'),
('滋賀県', '大津'),
('京都府', '京都'),
('大阪府', '大阪'),
('兵庫県', '神戸'),
('奈良県', '奈良'),
('和歌山県', '和歌山'),
('鳥取県', '鳥取'),
('島根県', '松江'),
('岡山県', '岡山'),
('広島県', '広島'),
('山口県', '山口'),
('徳島県', '徳島'),
('香川県', '高松'),
('愛媛県', '松山'),
('高知県', '高知'),
('福岡県', '福岡'),
('佐賀県', '佐賀'),
('長崎県', '長崎'),
('熊本県', '熊本'),
('大分県', '大分'),
('宮崎県', '宮崎'),
('鹿児島県', '鹿児島'),
('沖縄県', '那覇');

--5. 正常にデータが入っているか確認
SELECT * FROM users;
SELECT * FROM questions;
SELECT * FROM user_answers;