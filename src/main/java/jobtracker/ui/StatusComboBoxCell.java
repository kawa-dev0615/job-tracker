package jobtracker.ui;

import javafx.collections.FXCollections;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableCell;

import jobtracker.model.JobApplication;
import jobtracker.model.Status;

/**
 * ステータス列のセル。行ごとに常にプルダウンを表示し、選んだ瞬間にステータスを変更する。
 * ステータスごとに色分けするため、CSS クラス "status-xxx" を付け替える。
 */
public class StatusComboBoxCell extends TableCell<JobApplication, Status> {

    private final ComboBox<Status> comboBox = new ComboBox<>(FXCollections.observableArrayList(Status.values()));

    /** updateItem で値をセットしている最中は変更イベントを無視するためのフラグ */
    private boolean updating = false;

    public StatusComboBoxCell() {
        comboBox.setMaxWidth(Double.MAX_VALUE);
        comboBox.getStyleClass().add("status-combo");
        comboBox.setOnAction(event -> onSelected());
    }

    private void onSelected() {
        if (updating) {
            return;
        }
        JobApplication app = getTableRow().getItem();
        Status selected = comboBox.getValue();
        if (app == null || selected == null) {
            return;
        }
        app.setStatus(selected);
    }

    @Override
    protected void updateItem(Status status, boolean empty) {
        super.updateItem(status, empty);
        if (empty || status == null) {
            setGraphic(null);
            setText(null);
            return;
        }
        updating = true;
        comboBox.setValue(status);
        updating = false;
        applyStatusStyle(status);
        setText(null);
        setGraphic(comboBox);
    }

    private void applyStatusStyle(Status status) {
        for (Status s : Status.values()) {
            comboBox.getStyleClass().remove(styleClassOf(s));
        }
        comboBox.getStyleClass().add(styleClassOf(status));
    }

    static String styleClassOf(Status status) {
        return "status-" + status.name().toLowerCase().replace('_', '-');
    }
}
