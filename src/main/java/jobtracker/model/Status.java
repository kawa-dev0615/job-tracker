package jobtracker.model;

/**
 * 応募ステータス。
 * 画面表示用のラベルと、面接日時の入力が必要かどうかを持つ。
 */
public enum Status {
    APPLIED("応募済み", false),
    CASUAL_INTERVIEW("カジュアル面談", true),
    IN_SELECTION("選考中", true),
    OFFERED("内定", false),
    OFFER_DECLINED("内定辞退", false),
    REJECTED("落選", false);

    private final String label;
    private final boolean interviewDateRequired;

    Status(String label, boolean interviewDateRequired) {
        this.label = label;
        this.interviewDateRequired = interviewDateRequired;
    }

    public String getLabel() {
        return label;
    }

    /** カジュアル面談・選考中のときだけ面接日時を入力する */
    public boolean isInterviewDateRequired() {
        return interviewDateRequired;
    }

    @Override
    public String toString() {
        return label;
    }
}
