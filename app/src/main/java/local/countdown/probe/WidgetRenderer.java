package local.countdown.probe;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.TypedValue;
import android.view.View;
import android.widget.RemoteViews;
import java.util.ArrayList;
import java.util.List;
import java.time.format.DateTimeFormatter;

/** The in-app preview and desktop use the same RemoteViews, dimensions and typography. */
final class WidgetRenderer {
    static RemoteViews render(Context context, CountdownStore.Result data, int heightDp) {
        return render(context, data, heightDp, 360);
    }
    static RemoteViews render(Context context, CountdownStore.Result data, int heightDp, int widthDp) {
        SharedPreferences prefs = context.getSharedPreferences("settings", 0);
        if (Build.VERSION.SDK_INT >= 31) return ScrollingRenderer.render(context,data,heightDp,widthDp);
        if (prefs.getBoolean("wide_anniversaries", true) && data.error == null && !data.anniversaries.isEmpty())
            return SectionRenderer.render(context,data,heightDp,widthDp);
        boolean birthdayColumn = prefs.getBoolean("birthday_column", false);
        WidgetStyle style = new WidgetStyle(context);
        int font = Math.max(style.nameSize, style.valueSize);
        float fontScale = context.getResources().getConfiguration().fontScale;
        int rowHeight = Math.max(23, (int)Math.ceil(font * fontScale * 1.35f) + 7);
        int capacity = Math.max(1, Math.min(prefs.getInt("table_rows", 5), (heightDp - 42) / (rowHeight + 1)));
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget);
        views.setImageViewBitmap(R.id.card, style.background(widthDp, heightDp));
        views.setInt(R.id.card, "setImageAlpha", prefs.getInt("opacity", 85) * 255 / 100);
        PendingIntent calendar = CalendarNavigation.pending(context);
        views.setOnClickPendingIntent(R.id.widget_root, calendar);
        views.setViewVisibility(R.id.refresh, View.GONE);
        int[] containers = {R.id.entries_one, R.id.entries_two, R.id.entries_three};
        int[] headers = {R.id.header_one, R.id.header_two, R.id.header_three};
        String[] headings = {"倒数日", "纪念日", birthdayColumn ? "生日" : "周期倒数日"};
        List<List<CountdownStore.Item>> columns = new ArrayList<>();
        for (int col = 0; col < 3; col++) {
            columns.add(new ArrayList<>());
            views.removeAllViews(containers[col]);
            views.setTextViewText(headers[col], headings[col]);
            views.setTextColor(headers[col], style.accent);
            views.setOnClickPendingIntent(headers[col], calendar);
        }
        views.setTextColor(R.id.message, style.text);
        views.setTextColor(R.id.refresh, style.accent);
        for (CountdownStore.Item item : data.active) {
            if (item.repeat == 0) columns.get(0).add(item);
            else if (!birthdayColumn) columns.get(2).add(item);
        }
        columns.get(1).addAll(data.anniversaries);
        if (birthdayColumn) columns.get(2).addAll(data.birthdays);
        if (data.error != null) {
            views.setViewVisibility(R.id.message, View.VISIBLE);
            views.setTextViewText(R.id.message, data.error);
        } else if (columns.get(0).isEmpty() && columns.get(1).isEmpty() && columns.get(2).isEmpty()) {
            views.setViewVisibility(R.id.message, View.VISIBLE);
            views.setTextViewText(R.id.message, "暂无可显示的记录");
        }
        android.graphics.Bitmap divider = style.line();
        for (int col = 0; col < 3; col++) {
            List<CountdownStore.Item> items = columns.get(col);
            int columnCapacity = col == 1 ? Math.max(1, Math.min(prefs.getInt("table_rows", 5), (heightDp - 42) / (rowHeight + 18))) : capacity;
            for (int rowIndex = 0; rowIndex < columnCapacity; rowIndex++) {
                CountdownStore.Item item = rowIndex < items.size() ? items.get(rowIndex) : null;
                RemoteViews row = new RemoteViews(context.getPackageName(), col == 1 ? R.layout.widget_anniversary_row : R.layout.widget_row);
                if (Build.VERSION.SDK_INT >= 31) row.setViewLayoutHeight(R.id.row_content, rowHeight, TypedValue.COMPLEX_UNIT_DIP);
                boolean emptyMarker = items.isEmpty() && rowIndex == 0;
                row.setTextViewText(R.id.title, item == null ? emptyMarker ? "无" : "" : item.title);
                row.setTextViewText(R.id.subtitle, item == null ? "" : item.kind == 2 ? item.dateLabel != null ? item.dateLabel : item.date.format(DateTimeFormatter.ofPattern("MM-dd"))
                        : item.kind == 1 ? AnniversaryRules.elapsed(item.title, item.days)
                        : item.days == 0 ? "今天" : item.days + "天后");
                row.setTextViewTextSize(R.id.title, TypedValue.COMPLEX_UNIT_SP, style.nameSize);
                row.setTextViewTextSize(R.id.subtitle, TypedValue.COMPLEX_UNIT_SP, style.valueSize);
                row.setTextColor(R.id.title, style.text); row.setTextColor(R.id.subtitle, style.text); row.setTextColor(R.id.bullet, style.header);
                if (col == 1) {
                    AnniversaryRules.Info info = item == null ? null : item.anniversary;
                    row.setTextViewText(R.id.anniversary_detail, info == null ? "" : item.originDate + " ·" + AnniversaryRules.compact(info));
                    row.setTextColor(R.id.anniversary_detail, style.header);
                    if (item != null && info != null) row.setContentDescription(R.id.row, item.title + "，" + AnniversaryRules.elapsed(item.title, item.days)
                            + "，原始日期" + item.originDate + "，" + AnniversaryRules.nextLabel(info));
                }
                row.setImageViewBitmap(R.id.divider, divider);
                row.setOnClickPendingIntent(R.id.row, calendar);
                row.setViewVisibility(R.id.bullet, item == null && !emptyMarker ? View.INVISIBLE : View.VISIBLE);
                row.setViewVisibility(R.id.divider, !style.showDividers || rowIndex == columnCapacity - 1 ? View.INVISIBLE : View.VISIBLE);
                views.addView(containers[col], row);
            }
        }
        return views;
    }
}
