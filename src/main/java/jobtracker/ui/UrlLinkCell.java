package jobtracker.ui;

import java.util.function.Consumer;

import javafx.scene.control.Hyperlink;
import javafx.scene.control.TableCell;

/** 求人URLセル。リンクをクリックするとブラウザで開く */
public class UrlLinkCell<S> extends TableCell<S, String> {

    private final Hyperlink link = new Hyperlink("開く");

    /** @param opener URL を開く処理 (HostServices#showDocument など) */
    public UrlLinkCell(Consumer<String> opener) {
        link.setOnAction(event -> opener.accept(getItem()));
    }

    @Override
    protected void updateItem(String url, boolean empty) {
        super.updateItem(url, empty);
        if (empty || url == null || url.isBlank()) {
            setGraphic(null);
        } else {
            setGraphic(link);
        }
    }
}
