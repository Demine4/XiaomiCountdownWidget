package local.countdown.probe;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.widget.RemoteViews;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class CountdownWidget extends AppWidgetProvider {
    static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static final String REFRESH = "local.countdown.probe.REFRESH";
    @Override public void onReceive(Context context, Intent intent) {
        final PendingResult pending = goAsync();
        Context app = context.getApplicationContext();
        EXECUTOR.execute(() -> {
            try { updateNow(app); CalendarWatch.schedule(app); }
            finally { pending.finish(); }
        });
    }
    static void refresh(Context context) {
        Context app = context.getApplicationContext();
        EXECUTOR.execute(() -> { updateNow(app); CalendarWatch.schedule(app); });
    }
    static PendingIntent refreshIntent(Context context) {
        return PendingIntent.getBroadcast(context, 0, new Intent(context, CountdownWidget.class).setAction(REFRESH),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
    static void updateNow(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        int[] ids = manager.getAppWidgetIds(new ComponentName(context, CountdownWidget.class));
        AlarmManager alarm = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (ids.length == 0) { alarm.cancel(refreshIntent(context)); return; }
        CountdownStore.Result data = CountdownStore.read(context);
        LocalDate today = LocalDate.now();
        for (int id : ids) {
            Bundle options = manager.getAppWidgetOptions(id);
            int height = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 165);
            int width = Math.max(250, options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 360));
            RemoteViews views = WidgetRenderer.render(context, data, height, width);
            manager.updateAppWidget(id, views);
        }
        context.getSharedPreferences("settings", 0).edit()
                .putLong("last_widget_refresh", System.currentTimeMillis())
                .putBoolean("last_widget_read_ok", data.error == null).apply();
        long midnight = today.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        // Inexact, non-wakeup alarm: no exact-alarm permission or persistent service.
        alarm.set(AlarmManager.RTC, midnight, refreshIntent(context));
    }
}
