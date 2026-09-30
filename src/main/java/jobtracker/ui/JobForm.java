package jobtracker.ui;

import java.time.LocalDate;

import jobtracker.model.JobApplication;
import jobtracker.model.WatchItem;

/**
 * 追加・編集ダイアログの入力内容。
 * 応募管理と「気になる」で同じダイアログを使うため、両方の項目をまとめて持つ。
 *
 * @param appliedDate 応募日。null ならダイアログに応募日欄を出さない (「気になる」用)
 */
public record JobForm(String companyName, String position, String salary, String url, String note,
                      LocalDate appliedDate) {

    public static JobForm of(JobApplication app) {
        return new JobForm(app.getCompanyName(), app.getPosition(), app.getSalary(), app.getUrl(),
                app.getNote(), app.getAppliedDate());
    }

    public static JobForm of(WatchItem item) {
        return new JobForm(item.getCompanyName(), item.getPosition(), item.getSalary(), item.getUrl(),
                item.getNote(), null);
    }

    public boolean hasAppliedDate() {
        return appliedDate != null;
    }

    public void applyTo(JobApplication app) {
        app.setCompanyName(companyName);
        app.setPosition(position);
        app.setSalary(salary);
        app.setUrl(url);
        app.setNote(note);
        if (appliedDate != null) {
            app.setAppliedDate(appliedDate);
        }
    }

    public void applyTo(WatchItem item) {
        item.setCompanyName(companyName);
        item.setPosition(position);
        item.setSalary(salary);
        item.setUrl(url);
        item.setNote(note);
    }
}
