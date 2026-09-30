package jobtracker;

import javafx.application.Application;

/**
 * エントリポイント。
 *
 * Application を継承したクラスを直接 main にすると、実行可能jarから起動したときに
 * 「JavaFX runtime components are missing」エラーになるため、別クラスから起動している。
 */
public final class Launcher {

    private static final String TRUST_STORE_TYPE_KEY = "javax.net.ssl.trustStoreType";
    private static final String WINDOWS_TRUST_STORE_TYPE = "Windows-ROOT";

    private Launcher() {
    }

    public static void main(String[] args) {
        useWindowsTrustStoreIfAvailable();
        Application.launch(JobTrackerApp.class, args);
    }

    /**
     * Windows ではOSの証明書ストアを使って HTTPS 接続する。
     *
     * JDK 標準の cacerts はOSの証明書ストアと別管理のため、ウイルス対策ソフトの HTTPS スキャンや
     * 社内プロキシが証明書を差し替える環境では、ブラウザで開けるページでも証明書エラーになる。
     * 起動時に -D で明示指定されている場合はそちらを優先する。
     */
    private static void useWindowsTrustStoreIfAvailable() {
        String osName = System.getProperty("os.name", "");
        if (!osName.startsWith("Windows")) {
            return;
        }
        if (System.getProperty(TRUST_STORE_TYPE_KEY) != null) {
            return;
        }
        System.setProperty(TRUST_STORE_TYPE_KEY, WINDOWS_TRUST_STORE_TYPE);
    }
}
