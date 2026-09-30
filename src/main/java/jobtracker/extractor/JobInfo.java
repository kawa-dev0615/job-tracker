package jobtracker.extractor;

/**
 * 求人ページから抽出した情報。抽出できなかった項目は空文字。
 *
 * @param companyName 会社名
 * @param position    ポジション(職種名)
 * @param salary      想定年収 (例: "年収 500万円〜800万円")
 */
public record JobInfo(String companyName, String position, String salary) {

    public JobInfo {
        if (companyName == null) {
            companyName = "";
        }
        if (position == null) {
            position = "";
        }
        if (salary == null) {
            salary = "";
        }
    }

    public static JobInfo empty() {
        return new JobInfo("", "", "");
    }

    /** 3項目すべて取れていれば true */
    public boolean isComplete() {
        return !companyName.isEmpty() && !position.isEmpty() && !salary.isEmpty();
    }
}
