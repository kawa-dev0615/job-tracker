package jobtracker.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import jobtracker.model.JobApplication;
import jobtracker.model.Status;
import jobtracker.model.WatchItem;
import jobtracker.model.WatchStatus;

class JsonJobApplicationRepositoryTest {

    @TempDir
    Path tempDir;

    @Test
    void ファイルがなければ空リスト() {
        JsonJobApplicationRepository repository =
                new JsonJobApplicationRepository(tempDir.resolve("none.json"));

        TrackerData loaded = repository.load();

        assertTrue(loaded.applications().isEmpty());
        assertTrue(loaded.watchItems().isEmpty());
    }

    @Test
    void 保存して読み込むと同じ内容になる() {
        Path file = tempDir.resolve("sub").resolve("applications.json");
        JsonJobApplicationRepository repository = new JsonJobApplicationRepository(file);

        JobApplication first = JobApplication.create("株式会社A", "Javaエンジニア", "年収 600万円〜", "https://example.com/1");
        first.setAppliedDate(LocalDate.of(2026, 9, 1));
        first.setNote("一次面接はオンライン\nリクルーター経由");

        JobApplication second = JobApplication.create("B株式会社", "SRE", "", "https://example.com/2");
        second.setStatus(Status.IN_SELECTION);
        second.setInterviewAt(LocalDateTime.of(2026, 10, 3, 14, 0));

        List<JobApplication> list = new ArrayList<>();
        list.add(first);
        list.add(second);
        repository.save(new TrackerData(list, new ArrayList<>()));

        List<JobApplication> loaded = repository.load().applications();

        assertEquals(2, loaded.size());
        JobApplication a = loaded.get(0);
        assertEquals(first.getId(), a.getId());
        assertEquals("株式会社A", a.getCompanyName());
        assertEquals(Status.APPLIED, a.getStatus());
        assertNull(a.getInterviewAt());
        assertEquals(LocalDate.of(2026, 9, 1), a.getAppliedDate());
        assertEquals("一次面接はオンライン\nリクルーター経由", a.getNote());

        JobApplication b = loaded.get(1);
        assertEquals(Status.IN_SELECTION, b.getStatus());
        assertEquals(LocalDateTime.of(2026, 10, 3, 14, 0), b.getInterviewAt());
        assertEquals("", b.getNote());
    }

    @Test
    void 備考のない古いJSONも読み込める() throws IOException {
        Path file = tempDir.resolve("old.json");
        String json = "{\"version\":1,\"applications\":[{\"id\":\"a1\",\"companyName\":\"株式会社A\",\"status\":\"APPLIED\"}]}";
        Files.writeString(file, json, StandardCharsets.UTF_8);
        JsonJobApplicationRepository repository = new JsonJobApplicationRepository(file);

        TrackerData loaded = repository.load();

        assertEquals(1, loaded.applications().size());
        assertEquals("株式会社A", loaded.applications().get(0).getCompanyName());
        assertEquals("", loaded.applications().get(0).getNote());
        assertTrue(loaded.watchItems().isEmpty());
    }

    @Test
    void 気になるリストも保存して読み込める() {
        Path file = tempDir.resolve("applications.json");
        JsonJobApplicationRepository repository = new JsonJobApplicationRepository(file);

        WatchItem notApplied = WatchItem.create("C社", "バックエンド", "年収 700万円〜", "https://example.com/3");
        notApplied.setNote("カジュアル面談から入れるか確認");
        WatchItem applied = WatchItem.create("D社", "SRE", "", "https://example.com/4");
        applied.setStatus(WatchStatus.APPLIED);

        List<WatchItem> watchItems = new ArrayList<>();
        watchItems.add(notApplied);
        watchItems.add(applied);
        repository.save(new TrackerData(new ArrayList<>(), watchItems));

        List<WatchItem> loaded = repository.load().watchItems();

        assertEquals(2, loaded.size());
        WatchItem c = loaded.get(0);
        assertEquals(notApplied.getId(), c.getId());
        assertEquals("C社", c.getCompanyName());
        assertEquals("バックエンド", c.getPosition());
        assertEquals("年収 700万円〜", c.getSalary());
        assertEquals("https://example.com/3", c.getUrl());
        assertEquals("カジュアル面談から入れるか確認", c.getNote());
        assertEquals(WatchStatus.NOT_APPLIED, c.getStatus());
        assertEquals(WatchStatus.APPLIED, loaded.get(1).getStatus());
    }

    @Test
    void 新規作成時のデフォルトステータスは応募済み() {
        JobApplication app = JobApplication.create("C社", "PM", "", "");
        assertEquals(Status.APPLIED, app.getStatus());
        assertNull(app.getInterviewAt());
    }
}
