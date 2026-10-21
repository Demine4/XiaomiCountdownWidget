package local.countdown.probe;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;

/** Day-based countdown: keep today until the end of the local day, never use 24h division. */
public final class CountdownRules {
    private CountdownRules() {}
    public static LocalDate date(long millis, ZoneId zone) {
        return Instant.ofEpochMilli(millis).atZone(zone).toLocalDate();
    }
    public static long days(LocalDate today, LocalDate target) { return ChronoUnit.DAYS.between(today, target); }
    public static boolean visible(long days, boolean includeToday) { return days > 0 || (days == 0 && includeToday); }
    public static boolean supportedRepeat(int repeatType) { return repeatType >= 0 && repeatType <= 2; }
    public static LocalDate next(LocalDate origin, int repeatType, LocalDate today, boolean includeToday) {
        if (!supportedRepeat(repeatType)) throw new IllegalArgumentException("Unknown repeat type");
        if (repeatType == 0 || origin.isAfter(today)) return origin;
        LocalDate threshold = includeToday ? today : today.plusDays(1);
        if (repeatType == 1) {
            YearMonth month = YearMonth.from(threshold);
            LocalDate candidate = month.atDay(Math.min(origin.getDayOfMonth(), month.lengthOfMonth()));
            if (candidate.isBefore(threshold)) {
                month = month.plusMonths(1);
                candidate = month.atDay(Math.min(origin.getDayOfMonth(), month.lengthOfMonth()));
            }
            return candidate;
        }
        YearMonth month = YearMonth.of(threshold.getYear(), origin.getMonthValue());
        LocalDate candidate = month.atDay(Math.min(origin.getDayOfMonth(), month.lengthOfMonth()));
        if (candidate.isBefore(threshold)) {
            month = month.plusYears(1);
            candidate = month.atDay(Math.min(origin.getDayOfMonth(), month.lengthOfMonth()));
        }
        return candidate;
    }
}
