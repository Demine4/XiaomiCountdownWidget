package local.countdown.probe;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.TypedValue;
import android.view.View;
import android.widget.RemoteViews;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Android 12+ collection widgets: fixed headers and three independent, variable-height lists. */
final class ScrollingRenderer {
    static RemoteViews render(Context context, CountdownStore.Result data, int height, int width) {
        SharedPreferences prefs = context.getSharedPreferences("settings",0);
        WidgetStyle style = new WidgetStyle(context);
        boolean birthdays = prefs.getBoolean("birthday_column",false);
        RemoteViews views = new RemoteViews(context.getPackageName(),R.layout.widget_scroll);
        views.setImageViewBitmap(R.id.card,style.background(width,height));
        views.setInt(R.id.card,"setImageAlpha",prefs.getInt("opacity",85)*255/100);
        views.setOnClickPendingIntent(R.id.widget_root,CalendarNavigation.pending(context));
        List<CountdownStore.Item> left = new ArrayList<>(), right = new ArrayList<>();
        for (CountdownStore.Item item : data.active) {
            if (item.repeat == 0) left.add(item); else if (!birthdays) right.add(item);
        }
        if (birthdays) right.addAll(data.birthdays);
        List<List<CountdownStore.Item>> columns = Arrays.asList(left,data.anniversaries,right);
        int[] lists = {R.id.entries_one,R.id.entries_two,R.id.entries_three};
        int[] headers = {R.id.header_one,R.id.header_two,R.id.header_three};
        int[] surfaces = {R.id.column_one,R.id.column_two,R.id.column_three};
        int[] emptyViews = {R.id.empty_one,R.id.empty_two,R.id.empty_three};
        String[] titles = {"倒数日","纪念日",birthdays ? "生日" : "周期倒数日"};
        for (int col=0;col<3;col++) {
            views.setTextViewText(headers[col],titles[col]);
            views.setTextColor(headers[col],style.accent);
            views.setOnClickPendingIntent(headers[col],CalendarNavigation.pending(context));
            // The list only occupies its content height (bounded by the column).
            // Remaining space and entirely empty columns use this same calendar action.
            views.setOnClickPendingIntent(surfaces[col],CalendarNavigation.pending(context));
            views.setOnClickPendingIntent(emptyViews[col],CalendarNavigation.pending(context));
            views.setTextColor(emptyViews[col],style.text);
            views.setTextViewTextSize(emptyViews[col],TypedValue.COMPLEX_UNIT_SP,style.nameSize);
            views.setEmptyView(lists[col],emptyViews[col]);
            views.setPendingIntentTemplate(lists[col],CalendarNavigation.collectionPending(context));
            RemoteViews.RemoteCollectionItems.Builder collection = new RemoteViews.RemoteCollectionItems.Builder()
                    .setHasStableIds(true).setViewTypeCount(1);
            List<CountdownStore.Item> items = columns.get(col);
            for (int i=0;i<items.size();i++) collection.addItem(items.get(i).id,row(context,items.get(i),col==1,style,i==items.size()-1));
            views.setRemoteAdapter(lists[col],collection.build());
        }
        views.setViewVisibility(R.id.message,data.error == null ? View.GONE : View.VISIBLE);
        views.setTextViewText(R.id.message,data.error == null ? "" : data.error);
        views.setTextColor(R.id.message,style.text);
        return views;
    }
    static RemoteViews row(Context context,CountdownStore.Item item,boolean anniversary,WidgetStyle style,boolean last) {
        RemoteViews row = new RemoteViews(context.getPackageName(),R.layout.widget_scroll_row);
        row.setTextViewText(R.id.title,item == null ? "暂无" : item.title);
        String value = item == null ? "" : anniversary ? AnniversaryRules.elapsed(item.title,item.days)
                : item.kind == 2 ? item.dateLabel != null ? item.dateLabel : item.date.format(DateTimeFormatter.ofPattern("MM-dd"))
                : item.days == 0 ? "今天" : item.days+"天后";
        row.setTextViewText(R.id.subtitle,value);
        row.setViewVisibility(R.id.subtitle,item == null ? View.GONE : View.VISIBLE);
        row.setTextViewTextSize(R.id.title,TypedValue.COMPLEX_UNIT_SP,style.nameSize);
        row.setTextViewTextSize(R.id.subtitle,TypedValue.COMPLEX_UNIT_SP,style.valueSize);
        row.setTextColor(R.id.title,style.text); row.setTextColor(R.id.subtitle,style.text);
        boolean detail = anniversary && item != null;
        row.setViewVisibility(R.id.origin_date,detail ? View.VISIBLE : View.GONE);
        row.setViewVisibility(R.id.anniversary_detail,detail ? View.VISIBLE : View.GONE);
        if (detail) {
            String origin = "始于 " + item.originDate;
            String next = item.anniversary == null ? "周年日期待同步" : AnniversaryRules.nextLabel(item.anniversary);
            row.setTextViewText(R.id.origin_date,origin);
            row.setTextViewText(R.id.anniversary_detail,next);
            for (int id : new int[]{R.id.origin_date,R.id.anniversary_detail}) {
                row.setTextViewTextSize(id,TypedValue.COMPLEX_UNIT_SP,Math.max(10,style.valueSize-1));
                row.setTextColor(id,style.header);
            }
            row.setContentDescription(R.id.row,item.title+"，"+value+"，"+origin+"，"+next);
        }
        row.setImageViewBitmap(R.id.divider,style.line());
        row.setViewVisibility(R.id.divider,style.showDividers && !last ? View.VISIBLE : View.GONE);
        row.setOnClickFillInIntent(R.id.row,new Intent());
        return row;
    }
}
