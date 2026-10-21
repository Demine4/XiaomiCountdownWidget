package local.countdown.probe;

import android.content.Context;
import android.content.SharedPreferences;
import android.app.PendingIntent;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Build;
import android.util.TypedValue;
import android.view.View;
import android.widget.RemoteViews;
import java.util.ArrayList;
import java.util.List;
import java.time.format.DateTimeFormatter;

final class SectionRenderer {
    private static String detail(CountdownStore.Item item) {
        return "始于 " + item.originDate + " · " + AnniversaryRules.nextLabel(item.anniversary);
    }
    static RemoteViews render(Context context, CountdownStore.Result data, int height, int width) {
        SharedPreferences prefs = context.getSharedPreferences("settings", 0);
        WidgetStyle style = new WidgetStyle(context);
        boolean birthdays = prefs.getBoolean("birthday_column", false);
        List<CountdownStore.Item> left = new ArrayList<>(), right = new ArrayList<>();
        for (CountdownStore.Item item : data.active) {
            if (item.repeat == 0) left.add(item); else if (!birthdays) right.add(item);
        }
        if (birthdays) right.addAll(data.birthdays);
        float density = context.getResources().getDisplayMetrics().density;
        float scaled = context.getResources().getDisplayMetrics().scaledDensity;
        float fontScale = context.getResources().getConfiguration().fontScale;
        int rowHeight = Math.max(23, (int)Math.ceil(Math.max(style.nameSize, style.valueSize)*fontScale*1.35f)+7);
        int detailFont = Math.max(10, style.valueSize - 1);
        int[] titleHeights = new int[data.anniversaries.size()], detailHeights = new int[titleHeights.length], fullHeights = new int[titleHeights.length];
        Paint measure = new Paint();
        for (int i=0; i<fullHeights.length; i++) {
            CountdownStore.Item item = data.anniversaries.get(i);
            measure.setTypeface(Typeface.DEFAULT); measure.setTextSize(style.valueSize * scaled);
            float titleWidth = Math.max(30*density, (width-38)*density - measure.measureText(AnniversaryRules.elapsed(item.title,item.days)));
            measure.setTypeface(Typeface.DEFAULT_BOLD); measure.setTextSize(style.nameSize*scaled);
            int titleLines = measure.measureText(item.title) > titleWidth ? 2 : 1;
            titleHeights[i] = Math.max(rowHeight, (int)Math.ceil(style.nameSize*fontScale*1.35f)*titleLines+6);
            measure.setTypeface(Typeface.DEFAULT); measure.setTextSize(detailFont*scaled);
            int detailLines = measure.measureText(detail(item)) > (width-32)*density ? 2 : 1;
            detailHeights[i] = ((int)Math.ceil(detailFont*fontScale*1.35f)+4)*detailLines;
            fullHeights[i] = titleHeights[i]+detailHeights[i]+1;
        }
        SectionLayout plan = SectionLayout.plan(height,rowHeight,Math.max(left.size(),right.size()),prefs.getInt("table_rows",5),fullHeights);
        RemoteViews views = new RemoteViews(context.getPackageName(),R.layout.widget_sections);
        views.setImageViewBitmap(R.id.card,style.background(width,height));
        views.setInt(R.id.card,"setImageAlpha",prefs.getInt("opacity",85)*255/100);
        PendingIntent calendar = CalendarNavigation.pending(context);
        views.setOnClickPendingIntent(R.id.widget_root,calendar);
        views.setViewVisibility(R.id.refresh,View.GONE);
        for (int id : new int[]{R.id.entries_one,R.id.entries_two,R.id.entries_three}) views.removeAllViews(id);
        for (int id : new int[]{R.id.header_one,R.id.header_two,R.id.header_three}) { views.setTextColor(id,style.accent); views.setOnClickPendingIntent(id,calendar); }
        views.setTextColor(R.id.refresh,style.accent);
        views.setViewVisibility(R.id.sections_top,plan.topHidden ? View.GONE : View.VISIBLE);
        views.setViewVisibility(R.id.sections_gap,plan.topHidden ? View.GONE : View.VISIBLE);
        views.setTextViewText(R.id.header_one,heading("倒数日",left.size(),plan.topRows));
        views.setTextViewText(R.id.header_three,heading(birthdays ? "生日" : "周期倒数日",right.size(),plan.topRows));
        views.setTextViewText(R.id.header_two,heading("纪念日",data.anniversaries.size(),plan.anniversaryRows)
                + (plan.topHidden && (!left.isEmpty() || !right.isEmpty()) ? " · 拉高查看其他项目" : ""));
        android.graphics.Bitmap divider = style.line();
        List<List<CountdownStore.Item>> lists = java.util.Arrays.asList(left,right);
        int[] containers = {R.id.entries_one,R.id.entries_three};
        for(int c=0;c<2;c++) for(int index=0; index<plan.topRows; index++) {
            CountdownStore.Item item = index<lists.get(c).size() ? lists.get(c).get(index) : null;
            RemoteViews row = new RemoteViews(context.getPackageName(),R.layout.widget_row);
            if(Build.VERSION.SDK_INT>=31) row.setViewLayoutHeight(R.id.row_content,rowHeight,TypedValue.COMPLEX_UNIT_DIP);
            row.setTextViewText(R.id.title,item==null ? index==0 ? "无" : "" : item.title);
            row.setTextViewText(R.id.subtitle,item==null ? "" : item.kind==2 ? item.dateLabel!=null ? item.dateLabel : item.date.format(DateTimeFormatter.ofPattern("MM-dd")) : item.days==0 ? "今天" : item.days+"天后");
            row.setTextViewTextSize(R.id.title,TypedValue.COMPLEX_UNIT_SP,style.nameSize);
            row.setTextViewTextSize(R.id.subtitle,TypedValue.COMPLEX_UNIT_SP,style.valueSize);
            row.setTextColor(R.id.title,style.text); row.setTextColor(R.id.subtitle,style.text); row.setTextColor(R.id.bullet,style.header);
            row.setViewVisibility(R.id.bullet,item==null && index>0 ? View.INVISIBLE : View.VISIBLE);
            row.setImageViewBitmap(R.id.divider,divider); row.setViewVisibility(R.id.divider,!style.showDividers || index==plan.topRows-1 ? View.INVISIBLE : View.VISIBLE);
            row.setOnClickPendingIntent(R.id.row,calendar); views.addView(containers[c],row);
        }
        for(int i=0;i<plan.anniversaryRows;i++) {
            CountdownStore.Item item=data.anniversaries.get(i);
            RemoteViews row=new RemoteViews(context.getPackageName(),R.layout.widget_anniversary_wide_row);
            if(Build.VERSION.SDK_INT>=31) {
                row.setViewLayoutHeight(R.id.row_content,titleHeights[i],TypedValue.COMPLEX_UNIT_DIP);
                row.setViewLayoutHeight(R.id.anniversary_detail,detailHeights[i],TypedValue.COMPLEX_UNIT_DIP);
            }
            row.setTextViewText(R.id.title,item.title); row.setTextViewText(R.id.subtitle,AnniversaryRules.elapsed(item.title,item.days));
            row.setTextViewText(R.id.anniversary_detail,detail(item));
            row.setTextViewTextSize(R.id.title,TypedValue.COMPLEX_UNIT_SP,style.nameSize);
            row.setTextViewTextSize(R.id.subtitle,TypedValue.COMPLEX_UNIT_SP,style.valueSize);
            row.setTextViewTextSize(R.id.anniversary_detail,TypedValue.COMPLEX_UNIT_SP,detailFont);
            row.setTextColor(R.id.title,style.text); row.setTextColor(R.id.subtitle,style.text); row.setTextColor(R.id.anniversary_detail,style.header);
            row.setImageViewBitmap(R.id.divider,divider); row.setViewVisibility(R.id.divider,!style.showDividers || i==plan.anniversaryRows-1 ? View.INVISIBLE : View.VISIBLE);
            row.setOnClickPendingIntent(R.id.row,calendar); views.addView(R.id.entries_two,row);
        }
        return views;
    }
    private static String heading(String title,int total,int shown) { return total>shown ? title+"（还有"+(total-shown)+"条）" : title; }
}
