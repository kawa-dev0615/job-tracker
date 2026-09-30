package jobtracker.extractor;

import java.io.IOException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

/**
 * 求人ページのURLを受け取り、HTMLを取得して求人情報を抽出する。
 * (HTML取得 → {@link JobInfoExtractor} で抽出)
 */
public class JobPageAnalyzer {

    private static final int TIMEOUT_MILLIS = 15_000;

    /**
     * ブラウザを装わず、このアプリからのアクセスであることを名乗る。
     * サイト側がアクセス元を判別・制御できるようにするため。
     */
    public static final String DEFAULT_USER_AGENT =
            "JobTracker/0.1.0 (+https://github.com/kawa-dev0615/job-tracker; personal job application tracker; fetches one page per user action)";

    private final JobInfoExtractor extractor;
    private final String userAgent;

    public JobPageAnalyzer(JobInfoExtractor extractor) {
        this(extractor, DEFAULT_USER_AGENT);
    }

    public JobPageAnalyzer(JobInfoExtractor extractor, String userAgent) {
        this.extractor = extractor;
        this.userAgent = userAgent;
    }

    public JobInfo analyze(String url) throws IOException {
        Document document = fetch(url);
        return extractor.extract(document);
    }

    Document fetch(String url) throws IOException {
        return Jsoup.connect(url)
                .userAgent(userAgent)
                .timeout(TIMEOUT_MILLIS)
                .followRedirects(true)
                .get();
    }
}
