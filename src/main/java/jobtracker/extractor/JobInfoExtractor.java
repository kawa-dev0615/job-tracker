package jobtracker.extractor;

import org.jsoup.nodes.Document;

/**
 * 取得済みの HTML から求人情報を抽出する。
 *
 * 抽出ロジックを差し替えられるようにインターフェースにしている。
 * 現在はルールベース実装 ({@link RuleBasedJobInfoExtractor}) のみ。
 * 将来 LLM で抽出する実装を追加する場合もこのインターフェースを実装する。
 */
public interface JobInfoExtractor {

    JobInfo extract(Document document);
}
