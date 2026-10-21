import local.countdown.probe.CountdownRules;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public class CountdownRulesTest {
    private static int passed;
    private static void check(boolean ok, String name) {
        if (!ok) throw new AssertionError(name);
        passed++;
    }
    public static void main(String[] args) {
        ZoneId shanghai = ZoneId.of("Asia/Shanghai");
        // Synthetic fixture: do not retain the user's private event title in tests.
        check(CountdownRules.date(1789178400000L, shanghai).equals(LocalDate.of(2026, 9, 12)), "confirmed timestamp interpretation");
        LocalDate today = LocalDate.of(2026, 10, 19);
        check(CountdownRules.days(today, today.minusDays(1)) == -1, "past");
        check(!CountdownRules.visible(-1, true), "past hidden");
        check(CountdownRules.visible(0, true), "today visible");
        check(!CountdownRules.visible(0, false), "today option");
        check(CountdownRules.visible(1, false), "future");
        check(CountdownRules.days(LocalDate.of(2028, 2, 28), LocalDate.of(2028, 3, 1)) == 2, "leap year");
        check(CountdownRules.days(LocalDate.of(2026, 12, 31), LocalDate.of(2027, 1, 1)) == 1, "year boundary");
        ZoneId ny = ZoneId.of("America/New_York");
        long before = ZonedDateTime.of(2026, 3, 8, 0, 0, 0, 0, ny).toInstant().toEpochMilli();
        long after = ZonedDateTime.of(2026, 3, 9, 0, 0, 0, 0, ny).toInstant().toEpochMilli();
        check(after - before == 23 * 3600000L, "DST fixture");
        check(CountdownRules.days(CountdownRules.date(before, ny), CountdownRules.date(after, ny)) == 1, "DST day count");
        check(CountdownRules.supportedRepeat(0), "one-time supported");
        check(CountdownRules.supportedRepeat(1) && CountdownRules.supportedRepeat(2) && !CountdownRules.supportedRepeat(-1) && !CountdownRules.supportedRepeat(3), "confirmed repeat mapping");
        LocalDate origin = LocalDate.of(2026, 1, 31);
        check(CountdownRules.next(origin, 1, LocalDate.of(2026, 2, 1), true).equals(LocalDate.of(2026, 2, 28)), "month end clamp");
        check(CountdownRules.next(origin, 1, LocalDate.of(2026, 3, 1), true).equals(LocalDate.of(2026, 3, 31)), "retain original day after short month");
        check(CountdownRules.next(origin, 1, LocalDate.of(2026, 3, 31), true).equals(LocalDate.of(2026, 3, 31)), "monthly today");
        check(CountdownRules.next(origin, 1, LocalDate.of(2026, 3, 31), false).equals(LocalDate.of(2026, 4, 30)), "monthly skip today");
        LocalDate leap = LocalDate.of(2024, 2, 29);
        check(CountdownRules.next(leap, 2, LocalDate.of(2026, 3, 1), true).equals(LocalDate.of(2027, 2, 28)), "yearly leap clamp");
        check(CountdownRules.next(leap, 2, LocalDate.of(2028, 2, 1), true).equals(LocalDate.of(2028, 2, 29)), "yearly retains leap day");
        check(CountdownRules.next(LocalDate.of(2027, 1, 1), 1, today, true).equals(LocalDate.of(2027, 1, 1)), "do not recur before initial date");
        check(CountdownRules.next(today.minusDays(1), 0, today, true).equals(today.minusDays(1)), "one-time never advances");
        System.out.println("PASS: " + passed + " countdown boundary checks");
    }
}
