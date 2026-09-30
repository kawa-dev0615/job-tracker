package jobtracker.ui;

import java.time.LocalDate;
import java.util.Optional;

import javafx.application.HostServices;
import javafx.beans.value.ChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.stage.Window;

import jobtracker.extractor.JobInfo;
import jobtracker.model.JobApplication;
import jobtracker.model.WatchItem;
import jobtracker.model.WatchStatus;

/**
 * 「気になる」タブのコントローラ。
 *
 * ステータスを「応募済み」に変えると、確認のうえ応募管理へ転記する。
 * 転記済みの項目はグレーアウトし、編集できないようにする (求人リンクと削除のみ可) 。
 */
public class WatchListController {

    /** 転記済みの行に付ける疑似クラス (CSS でグレーアウトする) */
    private static final PseudoClass TRANSFERRED = PseudoClass.getPseudoClass("transferred");

    private final TrackerStore store;
    private final JobPageAnalysisRunner analysisRunner;
    private final HostServices hostServices;
    private final ObservableList<WatchItem> items;

    @FXML private TextField urlField;
    @FXML private Button analyzeButton;
    @FXML private Button manualAddButton;
    @FXML private ProgressIndicator progress;
    @FXML private TableView<WatchItem> table;
    @FXML private TableColumn<WatchItem, String> companyColumn;
    @FXML private TableColumn<WatchItem, String> positionColumn;
    @FXML private TableColumn<WatchItem, String> salaryColumn;
    @FXML private TableColumn<WatchItem, WatchStatus> statusColumn;
    @FXML private TableColumn<WatchItem, String> noteColumn;
    @FXML private TableColumn<WatchItem, String> urlColumn;

    public WatchListController(TrackerStore store, JobPageAnalysisRunner analysisRunner, HostServices hostServices) {
        this.store = store;
        this.analysisRunner = analysisRunner;
        this.hostServices = hostServices;
        this.items = store.watchItems();
    }

    // =========================================================
    // 初期化
    // =========================================================

    @FXML
    private void initialize() {
        SortedList<WatchItem> sortedList = new SortedList<>(items);
        sortedList.comparatorProperty().bind(table.comparatorProperty());
        table.setItems(sortedList);

        setUpColumns();
        setUpRows();
        table.setPlaceholder(new Label("気になる求人のURLを入力して「解析して追加」を押してください"));
    }

    private void setUpColumns() {
        companyColumn.setCellValueFactory(cell -> cell.getValue().companyNameProperty());
        positionColumn.setCellValueFactory(cell -> cell.getValue().positionProperty());
        salaryColumn.setCellValueFactory(cell -> cell.getValue().salaryProperty());

        statusColumn.setCellValueFactory(cell -> cell.getValue().statusProperty());
        statusColumn.setCellFactory(column -> new WatchStatusCell(this::confirmAndTransfer));

        noteColumn.setCellValueFactory(cell -> cell.getValue().noteProperty());
        noteColumn.setCellFactory(column -> new NoteCell<>());

        urlColumn.setCellValueFactory(cell -> cell.getValue().urlProperty());
        urlColumn.setCellFactory(column -> new UrlLinkCell<>(this::openUrl));
    }

    private void setUpRows() {
        table.setRowFactory(tableView -> {
            TableRow<WatchItem> row = new TableRow<>();

            // ダブルクリックで編集 (転記済みは編集不可)
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    editItem(row.getItem());
                }
            });

            MenuItem editItem = new MenuItem("編集");
            editItem.setOnAction(event -> editItem(row.getItem()));
            MenuItem openItem = new MenuItem("求人ページを開く");
            openItem.setOnAction(event -> openUrl(row.getItem().getUrl()));
            MenuItem deleteItem = new MenuItem("削除");
            deleteItem.setOnAction(event -> deleteItem(row.getItem()));
            ContextMenu menu = new ContextMenu(editItem, openItem, new SeparatorMenuItem(), deleteItem);

            // 行に表示する項目のステータスが変わったら、グレーアウトと「編集」メニューを切り替える
            ChangeListener<WatchStatus> statusListener =
                    (obs, oldStatus, newStatus) -> updateRowState(row, editItem);
            row.itemProperty().addListener((obs, oldItem, newItem) -> {
                if (oldItem != null) {
                    oldItem.statusProperty().removeListener(statusListener);
                }
                if (newItem != null) {
                    newItem.statusProperty().addListener(statusListener);
                }
                updateRowState(row, editItem);
            });

            // 空行では右クリックメニューを出さない
            row.emptyProperty().addListener((obs, wasEmpty, isEmpty) -> {
                if (isEmpty) {
                    row.setContextMenu(null);
                } else {
                    row.setContextMenu(menu);
                }
            });
            return row;
        });
    }

    private void updateRowState(TableRow<WatchItem> row, MenuItem editItem) {
        boolean transferred = isTransferred(row.getItem());
        row.pseudoClassStateChanged(TRANSFERRED, transferred);
        editItem.setDisable(transferred);
    }

    // =========================================================
    // 画面操作
    // =========================================================

    /** 「解析して追加」: 求人ページを取得・解析し、確認ダイアログを出して登録する */
    @FXML
    private void onAnalyze() {
        String url = urlField.getText().trim();
        if (!JobPageAnalysisRunner.isValidUrl(url)) {
            Dialogs.showError(window(), "URLが正しくありません", "http:// または https:// で始まるURLを入力してください。");
            return;
        }
        if (!confirmIfDuplicated(url)) {
            return;
        }
        analysisRunner.run(url, this::setBusy, (info, message) -> addWithDialog(info, url, message));
    }

    /** 「手動で追加」 */
    @FXML
    private void onManualAdd() {
        addWithDialog(JobInfo.empty(), urlField.getText().trim(), null);
    }

    private void addWithDialog(JobInfo info, String url, String message) {
        WatchItem item = WatchItem.create(info.companyName(), info.position(), info.salary(), url);
        Optional<JobForm> form = JobFormDialog.show(window(), "気になる求人を追加", JobForm.of(item), message);
        if (form.isEmpty()) {
            return;
        }
        form.get().applyTo(item);
        items.add(item);
        urlField.clear();
        table.getSelectionModel().select(item);
        table.scrollTo(item);
    }

    private void editItem(WatchItem item) {
        if (isTransferred(item)) {
            return;
        }
        Optional<JobForm> form = JobFormDialog.show(window(), "気になる求人を編集", JobForm.of(item), null);
        if (form.isPresent()) {
            form.get().applyTo(item);
        }
    }

    private void deleteItem(WatchItem item) {
        String content = "この操作は元に戻せません。";
        if (isTransferred(item)) {
            content = "応募管理に転記した内容は削除されません。\n" + content;
        }
        boolean ok = Dialogs.confirm(window(), "削除の確認", item.getCompanyName() + " を削除しますか？", content);
        if (ok) {
            items.remove(item);
        }
    }

    /**
     * 確認のうえ応募管理へ転記する。
     *
     * @return 転記したら true、キャンセルされたら false
     */
    private boolean confirmAndTransfer(WatchItem item) {
        LocalDate today = LocalDate.now();
        String content = "応募日は今日 (" + DisplayFormats.date(today) + ") で登録します。\n"
                + "転記後、この項目はグレーアウトされ、編集できなくなります。";
        boolean ok = Dialogs.confirm(window(), "応募管理へ転記",
                item.getCompanyName() + " を応募管理に転記しますか？", content);
        if (!ok) {
            return false;
        }
        JobApplication app = store.transferToApplications(item, today);
        return app != null;
    }

    private void openUrl(String url) {
        if (url == null || url.isBlank()) {
            return;
        }
        hostServices.showDocument(url);
    }

    /** 同じURLが「気になる」または応募管理に登録済みなら確認する。続行するなら true */
    private boolean confirmIfDuplicated(String url) {
        for (WatchItem item : items) {
            if (url.equals(item.getUrl())) {
                return Dialogs.confirm(window(), "登録済みのURL",
                        "このURLは「気になる」に登録済みです (" + item.getCompanyName() + ")", "もう一度追加しますか？");
            }
        }
        for (JobApplication app : store.applications()) {
            if (url.equals(app.getUrl())) {
                return Dialogs.confirm(window(), "応募済みのURL",
                        "このURLは応募管理に登録済みです (" + app.getCompanyName() + ")", "「気になる」にも追加しますか？");
            }
        }
        return true;
    }

    // =========================================================
    // ユーティリティ
    // =========================================================

    private static boolean isTransferred(WatchItem item) {
        return item != null && item.getStatus() == WatchStatus.APPLIED;
    }

    private void setBusy(boolean busy) {
        progress.setVisible(busy);
        analyzeButton.setDisable(busy);
        manualAddButton.setDisable(busy);
        urlField.setDisable(busy);
    }

    private Window window() {
        return table.getScene().getWindow();
    }
}
