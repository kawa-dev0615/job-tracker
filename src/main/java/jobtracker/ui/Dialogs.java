package jobtracker.ui;

import java.util.Optional;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Window;

/** 確認・エラー表示の共通処理 */
public final class Dialogs {

    private Dialogs() {
    }

    /** 確認ダイアログを出す。OK が押されたら true */
    public static boolean confirm(Window owner, String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        if (owner != null) {
            alert.initOwner(owner);
        }
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    public static void showError(Window owner, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        if (owner != null) {
            alert.initOwner(owner);
        }
        alert.setTitle("エラー");
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
