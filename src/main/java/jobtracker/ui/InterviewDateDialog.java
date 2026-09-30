package jobtracker.ui;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import javafx.util.StringConverter;

import jobtracker.model.JobApplication;

/**
 * 面接日時の入力ダイアログ。
 * 「未定に戻す」を押すと面接日時を消して "---" 表示に戻す。
 */
public final class InterviewDateDialog {

    private static final int DEFAULT_HOUR = 10;

    private InterviewDateDialog() {
    }

    /** @return 値を変更したら true */
    public static boolean showAndApply(Window owner, JobApplication target) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.initOwner(owner);
        dialog.setTitle("面接日時の設定");

        LocalDateTime current = target.getInterviewAt();

        DatePicker datePicker = new DatePicker();
        ComboBox<Integer> hourBox = new ComboBox<>();
        ComboBox<Integer> minuteBox = new ComboBox<>();
        for (int h = 0; h < 24; h++) {
            hourBox.getItems().add(h);
        }
        for (int m = 0; m < 60; m += 5) {
            minuteBox.getItems().add(m);
        }
        hourBox.setConverter(twoDigits());
        minuteBox.setConverter(twoDigits());

        if (current != null) {
            datePicker.setValue(current.toLocalDate());
            hourBox.setValue(current.getHour());
            minuteBox.setValue(current.getMinute() - current.getMinute() % 5);
        } else {
            datePicker.setValue(LocalDate.now());
            hourBox.setValue(DEFAULT_HOUR);
            minuteBox.setValue(0);
        }

        HBox timeRow = new HBox(6, hourBox, new Label("時"), minuteBox, new Label("分"));
        timeRow.setAlignment(Pos.CENTER_LEFT);

        VBox content = new VBox(10,
                new Label(target.getCompanyName() + " / " + target.getStatus().getLabel()),
                datePicker,
                timeRow);
        content.setPadding(new Insets(10));
        dialog.getDialogPane().setContent(content);

        ButtonType okType = new ButtonType("保存", ButtonBar.ButtonData.OK_DONE);
        ButtonType clearType = new ButtonType("未定に戻す", ButtonBar.ButtonData.LEFT);
        ButtonType cancelType = new ButtonType("キャンセル", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().add(clearType);
        dialog.getDialogPane().getButtonTypes().add(okType);
        dialog.getDialogPane().getButtonTypes().add(cancelType);

        dialog.getDialogPane().lookupButton(okType).disableProperty()
                .bind(datePicker.valueProperty().isNull());

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isEmpty()) {
            return false;
        }
        if (result.get() == clearType) {
            target.setInterviewAt(null);
            return true;
        }
        if (result.get() == okType) {
            LocalTime time = LocalTime.of(hourBox.getValue(), minuteBox.getValue());
            target.setInterviewAt(LocalDateTime.of(datePicker.getValue(), time));
            return true;
        }
        return false;
    }

    private static StringConverter<Integer> twoDigits() {
        return new StringConverter<>() {
            @Override
            public String toString(Integer value) {
                if (value == null) {
                    return "";
                }
                return String.format("%02d", value);
            }

            @Override
            public Integer fromString(String text) {
                return Integer.valueOf(text);
            }
        };
    }
}
