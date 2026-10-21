package local.countdown.probe;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;

final class WidgetStyle {
    final int text, header, accent, divider, background;
    final boolean showDividers, grid, pastel, dark;
    final int nameSize, valueSize;
    private final Context context;
    private final SharedPreferences prefs;
    private final float brightness;
    private final boolean photo;
    private final boolean gradientDark;
    WidgetStyle(Context context) {
        this.context = context.getApplicationContext();
        SharedPreferences p = context.getSharedPreferences("settings", 0);
        prefs = p;
        int mode = p.getInt("color_mode", 0);
        boolean systemDark = (context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        boolean themeDark = mode == 2 || mode == 0 && systemDark;
        gradientDark = themeDark;
        int type = p.getInt("background_type", 1);
        int base = type == 2 ? parse(p.getString("background_hex", "#FFFFFF"), Color.WHITE) : themeDark ? 0xFF25262B : Color.WHITE;
        float factor = (p.getInt("brightness", 50) - 50) / 50f;
        brightness = factor;
        background = factor >= 0 ? mix(base, Color.WHITE, factor) : mix(base, Color.BLACK, -factor);
        dark = type == 2 || factor != 0 ? Color.red(background) * .299f + Color.green(background) * .587f + Color.blue(background) * .114f < 128 : themeDark;
        int defaultText = dark ? 0xFFECECF0 : 0xFF4A4B50;
        int choice = p.getInt("text_color", 0);
        text = choice == 1 ? 0xFF73757C : choice == 2 ? Color.BLACK : choice == 3 ? Color.WHITE
                : choice == 4 ? parse(p.getString("text_hex", "#4A4B50"), defaultText) : defaultText;
        header = mix(text, background, .32f);
        accent = p.getBoolean("highlight", false) ? dark ? 0xFFB9B4FF : 0xFF7065DE : header;
        divider = dark ? 0xFF65656F : 0xFFC2C7D8;
        showDividers = p.getBoolean("dividers", true); grid = p.getBoolean("grid", false); pastel = type == 1;
        photo = type == 3;
        nameSize = p.getInt("table_font", 11); valueSize = p.getInt("value_font", nameSize);
    }
    static int parse(String raw, int fallback) { try { return Color.parseColor(raw); } catch (RuntimeException invalid) { return fallback; } }
    private static int mix(int from, int to, float t) {
        return Color.rgb(Math.round(Color.red(from) * (1-t) + Color.red(to)*t), Math.round(Color.green(from) * (1-t) + Color.green(to)*t), Math.round(Color.blue(from) * (1-t) + Color.blue(to)*t));
    }
    Bitmap background(int widthDp, int heightDp) {
        int w = Math.min(720, Math.max(300, widthDp * 2));
        int h = Math.max(100, Math.min(1400, Math.round(w * heightDp / (float)widthDp)));
        Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(background);
        float radius = 22 * w / (float)widthDp;
        android.graphics.Path clip = new android.graphics.Path();
        clip.addRoundRect(0, 0, w, h, radius, radius, android.graphics.Path.Direction.CW);
        canvas.save(); canvas.clipPath(clip); canvas.drawRect(0, 0, w, h, paint);
        if (pastel) {
            Bitmap clouds = Bitmap.createBitmap(HaloBackground.render(w, h, gradientDark,
                    prefs.getInt("gradient_strength", 85) / 100f, brightness,
                    prefs.getInt("halo_size", 70), prefs.getInt("halo_fade", 3),
                    prefs.getInt("halo_x", 18), prefs.getInt("halo_y", 31), HaloPaletteDialog.read(prefs)), w, h, Bitmap.Config.ARGB_8888);
            canvas.drawBitmap(clouds, 0, 0, paint);
            clouds.recycle();
        }
        if (photo) {
            Bitmap image = PhotoBackground.load(context);
            if (image != null) {
                paint.setShader(null); paint.setFilterBitmap(true);
                float[] position = ImagePlacement.cover(image.getWidth(), image.getHeight(), w, h,
                        prefs.getInt("photo_zoom", 100), prefs.getInt("photo_x", 50), prefs.getInt("photo_y", 50));
                canvas.drawBitmap(image, null, new android.graphics.RectF(position[0], position[1], position[2], position[3]), paint);
                image.recycle();
                if (brightness != 0) {
                    paint.setColor(brightness >= 0 ? Color.WHITE : Color.BLACK); paint.setAlpha(Math.round(Math.abs(brightness) * 255));
                    canvas.drawRect(0, 0, w, h, paint); paint.setAlpha(255);
                }
            }
        }
        if (grid) {
            paint.setShader(null); paint.setColor(divider); paint.setAlpha(50); paint.setStrokeWidth(1);
            for (int x = 0; x < w; x += 24) canvas.drawLine(x, 0, x, h, paint);
            for (int y = 0; y < h; y += 24) canvas.drawLine(0, y, w, y, paint);
        }
        canvas.restore(); return bitmap;
    }
    Bitmap line() {
        Bitmap image = Bitmap.createBitmap(180, 2, Bitmap.Config.ARGB_8888);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG); p.setColor(divider); p.setStrokeWidth(1); p.setPathEffect(new DashPathEffect(new float[]{6,6}, 0));
        new Canvas(image).drawLine(0, 1, 180, 1, p); return image;
    }
}
