package local.countdown.probe;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.provider.CalendarContract;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/** Content-triggered jobs survive process death. Re-register after each trigger and reboot. */
public final class CalendarWatch extends JobService {
    private static final int JOB_ID = 42;
    private static final int PERIODIC_ID = 43;
    private final ConcurrentHashMap<Integer, Integer> runs = new ConcurrentHashMap<>();
    private final AtomicInteger sequence = new AtomicInteger();
    static void schedule(Context context) {
        JobScheduler scheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        if (AppWidgetManager.getInstance(context).getAppWidgetIds(new ComponentName(context, CountdownWidget.class)).length == 0) {
            scheduler.cancel(JOB_ID); scheduler.cancel(PERIODIC_ID); return;
        }
        if (scheduler.getPendingJob(PERIODIC_ID) == null) {
            JobInfo periodic = new JobInfo.Builder(PERIODIC_ID, new ComponentName(context, CalendarWatch.class))
                    .setPeriodic(30 * 60 * 1000L).setPersisted(true).build();
            scheduler.schedule(periodic);
        }
        if (scheduler.getPendingJob(JOB_ID) != null) return;
        JobInfo job = new JobInfo.Builder(JOB_ID, new ComponentName(context, CalendarWatch.class))
                .addTriggerContentUri(new JobInfo.TriggerContentUri(CalendarContract.Events.CONTENT_URI, JobInfo.TriggerContentUri.FLAG_NOTIFY_FOR_DESCENDANTS))
                .addTriggerContentUri(new JobInfo.TriggerContentUri(CalendarContract.ExtendedProperties.CONTENT_URI, JobInfo.TriggerContentUri.FLAG_NOTIFY_FOR_DESCENDANTS))
                .setTriggerContentUpdateDelay(1000).setTriggerContentMaxDelay(5000).build();
        scheduler.schedule(job);
    }
    @Override public boolean onStartJob(JobParameters params) {
        int token = sequence.incrementAndGet();
        runs.put(params.getJobId(), token);
        CountdownWidget.EXECUTOR.execute(() -> {
            try { CountdownWidget.updateNow(this); }
            finally {
                if (runs.remove(params.getJobId(), token)) {
                    jobFinished(params, false);
                    if (params.getJobId() == JOB_ID) {
                        // Only the one-shot content job needs re-arming. Leave the periodic job intact.
                        ((JobScheduler)getSystemService(JOB_SCHEDULER_SERVICE)).cancel(JOB_ID);
                    }
                    schedule(this);
                }
            }
        });
        return true;
    }
    @Override public boolean onStopJob(JobParameters params) { runs.remove(params.getJobId()); return true; }
}
