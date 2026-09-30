package jobtracker.extractor;

/**
 * 給与の数値を「年収 500万円〜800万円」のような表示用文字列に整形する。
 */
public final class SalaryFormatter {

    public enum Unit {
        YEAR("年収"),
        MONTH("月給"),
        HOUR("時給");

        private final String label;

        Unit(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    private SalaryFormatter() {
    }

    /**
     * @param min  下限 (円)。不明なら null
     * @param max  上限 (円)。不明なら null
     * @param unit 年収/月給/時給
     * @return 整形した文字列。下限・上限とも不明なら空文字
     */
    public static String format(Long min, Long max, Unit unit) {
        if (min == null && max == null) {
            return "";
        }
        String prefix = unit.getLabel() + " ";

        if (min != null && max != null) {
            if (min.equals(max)) {
                return prefix + toDisplay(min, unit);
            }
            return prefix + toDisplay(min, unit) + "〜" + toDisplay(max, unit);
        }
        if (min != null) {
            return prefix + toDisplay(min, unit) + "〜";
        }
        return prefix + "〜" + toDisplay(max, unit);
    }

    /** 1万円以上は「万円」表記、それ未満(時給など)は「円」表記 */
    private static String toDisplay(long yen, Unit unit) {
        if (unit == Unit.HOUR || yen < 10_000) {
            return String.format("%,d円", yen);
        }
        long man = yen / 10_000;
        long remainder = yen % 10_000;
        if (remainder == 0) {
            return man + "万円";
        }
        // 例: 4,555,000円 → 455.5万円 (万単位で割り切れない場合は小数1桁まで出す)
        double manWithFraction = yen / 10_000.0;
        return String.format("%.1f万円", manWithFraction);
    }

    /**
     * 求人ページ上の数値を円に換算する。
     * 「500」「800万」のように1万未満の数値は「万円」単位とみなす。
     */
    public static long toYen(long number) {
        if (number >= 10_000) {
            return number;
        }
        return number * 10_000;
    }
}
