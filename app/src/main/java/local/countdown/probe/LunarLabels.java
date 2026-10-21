package local.countdown.probe;

import android.icu.util.ChineseCalendar;
import android.icu.util.Calendar;
import android.icu.util.TimeZone;
import android.icu.util.ULocale;

final class LunarLabels {
    static int anniversaryYears(long original, java.time.LocalDate next) {
        ChineseCalendar lunar = new ChineseCalendar(TimeZone.getTimeZone("Asia/Shanghai"), ULocale.CHINA);
        lunar.setTimeInMillis(original); int fromYear = lunar.get(Calendar.EXTENDED_YEAR);
        lunar.setTimeInMillis(next.atTime(12, 0).atZone(java.time.ZoneId.of("Asia/Shanghai")).toInstant().toEpochMilli());
        return Math.max(0, lunar.get(Calendar.EXTENDED_YEAR) - fromYear);
    }
    static String birthday(long firstBirthMillis) {
        ChineseCalendar lunar = new ChineseCalendar(TimeZone.getTimeZone("Asia/Shanghai"), ULocale.CHINA);
        lunar.setTimeInMillis(firstBirthMillis);
        String[] months = {"正月", "二月", "三月", "四月", "五月", "六月", "七月", "八月", "九月", "十月", "冬月", "腊月"};
        int day = lunar.get(Calendar.DATE);
        String label = day == 10 ? "初十" : day == 20 ? "二十" : day == 30 ? "三十"
                : (day < 10 ? "初" : day < 20 ? "十" : "廿") + "一二三四五六七八九".charAt(day % 10 - 1);
        return (lunar.get(Calendar.IS_LEAP_MONTH) == 1 ? "闰" : "") + months[lunar.get(Calendar.MONTH)] + label;
    }
}
