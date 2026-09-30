package jobtracker.model;

/** 「気になる」リストのステータス */
public enum WatchStatus {
    NOT_APPLIED("未応募"),
    APPLIED("応募済み");

    private final String label;

    WatchStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /** ComboBox などでの表示用 */
    @Override
    public String toString() {
        return label;
    }
}
