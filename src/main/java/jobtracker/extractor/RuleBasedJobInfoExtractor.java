package jobtracker.extractor;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * ルールベースで求人情報を抽出する実装。
 *
 * 抽出の優先順位:
 * <ol>
 *   <li>構造化データ (JSON-LD の schema.org JobPosting)。Google しごと検索対応の求人ページには大抵入っている</li>
 *   <li>タイトル・見出し・meta タグ・本文に対する正規表現</li>
 * </ol>
 * どちらでも取れなかった項目は空文字のまま返し、画面で手入力してもらう。
 */
public class RuleBasedJobInfoExtractor implements JobInfoExtractor {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** 会社名に含まれないとみなす文字 (区切り記号・助詞など) */
    private static final String NAME_CHAR = "[^\\s|｜/／:：【】\\[\\]()（）「」『』<>＜＞,、。のがはをにで]";

    private static final Pattern COMPANY_PATTERN = Pattern.compile(
            "(?:株式会社|有限会社|合同会社)" + NAME_CHAR + "{1,30}"
            + "|" + NAME_CHAR + "{1,30}(?:株式会社|有限会社|合同会社)");

    /**
     * 給与表記の例:
     * 「想定年収 500万円〜800万円」「年収：500〜800万円」「月給30万円以上」「年収 4,000,000円～」
     */
    private static final Pattern SALARY_PATTERN = Pattern.compile(
            "(想定年収|年収|年俸|月給|給与)[^0-9]{0,10}?([0-9][0-9,]*)\\s*(万)?\\s*円?"
            + "(?:\\s*[〜~～\\-－―]\\s*([0-9][0-9,]*)\\s*(万)?\\s*円?)?");

    /** タイトルを分割する区切り文字 */
    private static final Pattern TITLE_SEPARATOR = Pattern.compile("\\s*[|｜]\\s*|\\s+[-–—/／]\\s+");

    @Override
    public JobInfo extract(Document document) {
        JobInfo fromJsonLd = extractFromJsonLd(document);

        String company = fromJsonLd.companyName();
        if (company.isEmpty()) {
            company = extractCompanyByRule(document);
        }

        String position = fromJsonLd.position();
        if (position.isEmpty()) {
            position = extractPositionByRule(document, company);
        }

        String salary = fromJsonLd.salary();
        if (salary.isEmpty()) {
            salary = extractSalaryByRule(document);
        }
        if (salary.isEmpty()) {
            // 本文を JavaScript で描画するサイトでは、静的 HTML の本文が空で
            // JSON-LD の description にだけ求人本文が入っていることがある
            salary = extractSalaryFromText(extractDescriptionFromJsonLd(document));
        }

        return new JobInfo(company, position, salary);
    }

    // =========================================================
    // 1. JSON-LD (schema.org JobPosting)
    // =========================================================

    JobInfo extractFromJsonLd(Document document) {
        JsonNode jobPosting = findJobPosting(document);
        if (jobPosting == null) {
            return JobInfo.empty();
        }
        String company = readCompanyName(jobPosting.get("hiringOrganization"));
        String position = readText(jobPosting.get("title"));
        String salary = readSalary(jobPosting.get("baseSalary"));
        return new JobInfo(company, position, salary);
    }

    /** JobPosting の description を平文にして返す。description は HTML のことが多いのでタグを除く */
    String extractDescriptionFromJsonLd(Document document) {
        JsonNode jobPosting = findJobPosting(document);
        if (jobPosting == null) {
            return "";
        }
        String description = readText(jobPosting.get("description"));
        if (description.isEmpty()) {
            return "";
        }
        return Jsoup.parse(description).text();
    }

    /** ページ内の JSON-LD から最初に見つかった JobPosting を返す。なければ null */
    private JsonNode findJobPosting(Document document) {
        for (Element script : document.select("script")) {
            if (!"application/ld+json".equalsIgnoreCase(script.attr("type").trim())) {
                continue;
            }
            JsonNode root;
            try {
                root = MAPPER.readTree(script.data());
            } catch (Exception e) {
                // 壊れた JSON-LD は無視して次へ
                continue;
            }
            JsonNode jobPosting = findJobPosting(root);
            if (jobPosting != null) {
                return jobPosting;
            }
        }
        return null;
    }

    /** JSON-LD の中から @type が JobPosting のノードを探す (配列や @graph の中も探す) */
    private JsonNode findJobPosting(JsonNode node) {
        if (node == null) {
            return null;
        }
        if (node.isArray()) {
            for (JsonNode child : node) {
                JsonNode found = findJobPosting(child);
                if (found != null) {
                    return found;
                }
            }
            return null;
        }
        if (node.isObject()) {
            if (isJobPostingType(node.get("@type"))) {
                return node;
            }
            JsonNode graph = node.get("@graph");
            if (graph != null) {
                return findJobPosting(graph);
            }
        }
        return null;
    }

    private boolean isJobPostingType(JsonNode typeNode) {
        if (typeNode == null) {
            return false;
        }
        if (typeNode.isTextual()) {
            return "JobPosting".equals(typeNode.asText());
        }
        if (typeNode.isArray()) {
            for (JsonNode type : typeNode) {
                if ("JobPosting".equals(type.asText())) {
                    return true;
                }
            }
        }
        return false;
    }

    private String readCompanyName(JsonNode organization) {
        if (organization == null) {
            return "";
        }
        if (organization.isTextual()) {
            return organization.asText().trim();
        }
        return readText(organization.get("name"));
    }

    /**
     * baseSalary の形式:
     * { "value": { "minValue": 5000000, "maxValue": 8000000, "unitText": "YEAR" } }
     * { "value": 5000000, "unitText": "YEAR" } など
     */
    private String readSalary(JsonNode baseSalary) {
        if (baseSalary == null || baseSalary.isNull()) {
            return "";
        }
        JsonNode value = baseSalary.get("value");
        String unitText = readText(baseSalary.get("unitText"));

        Long min = null;
        Long max = null;
        if (value == null) {
            return "";
        }
        if (value.isObject()) {
            min = readLong(value.get("minValue"));
            max = readLong(value.get("maxValue"));
            Long single = readLong(value.get("value"));
            if (min == null && max == null && single != null) {
                min = single;
                max = single;
            }
            String innerUnit = readText(value.get("unitText"));
            if (!innerUnit.isEmpty()) {
                unitText = innerUnit;
            }
        } else {
            Long single = readLong(value);
            min = single;
            max = single;
        }
        return SalaryFormatter.format(min, max, toUnit(unitText));
    }

    private SalaryFormatter.Unit toUnit(String unitText) {
        String upper = unitText.toUpperCase();
        if (upper.equals("MONTH")) {
            return SalaryFormatter.Unit.MONTH;
        }
        if (upper.equals("HOUR")) {
            return SalaryFormatter.Unit.HOUR;
        }
        return SalaryFormatter.Unit.YEAR;
    }

    private String readText(JsonNode node) {
        if (node == null || node.isNull()) {
            return "";
        }
        return node.asText("").trim();
    }

    private Long readLong(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isNumber()) {
            return node.asLong();
        }
        if (node.isTextual()) {
            String digits = node.asText().replace(",", "").trim();
            try {
                return Math.round(Double.parseDouble(digits));
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    // =========================================================
    // 2. ルール (正規表現) による抽出
    // =========================================================

    /** タイトル・見出し・meta から「株式会社○○」「○○株式会社」を探す */
    String extractCompanyByRule(Document document) {
        for (String text : titleCandidates(document)) {
            Matcher matcher = COMPANY_PATTERN.matcher(text);
            if (matcher.find()) {
                return matcher.group().trim();
            }
        }
        String description = metaContent(document, "meta[name=description]");
        Matcher matcher = COMPANY_PATTERN.matcher(description);
        if (matcher.find()) {
            return matcher.group().trim();
        }
        return "";
    }

    /** h1 → og:title → title の順に、会社名などを取り除いた残りを職種名とみなす */
    String extractPositionByRule(Document document, String companyName) {
        for (String text : titleCandidates(document)) {
            String position = cleanPosition(text, companyName);
            if (!position.isEmpty()) {
                return position;
            }
        }
        return "";
    }

    private String cleanPosition(String text, String companyName) {
        String[] segments = TITLE_SEPARATOR.split(text);
        for (String segment : segments) {
            String s = segment;
            if (!companyName.isEmpty()) {
                s = s.replace(companyName, "");
            }
            s = s.replace("の求人・採用情報", "")
                 .replace("の求人情報", "")
                 .replace("の採用情報", "")
                 .replace("の求人", "")
                 .replace("求人情報", "")
                 .replace("採用情報", "");
            s = s.trim();
            // 会社名を消した後に残った先頭の「の」「で」や記号を取り除く
            while (!s.isEmpty() && "のでがは:：-–—・".indexOf(s.charAt(0)) >= 0) {
                s = s.substring(1).trim();
            }
            if (s.length() >= 2) {
                return s;
            }
        }
        return "";
    }

    /** ページ本文から給与表記を探す */
    String extractSalaryByRule(Document document) {
        if (document.body() == null) {
            return "";
        }
        return extractSalaryFromText(document.body().text());
    }

    /** テキストから給与表記を探す。年収 → 月給 → 給与 の優先順で採用する */
    String extractSalaryFromText(String rawText) {
        String text = normalizeDigits(rawText);
        Matcher matcher = SALARY_PATTERN.matcher(text);

        String best = "";
        int bestRank = Integer.MAX_VALUE;
        while (matcher.find()) {
            String keyword = matcher.group(1);
            int rank = keywordRank(keyword);
            if (rank >= bestRank) {
                continue;
            }
            String formatted = formatSalaryMatch(matcher);
            if (!formatted.isEmpty()) {
                best = formatted;
                bestRank = rank;
            }
        }
        return best;
    }

    private int keywordRank(String keyword) {
        if (keyword.equals("想定年収")) {
            return 0;
        }
        if (keyword.equals("年収") || keyword.equals("年俸")) {
            return 1;
        }
        if (keyword.equals("月給")) {
            return 2;
        }
        return 3;
    }

    private String formatSalaryMatch(Matcher matcher) {
        String keyword = matcher.group(1);
        Long min = parseAmount(matcher.group(2), matcher.group(3));
        Long max = parseAmount(matcher.group(4), matcher.group(5));
        if (min == null) {
            return "";
        }
        if (max != null && max < min) {
            max = null;
        }

        SalaryFormatter.Unit unit;
        if (keyword.equals("月給")) {
            unit = SalaryFormatter.Unit.MONTH;
        } else if (keyword.equals("給与")) {
            // 「給与」は年収・月給どちらもあり得るので金額で判断する
            if (min >= 1_500_000) {
                unit = SalaryFormatter.Unit.YEAR;
            } else {
                unit = SalaryFormatter.Unit.MONTH;
            }
        } else {
            unit = SalaryFormatter.Unit.YEAR;
        }

        // 明らかに給与ではない数値 (「年収アップ率30%」など) を除外する
        if (unit == SalaryFormatter.Unit.YEAR && min < 1_000_000) {
            return "";
        }
        if (unit == SalaryFormatter.Unit.MONTH && min < 100_000) {
            return "";
        }

        if (max == null) {
            return SalaryFormatter.format(min, null, unit);
        }
        return SalaryFormatter.format(min, max, unit);
    }

    private Long parseAmount(String number, String manSuffix) {
        if (number == null) {
            return null;
        }
        long value;
        try {
            value = Long.parseLong(number.replace(",", ""));
        } catch (NumberFormatException e) {
            return null;
        }
        if (manSuffix != null) {
            return value * 10_000;
        }
        return SalaryFormatter.toYen(value);
    }

    // =========================================================
    // 共通処理
    // =========================================================

    private List<String> titleCandidates(Document document) {
        List<String> candidates = new ArrayList<>();
        Element h1 = document.selectFirst("h1");
        if (h1 != null) {
            addIfNotEmpty(candidates, h1.text());
        }
        addIfNotEmpty(candidates, metaContent(document, "meta[property=og:title]"));
        addIfNotEmpty(candidates, document.title());
        return candidates;
    }

    private void addIfNotEmpty(List<String> list, String value) {
        if (value != null && !value.isBlank()) {
            list.add(value.trim());
        }
    }

    private String metaContent(Document document, String selector) {
        Element meta = document.selectFirst(selector);
        if (meta == null) {
            return "";
        }
        return meta.attr("content");
    }

    /** 全角数字・全角カンマを半角に揃える */
    static String normalizeDigits(String text) {
        StringBuilder sb = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c >= '０' && c <= '９') {
                sb.append((char) (c - '０' + '0'));
            } else if (c == '，') {
                sb.append(',');
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
