package jobtracker.repository;

import java.util.ArrayList;
import java.util.List;

import jobtracker.model.JobApplication;
import jobtracker.model.WatchItem;

/**
 * 保存・読み込みの単位。応募管理と「気になる」リストを 1 回でまとめて読み書きする。
 * (転記時に片方だけ保存された状態にならないようにするため)
 *
 * @param applications 応募管理の一覧
 * @param watchItems   「気になる」リストの一覧
 */
public record TrackerData(List<JobApplication> applications, List<WatchItem> watchItems) {

    public TrackerData {
        if (applications == null) {
            applications = new ArrayList<>();
        }
        if (watchItems == null) {
            watchItems = new ArrayList<>();
        }
        applications = List.copyOf(applications);
        watchItems = List.copyOf(watchItems);
    }

    public static TrackerData empty() {
        return new TrackerData(new ArrayList<>(), new ArrayList<>());
    }
}
