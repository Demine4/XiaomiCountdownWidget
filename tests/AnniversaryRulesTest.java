import local.countdown.probe.AnniversaryRules;
import java.time.LocalDate;

public class AnniversaryRulesTest {
    private static int passed;
    private static void check(boolean ok, String name) { if (!ok) throw new AssertionError(name); passed++; }
    public static void main(String[] args) {
        LocalDate origin = LocalDate.of(2023, 6, 18);
        AnniversaryRules.Info info = AnniversaryRules.calculate(origin, LocalDate.of(2026, 10, 19), null, false);
        check(info.elapsed == 1219, "elapsed preserves original year");
        check(info.next.equals(LocalDate.of(2027, 6, 18)) && info.years == 4, "next anniversary");
        check(info.remaining == 242, "days to next anniversary");
        check(AnniversaryRules.elapsed("结婚纪念日", info.elapsed).equals("已婚1219天"), "wedding label");
        check(AnniversaryRules.elapsed("入职纪念日", info.elapsed).equals("已过1219天"), "ordinary label");
        AnniversaryRules.Info anniversaryToday = AnniversaryRules.calculate(origin, LocalDate.of(2026, 6, 18), null, false);
        check(anniversaryToday.years == 3 && anniversaryToday.remaining == 0 && AnniversaryRules.nextLabel(anniversaryToday).equals("今天是3周年"), "anniversary today");
        AnniversaryRules.Info dayAfter = AnniversaryRules.calculate(origin, LocalDate.of(2026, 6, 19), null, false);
        check(dayAfter.years == 4 && dayAfter.next.equals(LocalDate.of(2027, 6, 18)), "roll forward after anniversary");
        AnniversaryRules.Info firstDay = AnniversaryRules.calculate(origin, origin, null, false);
        check(firstDay.elapsed == 0 && firstDay.years == 0 && AnniversaryRules.nextLabel(firstDay).equals("就是今天"), "first day not zeroth anniversary");
        AnniversaryRules.Info future = AnniversaryRules.calculate(origin, origin.minusDays(10), null, false);
        check(future.elapsed == -10 && future.remaining == 10 && AnniversaryRules.elapsed("结婚", future.elapsed).equals("还有10天"), "future wedding not negative married days");
        AnniversaryRules.Info leap = AnniversaryRules.calculate(LocalDate.of(2024, 2, 29), LocalDate.of(2025, 2, 28), null, false);
        check(leap.years == 1 && leap.remaining == 0, "leap day anniversary clamp");
        AnniversaryRules.Info lunar = AnniversaryRules.calculate(origin, LocalDate.of(2026, 10, 19), LocalDate.of(2026, 10, 22), true);
        check(lunar.remaining == 3 && lunar.next.equals(LocalDate.of(2026, 10, 22)), "supplied lunar date used");
        AnniversaryRules.Info missing = AnniversaryRules.calculate(origin, LocalDate.of(2026, 10, 19), null, true);
        check(missing.elapsed == 1219 && missing.next == null && AnniversaryRules.nextLabel(missing).contains("待同步"), "missing lunar recurrence still counts days");
        System.out.println("PASS: " + passed + " anniversary checks");
    }
}
