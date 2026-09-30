package jobtracker;

import java.io.IOException;
import java.net.URL;

import javafx.application.Application;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import jobtracker.extractor.JobPageAnalyzer;
import jobtracker.extractor.RuleBasedJobInfoExtractor;
import jobtracker.repository.JsonJobApplicationRepository;
import jobtracker.ui.Dialogs;
import jobtracker.ui.JobPageAnalysisRunner;
import jobtracker.ui.MainController;
import jobtracker.ui.TrackerStore;
import jobtracker.ui.WatchListController;

/**
 * JavaFX アプリケーション本体。
 * 各部品 (保存先・求人ページ解析) を組み立て、「応募管理」「気になる」の 2 タブの画面を作る。
 */
public class JobTrackerApp extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        JsonJobApplicationRepository repository = JsonJobApplicationRepository.createDefault();
        JobPageAnalysisRunner analysisRunner =
                new JobPageAnalysisRunner(new JobPageAnalyzer(new RuleBasedJobInfoExtractor()));

        TrackerStore store = new TrackerStore(repository);
        store.load();

        Parent applicationsView = loadView("applications.fxml",
                new MainController(store, analysisRunner, getHostServices()));
        Parent watchListView = loadView("watchlist.fxml",
                new WatchListController(store, analysisRunner, getHostServices()));

        Tab applicationsTab = new Tab("応募管理", applicationsView);
        Tab watchListTab = new Tab();
        watchListTab.setContent(watchListView);
        // 未応募の件数をタブ名に出す
        watchListTab.textProperty().bind(Bindings.createStringBinding(
                () -> "気になる (" + store.countNotApplied() + ")",
                store.watchItems()));
        TabPane tabPane = new TabPane(applicationsTab, watchListTab);
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabPane.getStyleClass().add("main-tabs");

        Label title = new Label("転職活動ダッシュボード");
        title.getStyleClass().add("app-title");
        BorderPane.setMargin(title, new Insets(16, 20, 8, 20));

        Label footerLabel = new Label();
        footerLabel.getStyleClass().add("footer-label");
        footerLabel.textProperty().bind(store.messageProperty());
        HBox footer = new HBox(footerLabel);
        footer.getStyleClass().add("footer");
        footer.setPadding(new Insets(6, 20, 8, 20));

        BorderPane root = new BorderPane(tabPane);
        root.setTop(title);
        root.setBottom(footer);
        root.getStyleClass().add("root-pane");

        Scene scene = new Scene(root, 1280, 760);
        URL css = getClass().getResource("/jobtracker/ui/app.css");
        scene.getStylesheets().add(css.toExternalForm());

        stage.setTitle("Job Tracker - 転職活動ダッシュボード");
        // テーブル列の最小幅合計 + 右パネルが収まる幅
        stage.setMinWidth(1150);
        stage.setMinHeight(500);
        stage.setScene(scene);
        stage.show();

        if (store.getLoadError() != null) {
            Dialogs.showError(stage, "データの読み込みに失敗しました",
                    store.getLoadError() + "\n\nファイルを上書きしないよう、この起動中の変更は保存されません。");
        }
    }

    /** FXML を読み込む。コントローラはコンストラクタ引数が必要なので、ここで生成して渡す */
    private Parent loadView(String fileName, Object controller) throws IOException {
        URL fxml = getClass().getResource("/jobtracker/ui/" + fileName);
        FXMLLoader loader = new FXMLLoader(fxml);
        loader.setController(controller);
        return loader.load();
    }
}
