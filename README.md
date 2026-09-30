# Job Tracker — 転職活動ダッシュボード

[![CI](https://github.com/kawa-dev0615/job-tracker/actions/workflows/ci.yml/badge.svg)](https://github.com/kawa-dev0615/job-tracker/actions/workflows/ci.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

転職活動の応募状況を管理する JavaFX 製のデスクトップアプリです。
求人ページの URL を貼るだけで会社名・ポジション・想定年収を読み取り、応募一覧として管理できます。
データはすべて手元の PC 上 (JSON ファイル) に保存されます。

## 主な機能

- **求人ページの解析**: URL を入力して「解析して追加」を押すと、ページから会社名・ポジション・想定年収を抽出
  - 読み取った内容は確認ダイアログで修正してから保存できる (取れなかった項目は手入力)
- **応募一覧**: 応募日・会社名・ポジション・想定年収・ステータス・面接日時・備考を表で表示。列ヘッダでソート可能
- **ステータス管理**: 各行のプルダウンで変更 (新規登録時は「応募済み」)
  - 応募済み / カジュアル面談 / 選考中 / 内定 / 内定辞退 / 落選
- **ステータス別タブ**: 画面上部のタブで絞り込み。タブには件数を表示
- **面接日時**: ステータスが「カジュアル面談」「選考中」のときだけ表示。未設定時は `---`、クリックで日時を入力
- **備考**: 追加・編集ダイアログで 100 文字まで入力可能。一覧では 1 行で表示し、マウスを乗せると全文を表示
- **今後の面接予定**: 右側パネルに直近の面接を日時順で表示
- 行のダブルクリックで編集、右クリックで「求人ページを開く」「削除」
- **気になるリスト**: 「気になる」タブで、まだ応募していない求人を控えておける
  - 項目は会社名・ポジション・想定年収・ステータス (未応募 / 応募済み)・備考・求人。登録方法は応募一覧と同じ
  - ステータスを「応募済み」にすると、確認のうえ応募一覧へ転記 (応募日は当日、ステータスは「応募済み」)
  - 転記済みの項目はグレーアウトし、編集できなくなる (求人リンクと削除のみ可)

## ダウンロードして使う (Windows)

[Releases](https://github.com/kawa-dev0615/job-tracker/releases) から `JobTracker-<バージョン>-windows.zip` をダウンロードし、
展開したフォルダの `JobTracker.exe` を起動します。Java 実行環境を同梱しているので、Java のインストールは不要です。

- `JobTracker.exe` だけを取り出すと起動できません (同じフォルダの `app/` と `runtime/` を使うため)
- インストーラー形式ではないので、アンインストールはフォルダを削除するだけです (データは下記の場所に残ります)
- 署名していない exe のため、初回起動時に Windows SmartScreen の警告が出ることがあります

## ソースからビルドする

必要なのは JDK 21 以上だけです。Maven は同梱の Maven Wrapper (`mvnw`) が自動で取得します。

```bash
# 開発時の起動 (Windows では ./mvnw の代わりに mvnw.cmd)
./mvnw javafx:run

# テスト
./mvnw test

# 実行可能 jar を作成して起動 (jar はビルドした OS 用になる)
./mvnw package
java -jar target/job-tracker.jar

# Java 実行環境を同梱した exe を作成 (Windows。JDK 付属の jpackage を使用)
mvnw.cmd clean package -Pexe
# → target/dist/JobTracker/JobTracker.exe
```

データは `ユーザーのホーム/.job-tracker/applications.json` に保存されます。

## 利用上の注意

- **個人利用を前提としたツールです。** 解析対象の求人サイトの利用規約・robots.txt に従って使ってください
- 求人ページへのアクセスは「解析して追加」を押したときの 1 回だけです。巡回や定期取得は行いません
  - 大量の URL を連続で解析するなど、サイトに負荷をかける使い方はしないでください
- アクセス時の User-Agent はブラウザを装わず、`JobTracker/<バージョン>` と名乗ります。
  そのため、ブラウザ以外のアクセスを拒否するサイトでは解析に失敗することがあります (その場合は手入力で登録できます)
- 抽出するのは会社名・ポジション・想定年収のみで、求人本文は保存しません
- 保存データ (`applications.json`) には応募先や選考状況が含まれます。
  リポジトリへのコミットや、実データが写ったスクリーンショットの公開は避けてください
- Windows では OS の証明書ストアを使って HTTPS 接続します。
  ウイルス対策ソフトの HTTPS スキャンや社内プロキシがある環境でも、ブラウザと同じ条件で接続できます

## 技術スタック

| 用途 | ライブラリ |
|---|---|
| GUI | JavaFX 21 (FXML + CSS) |
| HTML 取得・解析 | jsoup |
| JSON 保存 / JSON-LD 解析 | Jackson |
| テスト | JUnit 5 |
| ビルド | Maven |

## 設計

```
jobtracker
├── Launcher / JobTrackerApp   … 起動・部品の組み立て
├── model                      … JobApplication (応募1件), Status (ステータス), WatchItem / WatchStatus (気になるリスト)
├── repository                 … 保存先の抽象化 (JobApplicationRepository) と JSON 実装
├── extractor                  … 求人ページ解析
│   ├── JobPageAnalyzer            URL → HTML 取得 → 抽出
│   ├── JobInfoExtractor           抽出ロジックのインターフェース
│   └── RuleBasedJobInfoExtractor  ルールベース実装
└── ui                         … 画面 (FXML コントローラ, ダイアログ, セル)
    └── TrackerStore               応募一覧と気になるリストを保持し、変更のたびに保存する
```

### 求人ページの解析方法

`RuleBasedJobInfoExtractor` は次の順に抽出を試みます。

1. **構造化データ (JSON-LD)**: Google しごと検索に対応した求人ページには schema.org の `JobPosting` が埋め込まれていることが多く、
   `hiringOrganization.name` / `title` / `baseSalary` から正確に取得できる
2. **正規表現によるフォールバック**:
   - 会社名: タイトル・見出し・meta から「株式会社○○」「○○株式会社」などを検索
   - ポジション: `h1` → `og:title` → `<title>` から会社名やサイト名を除いた部分
   - 想定年収: 本文から「想定年収」「年収」「月給」「給与」の表記を検索 (全角数字・円表記・範囲表記に対応)。
     本文で見つからない場合は JSON-LD の `description` からも検索する (本文を JavaScript で描画するサイト向け)

抽出ロジックは `JobInfoExtractor` インターフェースで差し替え可能にしているので、
将来 LLM を使った抽出などを追加する場合も画面側を変更せずに済みます。

### データ保存

想定件数が 100 件程度のため、DB ではなく JSON ファイル 1 つに全件 (応募一覧と気になるリスト) を保存しています。

- 変更のたびに全件を書き込む (件数が少ないので十分高速)
- 一時ファイルに書いてから置き換えることで、書き込み中にアプリが落ちてもファイルが壊れない
- ファイルに `version` を持たせ、将来の項目追加時に移行できるようにしている (現在は 2。1 のファイルもそのまま読める)
- 気になるリストから応募一覧への転記は、2 つのリストの変更を 1 回の書き込みにまとめる (片方だけ保存された状態にならない)
- 保存処理は `JobApplicationRepository` インターフェースで抽象化しており、SQLite などへの差し替えも可能

### 画面の更新

`ObservableList` に extractor を設定し、各応募のプロパティ変更も変更通知として扱っています。
これにより、ステータスを変更するだけで

- JSON ファイルへの自動保存
- タブの件数更新
- タブによる絞り込みの再評価 (`FilteredList`)
- 面接日時列の表示切り替え

がまとめて行われます。

## 今後の予定

- LLM を使った求人ページ解析 (精度向上)
- 選考ステップの履歴管理

## ライセンス

[MIT License](LICENSE)
