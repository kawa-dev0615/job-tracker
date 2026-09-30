package jobtracker.ui;

import java.time.LocalDate;
import java.util.Optional;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import jobtracker.model.JobApplication;

/**
 * 求人情報の登録・編集ダイアログ。応募管理と「気になる」で共用する。
 * 解析結果の確認 (取れなかった項目の手入力) にも使う。
 */
public final class JobFormDialog {

    private JobFormDialog() {
    }

    /**
     * ダイアログを表示する。
     *
     * @param initial 初期値。応募日が null なら応募日欄を出さない
     * @param message ダイアログ上部に出す補足メッセージ (null可)
     * @return 「保存」が押されたら入力内容、キャンセルなら空
     */
    public static Optional<JobForm> show(Window owner, String title, JobForm initial, String message) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.initOwner(owner);
        dialog.setTitle(title);
        applyOwnerStylesheets(dialog, owner);

        TextField companyField = new TextField(initial.companyName());
        TextField positionField = new TextField(initial.position());
        TextField salaryField = new TextField(initial.salary());
        salaryField.setPromptText("例: 年収 500万円〜800万円");
        TextField urlField = new TextField(initial.url());
        DatePicker appliedDatePicker = new DatePicker(initial.appliedDate());
        TextArea noteArea = new TextArea(initial.note());
        noteArea.setWrapText(true);
        noteArea.setPrefRowCount(3);
        noteArea.setPromptText("例: 一次面接はオンライン。リクルーター経由");
        Label noteCounter = new Label();
        noteCounter.getStyleClass().add("note-counter");
        updateNoteCounter(noteCounter, noteArea.getText());
        noteArea.textProperty().addListener((obs, oldText, newText) -> updateNoteCounter(noteCounter, newText));
        VBox noteBox = new VBox(4, noteArea, noteCounter);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        ColumnConstraints labelColumn = new ColumnConstraints();
        ColumnConstraints fieldColumn = new ColumnConstraints();
        fieldColumn.setHgrow(Priority.ALWAYS);
        fieldColumn.setPrefWidth(420);
        grid.getColumnConstraints().add(labelColumn);
        grid.getColumnConstraints().add(fieldColumn);

        int row = 0;
        addRow(grid, row++, "会社名 *", companyField);
        addRow(grid, row++, "ポジション", positionField);
        addRow(grid, row++, "想定年収", salaryField);
        addRow(grid, row++, "求人URL", urlField);
        if (initial.hasAppliedDate()) {
            addRow(grid, row++, "応募日", appliedDatePicker);
        }
        addRow(grid, row, "備考", noteBox);

        VBox content = new VBox(12);
        content.setPadding(new Insets(10, 10, 0, 10));
        if (message != null && !message.isEmpty()) {
            Label messageLabel = new Label(message);
            messageLabel.setWrapText(true);
            messageLabel.setMaxWidth(500);
            messageLabel.getStyleClass().add("dialog-message");
            content.getChildren().add(messageLabel);
        }
        content.getChildren().add(grid);
        dialog.getDialogPane().setContent(content);

        ButtonType okType = new ButtonType("保存", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelType = new ButtonType("キャンセル", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().add(okType);
        dialog.getDialogPane().getButtonTypes().add(cancelType);

        // 会社名が空のとき、備考が文字数を超えているときは保存できないようにする
        BooleanBinding noteTooLong = Bindings.createBooleanBinding(
                () -> JobApplication.countNoteLength(noteArea.getText()) > JobApplication.NOTE_MAX_LENGTH,
                noteArea.textProperty());
        Node okButton = dialog.getDialogPane().lookupButton(okType);
        okButton.disableProperty().bind(companyField.textProperty().isEmpty().or(noteTooLong));

        Platform.runLater(() -> companyField.requestFocus());

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isEmpty() || result.get() != okType) {
            return Optional.empty();
        }

        // 応募日が空欄にされた場合は元の日付のままにする
        LocalDate appliedDate = initial.appliedDate();
        if (initial.hasAppliedDate() && appliedDatePicker.getValue() != null) {
            appliedDate = appliedDatePicker.getValue();
        }
        return Optional.of(new JobForm(
                companyField.getText(),
                positionField.getText(),
                salaryField.getText(),
                urlField.getText(),
                noteArea.getText(),
                appliedDate));
    }

    /** ダイアログは別ウィンドウになるため、メイン画面と同じスタイルシートを適用する */
    private static void applyOwnerStylesheets(Dialog<?> dialog, Window owner) {
        if (owner == null || owner.getScene() == null) {
            return;
        }
        dialog.getDialogPane().getStylesheets().addAll(owner.getScene().getStylesheets());
    }

    /** 「12 / 100」形式で文字数を表示し、超えたら赤字にする */
    private static void updateNoteCounter(Label counter, String text) {
        int length = JobApplication.countNoteLength(text);
        counter.setText(length + " / " + JobApplication.NOTE_MAX_LENGTH);
        counter.getStyleClass().remove("note-counter-over");
        if (length > JobApplication.NOTE_MAX_LENGTH) {
            counter.getStyleClass().add("note-counter-over");
        }
    }

    private static void addRow(GridPane grid, int row, String labelText, Node field) {
        Label label = new Label(labelText);
        grid.add(label, 0, row);
        grid.add(field, 1, row);
    }
}
