import local.countdown.probe.RecurrenceDates;
import java.time.LocalDate;

public class RecurrenceDatesTest {
    private static int passed;
    private static void check(boolean value, String name) { if (!value) throw new AssertionError(name); passed++; }
    public static void main(String[] args) {
        String dates = "20261022,20271012,20281030,20291020";
        check(RecurrenceDates.next(dates, null, LocalDate.of(2026, 10, 19), true).equals(LocalDate.of(2026, 10, 22)), "provided lunar instances");
        check(RecurrenceDates.next(dates, null, LocalDate.of(2026, 10, 23), true).equals(LocalDate.of(2027, 10, 12)), "next lunar date differs from Gregorian anniversary");
        check(RecurrenceDates.next(dates, null, LocalDate.of(2026, 10, 22), true).equals(LocalDate.of(2026, 10, 22)), "keep today");
        check(RecurrenceDates.next(dates, null, LocalDate.of(2026, 10, 22), false).equals(LocalDate.of(2027, 10, 12)), "skip today");
        check(RecurrenceDates.next(dates, "20261022", LocalDate.of(2026, 10, 19), true).equals(LocalDate.of(2027, 10, 12)), "excluded instance");
        check(RecurrenceDates.next("20281030,20261022,20271012,20261022", null, LocalDate.of(2026, 10, 19), true).equals(LocalDate.of(2026, 10, 22)), "unordered duplicates");
        check(RecurrenceDates.next("garbage,20260230,20261022", null, LocalDate.of(2026, 10, 19), true).equals(LocalDate.of(2026, 10, 22)), "invalid dates ignored");
        check(RecurrenceDates.next(null, null, LocalDate.of(2026, 10, 19), true) == null, "no guessing missing instances");
        check(RecurrenceDates.next(dates, null, LocalDate.of(2030, 1, 1), true) == null, "exhausted list not fabricated");
        check(RecurrenceDates.next("20261022T020000Z", null, LocalDate.of(2026, 10, 19), true).equals(LocalDate.of(2026, 10, 22)), "date-time entry");
        System.out.println("PASS: " + passed + " supplied recurrence date checks");
    }
}
