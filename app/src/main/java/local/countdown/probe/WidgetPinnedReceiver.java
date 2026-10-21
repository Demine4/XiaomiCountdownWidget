package local.countdown.probe;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** A callback means the launcher confirmed pinning, not merely that a request was sent. */
public final class WidgetPinnedReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        context.getSharedPreferences("settings", 0).edit().putLong("pin_confirmed", System.currentTimeMillis()).apply();
        final PendingResult pending = goAsync();
        Context app = context.getApplicationContext();
        CountdownWidget.EXECUTOR.execute(() -> {
            try { CountdownWidget.updateNow(app); CalendarWatch.schedule(app); }
            finally { pending.finish(); }
        });
    }
}
