package jobtracker.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class WatchItemTest {

    @Test
    void 新規作成時のデフォルトステータスは未応募() {
        WatchItem item = WatchItem.create("A社", "Javaエンジニア", "", "");
        assertEquals(WatchStatus.NOT_APPLIED, item.getStatus());
    }

    @Test
    void 転記用の応募情報は同じ項目を持ちステータスは応募済みになる() {
        WatchItem item = WatchItem.create("A社", "Javaエンジニア", "年収 600万円〜", "https://example.com/1");
        item.setNote("リファラル");

        JobApplication app = item.toApplication(LocalDate.of(2026, 9, 30));

        assertEquals("A社", app.getCompanyName());
        assertEquals("Javaエンジニア", app.getPosition());
        assertEquals("年収 600万円〜", app.getSalary());
        assertEquals("https://example.com/1", app.getUrl());
        assertEquals("リファラル", app.getNote());
        assertEquals(Status.APPLIED, app.getStatus());
        assertEquals(LocalDate.of(2026, 9, 30), app.getAppliedDate());
        assertNull(app.getInterviewAt());
        assertNotEquals(item.getId(), app.getId());
    }

    @Test
    void 転記用の応募情報を作っても元の項目は変わらない() {
        WatchItem item = WatchItem.create("A社", "", "", "");

        item.toApplication(LocalDate.of(2026, 9, 30));

        assertEquals(WatchStatus.NOT_APPLIED, item.getStatus());
    }
}
