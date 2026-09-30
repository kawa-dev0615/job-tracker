package jobtracker.repository;

/**
 * 応募データの保存先を抽象化したインターフェース。
 * 現在は JSON ファイル実装のみだが、SQLite 等に差し替えられるようにしている。
 */
public interface JobApplicationRepository {

    /** 保存されている全データを読み込む。データがなければ空のデータを返す。 */
    TrackerData load();

    /** 全データをまとめて保存する (件数が少ない前提なので全件上書き方式) 。 */
    void save(TrackerData data);
}
