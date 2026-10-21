package local.countdown.probe;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.*;
import java.util.Arrays;
import java.util.Locale;

/** Edits a draft; only Apply changes the saved palette and desktop widgets. */
final class HaloPaletteDialog {
    private static final int[] DEFAULTS = {0xFFF0D4DF, 0xFFC7E3F5, 0xFFCCB8F4, 0xFFF4D7B5};
    static int[] read(SharedPreferences prefs) {
        int count = prefs.getInt("halo_color_count", 0);
        if (count < 1 || count > 4) return null;
        int[] colors = new int[count];
        for (int i = 0; i < count; i++) colors[i] = prefs.getInt("halo_color_" + i, DEFAULTS[i]) | 0xFF000000;
        return colors;
    }
    static String caption(SharedPreferences prefs) {
        int[] colors = read(prefs);
        return colors == null ? "默认粉紫蓝" : "自定义 " + colors.length + " 色";
    }
    static void show(Activity activity, SharedPreferences prefs, Runnable applied) {
        int[] saved = read(prefs);
        int[] colors = DEFAULTS.clone();
        if (saved != null) System.arraycopy(saved, 0, colors, 0, saved.length);
        int[] count = {saved == null ? 3 : saved.length};
        LinearLayout panel = column(activity);
        TextView hint = new TextView(activity);
        hint.setText("选择 1～4 个颜色，点击色块调色。颜色会在局部柔和交叠；范围、位置和消散速度沿用当前设置。");
        panel.addView(hint);
        ImageView preview = new ImageView(activity);
        preview.setScaleType(ImageView.ScaleType.FIT_XY);
        panel.addView(preview, new LinearLayout.LayoutParams(-1, dp(activity, 110)));
        LinearLayout controls = new LinearLayout(activity);
        Button less = new Button(activity), more = new Button(activity);
        TextView number = new TextView(activity); number.setGravity(android.view.Gravity.CENTER);
        less.setText("− 减少"); more.setText("＋ 增加");
        controls.addView(less, new LinearLayout.LayoutParams(0, -2, 1));
        controls.addView(number, new LinearLayout.LayoutParams(0, -1, 1));
        controls.addView(more, new LinearLayout.LayoutParams(0, -2, 1));
        panel.addView(controls);
        Button[] swatches = new Button[4];
        for (int i = 0; i < 4; i++) { swatches[i] = new Button(activity); panel.addView(swatches[i]); }
        Bitmap[] bitmap = {null};
        Runnable redraw = () -> {
            number.setText(count[0] + " 个颜色"); less.setEnabled(count[0] > 1); more.setEnabled(count[0] < 4);
            for (int i = 0; i < 4; i++) {
                swatches[i].setVisibility(i < count[0] ? android.view.View.VISIBLE : android.view.View.GONE);
                swatches[i].setText("颜色 " + (i+1) + "    " + hex(colors[i]) + "    点击调整");
                swatches[i].setBackgroundTintList(android.content.res.ColorStateList.valueOf(colors[i]));
                swatches[i].setTextColor(contrast(colors[i]));
            }
            int mode = prefs.getInt("color_mode", 0);
            boolean dark = mode == 2 || (mode == 0 && (activity.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES);
            Bitmap next = Bitmap.createBitmap(HaloBackground.render(600, 220, dark,
                    prefs.getInt("gradient_strength", 85)/100f, (prefs.getInt("brightness",50)-50)/50f,
                    prefs.getInt("halo_size",70), prefs.getInt("halo_fade",3),
                    prefs.getInt("halo_x",18), prefs.getInt("halo_y",31), Arrays.copyOf(colors,count[0])),600,220,Bitmap.Config.ARGB_8888);
            preview.setImageBitmap(next);
            if (bitmap[0] != null) bitmap[0].recycle();
            bitmap[0] = next;
        };
        less.setOnClickListener(v -> { if (count[0] > 1) count[0]--; redraw.run(); });
        more.setOnClickListener(v -> { if (count[0] < 4) count[0]++; redraw.run(); });
        for (int i = 0; i < 4; i++) {
            final int index = i;
            swatches[i].setOnClickListener(v -> editColor(activity, colors[index], color -> { colors[index] = color; redraw.run(); }));
        }
        ScrollView scroll = new ScrollView(activity); scroll.addView(panel);
        AlertDialog dialog = new AlertDialog.Builder(activity).setTitle("自定义光晕配色").setView(scroll)
                .setNegativeButton("取消", null)
                .setNeutralButton("恢复默认", (d,w) -> {
                    prefs.edit().putInt("halo_color_count",0).putInt("background_type",1).apply(); applied.run();
                })
                .setPositiveButton("应用", (d,w) -> {
                    SharedPreferences.Editor editor = prefs.edit().putInt("halo_color_count", count[0]).putInt("background_type",1);
                    for (int i = 0; i < 4; i++) editor.putInt("halo_color_"+i,colors[i]);
                    editor.apply(); applied.run();
                }).create();
        dialog.setOnDismissListener(d -> { preview.setImageDrawable(null); if (bitmap[0] != null) bitmap[0].recycle(); });
        redraw.run(); dialog.show();
    }
    private interface ColorResult { void accept(int color); }
    private static void editColor(Activity activity, int original, ColorResult result) {
        LinearLayout panel = column(activity);
        TextView sample = new TextView(activity); sample.setText("颜色预览"); sample.setGravity(17);
        panel.addView(sample, new LinearLayout.LayoutParams(-1,dp(activity,56)));
        EditText hex = new EditText(activity); hex.setSingleLine(true); hex.setHint("#RRGGBB");
        panel.addView(hex);
        SeekBar[] bars = new SeekBar[3]; TextView[] labels = new TextView[3];
        String[] names = {"红", "绿", "蓝"}; int[] selected = {original}; boolean[] syncing = {false};
        Runnable sync = () -> {
            syncing[0] = true;
            sample.setBackgroundColor(selected[0]); sample.setTextColor(contrast(selected[0]));
            hex.setText(hex(selected[0]));
            for (int i = 0; i < 3; i++) {
                int value = (selected[0] >> (16-i*8)) & 255;
                bars[i].setProgress(value); labels[i].setText(names[i]+"："+value);
            }
            syncing[0] = false;
        };
        for (int i = 0; i < 3; i++) {
            labels[i] = new TextView(activity); panel.addView(labels[i]);
            bars[i] = new SeekBar(activity); bars[i].setMax(255); panel.addView(bars[i]);
            bars[i].setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                public void onStartTrackingTouch(SeekBar bar) {}
                public void onStopTrackingTouch(SeekBar bar) {}
                public void onProgressChanged(SeekBar bar, int value, boolean user) {
                    if (!user || syncing[0]) return;
                    selected[0] = Color.rgb(bars[0].getProgress(), bars[1].getProgress(), bars[2].getProgress()); sync.run();
                }
            });
        }
        sync.run();
        hex.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s,int start,int count,int after) {}
            public void onTextChanged(CharSequence s,int start,int before,int count) {
                if (syncing[0] || !s.toString().matches("#[0-9a-fA-F]{6}")) return;
                selected[0] = Color.parseColor(s.toString());
                syncing[0] = true;
                sample.setBackgroundColor(selected[0]); sample.setTextColor(contrast(selected[0]));
                for (int i = 0; i < 3; i++) { int value = (selected[0] >> (16-i*8)) & 255; bars[i].setProgress(value); labels[i].setText(names[i]+"："+value); }
                syncing[0] = false;
            }
            public void afterTextChanged(Editable s) {}
        });
        ScrollView scroll = new ScrollView(activity); scroll.addView(panel);
        AlertDialog dialog = new AlertDialog.Builder(activity).setTitle("调整颜色").setView(scroll)
                .setNegativeButton("取消",null).setPositiveButton("确定",null).create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String value = hex.getText().toString().trim();
            if (!value.matches("#[0-9a-fA-F]{6}")) { hex.setError("请输入 # 加 6 位颜色值，例如 #CCB8F4"); return; }
            result.accept(Color.parseColor(value)); dialog.dismiss();
        }));
        dialog.show();
    }
    private static int contrast(int color) { return Color.luminance(color) > .4 ? Color.rgb(34,39,49) : Color.WHITE; }
    private static String hex(int color) { return String.format(Locale.ROOT,"#%06X",color & 0xFFFFFF); }
    private static int dp(Activity activity,int value) { return Math.round(value*activity.getResources().getDisplayMetrics().density); }
    private static LinearLayout column(Activity activity) {
        LinearLayout view = new LinearLayout(activity); view.setOrientation(LinearLayout.VERTICAL);
        int padding = dp(activity,18); view.setPadding(padding,padding,padding,padding); return view;
    }
}
