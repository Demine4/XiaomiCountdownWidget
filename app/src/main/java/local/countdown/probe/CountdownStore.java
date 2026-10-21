package local.countdown.probe;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.provider.CalendarContract;
import org.json.JSONObject;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class CountdownStore {
    static final String KEY = "key_countdown_info";
    static final String ANNIVERSARY = "key_anniversary_info", BIRTHDAY = "key_birthday_info";
    static final class Item {
        long id, days;
        int repeat, kind;
        String title, dateLabel;
        LocalDate date;
        LocalDate originDate;
        AnniversaryRules.Info anniversary;
    }
    static final class Result {
        final List<Item> active = new ArrayList<>(), past = new ArrayList<>();
        final List<Item> anniversaries = new ArrayList<>(), birthdays = new ArrayList<>();
        final List<Item> periodic = new ArrayList<>();
        int regrouped;
        int repeating, invalid, hiddenToday, unsupportedDates, missingLunarDates;
        String error;
        String note() {
            return "未来及今天 " + active.size() + " 条 · 已过期隐藏 " + past.size() + " 条"
                    + "\n纪念日 " + anniversaries.size() + " 条 · 生日 " + birthdays.size() + " 条"
                    + (regrouped > 0 ? "（已归入周期倒数日 " + regrouped + " 条）" : "")
                    + (hiddenToday > 0 ? " · 今天已隐藏 " + hiddenToday + " 条" : "")
                    + (repeating > 0 ? "\n另有 " + repeating + " 条未知重复类型，暂不显示" : "")
                    + (unsupportedDates > 0 ? "\n另有 " + unsupportedDates + " 条其他日期类型待适配，暂不显示" : "")
                    + (missingLunarDates > 0 ? "\n另有 " + missingLunarDates + " 条农历记录未提供未来重复日期，暂不显示" : "")
                    + (invalid > 0 ? "\n另有 " + invalid + " 条字段不完整，暂不显示" : "");
        }
    }
    static Result read(Context context) {
        Result result = new Result();
        if (context.checkSelfPermission(Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            result.error = "请打开应用，允许读取日历";
            return result;
        }
        Map<Long, String> events = new HashMap<>();
        Map<Long, String> rdates = new HashMap<>(), exdates = new HashMap<>();
        try {
            try (Cursor cursor = context.getContentResolver().query(CalendarContract.Events.CONTENT_URI,
                    new String[]{"_id", "title", "rdate", "exdate"}, "deleted = ?", new String[]{"0"}, null)) {
                if (cursor == null) throw new IllegalStateException();
                while (cursor.moveToNext()) {
                    long id = cursor.getLong(0);
                    events.put(id, cursor.isNull(1) ? "（无标题）" : cursor.getString(1));
                    rdates.put(id, cursor.getString(2)); exdates.put(id, cursor.getString(3));
                }
            }
            boolean includeToday = context.getSharedPreferences("settings", 0).getBoolean("today", true);
            Set<String> selected = context.getSharedPreferences("settings", 0).getStringSet("anniversary_ids", java.util.Collections.emptySet());
            ZoneId zone = ZoneId.systemDefault();
            LocalDate today = LocalDate.now(zone);
            Set<Long> seen = new HashSet<>();
            // Use the confirmed extension key, not an assumed numerical event type.
            try (Cursor cursor = context.getContentResolver().query(CalendarContract.ExtendedProperties.CONTENT_URI,
                    new String[]{"event_id", "value", "name"}, "name IN (?, ?, ?)", new String[]{KEY, ANNIVERSARY, BIRTHDAY}, null)) {
                if (cursor == null) throw new IllegalStateException();
                while (cursor.moveToNext()) {
                    long id = cursor.getLong(0);
                    if (!events.containsKey(id) || !seen.add(id)) continue;
                    try {
                        JSONObject data = new JSONObject(cursor.getString(1));
                        String key = cursor.getString(2);
                        int kind = ANNIVERSARY.equals(key) ? 1 : BIRTHDAY.equals(key) ? 2 : 0;
                        if (kind != 0 && !data.has("dateType")) { result.invalid++; continue; }
                        int dateType = data.optInt("dateType", 0);
                        if (dateType != 0 && dateType != 1) { result.unsupportedDates++; continue; }
                        if (kind == 0 && (!data.has("repeatType") || data.isNull("repeatType"))) { result.invalid++; continue; }
                        int repeat = kind == 0 ? data.getInt("repeatType") : kind == 2 ? 2 : 0;
                        if (!CountdownRules.supportedRepeat(repeat)) { result.repeating++; continue; }
                        long time = data.getLong(kind == 2 ? "firstBirthMillis" : "timeMillis");
                        Item item = new Item();
                        item.id = id; item.title = events.get(id); item.repeat = repeat; item.kind = kind;
                        if (kind == 2 && !data.optString("name").trim().isEmpty()) item.title = data.getString("name");
                        LocalDate origin = CountdownRules.date(time, zone);
                        item.originDate = origin;
                        if (kind == 0 && repeat != 0) result.periodic.add(item);
                        boolean regroup = kind == 0 && repeat != 0 && selected.contains(Long.toString(id));
                        if (kind == 1 || regroup) {
                            item.kind = 1; item.date = origin;
                            // Lunar annual instances are supplied by Xiaomi; monthly lunar lists are not annual rules.
                            LocalDate supplied = dateType == 1 && (!regroup || repeat == 2)
                                    ? RecurrenceDates.next(rdates.get(id), exdates.get(id), today, true) : null;
                            item.anniversary = AnniversaryRules.calculate(origin, today, supplied, dateType == 1);
                            if (dateType == 1 && supplied != null) item.anniversary.years = LunarLabels.anniversaryYears(time, supplied);
                            item.days = item.anniversary.elapsed;
                            result.anniversaries.add(item);
                            if (regroup) result.regrouped++;
                            continue;
                        }
                        item.date = CountdownRules.next(origin, repeat, today, kind == 0 ? includeToday : true);
                        if (dateType == 1 && (kind == 2 || kind == 0 && repeat != 0)) {
                            LocalDate occurrence = RecurrenceDates.next(rdates.get(id), exdates.get(id), today, kind != 0 || includeToday);
                            if (occurrence == null) { result.missingLunarDates++; continue; }
                            item.date = occurrence;
                        }
                        if (kind == 2 && dateType == 1) item.dateLabel = LunarLabels.birthday(time);
                        item.days = CountdownRules.days(today, item.date);
                        if (kind == 2) { result.birthdays.add(item); continue; }
                        if (item.days < 0) result.past.add(item);
                        else if (CountdownRules.visible(item.days, includeToday)) result.active.add(item);
                        else result.hiddenToday++;
                    } catch (Exception invalid) { result.invalid++; }
                }
            }
            Comparator<Item> compare = Comparator.comparing((Item item) -> item.date).thenComparingLong(item -> item.id);
            Collections.sort(result.active, compare);
            Collections.sort(result.past, compare.reversed());
            Collections.sort(result.anniversaries, compare);
            Collections.sort(result.birthdays, compare);
            result.periodic.sort(Comparator.comparing((Item item) -> item.title).thenComparingLong(item -> item.id));
        } catch (Exception error) {
            result.active.clear(); result.past.clear();
            result.anniversaries.clear(); result.birthdays.clear();
            result.periodic.clear(); result.regrouped = 0;
            result.error = error instanceof SecurityException ? "日历权限不足，请检查系统权限设置" : "读取日历失败，请打开读取检测查看接口情况";
        }
        return result;
    }
}
