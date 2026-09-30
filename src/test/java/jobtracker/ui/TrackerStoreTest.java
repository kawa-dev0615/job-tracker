package jobtracker.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import jobtracker.model.JobApplication;
import jobtracker.model.Status;
import jobtracker.model.WatchItem;
import jobtracker.model.WatchStatus;
import jobtracker.repository.JobApplicationRepository;
import jobtracker.repository.RepositoryException;
import jobtracker.repository.TrackerData;

class TrackerStoreTest {

    /** 保存内容を記録するだけのリポジトリ */
    private static class RecordingRepository implements JobApplicationRepository {
        private final TrackerData initial;
        private final boolean failOnLoad;
        final List<TrackerData> saved = new ArrayList<>();

        RecordingRepository(TrackerData initial, boolean failOnLoad) {
            this.initial = initial;
            this.failOnLoad = failOnLoad;
        }

        @Override
        public TrackerData load() {
            if (failOnLoad) {
                throw new RepositoryException("壊れたファイル", null);
            }
            return initial;
        }

        @Override
        public void save(TrackerData data) {
            saved.add(data);
        }
    }

    private static TrackerData dataWith(WatchItem item) {
        List<WatchItem> watchItems = new ArrayList<>();
        watchItems.add(item);
        return new TrackerData(new ArrayList<>(), watchItems);
    }

    @Test
    void 転記すると応募管理に追加され元の項目は応募済みになる() {
        WatchItem item = WatchItem.create("A社", "SRE", "年収 700万円〜", "https://example.com/1");
        RecordingRepository repository = new RecordingRepository(dataWith(item), false);
        TrackerStore store = new TrackerStore(repository);
        store.load();

        JobApplication app = store.transferToApplications(item, LocalDate.of(2026, 9, 30));

        assertNotNull(app);
        assertEquals(1, store.applications().size());
        assertSame(app, store.applications().get(0));
        assertEquals(Status.APPLIED, app.getStatus());
        assertEquals(LocalDate.of(2026, 9, 30), app.getAppliedDate());
        assertEquals(WatchStatus.APPLIED, item.getStatus());
    }

    @Test
    void 転記は1回の保存にまとめる() {
        WatchItem item = WatchItem.create("A社", "", "", "");
        RecordingRepository repository = new RecordingRepository(dataWith(item), false);
        TrackerStore store = new TrackerStore(repository);
        store.load();

        store.transferToApplications(item, LocalDate.of(2026, 9, 30));

        assertEquals(1, repository.saved.size());
        TrackerData saved = repository.saved.get(0);
        assertEquals(1, saved.applications().size());
        assertEquals(WatchStatus.APPLIED, saved.watchItems().get(0).getStatus());
    }

    @Test
    void 転記済みの項目は二重に転記しない() {
        WatchItem item = WatchItem.create("A社", "", "", "");
        RecordingRepository repository = new RecordingRepository(dataWith(item), false);
        TrackerStore store = new TrackerStore(repository);
        store.load();
        store.transferToApplications(item, LocalDate.of(2026, 9, 30));

        JobApplication second = store.transferToApplications(item, LocalDate.of(2026, 10, 1));

        assertNull(second);
        assertEquals(1, store.applications().size());
    }

    @Test
    void 未応募の件数を数える() {
        WatchItem notApplied = WatchItem.create("A社", "", "", "");
        WatchItem applied = WatchItem.create("B社", "", "", "");
        applied.setStatus(WatchStatus.APPLIED);
        List<WatchItem> watchItems = new ArrayList<>();
        watchItems.add(notApplied);
        watchItems.add(applied);
        TrackerStore store = new TrackerStore(
                new RecordingRepository(new TrackerData(new ArrayList<>(), watchItems), false));
        store.load();

        assertEquals(1, store.countNotApplied());
    }

    @Test
    void 読み込みに失敗したら変更しても保存しない() {
        RecordingRepository repository = new RecordingRepository(TrackerData.empty(), true);
        TrackerStore store = new TrackerStore(repository);
        store.load();

        store.watchItems().add(WatchItem.create("A社", "", "", ""));

        assertNotNull(store.getLoadError());
        assertEquals(0, repository.saved.size());
    }

    @Test
    void 項目を編集すると保存される() {
        WatchItem item = WatchItem.create("A社", "", "", "");
        RecordingRepository repository = new RecordingRepository(dataWith(item), false);
        TrackerStore store = new TrackerStore(repository);
        store.load();

        item.setNote("備考だけ変更");

        assertEquals(1, repository.saved.size());
    }
}
