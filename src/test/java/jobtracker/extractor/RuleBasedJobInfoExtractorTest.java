package jobtracker.extractor;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;

class RuleBasedJobInfoExtractorTest {

    private final RuleBasedJobInfoExtractor extractor = new RuleBasedJobInfoExtractor();

    @Test
    void JSON_LDのJobPostingから抽出できる() {
        String html = """
                <html><head>
                <title>求人サイト</title>
                <script type="application/ld+json">
                {
                  "@context": "https://schema.org",
                  "@type": "JobPosting",
                  "title": "バックエンドエンジニア（Java）",
                  "hiringOrganization": { "@type": "Organization", "name": "株式会社サンプル" },
                  "baseSalary": {
                    "@type": "MonetaryAmount",
                    "currency": "JPY",
                    "value": { "@type": "QuantitativeValue", "minValue": 5000000, "maxValue": 8000000, "unitText": "YEAR" }
                  }
                }
                </script>
                </head><body></body></html>
                """;

        JobInfo info = extractor.extract(Jsoup.parse(html));

        assertEquals("株式会社サンプル", info.companyName());
        assertEquals("バックエンドエンジニア（Java）", info.position());
        assertEquals("年収 500万円〜800万円", info.salary());
    }

    @Test
    void graphの中のJobPostingも探せる() {
        String html = """
                <script type="application/ld+json">
                {"@context":"https://schema.org","@graph":[
                  {"@type":"WebPage","name":"x"},
                  {"@type":["JobPosting"],"title":"SRE","hiringOrganization":"テスト合同会社",
                   "baseSalary":{"value":{"value":600000,"unitText":"MONTH"}}}
                ]}
                </script>
                """;

        JobInfo info = extractor.extract(Jsoup.parse(html));

        assertEquals("テスト合同会社", info.companyName());
        assertEquals("SRE", info.position());
        assertEquals("月給 60万円", info.salary());
    }

    @Test
    void JSON_LDがなければタイトルと本文から抽出する() {
        String html = """
                <html><head><title>株式会社ABCテック | サーバーサイドエンジニア | 転職サイト</title></head>
                <body>
                  <p>年収アップ率 30%</p>
                  <dl><dt>想定年収</dt><dd>５００万円～８００万円</dd></dl>
                </body></html>
                """;

        JobInfo info = extractor.extract(Jsoup.parse(html));

        assertEquals("株式会社ABCテック", info.companyName());
        assertEquals("サーバーサイドエンジニア", info.position());
        assertEquals("年収 500万円〜800万円", info.salary());
    }

    @Test
    void 会社名が後ろに付く形式とh1の職種名() {
        String html = """
                <html><head><title>ABC株式会社の求人情報</title></head>
                <body>
                  <h1>【リモート可】Javaエンジニア</h1>
                  <p>給与：月給30万円以上</p>
                </body></html>
                """;

        JobInfo info = extractor.extract(Jsoup.parse(html));

        assertEquals("ABC株式会社", info.companyName());
        assertEquals("【リモート可】Javaエンジニア", info.position());
        assertEquals("月給 30万円〜", info.salary());
    }

    @Test
    void 年収の範囲で下限に万がない表記() {
        Document doc = Jsoup.parse("<body>年収：450〜700万円</body>");
        assertEquals("年収 450万円〜700万円", extractor.extractSalaryByRule(doc));
    }

    @Test
    void 円表記の年収() {
        Document doc = Jsoup.parse("<body>年収 4,000,000円～6,500,000円</body>");
        assertEquals("年収 400万円〜650万円", extractor.extractSalaryByRule(doc));
    }

    @Test
    void 本文が空でもJSON_LDのdescriptionから年収を拾う() {
        // Talentio のように本文を JavaScript で描画し、baseSalary を持たないページ
        String html = """
                <html><head>
                <script type="application/ld+json">
                {"@context":"https://schema.org","@type":"JobPosting",
                 "title":"社内システム開発エンジニア",
                 "hiringOrganization":{"@type":"Organization","name":"サンプル株式会社"},
                 "description":"<h1>賃金</h1><br /><p>想定年収500～1200万<br />月給 35.7万円～85.7万円</p>"}
                </script>
                </head><body><div id="app"></div></body></html>
                """;

        JobInfo info = extractor.extract(Jsoup.parse(html));

        assertEquals("年収 500万円〜1200万円", info.salary());
    }

    @Test
    void 何も取れないときは空文字() {
        JobInfo info = extractor.extract(Jsoup.parse("<html><body>hello</body></html>"));

        assertEquals("", info.companyName());
        assertEquals("", info.position());
        assertEquals("", info.salary());
    }
}
