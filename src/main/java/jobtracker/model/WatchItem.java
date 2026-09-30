package jobtracker.model;

import java.time.LocalDate;
import java.util.UUID;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * 「気になる」リストの 1 件。まだ応募していない求人を控えておくためのもの。
 *
 * ステータスを「応募済み」にすると応募管理へ転記し、こちらはグレーアウトして閲覧のみになる。
 * JSON 保存は JobApplication と同じく通常の getter / setter 経由で行う。
 */
public class WatchItem {

    private String id;
    private final StringProperty companyName = new SimpleStringProperty("");
    private final StringProperty position = new SimpleStringProperty("");
    private final StringProperty salary = new SimpleStringProperty("");
    private final StringProperty url = new SimpleStringProperty("");
    private final StringProperty note = new SimpleStringProperty("");
    private final ObjectProperty<WatchStatus> status = new SimpleObjectProperty<>(WatchStatus.NOT_APPLIED);

    /** JSON読み込み用 */
    public WatchItem() {
        this.id = UUID.randomUUID().toString();
    }

    /** 新規登録用。ステータスは「未応募」になる。 */
    public static WatchItem create(String companyName, String position, String salary, String url) {
        WatchItem item = new WatchItem();
        item.setCompanyName(companyName);
        item.setPosition(position);
        item.setSalary(salary);
        item.setUrl(url);
        return item;
    }

    /**
     * 応募管理に転記するための応募情報を作る。
     * 自分自身は変更しない (ステータスの更新は呼び出し側で行う) 。
     *
     * @param appliedDate 応募日
     */
    public JobApplication toApplication(LocalDate appliedDate) {
        JobApplication app = JobApplication.create(getCompanyName(), getPosition(), getSalary(), getUrl());
        app.setNote(getNote());
        app.setStatus(Status.APPLIED);
        app.setAppliedDate(appliedDate);
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

    // ---- 備考 (文字数の上限は JobApplication.NOTE_MAX_LENGTH と同じ) ----
    public String getNote() {
        return note.get();
    }

    public void setNote(String value) {
        note.set(nullToEmpty(value));
    }

    public StringProperty noteProperty() {
        return note;
    }

    // ---- ステータス ----
    public WatchStatus getStatus() {
        return status.get();
    }

    public void setStatus(WatchStatus value) {
        if (value == null) {
            status.set(WatchStatus.NOT_APPLIED);
        } else {
            status.set(value);
        }
    }

    public ObjectProperty<WatchStatus> statusProperty() {
        return status;
    }

    private static String nullToEmpty(String value) {
        if (value == null) {
            return "";
        }
        return value.trim();
    }
}
