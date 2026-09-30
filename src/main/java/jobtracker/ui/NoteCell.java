package jobtracker.ui;

import javafx.scene.control.TableCell;
import javafx.scene.control.Tooltip;

/** 備考セル。1 行で表示し、全文はツールチップで見せる */
public class NoteCell<S> extends TableCell<S, String> {

    private static final double TOOLTIP_MAX_WIDTH = 360;

    @Override
    protected void updateItem(String note, boolean empty) {
        super.updateItem(note, empty);
        if (empty || note == null || note.isEmpty()) {
            setText(null);
            setTooltip(null);
            return;
        }
        setText(DisplayFormats.singleLine(note));
        Tooltip tooltip = new Tooltip(note);
        tooltip.setWrapText(true);
        tooltip.setMaxWidth(TOOLTIP_MAX_WIDTH);
        setTooltip(tooltip);
    }
}
