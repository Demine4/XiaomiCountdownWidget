package local.countdown.probe;

import java.time.LocalDate;

/** Accumulated days use the original date; anniversary recurrence never changes that date. */
public final class AnniversaryRules {
    public static final class Info {
        public long elapsed, remaining;
        public int years;
        public LocalDate next;
    }
    public static Info calculate(LocalDate origin, LocalDate today, LocalDate suppliedNext, boolean lunar) {
        Info info = new Info();
        info.elapsed = CountdownRules.days(origin, today);
        info.next = lunar ? suppliedNext : CountdownRules.next(origin, 2, today, true);
        if (info.next != null) {
            info.remaining = CountdownRules.days(today, info.next);
            info.years = Math.max(0, info.next.getYear() - origin.getYear());
        }
        return info;
    }
    public static String elapsed(String title, long days) {
        if (days < 0) return "还有" + -days + "天";
        boolean wedding = title.contains("结婚") || title.contains("婚礼") || title.contains("领证");
        return (wedding ? "已婚" : "已过") + days + "天";
    }
    public static String nextLabel(Info info) {
        if (info.next == null) return "周年日期待同步";
        if (info.remaining == 0) return info.years == 0 ? "就是今天" : "今天是" + info.years + "周年";
        return info.years == 0 ? "距首次还有" + info.remaining + "天" : "距" + info.years + "周年还有" + info.remaining + "天";
    }
    public static String compact(Info info) {
        if (info.next == null) return "周年待同步";
        if (info.remaining == 0) return info.years == 0 ? "今天" : "今天" + info.years + "周年";
        return info.years == 0 ? "还有" + info.remaining + "天" : "距" + info.years + "周年" + info.remaining + "天";
    }
}
