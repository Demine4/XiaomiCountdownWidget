package local.countdown.probe;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.CalendarContract;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.FrameLayout;
import android.widget.EditText;
import android.widget.Toast;
import java.text.DateFormat;
import java.util.Date;

public final class HomeActivity extends Activity {
    private LinearLayout root, list;
    private TextView status;
    private TextView pinStatus, backgroundStatus;
    private TextView photoStatus;
    private TextView groupingStatus;
    private CountdownStore.Result lastData;
    private FrameLayout preview;
    private SharedPreferences prefs;
    private final java.util.Map<String, Runnable> choiceCaptions = new java.util.HashMap<>();
    private final java.util.Map<String, Runnable> sliderCaptions = new java.util.HashMap<>();
    private int generation;
    private boolean watching;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable reload = () -> refresh();
    private final ContentObserver observer = new ContentObserver(handler) {
        @Override public void onChange(boolean selfChange) {
            handler.removeCallbacks(reload); handler.postDelayed(reload, 500);
        }
    };
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = getSharedPreferences("settings", 0);
        int configuringWidget = getIntent().getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        if (configuringWidget != AppWidgetManager.INVALID_APPWIDGET_ID) setResult(RESULT_CANCELED);
        ScrollView scroll = new ScrollView(this);
        root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(20), dp(20), dp(20));
        root.setBackgroundColor(Color.rgb(247, 248, 250));
        scroll.setOnApplyWindowInsetsListener((v, insets) -> {
            android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars());
            root.setPadding(dp(20) + bars.left, dp(12) + bars.top, dp(20) + bars.right, dp(20) + bars.bottom);
            return insets;
        });
        scroll.addView(root); setContentView(scroll);
        TextView heading = label("日历倒数日", 26); heading.setTypeface(null, Typeface.BOLD); root.addView(heading);
        root.addView(label("沿用小米日历 · 到期后自动从桌面隐藏", 15));
        preview = new FrameLayout(this);
        root.addView(preview, new LinearLayout.LayoutParams(-1, dp(165)));
        root.addView(label("点击卡片打开小米日历；设置请从应用图标进入。农历生日使用小米日历提供的重复日期列表。", 13));
        status = label("正在读取……", 14); root.addView(status);
        button("授权 / 刷新", () -> {
            if (checkSelfPermission(Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED)
                requestPermissions(new String[]{Manifest.permission.READ_CALENDAR}, 2);
            else refresh();
        });
        button("添加到桌面", () -> requestDesktop());
        pinStatus = label("如添加没有反应，请先打开桌面权限，允许创建桌面快捷方式，再重试。", 12);
        root.addView(pinStatus);
        button("打开桌面权限设置", () -> DeviceSettings.desktopPermission(this));
        button("手动添加说明", () -> new AlertDialog.Builder(this).setTitle("手动添加桌面小部件")
                .setMessage("在桌面空白处长按 → 添加小部件 → 安卓小部件 → 日历倒数日。\n\n如果没有看到入口，请在小部件页面查找“全部”或“安卓小部件”。这会添加日历卡片，而不是普通应用图标。")
                .setPositiveButton("知道了", null).show());
        root.addView(label("后台刷新设置", 18));
        root.addView(label("为让桌面在应用关闭后继续更新，请开启自启动，并将小米省电策略设为“无限制”。这些开关需要你在系统页面确认，应用不能自动代为开启。", 13));
        button("开启自启动", () -> DeviceSettings.autoStart(this));
        button("小米省电策略：设为无限制", () -> DeviceSettings.vendorBattery(this));
        button("申请忽略Android电池优化", () -> DeviceSettings.requestBatteryExemption(this));
        backgroundStatus = label("", 12); root.addView(backgroundStatus);
        Switch today = new Switch(this);
        today.setText("显示今天到期的项目"); today.setTextColor(Color.rgb(34, 39, 49)); today.setChecked(prefs.getBoolean("today", true));
        today.setPadding(0, dp(12), 0, dp(12));
        today.setOnCheckedChangeListener((button, checked) -> { prefs.edit().putBoolean("today", checked).apply(); refresh(); });
        root.addView(today);
        root.addView(label("配色与外观", 18));
        choice("显示模式", "color_mode", new String[]{"跟随系统", "浅色模式", "深色模式"}, 0);
        choice("背景类型", "background_type", new String[]{"纯色", "柔彩光晕（参考图）", "自定义颜色", "自定义背景图"}, 1);
        Button paletteButton = new Button(this);
        Runnable paletteCaption = () -> paletteButton.setText("光晕配色：" + HaloPaletteDialog.caption(prefs) + " · 编辑");
        paletteCaption.run(); choiceCaptions.put("halo_palette", paletteCaption);
        paletteButton.setOnClickListener(v -> HaloPaletteDialog.show(this, prefs, () -> refresh()));
        root.addView(paletteButton);
        slider("光晕色彩强度", "gradient_strength", 0, 100, 85, "%");
        slider("光晕范围（小 / 大）", "halo_size", 20, 240, 70, "%");
        slider("颜色消散速度（柔 / 快）", "halo_fade", 1, 10, 3, "");
        slider("光晕横向位置（最左 / 最右）", "halo_x", 0, 100, 18, "%");
        slider("光晕纵向位置（最上 / 最下）", "halo_y", 0, 100, 31, "%");
        button("重置光晕位置与范围", () -> {
            prefs.edit().putInt("halo_x", 18).putInt("halo_y", 31).putInt("halo_size", 70).putInt("halo_fade", 3).apply(); refresh();
        });
        color("自定义背景颜色", "background_hex", "#FFFFFF", true);
        button("选择 / 更换背景图", () -> choosePhoto());
        photoStatus = label("", 12); root.addView(photoStatus);
        slider("图片缩放", "photo_zoom", 100, 300, 100, "%");
        slider("图片水平位置（左 / 右）", "photo_x", 0, 100, 50, "%");
        slider("图片垂直位置（上 / 下）", "photo_y", 0, 100, 50, "%");
        root.addView(label("照片自动铺满圆角卡片。先调缩放，再移动位置；松开滑块后应用。明暗度和不透明度同样适用于照片。", 12));
        button("重置图片位置", () -> { prefs.edit().putInt("photo_zoom", 100).putInt("photo_x", 50).putInt("photo_y", 50).apply(); refresh(); });
        button("移除背景图", () -> new AlertDialog.Builder(this).setTitle("移除当前背景图？")
                .setMessage("只移除应用保存的背景副本，手机相册里的原图不受影响。")
                .setNegativeButton("取消", null).setPositiveButton("移除", (dialog, which) -> {
                    CountdownWidget.EXECUTOR.execute(() -> { PhotoBackground.remove(getApplicationContext());
                        if (prefs.getInt("background_type", 1) == 3) prefs.edit().putInt("background_type", 1).apply();
                        runOnUiThread(() -> { if (!isDestroyed()) refresh(); }); CountdownWidget.refresh(getApplicationContext());
                    });
                }).show());
        slider("背景明暗度（左黑 / 右白）", "brightness", 0, 100, 50, "");
        root.addView(label("使用光晕背景时，明暗度只改变底色；光晕颜色与强度单独调整。", 12));
        slider("背景不透明度", "opacity", 0, 100, 85, "%");
        choice("文字颜色", "text_color", new String[]{"自动适配背景", "灰色", "黑色", "白色", "自定义颜色"}, 0);
        color("自定义文字颜色", "text_hex", "#4A4B50", false);
        toggle("高亮列标题", "highlight", false);
        toggle("显示虚线分隔", "dividers", true);
        toggle("网格背景装饰", "grid", false);
        Switch birthdayColumn = new Switch(this);
        birthdayColumn.setText("第三列显示生日（关闭则显示周期倒数日）"); birthdayColumn.setTextColor(Color.rgb(34, 39, 49));
        birthdayColumn.setChecked(prefs.getBoolean("birthday_column", false));
        birthdayColumn.setOnCheckedChangeListener((button, checked) -> { prefs.edit().putBoolean("birthday_column", checked).apply(); refresh(); });
        root.addView(birthdayColumn);
        if (android.os.Build.VERSION.SDK_INT < 31) toggle("纪念日通栏显示", "wide_anniversaries", true);
        button("周期倒数日归入纪念日…", () -> chooseAnniversaries());
        groupingStatus = label("尚未选择归类项目", 12); root.addView(groupingStatus);
        root.addView(label("三列分别上下滑动，标题固定。纪念日分行显示名称、累计天数、起始日期和下次周年，长内容自动换行。请在日历中填写真实起始年份。", 12));
        slider("文字大小", "table_font", 10, 16, 11, "sp");
        slider("天数 / 日期字号", "value_font", 10, 16, prefs.getInt("table_font", 11), "sp");
        if (android.os.Build.VERSION.SDK_INT < 31) slider("每列最多行数", "table_rows", 1, 15, 5, "行");
        if (configuringWidget != AppWidgetManager.INVALID_APPWIDGET_ID) button("完成编辑，返回桌面", () -> {
            CountdownWidget.refresh(this);
            setResult(RESULT_OK, new Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, configuringWidget)); finish();
        });
        root.addView(label("设置应用于所有桌面实例。滚动模式展示全部项目；手指在对应列内上下滑动查看，也可长按组件调整大小。", 12));
        root.addView(label("日历修改后尝试自动刷新；系统省电可能延迟。如需手动刷新，请使用应用内的“授权 / 刷新”。", 12));
        button("打开小米日历", () -> {
            Intent launch = getPackageManager().getLaunchIntentForPackage("com.android.calendar");
            if (launch != null) startActivity(launch);
            else Toast.makeText(this, "未找到日历入口，请从桌面打开", Toast.LENGTH_SHORT).show();
        });
        button("读取检测与字段报告", () -> startActivity(new Intent(this, MainActivity.class)));
        list = new LinearLayout(this); list.setOrientation(LinearLayout.VERTICAL); root.addView(list);
        root.addView(label("0.17.0 · 只读日历 · 离线", 12));
    }
    private void chooseAnniversaries() {
        if (lastData == null || lastData.error != null) {
            Toast.makeText(this, "请先允许读取日历并刷新成功", Toast.LENGTH_LONG).show(); return;
        }
        java.util.List<CountdownStore.Item> candidates = new java.util.ArrayList<>(lastData.periodic);
        if (candidates.isEmpty()) {
            Toast.makeText(this, "暂无可归类的每月或每年重复倒数日", Toast.LENGTH_LONG).show(); return;
        }
        java.util.Set<String> ids = new java.util.HashSet<>(prefs.getStringSet("anniversary_ids", java.util.Collections.emptySet()));
        CharSequence[] labels = new CharSequence[candidates.size()];
        boolean[] checked = new boolean[candidates.size()];
        for (int i = 0; i < candidates.size(); i++) {
            CountdownStore.Item item = candidates.get(i);
            labels[i] = item.title + "（" + (item.repeat == 2 ? "每年" : "每月") + "）\n原始日期：" + item.originDate;
            checked[i] = ids.contains(Long.toString(item.id));
        }
        new AlertDialog.Builder(this).setTitle("选择归入纪念日的项目")
                .setMultiChoiceItems(labels, checked, (dialog, which, value) -> checked[which] = value)
                .setNegativeButton("取消", null).setPositiveButton("应用", (dialog, which) -> {
                    java.util.Set<String> selection = new java.util.HashSet<>();
                    for (int i = 0; i < candidates.size(); i++) if (checked[i]) selection.add(Long.toString(candidates.get(i).id));
                    prefs.edit().putStringSet("anniversary_ids", selection).apply(); refresh();
                }).show();
    }
    private void choosePhoto() {
        Intent picker = new Intent(Intent.ACTION_GET_CONTENT).setType("image/*").addCategory(Intent.CATEGORY_OPENABLE);
        try { startActivityForResult(picker, 60); }
        catch (RuntimeException unavailable) { Toast.makeText(this, "未找到图片选择器，请检查系统相册或文件应用", Toast.LENGTH_LONG).show(); }
    }
    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != 60 || resultCode != RESULT_OK || data == null || data.getData() == null) return;
        android.net.Uri image = data.getData();
        photoStatus.setText("正在导入背景图……");
        CountdownWidget.EXECUTOR.execute(() -> {
            String message;
            try { PhotoBackground.importPhoto(getApplicationContext(), image); message = "背景图已保存到应用，可使用缩放和位置滑块调整"; }
            catch (Exception failure) { message = "图片无法导入，请尝试常见的JPEG或PNG图片，原背景保持不变"; }
            CountdownWidget.refresh(getApplicationContext());
            final String notice = message;
            runOnUiThread(() -> { if (!isDestroyed()) { Toast.makeText(this, notice, Toast.LENGTH_LONG).show(); refresh(); } });
        });
    }
    private void choice(String title, String key, String[] options, int fallback) {
        Button button = new Button(this);
        int selected = Math.max(0, Math.min(options.length - 1, prefs.getInt(key, fallback)));
        button.setText(title + "：" + options[selected]);
        choiceCaptions.put(key, () -> button.setText(title + "：" + options[Math.max(0, Math.min(options.length - 1, prefs.getInt(key, fallback)))]));
        button.setOnClickListener(v -> new AlertDialog.Builder(this).setTitle(title)
                .setSingleChoiceItems(options, prefs.getInt(key, fallback), (dialog, which) -> {
                    prefs.edit().putInt(key, which).apply(); button.setText(title + "：" + options[which]); dialog.dismiss(); refresh();
                }).setNegativeButton("取消", null).show());
        root.addView(button);
    }
    private void toggle(String title, String key, boolean fallback) {
        Switch control = new Switch(this); control.setText(title); control.setTextColor(Color.rgb(34, 39, 49));
        control.setChecked(prefs.getBoolean(key, fallback)); control.setPadding(0, dp(8), 0, dp(8));
        control.setOnCheckedChangeListener((v, value) -> { prefs.edit().putBoolean(key, value).apply(); refresh(); }); root.addView(control);
    }
    private void color(String title, String key, String fallback, boolean background) {
        button(title, () -> {
            EditText input = new EditText(this); input.setSingleLine(true); input.setText(prefs.getString(key, fallback)); input.setHint("#RRGGBB，例如 #D9E7F4");
            AlertDialog dialog = new AlertDialog.Builder(this).setTitle(title).setView(input).setNegativeButton("取消", null).setPositiveButton("应用", null).create();
            dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String value = input.getText().toString().trim();
                if (!value.matches("#[0-9a-fA-F]{6}")) { input.setError("请输入 # 加六位颜色值，例如 #D9E7F4"); return; }
                prefs.edit().putString(key, value).putInt(background ? "background_type" : "text_color", background ? 2 : 4).apply();
                dialog.dismiss(); refresh();
            })); dialog.show();
        });
    }
    private void requestDesktop() {
        AppWidgetManager manager = AppWidgetManager.getInstance(this);
        if (!manager.isRequestPinAppWidgetSupported()) {
            pinStatus.setText("当前桌面不支持应用内添加，请使用“手动添加说明”。"); return;
        }
        long requestTime = System.currentTimeMillis();
        prefs.edit().putLong("pin_requested", requestTime).apply();
        PendingIntent callback = PendingIntent.getBroadcast(this, 0, new Intent(this, WidgetPinnedReceiver.class),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        try {
            boolean sent = manager.requestPinAppWidget(new ComponentName(this, CountdownWidget.class), null, callback);
            pinStatus.setText(sent ? "已向桌面发送请求，请确认添加。没有弹出时，请允许创建桌面快捷方式后重试，或手动添加。"
                    : "桌面没有接受添加请求，请先允许创建桌面快捷方式后重试，或手动添加。");
        } catch (RuntimeException unavailable) {
            pinStatus.setText("无法打开桌面添加确认，请检查桌面权限，或使用手动添加。");
        }
    }
    private void showBackgroundStatus() {
        PowerManager power = (PowerManager)getSystemService(POWER_SERVICE);
        long last = prefs.getLong("last_widget_refresh", 0);
        backgroundStatus.setText("Android电池优化：" + (power.isIgnoringBatteryOptimizations(getPackageName()) ? "已豁免" : "未豁免")
                + "\n自启动及小米省电策略：请在系统设置核对，应用无法可靠读取状态。"
                + "\n上次桌面刷新：" + (last == 0 ? "尚无记录（请先添加小部件）" : DateFormat.getDateTimeInstance().format(new Date(last))
                + (prefs.getBoolean("last_widget_read_ok", false) ? " · 日历读取成功" : " · 日历读取未成功"))
                + "\n周期刷新由系统调度，省电模式可能延迟；如使用“强行停止”，需重新打开应用恢复。");
        long requested = prefs.getLong("pin_requested", 0);
        if (requested > 0 && prefs.getLong("pin_confirmed", 0) >= requested) pinStatus.setText("桌面已确认添加小部件。");
    }
    private void slider(String title, String key, int min, int max, int fallback, String unit) {
        TextView caption = label(title + "：" + prefs.getInt(key, fallback) + unit, 14); root.addView(caption);
        SeekBar bar = new SeekBar(this); bar.setMax(max - min); bar.setProgress(prefs.getInt(key, fallback) - min);
        sliderCaptions.put(key, () -> bar.setProgress(prefs.getInt(key, fallback) - min));
        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onStartTrackingTouch(SeekBar v) {}
            public void onProgressChanged(SeekBar v, int value, boolean user) { caption.setText(title + "：" + (value + min) + unit); }
            public void onStopTrackingTouch(SeekBar v) { prefs.edit().putInt(key, v.getProgress() + min).apply(); refresh(); }
        });
        root.addView(bar);
    }
    private void refresh() {
        final int ticket = ++generation;
        status.setText("正在刷新小米日历……");
        CountdownWidget.EXECUTOR.execute(() -> {
            CountdownStore.Result result = CountdownStore.read(this);
            CountdownWidget.updateNow(this); CalendarWatch.schedule(this);
            runOnUiThread(() -> {
                if (isDestroyed() || ticket != generation) return;
                status.setText(result.error == null ? result.note() : result.error);
                lastData = result;
                groupingStatus.setText("已归入纪念日：" + result.regrouped + " 条周期倒数日。取消勾选可恢复原栏，原日历记录不会修改。");
                showBackgroundStatus();
                photoStatus.setText(PhotoBackground.exists(this) ? "已保存背景图；选择“自定义背景图”即可使用。删除原图或重启不会影响已保存的副本。" : "尚未选择背景图。请点“选择 / 更换背景图”。");
                for (Runnable updateCaption : choiceCaptions.values()) updateCaption.run();
                for (Runnable updateCaption : sliderCaptions.values()) updateCaption.run();
                preview.removeAllViews();
                int previewHeight = android.os.Build.VERSION.SDK_INT >= 31 ? 230 : prefs.getBoolean("wide_anniversaries",true) && !result.anniversaries.isEmpty() ? 270 : 165;
                preview.getLayoutParams().height = dp(previewHeight); preview.requestLayout();
                int previewWidth = preview.getWidth()>0 ? Math.round(preview.getWidth()/getResources().getDisplayMetrics().density) : 360;
                preview.addView(WidgetRenderer.render(this, result, previewHeight, previewWidth).apply(this, preview), new FrameLayout.LayoutParams(-1, -1));
                list.removeAllViews();
                list.addView(label("桌面将显示", 18));
                if (result.active.isEmpty()) list.addView(label(result.error != null ? "读取成功后在这里显示预览。" : "暂无待到期倒数日。", 14));
                int shown = 0;
                for (CountdownStore.Item item : result.active) {
                    if (shown++ == 80) { list.addView(label("仅预览前80条", 12)); break; }
                    String repeat = item.repeat == 1 ? "每月" : item.repeat == 2 ? "每年" : "不重复";
                    list.addView(label(item.title + "\n" + item.date + " · " + (item.days == 0 ? "今天" : item.days + "天后") + " · " + repeat, 15));
                }
                if (!result.past.isEmpty()) {
                    list.addView(label("已过期 · 仅在此查看", 18));
                    int pastShown = 0;
                    for (CountdownStore.Item item : result.past) {
                        if (pastShown++ == 80) break;
                        list.addView(label(item.title + "\n" + item.date + " · 已从桌面隐藏", 14));
                    }
                }
                if (!result.anniversaries.isEmpty()) {
                    list.addView(label("纪念日", 18));
                    for (CountdownStore.Item item : result.anniversaries) list.addView(label(item.title + " · " + AnniversaryRules.elapsed(item.title, item.days)
                            + "\n原始日期：" + item.originDate + " · " + AnniversaryRules.nextLabel(item.anniversary), 14));
                }
                if (!result.birthdays.isEmpty()) {
                    list.addView(label("生日", 18));
                    for (CountdownStore.Item item : result.birthdays) list.addView(label(item.title + " · " + (item.dateLabel == null ? item.date : item.dateLabel + "（" + item.date + "）") + " · " + item.days + "天后", 14));
                }
            });
        });
    }
    @Override protected void onResume() {
        super.onResume();
        if (checkSelfPermission(Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED && !watching) {
            getContentResolver().registerContentObserver(CalendarContract.Events.CONTENT_URI, true, observer);
            getContentResolver().registerContentObserver(CalendarContract.ExtendedProperties.CONTENT_URI, true, observer);
            watching = true;
        }
        refresh();
    }
    @Override protected void onPause() {
        if (watching) { getContentResolver().unregisterContentObserver(observer); watching = false; }
        handler.removeCallbacks(reload); super.onPause();
    }
    @Override public void onRequestPermissionsResult(int code, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(code, permissions, results);
        if (code == 2) refresh();
    }
    private void button(String title, Runnable action) { Button b = new Button(this); b.setText(title); b.setOnClickListener(v -> action.run()); root.addView(b); }
    private TextView label(String value, int size) {
        TextView v = new TextView(this); v.setText(value); v.setTextSize(size); v.setTextColor(Color.rgb(34, 39, 49));
        v.setPadding(0, dp(8), 0, dp(8)); return v;
    }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
}
