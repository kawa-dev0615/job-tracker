package jobtracker.ui;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import jobtracker.model.JobApplication;

/** 画面表示用の文字列整形 */
public final class DisplayFormats {

    /** 面接日時が未設定のときの表示 */
    public static final String NOT_SET = "---";

    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("yyyy/MM/dd(E) HH:mm", Locale.JAPANESE);
    private static final DateTimeFormatter DATE =
            DateTimeFormatter.ofPattern("yyyy/MM/dd", Locale.JAPANESE);

    private DisplayFormats() {
    }

    /**
     * 面接日時の表示文字列。
     * カジュアル面談・選考中以外のステータスでは項目自体を出さない (空文字) 。
     */
    public static String interview(JobApplication app) {
        if (!app.getStatus().isInterviewDateRequired()) {
            return "";
        }
        if (app.getInterviewAt() == null) {
            return NOT_SET;
        }
        return DATE_TIME.format(app.getInterviewAt());
    }

    /** 一覧表示用に改行をスペースに置き換えて 1 行にする */
    public static String singleLine(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceAll("\\R+", " ");
    }

    public static String dateTime(LocalDateTime value) {
        return DATE_TIME.format(value);
    }

    public static String date(LocalDate value) {
        if (value == null) {
            return "";
        }
        return DATE.format(value);
    }
}
