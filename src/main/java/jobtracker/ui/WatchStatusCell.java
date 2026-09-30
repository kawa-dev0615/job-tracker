package jobtracker.ui;

import java.util.function.Predicate;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableCell;

import jobtracker.model.WatchItem;
import jobtracker.model.WatchStatus;

/**
 * 「気になる」リストのステータス列のセル。
 * 「応募済み」を選ぶと転記処理を呼び、キャンセルされたら「未応募」に戻す。
 * 転記済みの項目はプルダウンを操作できないようにする。
 */
public class WatchStatusCell extends TableCell<WatchItem, WatchStatus> {

    private final ComboBox<WatchStatus> comboBox =
            new ComboBox<>(FXCollections.observableArrayList(WatchStatus.values()));

    /** 転記を行う処理。転記したら true、キャンセルされたら false を返す */
    private final Predicate<WatchItem> transferHandler;

    /** updateItem で値をセットしている最中は変更イベントを無視するためのフラグ */
    private boolean updating = false;

    public WatchStatusCell(Predicate<WatchItem> transferHandler) {
        this.transferHandler = transferHandler;
        comboBox.setMaxWidth(Double.MAX_VALUE);
        comboBox.getStyleClass().add("status-combo");
        comboBox.setOnAction(event -> onSelected());
    }

    private void onSelected() {
        if (updating) {
            return;
        }
        WatchItem item = getTableRow().getItem();
        WatchStatus selected = comboBox.getValue();
        if (item == null || selected != WatchStatus.APPLIED || item.getStatus() == WatchStatus.APPLIED) {
            return;
        }
        // プルダウンのポップアップが閉じてから確認ダイアログを出す
        Platform.runLater(() -> confirmTransfer(item));
    }

    private void confirmTransfer(WatchItem item) {
        boolean transferred = transferHandler.test(item);
        if (transferred) {
            return;
        }
        // キャンセルされたら「未応募」に戻す (待っている間にセルが別の行に使い回されていたら何もしない)
        if (getTableRow() != null && getTableRow().getItem() == item) {
            setComboValue(WatchStatus.NOT_APPLIED);
        }
    }

    @Override
    protected void updateItem(WatchStatus status, boolean empty) {
        super.updateItem(status, empty);
        if (empty || status == null) {
            setGraphic(null);
            setText(null);
            return;
        }
        setComboValue(status);
        comboBox.setDisable(status == WatchStatus.APPLIED);
        applyStatusStyle(status);
        setText(null);
        setGraphic(comboBox);
    }

    private void setComboValue(WatchStatus status) {
        updating = true;
        comboBox.setValue(status);
        updating = false;
    }

    private void applyStatusStyle(WatchStatus status) {
        for (WatchStatus s : WatchStatus.values()) {
            comboBox.getStyleClass().remove(styleClassOf(s));
        }
        comboBox.getStyleClass().add(styleClassOf(status));
    }

    static String styleClassOf(WatchStatus status) {
        return "watch-" + status.name().toLowerCase().replace('_', '-');
    }
}
