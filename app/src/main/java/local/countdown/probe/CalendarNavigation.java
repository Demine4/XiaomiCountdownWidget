package local.countdown.probe;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

final class CalendarNavigation {
    static Intent home(Context context) {
        Intent intent = context.getPackageManager().getLaunchIntentForPackage("com.android.calendar");
        if (intent == null) intent = new Intent(Intent.ACTION_VIEW, Uri.parse("content://com.android.calendar/time/" + System.currentTimeMillis())).setPackage("com.android.calendar");
        return intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    }
    static PendingIntent pending(Context context) {
        return PendingIntent.getActivity(context, 310, home(context), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
    static PendingIntent collectionPending(Context context) {
        Intent intent = home(context);
        // Mutable collection templates must target an explicit component on modern Android.
        if (intent.getComponent() == null) {
            android.content.pm.ResolveInfo target = context.getPackageManager().resolveActivity(intent,0);
            if (target != null) intent.setClassName(target.activityInfo.packageName,target.activityInfo.name);
            else return pending(context);
        }
        return PendingIntent.getActivity(context,311,intent,PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
    }
}
