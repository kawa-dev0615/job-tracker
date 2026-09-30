package jobtracker.ui;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javafx.application.HostServices;
import javafx.beans.binding.Bindings;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.stage.Window;

import jobtracker.extractor.JobInfo;
import jobtracker.model.JobApplication;
import jobtracker.model.Status;

/**
 * 応募管理タブのコントローラ。
 *
 * データの流れ:
 * masterList (全件) → FilteredList (タブで絞り込み) → SortedList (列ヘッダでソート) → TableView
 * masterList は TrackerStore が持っており、中身やプロパティが変わるたびに保存される。
 */
public class MainController {

    private static final int UPCOMING_LIMIT = 10;

    private final TrackerStore store;
    private final JobPageAnalysisRunner analysisRunner;
    private final HostServices hostServices;

    /** 全件リスト (TrackerStore と共有) */
    private final ObservableList<JobApplication> masterList;

    private FilteredList<JobApplication> filteredList;
    private final ToggleGroup tabGroup = new ToggleGroup();
    private final List<ToggleButton> tabButtons = new ArrayList<>();

    @FXML private TextField urlField;
    @FXML private Button analyzeButton;
    @FXML private Button manualAddButton;
    @FXML private ProgressIndicator progress;
    @FXML private HBox statusTabs;
    @FXML private TableView<JobApplication> table;
    @FXML private TableColumn<JobApplication, LocalDate> appliedDateColumn;
    @FXML private TableColumn<JobApplication, String> companyColumn;
    @FXML private TableColumn<JobApplication, String> positionColumn;
    @FXML private TableColumn<JobApplication, String> salaryColumn;
    @FXML private TableColumn<JobApplication, Status> statusColumn;
    @FXML private TableColumn<JobApplication, String> interviewColumn;
    @FXML private TableColumn<JobApplication, String> noteColumn;
    @FXML private TableColumn<JobApplication, String> urlColumn;
    @FXML private ListView<String> upcomingList;

    public MainController(TrackerStore store, JobPageAnalysisRunner analysisRunner, HostServices hostServices) {
        this.store = store;
        this.analysisRunner = analysisRunner;
        this.hostServices = hostServices;
        this.masterList = store.applications();
    }

    // =========================================================
    // 初期化
    // =========================================================

    @FXML
    private void initialize() {
        filteredList = new FilteredList<>(masterList);
        SortedList<JobApplication> sortedList = new SortedList<>(filteredList);
        sortedList.comparatorProperty().bind(table.comparatorProperty());
        table.setItems(sortedList);

        setUpTabs();
        setUpColumns();
        setUpRows();
        table.setPlaceholder(new Label("求人ページのURLを入力して「解析して追加」を押してください"));
        upcomingList.setPlaceholder(new Label("予定はありません"));

        // 初期表示は応募日の新しい順
        appliedDateColumn.setSortType(TableColumn.SortType.DESCENDING);
        table.getSortOrder().add(appliedDateColumn);

        // 「気になる」からの転記も含め、データが変わったら件数と面接予定を更新する
        masterList.addListener((ListChangeListener<JobApplication>) change -> refreshSummary());
        refreshSummary();
    }

    /** 画面上部のステータスタブ (すべて + 各ステータス) を作る */
    private void setUpTabs() {
        tabButtons.add(createTab(null));
        for (Status status : Status.values()) {
            tabButtons.add(createTab(status));
        }
        tabButtons.get(0).setSelected(true);

        tabGroup.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            if (newToggle == null) {
                // 選択中のタブをもう一度押しても未選択にならないようにする
                oldToggle.setSelected(true);
                return;
            }
            Status selected = (Status) newToggle.getUserData();
            if (selected == null) {
                filteredList.setPredicate(null);
            } else {
                filteredList.setPredicate(app -> app.getStatus() == selected);
            }
        });
    }

    private ToggleButton createTab(Status status) {
        ToggleButton button = new ToggleButton();
        button.setUserData(status);
        button.setToggleGroup(tabGroup);
        button.getStyleClass().add("status-tab");
        statusTabs.getChildren().add(button);
        return button;
    }

    private void setUpColumns() {
        appliedDateColumn.setCellValueFactory(cell -> cell.getValue().appliedDateProperty());
        appliedDateColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                if (empty) {
                    setText(null);
                } else {
                    setText(DisplayFormats.date(date));
                }
            }
        });

        companyColumn.setCellValueFactory(cell -> cell.getValue().companyNameProperty());
        positionColumn.setCellValueFactory(cell -> cell.getValue().positionProperty());
        salaryColumn.setCellValueFactory(cell -> cell.getValue().salaryProperty());

        statusColumn.setCellValueFactory(cell -> cell.getValue().statusProperty());
        statusColumn.setCellFactory(column -> new StatusComboBoxCell());

        // 面接日時: ステータスと面接日時のどちらが変わっても表示を更新する
        interviewColumn.setCellValueFactory(cell -> {
            JobApplication app = cell.getValue();
            return Bindings.createStringBinding(
                    () -> DisplayFormats.interview(app),
                    app.statusProperty(),
                    app.interviewAtProperty());
        });
        interviewColumn.setCellFactory(column -> new InterviewCell());

        noteColumn.setCellValueFactory(cell -> cell.getValue().noteProperty());
        noteColumn.setCellFactory(column -> new NoteCell<>());

        urlColumn.setCellValueFactory(cell -> cell.getValue().urlProperty());
        urlColumn.setCellFactory(column -> new UrlLinkCell<>(this::openUrl));
    }

    private void setUpRows() {
        table.setRowFactory(tableView -> {
            TableRow<JobApplication> row = new TableRow<>();

            // ダブルクリックで編集
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    editApplication(row.getItem());
                }
            });

            MenuItem editItem = new MenuItem("編集");
            editItem.setOnAction(event -> editApplication(row.getItem()));
            MenuItem openItem = new MenuItem("求人ページを開く");
            openItem.setOnAction(event -> openUrl(row.getItem().getUrl()));
            MenuItem deleteItem = new MenuItem("削除");
            deleteItem.setOnAction(event -> deleteApplication(row.getItem()));
            ContextMenu menu = new ContextMenu(editItem, openItem, new SeparatorMenuItem(), deleteItem);

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
        JobApplication app = JobApplication.create(info.companyName(), info.position(), info.salary(), url);
        Optional<JobForm> form = JobFormDialog.show(window(), "応募を追加", JobForm.of(app), message);
        if (form.isEmpty()) {
            return;
        }
        form.get().applyTo(app);
        masterList.add(app);
        urlField.clear();
        table.getSelectionModel().select(app);
        table.scrollTo(app);
    }

    private void editApplication(JobApplication app) {
        Optional<JobForm> form = JobFormDialog.show(window(), "応募を編集", JobForm.of(app), null);
        if (form.isPresent()) {
            form.get().applyTo(app);
        }
    }

    private void deleteApplication(JobApplication app) {
        boolean ok = Dialogs.confirm(window(), "削除の確認",
                app.getCompanyName() + " を削除しますか？", "この操作は元に戻せません。");
        if (ok) {
            masterList.remove(app);
        }
    }

    private void editInterviewDate(JobApplication app) {
        InterviewDateDialog.showAndApply(window(), app);
    }

    private void openUrl(String url) {
        if (url == null || url.isBlank()) {
            return;
        }
        hostServices.showDocument(url);
    }

    /** 同じURLが登録済みなら確認する。続行するなら true */
    private boolean confirmIfDuplicated(String url) {
        for (JobApplication app : masterList) {
            if (url.equals(app.getUrl())) {
                return Dialogs.confirm(window(), "登録済みのURL",
                        "このURLは登録済みです (" + app.getCompanyName() + ")", "もう一度追加しますか？");
            }
        }
        return true;
    }

    // =========================================================
    // データ変更時の処理
    // =========================================================

    /** タブの件数表示と「今後の面接予定」を更新する */
    private void refreshSummary() {
        for (ToggleButton button : tabButtons) {
            Status status = (Status) button.getUserData();
            if (status == null) {
                button.setText("すべて (" + masterList.size() + ")");
            } else {
                button.setText(status.getLabel() + " (" + countByStatus(status) + ")");
            }
        }
        refreshUpcomingInterviews();
    }

    private int countByStatus(Status status) {
        int count = 0;
        for (JobApplication app : masterList) {
            if (app.getStatus() == status) {
                count++;
            }
        }
        return count;
    }

    private void refreshUpcomingInterviews() {
        LocalDateTime now = LocalDateTime.now();
        List<JobApplication> upcoming = new ArrayList<>();
        for (JobApplication app : masterList) {
            if (!app.getStatus().isInterviewDateRequired()) {
                continue;
            }
            if (app.getInterviewAt() == null) {
                continue;
            }
            if (app.getInterviewAt().isBefore(now)) {
                continue;
            }
            upcoming.add(app);
        }
        upcoming.sort((a, b) -> a.getInterviewAt().compareTo(b.getInterviewAt()));

        List<String> lines = new ArrayList<>();
        for (int i = 0; i < upcoming.size() && i < UPCOMING_LIMIT; i++) {
            JobApplication app = upcoming.get(i);
            lines.add(DisplayFormats.dateTime(app.getInterviewAt()) + "\n"
                    + app.getCompanyName() + "（" + app.getStatus().getLabel() + "）");
        }
        upcomingList.getItems().setAll(lines);
    }

    // =========================================================
    // ユーティリティ
    // =========================================================

    private void setBusy(boolean busy) {
        progress.setVisible(busy);
        analyzeButton.setDisable(busy);
        manualAddButton.setDisable(busy);
        urlField.setDisable(busy);
    }

    private Window window() {
        return table.getScene().getWindow();
    }

    // =========================================================
    // セル
    // =========================================================

    /** 面接日時セル。カジュアル面談・選考中のときだけクリックで日時を入力できる */
    private class InterviewCell extends TableCell<JobApplication, String> {

        InterviewCell() {
            setOnMouseClicked(event -> {
                if (isEmpty()) {
                    return;
                }
                JobApplication app = getTableRow().getItem();
                if (app == null || !app.getStatus().isInterviewDateRequired()) {
                    return;
                }
                event.consume(); // 行のダブルクリック (編集) を発火させない
                editInterviewDate(app);
            });
        }

        @Override
        protected void updateItem(String text, boolean empty) {
            super.updateItem(text, empty);
            getStyleClass().remove("interview-editable");
            getStyleClass().remove("interview-not-set");
            if (empty || text == null || text.isEmpty()) {
                setText(null);
                setTooltipText(null);
                return;
            }
            setText(text);
            getStyleClass().add("interview-editable");
            if (DisplayFormats.NOT_SET.equals(text)) {
                getStyleClass().add("interview-not-set");
            }
            setTooltipText("クリックして面接日時を入力");
        }

        private void setTooltipText(String text) {
            if (text == null) {
                setTooltip(null);
            } else {
                setTooltip(new Tooltip(text));
            }
        }
    }
}
