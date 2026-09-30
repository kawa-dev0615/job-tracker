package jobtracker.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * 1件の応募情報。
 *
 * JavaFX のプロパティで値を持つので、値を変更すると画面(TableView)に自動で反映される。
 * JSON保存は通常の getter / setter 経由で行う (xxxProperty() は Jackson の対象外)。
 */
public class JobApplication {

    /** 備考の最大文字数 */
    public static final int NOTE_MAX_LENGTH = 100;

    private String id;
    private final StringProperty companyName = new SimpleStringProperty("");
    private final StringProperty position = new SimpleStringProperty("");
    private final StringProperty salary = new SimpleStringProperty("");
    private final StringProperty url = new SimpleStringProperty("");
    private final ObjectProperty<Status> status = new SimpleObjectProperty<>(Status.APPLIED);
    private final ObjectProperty<LocalDateTime> interviewAt = new SimpleObjectProperty<>(null);
    private final ObjectProperty<LocalDate> appliedDate = new SimpleObjectProperty<>(LocalDate.now());
    private final StringProperty note = new SimpleStringProperty("");

    /** JSON読み込み用 */
    public JobApplication() {
        this.id = UUID.randomUUID().toString();
    }

    /** 新規登録用。ステータスは「応募済み」、応募日は今日になる。 */
    public static JobApplication create(String companyName, String position, String salary, String url) {
        JobApplication app = new JobApplication();
        app.setCompanyName(companyName);
        app.setPosition(position);
        app.setSalary(salary);
        app.setUrl(url);
        return app;
    }

    // ---- id ----
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    // ---- 会社名 ----
    public String getCompanyName() {
        return companyName.get();
    }

    public void setCompanyName(String value) {
        companyName.set(nullToEmpty(value));
    }

    public StringProperty companyNameProperty() {
        return companyName;
    }

    // ---- ポジション ----
    public String getPosition() {
        return position.get();
    }

    public void setPosition(String value) {
        position.set(nullToEmpty(value));
    }

    public StringProperty positionProperty() {
        return position;
    }

    // ---- 想定年収 ----
    public String getSalary() {
        return salary.get();
    }

    public void setSalary(String value) {
        salary.set(nullToEmpty(value));
    }

    public StringProperty salaryProperty() {
        return salary;
    }

    // ---- 求人URL ----
    public String getUrl() {
        return url.get();
    }

    public void setUrl(String value) {
        url.set(nullToEmpty(value));
    }

    public StringProperty urlProperty() {
        return url;
    }

    // ---- ステータス ----
    public Status getStatus() {
        return status.get();
    }

    public void setStatus(Status value) {
        if (value == null) {
            status.set(Status.APPLIED);
        } else {
            status.set(value);
        }
    }

    public ObjectProperty<Status> statusProperty() {
        return status;
    }

    // ---- 面接日時 (未設定なら null。画面では "---" と表示) ----
    public LocalDateTime getInterviewAt() {
        return interviewAt.get();
    }

    public void setInterviewAt(LocalDateTime value) {
        interviewAt.set(value);
    }

    public ObjectProperty<LocalDateTime> interviewAtProperty() {
        return interviewAt;
    }

    // ---- 応募日 ----
    public LocalDate getAppliedDate() {
        return appliedDate.get();
    }

    public void setAppliedDate(LocalDate value) {
        appliedDate.set(value);
    }

    public ObjectProperty<LocalDate> appliedDateProperty() {
        return appliedDate;
    }

    // ---- 備考 ----
    public String getNote() {
        return note.get();
    }

    public void setNote(String value) {
        note.set(nullToEmpty(value));
    }

    public StringProperty noteProperty() {
        return note;
    }

    /**
     * 備考の文字数。絵文字などのサロゲートペアも 1 文字として数える。
     * 入力値の前後の空白は保存時に除かれるため、数えない。
     */
    public static int countNoteLength(String value) {
        if (value == null) {
            return 0;
        }
        String trimmed = value.trim();
        return trimmed.codePointCount(0, trimmed.length());
    }

    private static String nullToEmpty(String value) {
        if (value == null) {
            return "";
        }
        return value.trim();
    }
}
