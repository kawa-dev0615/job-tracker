package jobtracker.repository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import jobtracker.model.JobApplication;
import jobtracker.model.WatchItem;

/**
 * 応募データを JSON ファイル1つに保存するリポジトリ。
 *
 * 想定件数は100件程度なので、毎回全件を読み書きするシンプルな方式にしている。
 * 書き込み途中でアプリが落ちてもファイルが壊れないよう、一時ファイルに書いてから置き換える。
 */
public class JsonJobApplicationRepository implements JobApplicationRepository {

    /**
     * ファイル形式のバージョン。将来項目を増やしたときの移行判定用。
     * 1: 応募一覧のみ / 2: 「気になる」リスト (watchItems) を追加。1 のファイルもそのまま読める
     */
    static final int CURRENT_VERSION = 2;

    private final Path filePath;
    private final ObjectMapper mapper;

    public JsonJobApplicationRepository(Path filePath) {
        this.filePath = filePath;
        this.mapper = new ObjectMapper();
        this.mapper.registerModule(new JavaTimeModule());
        this.mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        this.mapper.enable(SerializationFeature.INDENT_OUTPUT);
        this.mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    /** ユーザーのホームディレクトリ配下に保存する標準のリポジトリを作る */
    public static JsonJobApplicationRepository createDefault() {
        Path home = Path.of(System.getProperty("user.home"));
        Path file = home.resolve(".job-tracker").resolve("applications.json");
        return new JsonJobApplicationRepository(file);
    }

    public Path getFilePath() {
        return filePath;
    }

    @Override
    public TrackerData load() {
        if (!Files.exists(filePath)) {
            return TrackerData.empty();
        }
        try {
            DataFile dataFile = mapper.readValue(filePath.toFile(), DataFile.class);
            return new TrackerData(dataFile.getApplications(), dataFile.getWatchItems());
        } catch (IOException e) {
            throw new RepositoryException("データファイルの読み込みに失敗しました: " + filePath, e);
        }
    }

    @Override
    public void save(TrackerData data) {
        DataFile dataFile = new DataFile();
        dataFile.setVersion(CURRENT_VERSION);
        dataFile.setApplications(new ArrayList<>(data.applications()));
        dataFile.setWatchItems(new ArrayList<>(data.watchItems()));

        try {
            Path dir = filePath.getParent();
            if (dir != null) {
                Files.createDirectories(dir);
            }
            Path tempFile = filePath.resolveSibling(filePath.getFileName() + ".tmp");
            mapper.writeValue(tempFile.toFile(), dataFile);
            Files.move(tempFile, filePath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RepositoryException("データファイルの保存に失敗しました: " + filePath, e);
        }
    }

    /** JSONファイルの最上位の構造 */
    public static class DataFile {
        private int version;
        private List<JobApplication> applications = new ArrayList<>();
        private List<WatchItem> watchItems = new ArrayList<>();

        public int getVersion() {
            return version;
        }

        public void setVersion(int version) {
            this.version = version;
        }

        public List<JobApplication> getApplications() {
            return applications;
        }

        public void setApplications(List<JobApplication> applications) {
            this.applications = applications;
        }

        public List<WatchItem> getWatchItems() {
            return watchItems;
        }

        public void setWatchItems(List<WatchItem> watchItems) {
            this.watchItems = watchItems;
        }
    }
}
