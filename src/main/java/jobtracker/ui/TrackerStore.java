package jobtracker.ui;

import java.time.LocalDate;

import javafx.beans.Observable;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;

import jobtracker.model.JobApplication;
import jobtracker.model.WatchItem;
import jobtracker.model.WatchStatus;
import jobtracker.repository.JobApplicationRepository;
import jobtracker.repository.JsonJobApplicationRepository;
import jobtracker.repository.RepositoryException;
import jobtracker.repository.TrackerData;

/**
 * 画面全体で共有するデータ (応募管理・「気になる」リスト) と、その保存を受け持つ。
 *
 * 各リストは extractor 付きなので、要素のプロパティ変更も変更通知になる。
 * 変更があるたびに両方のリストをまとめて保存する。
 */
public class TrackerStore {

    private final JobApplicationRepository repository;

    private final ObservableList<JobApplication> applications = FXCollections.observableArrayList(
            app -> new Observable[] {
                app.companyNameProperty(),
                app.positionProperty(),
                app.salaryProperty(),
                app.urlProperty(),
                app.statusProperty(),
                app.interviewAtProperty(),
                app.appliedDateProperty(),
                app.noteProperty()
            });

    private final ObservableList<WatchItem> watchItems = FXCollections.observableArrayList(
            item -> new Observable[] {
                item.companyNameProperty(),
                item.positionProperty(),
                item.salaryProperty(),
                item.urlProperty(),
                item.noteProperty(),
                item.statusProperty()
            });

    private final ReadOnlyStringWrapper message = new ReadOnlyStringWrapper("");

    /** 読み込みに失敗したときのエラー内容。null なら成功 */
    private String loadError;

    /** 転記中など、複数の変更をまとめて 1 回で保存したいときに true にする */
    private boolean saveSuspended = false;

    public TrackerStore(JobApplicationRepository repository) {
        this.repository = repository;
    }

    /** データを読み込み、以降の変更を自動保存するようにする */
    public void load() {
        try {
            TrackerData data = repository.load();
            applications.setAll(data.applications());
            watchItems.setAll(data.watchItems());
            message.set("データ: " + describeRepository());
        } catch (RepositoryException e) {
            // 読み込めないまま保存すると、空のデータでファイルを上書きしてしまうので保存を止める
            loadError = e.getMessage();
            message.set("読み込みに失敗したため、変更は保存されません: " + describeRepository());
        }

        // 読み込み後に変更監視を始める (読み込み時に保存が走らないように)
        applications.addListener((ListChangeListener<JobApplication>) change -> save());
        watchItems.addListener((ListChangeListener<WatchItem>) change -> save());
    }

    /**
     * 「気になる」の項目を応募管理に転記する。
     * 転記後、元の項目はステータスを「応募済み」にする。2 つの変更は 1 回の保存にまとめる。
     *
     * @return 応募管理に追加した応募。転記済みの項目なら null
     */
    public JobApplication transferToApplications(WatchItem item, LocalDate appliedDate) {
        if (item.getStatus() == WatchStatus.APPLIED) {
            return null;
        }
        JobApplication app = item.toApplication(appliedDate);
        saveSuspended = true;
        try {
            item.setStatus(WatchStatus.APPLIED);
            applications.add(app);
        } finally {
            saveSuspended = false;
        }
        save();
        return app;
    }

    public ObservableList<JobApplication> applications() {
        return applications;
    }

    public ObservableList<WatchItem> watchItems() {
        return watchItems;
    }

    /** 「気になる」のうち、まだ応募管理に転記していない件数 */
    public int countNotApplied() {
        int count = 0;
        for (WatchItem item : watchItems) {
            if (item.getStatus() == WatchStatus.NOT_APPLIED) {
                count++;
            }
        }
        return count;
    }

    /** フッターに出す保存状態のメッセージ */
    public ReadOnlyStringProperty messageProperty() {
        return message.getReadOnlyProperty();
    }

    public String getLoadError() {
        return loadError;
    }

    private void save() {
        if (saveSuspended || loadError != null) {
            return;
        }
        try {
            repository.save(new TrackerData(applications, watchItems));
            message.set("保存しました  |  データ: " + describeRepository());
        } catch (RepositoryException e) {
            message.set("保存に失敗しました: " + e.getMessage());
        }
    }

    private String describeRepository() {
        if (repository instanceof JsonJobApplicationRepository jsonRepository) {
            return jsonRepository.getFilePath().toString();
        }
        return repository.getClass().getSimpleName();
    }
}
