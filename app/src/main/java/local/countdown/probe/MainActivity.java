package local.countdown.probe;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Read-only, offline probe. Event type meanings are intentionally not guessed. */
public class MainActivity extends Activity {
    private static final int LIMIT = 10000;
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final List<Event> events = new ArrayList<>();
    private TextView status;
    private EditText search;
    private LinearLayout rows;
    private Button scanButton, copyButton;
    private String summary = "尚未检测";
    private boolean busy;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm z", Locale.CHINA);

    private static final class Event {
        long id;
        String title;
        JSONObject fields;
        String extendedStatus = "未读取";
        JSONArray extended = new JSONArray();
    }

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(20), dp(20), dp(12));
        root.setBackgroundColor(Color.rgb(247, 248, 250));
        root.setOnApplyWindowInsetsListener((v, insets) -> {
            android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.ime());
            v.setPadding(dp(20) + bars.left, dp(16) + bars.top, dp(20) + bars.right, dp(12) + bars.bottom);
            return insets;
        });
        setContentView(root);
        TextView title = text("小米倒数日 · 读取检测", 23);
        title.setTypeface(null, Typeface.BOLD);
        root.addView(title);
        root.addView(text("第 1 步：确认原有数据能被读取", 14));
        root.addView(text("只读取系统日历，不修改或删除记录，不联网。\n点击下方按钮授权；检测后搜索一条你已在小米日历创建的倒数日，并核对标题和日期。", 15));
        scanButton = new Button(this);
        scanButton.setText("授权并开始检测");
        scanButton.setOnClickListener(v -> authorize());
        root.addView(scanButton);
        status = text("尚未检测。此页面用于诊断原始字段，桌面小部件请返回应用首页添加。", 14);
        root.addView(status);
        copyButton = new Button(this);
        copyButton.setText("复制不含标题的检测摘要");
        copyButton.setEnabled(false);
        copyButton.setOnClickListener(v -> copy(summary));
        root.addView(copyButton);
        search = new EditText(this);
        search.setSingleLine(true);
        search.setHint("输入已知倒数日的标题进行查找");
        root.addView(search);
        root.addView(text("以下标题仅显示在本机。点击某条查看字段；不会自动认定它是倒数日。", 12));
        ScrollView scroller = new ScrollView(this);
        rows = new LinearLayout(this);
        rows.setOrientation(LinearLayout.VERTICAL);
        scroller.addView(rows);
        root.addView(scroller, new LinearLayout.LayoutParams(-1, 0, 1));
        search.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) { render(); }
            public void afterTextChanged(Editable e) {}
        });
    }

    private void authorize() {
        if (busy) return;
        if (checkSelfPermission(Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.READ_CALENDAR}, 1);
        } else scan();
    }

    @Override public void onRequestPermissionsResult(int code, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(code, permissions, results);
        if (code == 1 && results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) scan();
        else status.setText("未获得日历读取权限，尚无法验证。请在系统应用权限中允许读取日历，然后重新检测。");
    }

    private void scan() {
        busy = true;
        scanButton.setEnabled(false);
        copyButton.setEnabled(false);
        status.setText("正在读取系统日历……");
        rows.removeAllViews();
        worker.execute(() -> {
            List<Event> found = new ArrayList<>();
            Map<String, Integer> types = new TreeMap<>();
            String result;
            try {
                String[] projection = {"_id", "title", "dtstart", "dtend", "allDay", "eventTimezone", "rrule", "rdate", "exdate", "hasExtendedProperties", "calendar_id", "deleted"};
                try (Cursor cursor = getContentResolver().query(CalendarContract.Events.CONTENT_URI, projection, "deleted = ?", new String[]{"0"}, "dtstart DESC")) {
                    if (cursor == null) throw new IllegalStateException("接口返回 null Cursor");
                    while (found.size() < LIMIT && cursor.moveToNext()) {
                        Event event = new Event();
                        event.id = cursor.getLong(0);
                        event.title = cursor.isNull(1) ? "（无标题）" : cursor.getString(1);
                        event.fields = new JSONObject();
                        for (int i = 2; i < cursor.getColumnCount(); i++) {
                            String name = cursor.getColumnName(i);
                            if (cursor.isNull(i)) event.fields.put(name, JSONObject.NULL);
                            else if (cursor.getType(i) == Cursor.FIELD_TYPE_INTEGER) event.fields.put(name, cursor.getLong(i));
                            else event.fields.put(name, cursor.getString(i));
                        }
                        String type = event.fields.optString("hasExtendedProperties", "null");
                        types.put(type, types.containsKey(type) ? types.get(type) + 1 : 1);
                        found.add(event);
                    }
                    boolean truncated = found.size() == LIMIT && cursor.moveToNext();
                    result = deviceInfo() + "\n系统日历读取：成功\n本次读取记录数：" + found.size()
                            + (truncated ? "（达到上限，结果不完整）" : "")
                            + "\n类型字段分布（原始值，尚未判定含义）：" + types
                            + "\n倒数日兼容性：待用户核对已知倒数日的标题和日期\n摘要不包含标题、日历账号或事件内容。";
                }
            } catch (Exception e) {
                found.clear();
                result = deviceInfo() + "\n系统日历读取：失败\n原因：" + error(e)
                        + "\n这不代表手机没有倒数日，只表示当前接口或权限不足。";
            }
            final String message = result;
            runOnUiThread(() -> {
                if (isDestroyed()) return;
                events.clear(); events.addAll(found);
                summary = message;
                status.setText(message);
                busy = false;
                scanButton.setEnabled(true);
                scanButton.setText("重新检测");
                copyButton.setEnabled(true);
                render();
            });
        });
    }

    private void render() {
        rows.removeAllViews();
        if (busy) return;
        String filter = search.getText().toString().trim().toLowerCase(Locale.ROOT);
        int matched = 0;
        for (Event event : events) {
            if (!event.title.toLowerCase(Locale.ROOT).contains(filter)) continue;
            matched++;
            if (matched > 80) continue;
            Button row = new Button(this);
            row.setAllCaps(false);
            row.setGravity(android.view.Gravity.START | android.view.Gravity.CENTER_VERTICAL);
            row.setText(event.title + "\n" + date(event.fields.optLong("dtstart", 0))
                    + "  · 类型原始值 " + event.fields.optString("hasExtendedProperties"));
            row.setOnClickListener(v -> inspect(event, row));
            rows.addView(row, new LinearLayout.LayoutParams(-1, -2));
        }
        rows.addView(text(matched == 0 ? "未找到匹配记录。请核对权限、标题或尝试重新检测。" : "匹配 " + matched + " 条" + (matched > 80 ? "，仅显示前 80 条，请输入更完整的标题。" : "。"), 13));
    }

    private void inspect(Event event, Button row) {
        row.setEnabled(false);
        worker.execute(() -> {
            JSONArray details = new JSONArray();
            String state;
            try (Cursor cursor = getContentResolver().query(CalendarContract.ExtendedProperties.CONTENT_URI,
                    new String[]{"name", "value"}, "event_id = ?", new String[]{Long.toString(event.id)}, null)) {
                if (cursor == null) throw new IllegalStateException("接口返回 null Cursor");
                int count = 0;
                while (count < 50 && cursor.moveToNext()) {
                    JSONObject item = new JSONObject();
                    item.put("name", cursor.isNull(0) ? JSONObject.NULL : cursor.getString(0));
                    // Inspect only the chosen event. Never query descriptions, attendees or accounts.
                    String value = cursor.isNull(1) ? null : cursor.getString(1);
                    item.put("value", value == null ? JSONObject.NULL : value.substring(0, Math.min(12000, value.length())));
                    item.put("valueTruncated", value != null && value.length() > 12000);
                    details.put(item);
                    count++;
                }
                state = "读取成功" + (count == 50 ? "（最多显示50项，可能不完整）" : "");
            } catch (Exception e) { state = "扩展字段无法读取：" + error(e); }
            event.extended = details;
            event.extendedStatus = state;
            runOnUiThread(() -> {
                if (isDestroyed()) return;
                row.setEnabled(true);
                showDetails(event);
            });
        });
    }

    private void showDetails(Event event) {
        String detail;
        try { detail = event.title + "\n\n原始开始时间（按手机时区解释）：" + date(event.fields.optLong("dtstart", 0))
                    + "\n全天事件的原始时间可能与日历界面日期不同，请结合扩展字段核对。"
                    + "\n\n基础字段\n" + event.fields.toString(2)
                    + "\n\n" + event.extendedStatus + "\n" + event.extended.toString(2); }
        catch (Exception e) { detail = "字段显示失败：" + error(e); }
        TextView content = text(detail, 14);
        content.setTextIsSelectable(true);
        content.setPadding(dp(16), dp(12), dp(16), dp(12));
        ScrollView scroll = new ScrollView(this);
        scroll.addView(content);
        new AlertDialog.Builder(this).setTitle("核对这条倒数日").setView(scroll)
                .setPositiveButton("关闭", null)
                .setNeutralButton("复制去内容字段报告", (dialog, which) -> copy(sanitizedEvent(event)))
                .setNegativeButton("复制完整字段…", (dialog, which) -> new AlertDialog.Builder(this)
                        .setTitle("完整字段可能包含私人内容")
                        .setMessage("扩展字段可能包含标题、日期和其他个人信息。确认后仅复制到剪贴板，不会自动发送。发给别人之前，请检查并删去私人内容。")
                        .setNegativeButton("取消", null)
                        .setPositiveButton("复制到剪贴板", (d, w) -> copy(fullEvent(event))).show()).show();
    }

    private String sanitizedEvent(Event event) {
        try {
            JSONObject report = new JSONObject();
            report.put("device", deviceInfo());
            report.put("hasExtendedProperties", event.fields.opt("hasExtendedProperties"));
            report.put("allDay", event.fields.opt("allDay"));
            report.put("rrulePresent", nonempty(event.fields, "rrule"));
            report.put("rdatePresent", nonempty(event.fields, "rdate"));
            report.put("extendedStatus", event.extendedStatus);
            JSONArray properties = new JSONArray();
            for (int i = 0; i < event.extended.length(); i++) {
                JSONObject raw = event.extended.getJSONObject(i);
                JSONObject safe = new JSONObject();
                safe.put("name", raw.opt("name"));
                safe.put("valueTruncated", raw.optBoolean("valueTruncated"));
                Object value = raw.opt("value");
                try {
                    JSONObject parsed = new JSONObject(String.valueOf(value));
                    JSONArray keys = new JSONArray();
                    java.util.Iterator<String> iterator = parsed.keys();
                    while (iterator.hasNext()) keys.put(iterator.next());
                    safe.put("jsonKeysOnly", keys);
                } catch (Exception ignored) { safe.put("valueFormat", "非JSON对象或不可解析，已省略值"); }
                properties.put(safe);
            }
            report.put("extendedSchema", properties);
            report.put("note", "已省略标题、事件ID、日期及扩展字段值；属性名和JSON键仍建议发送前检查。");
            return report.toString(2);
        } catch (Exception e) { return "报告生成失败：" + error(e); }
    }

    private boolean nonempty(JSONObject json, String key) { return !json.isNull(key) && !json.optString(key).isEmpty(); }

    private String fullEvent(Event event) {
        try {
            JSONObject report = new JSONObject();
            report.put("device", deviceInfo());
            report.put("title", event.title);
            report.put("fields", event.fields);
            report.put("extendedStatus", event.extendedStatus);
            report.put("extended", event.extended);
            return report.toString(2);
        } catch (Exception e) { return "报告生成失败：" + error(e); }
    }

    private String deviceInfo() {
        String calendarVersion;
        try {
            PackageInfo info = getPackageManager().getPackageInfo("com.android.calendar", 0);
            calendarVersion = info.versionName;
        } catch (Exception e) { calendarVersion = "未能获取"; }
        return "日历倒数日 0.17.0\n设备：" + Build.MANUFACTURER + " " + Build.MODEL
                + "\nAndroid：" + Build.VERSION.RELEASE + " / API " + Build.VERSION.SDK_INT
                + "\n系统构建：" + Build.DISPLAY + "\n小米日历版本：" + calendarVersion;
    }

    private String error(Exception e) {
        // Provider error messages can contain query values; only copy the category.
        if (e instanceof SecurityException) return "SecurityException（权限不足或系统限制）";
        if (e instanceof IllegalArgumentException) return "IllegalArgumentException（接口或字段不兼容）";
        return e.getClass().getSimpleName();
    }
    private String date(long millis) { return millis == 0 ? "未提供开始时间" : dateFormat.format(new Date(millis)); }
    private void copy(String value) {
        ((ClipboardManager)getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("倒数日检测报告", value));
        Toast.makeText(this, "已复制到剪贴板；发送前请检查内容", Toast.LENGTH_LONG).show();
    }
    private TextView text(String value, int size) {
        TextView view = new TextView(this);
        view.setText(value); view.setTextSize(size); view.setTextColor(Color.rgb(34, 39, 49));
        view.setPadding(0, dp(6), 0, dp(6));
        return view;
    }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    @Override protected void onDestroy() { worker.shutdownNow(); super.onDestroy(); }
}
