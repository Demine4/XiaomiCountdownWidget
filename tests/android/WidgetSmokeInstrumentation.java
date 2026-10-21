package local.countdown.probe;

import android.app.Instrumentation;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDate;

/** Real Android RemoteViews inflation and rendering; uses synthetic data only. */
public final class WidgetSmokeInstrumentation extends Instrumentation {
    private long nextId = 1;
    @Override public void onCreate(Bundle arguments) { super.onCreate(arguments); start(); }
    @Override public void onStart() {
        Bundle result = new Bundle();
        try {
            runOnMainSync(() -> {
                try { verify(); }
                catch (Exception e) { throw new RuntimeException(e); }
            });
            result.putString("stream", "PASS: cycle/birthday/error RemoteViews rendered; preview PNGs saved.\n");
            finish(-1, result);
        } catch (Throwable e) {
            result.putString("stream", "FAIL: " + e + "\n"); finish(0, result);
        }
    }
    private CountdownStore.Item item(String title, long days, int kind, int repeat) {
        CountdownStore.Item i = new CountdownStore.Item(); i.id = nextId++; i.title = title; i.days = days; i.kind = kind; i.repeat = repeat;
        i.date = LocalDate.of(2026, 10, 19).plusDays(days);
        if (kind == 1) {
            i.originDate = LocalDate.of(2026, 10, 19).minusDays(days);
            i.date = i.originDate;
            i.anniversary = AnniversaryRules.calculate(i.originDate, LocalDate.of(2026, 10, 19), null, false);
        }
        return i;
    }
    private void verify() throws Exception {
        Context context = getTargetContext();
        context.getSharedPreferences("settings", 0).edit().putInt("table_font", 11).putInt("table_rows", 5).putInt("opacity", 95).putBoolean("birthday_column", false).commit();
        CountdownStore.Result data = new CountdownStore.Result();
        data.active.add(item("科目二", 0, 0, 0)); data.active.add(item("预约补考", 2, 0, 0));
        data.active.add(item("值班", 10, 0, 0)); data.active.add(item("聚餐", 20, 0, 0));
        data.active.add(item("体检", 1, 0, 2)); data.active.add(item("还信用卡", 5, 0, 1));
        data.active.add(item("还花呗", 10, 0, 1)); data.active.add(item("发工资", 15, 0, 1));
        data.anniversaries.add(item("来到人间", 9020, 1, 0)); data.anniversaries.add(item("认识王老板", 999, 1, 0));
        data.anniversaries.add(item("捡到大咪", 801, 1, 0)); data.anniversaries.add(item("入职已经", 365, 1, 0));
        data.anniversaries.add(item("练瑜伽", 80, 1, 0));
        data.birthdays.add(item("老爸", 1, 2, 2)); data.birthdays.add(item("老妈", 5, 2, 2));
        data.birthdays.add(item("王老板", 10, 2, 2)); data.birthdays.add(item("李老师", 15, 2, 2));
        CountdownStore.Item lunarBirthday = item("农历测试", 3, 2, 2);
        lunarBirthday.dateLabel = LunarLabels.birthday(1707530400000L);
        if (!"正月初一".equals(lunarBirthday.dateLabel)) throw new AssertionError("Lunar new year label");
        data.birthdays.add(lunarBirthday);
        View cycle = render(context, data);
        if (!((TextView)cycle.findViewById(R.id.header_three)).getText().toString().equals("周期倒数日")) throw new AssertionError("Cycle header");
        save(context, cycle, "preview-cycle.png");
        context.getSharedPreferences("settings", 0).edit().putBoolean("birthday_column", true).commit();
        View birthday = render(context, data);
        if (!((TextView)birthday.findViewById(R.id.header_three)).getText().toString().equals("生日")) throw new AssertionError("Birthday header");
        save(context, birthday, "preview-birthday.png");
        context.getSharedPreferences("settings", 0).edit().putInt("color_mode", 2).putInt("background_type", 0).commit();
        View dark = render(context, data);
        save(context, dark, "preview-dark.png");
        if (((TextView)dark.findViewById(R.id.title)).getCurrentTextColor() == 0xFF4A4B50) throw new AssertionError("Dark text color");
        context.getSharedPreferences("settings", 0).edit().putInt("background_type", 2).putString("background_hex", "#D9E7F4").putInt("text_color", 2).commit();
        View custom = render(context, data);
        if (((TextView)custom.findViewById(R.id.title)).getCurrentTextColor() != android.graphics.Color.BLACK) throw new AssertionError("Custom text color");
        save(context, custom, "preview-custom.png");
        if (android.os.Build.VERSION.SDK_INT >= 31) {
            for (int i=0;i<20;i++) data.active.add(item("用于滚动验证的完整长标题"+i,30+i,0,0));
            View scrolling = render(context,data);
            android.widget.ListView left = scrolling.findViewById(R.id.entries_one);
            android.widget.ListView middle = scrolling.findViewById(R.id.entries_two);
            if (left.getAdapter().getCount() != 24) throw new AssertionError("Scrolling list truncated data");
            if (middle.getAdapter().getCount() != data.anniversaries.size()) throw new AssertionError("Anniversary collection count");
            if (!left.canScrollVertically(1)) throw new AssertionError("List is not scrollable");
            View detail = middle.getAdapter().getView(0,null,middle);
            if (detail.findViewById(R.id.origin_date).getVisibility() != View.VISIBLE) throw new AssertionError("Original date hidden");
            if (((TextView)detail.findViewById(R.id.title)).getEllipsize() != null) throw new AssertionError("Title truncated");
            if (((TextView)detail.findViewById(R.id.anniversary_detail)).getMaxLines() < 3) throw new AssertionError("Anniversary details cannot wrap");
            save(context,scrolling,"preview-scrolling.png");
            CountdownStore.Result sparse = new CountdownStore.Result();
            sparse.active.add(item("短记录",1,0,0));
            View shortList = render(context,sparse);
            android.widget.ListView shortColumn = shortList.findViewById(R.id.entries_one);
            View surface = shortList.findViewById(R.id.column_one);
            if (shortColumn.getHeight() >= surface.getHeight()) throw new AssertionError("Short list still consumes blank space");
            if (!surface.hasOnClickListeners()) throw new AssertionError("Blank space has no calendar action");
            for (int id : new int[]{R.id.empty_two,R.id.empty_three}) {
                View empty = shortList.findViewById(id);
                if (empty.getVisibility() != View.VISIBLE || !empty.hasOnClickListeners()) throw new AssertionError("Empty column has no calendar action");
            }
            if (shortList.findViewById(R.id.refresh) != null) throw new AssertionError("Ambiguous refresh arrow remains");
        }
        data.error = "请打开应用，允许读取日历";
        if (render(context, data).findViewById(R.id.message).getVisibility() != View.VISIBLE) throw new AssertionError("Permission message");
        context.getSharedPreferences("settings", 0).edit().putBoolean("birthday_column", false).commit();
    }
    private View render(Context context, CountdownStore.Result result) {
        FrameLayout parent = new FrameLayout(context);
        View view = WidgetRenderer.render(context, result, 165).apply(context, parent);
        int w = Math.round(360 * context.getResources().getDisplayMetrics().density);
        int h = Math.round(165 * context.getResources().getDisplayMetrics().density);
        view.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY));
        view.layout(0, 0, w, h); return view;
    }
    private void save(Context context, View view, String filename) throws Exception {
        Bitmap bitmap = Bitmap.createBitmap(view.getWidth() * 2, view.getHeight() * 2, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap); canvas.scale(2, 2); view.draw(canvas);
        File output = new File(context.getExternalFilesDir(null), filename);
        try (FileOutputStream stream = new FileOutputStream(output)) { bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream); }
        bitmap.recycle();
    }
}
