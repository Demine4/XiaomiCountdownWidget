package local.countdown.probe;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Set;

/** Use Xiaomi's supplied Gregorian instances for lunar recurrences, without reconstructing rules. */
public final class RecurrenceDates {
    private static Set<LocalDate> dates(String values) {
        Set<LocalDate> result = new HashSet<>();
        if (values == null) return result;
        for (String raw : values.split(",")) {
            String value = raw.trim();
            try {
                if (value.length() == 8 || value.length() >= 15 && value.charAt(8) == 'T')
                    result.add(LocalDate.parse(value.substring(0, 8), DateTimeFormatter.BASIC_ISO_DATE));
            } catch (RuntimeException ignored) { /* malformed dates are not displayed */ }
        }
        return result;
    }
    public static LocalDate next(String rdate, String exdate, LocalDate today, boolean includeToday) {
        Set<LocalDate> excluded = dates(exdate);
        LocalDate best = null;
        for (LocalDate candidate : dates(rdate)) {
            if (excluded.contains(candidate) || candidate.isBefore(today) || !includeToday && candidate.equals(today)) continue;
            if (best == null || candidate.isBefore(best)) best = candidate;
        }
        return best;
    }
}
