package jobtracker.ui;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

import javafx.concurrent.Task;

import jobtracker.extractor.JobInfo;
import jobtracker.extractor.JobPageAnalyzer;

/**
 * 求人ページの解析を別スレッドで実行する。応募管理と「気になる」で共用する。
 * 通信中に画面が固まらないよう、javafx.concurrent.Task で実行する。
 */
public class JobPageAnalysisRunner {

    private final JobPageAnalyzer analyzer;

    public JobPageAnalysisRunner(JobPageAnalyzer analyzer) {
        this.analyzer = analyzer;
    }

    /**
     * URL を解析する。成功・失敗どちらでも onFinished を呼ぶ (失敗時は空の JobInfo で手入力してもらう) 。
     *
     * @param onBusyChanged 解析開始時に true、終了時に false で呼ばれる
     * @param onFinished    解析結果と、ダイアログに出す補足メッセージを受け取る
     */
    public void run(String url, Consumer<Boolean> onBusyChanged, BiConsumer<JobInfo, String> onFinished) {
        Task<JobInfo> task = new Task<>() {
            @Override
            protected JobInfo call() throws Exception {
                return analyzer.analyze(url);
            }
        };
        task.setOnSucceeded(event -> {
            onBusyChanged.accept(false);
            JobInfo info = task.getValue();
            String message;
            if (info.isComplete()) {
                message = "求人ページから以下の内容を読み取りました。内容を確認して保存してください。";
            } else {
                message = "一部の項目を読み取れませんでした。空欄を手入力して保存してください。";
            }
            onFinished.accept(info, message);
        });
        task.setOnFailed(event -> {
            onBusyChanged.accept(false);
            Throwable error = task.getException();
            System.err.println("求人ページの取得に失敗しました: " + url);
            error.printStackTrace();
            String message = "ページを取得できませんでした (" + error.getMessage() + ")。\n手入力で登録できます。";
            onFinished.accept(JobInfo.empty(), message);
        });

        onBusyChanged.accept(true);
        Thread thread = new Thread(task, "job-page-analyzer");
        thread.setDaemon(true);
        thread.start();
    }

    /** http:// または https:// で始まっていれば true */
    public static boolean isValidUrl(String url) {
        return url.startsWith("http://") || url.startsWith("https://");
    }
}
