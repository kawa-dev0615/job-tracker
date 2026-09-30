package jobtracker.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class JobApplicationTest {

    @Test
    void 備考の文字数は日本語も1文字ずつ数える() {
        assertEquals(5, JobApplication.countNoteLength("一次面接済"));
    }

    @Test
    void 備考の文字数は絵文字も1文字として数える() {
        assertEquals(2, JobApplication.countNoteLength("👍!"));
    }

    @Test
    void 備考の文字数は前後の空白を数えない() {
        assertEquals(2, JobApplication.countNoteLength("  メモ \n"));
    }

    @Test
    void 備考がnullなら0文字() {
        assertEquals(0, JobApplication.countNoteLength(null));
    }

    @Test
    void 備考は前後の空白を除いて保存する() {
        JobApplication app = new JobApplication();
        app.setNote("  メモ  ");
        assertEquals("メモ", app.getNote());
    }
}
