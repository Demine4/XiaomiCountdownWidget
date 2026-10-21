package local.countdown.probe;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
import android.os.PowerManager;
import android.provider.Settings;
import android.widget.Toast;

/** Vendor settings are best-effort deep links; never infer that a setting was granted. */
final class DeviceSettings {
    static boolean open(Activity activity, Intent intent) {
        try { activity.startActivity(intent); return true; }
        catch (RuntimeException unavailable) { return false; }
    }
    static void appDetails(Activity activity) {
        if (!open(activity, new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + activity.getPackageName()))))
            Toast.makeText(activity, "请在系统设置中搜索“日历倒数日”，打开应用详情", Toast.LENGTH_LONG).show();
    }
    static void desktopPermission(Activity activity) {
        Intent editor = new Intent("miui.intent.action.APP_PERM_EDITOR").setPackage("com.miui.securitycenter")
                .putExtra("extra_pkgname", activity.getPackageName());
        if (!open(activity, editor)) appDetails(activity);
        Toast.makeText(activity, "请在其他权限中允许“创建桌面快捷方式”，返回后重新添加小部件", Toast.LENGTH_LONG).show();
    }
    static void autoStart(Activity activity) {
        Intent intent = new Intent().setComponent(new ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity"))
                .putExtra("package_name", activity.getPackageName()).putExtra("extra_pkgname", activity.getPackageName());
        if (!open(activity, intent)) appDetails(activity);
        Toast.makeText(activity, "请找到“日历倒数日”并开启自启动；若页面不同，可在系统设置搜索“自启动”", Toast.LENGTH_LONG).show();
    }
    static void vendorBattery(Activity activity) {
        Intent intent = new Intent().setComponent(new ComponentName("com.miui.powerkeeper", "com.miui.powerkeeper.ui.HiddenAppsConfigActivity"))
                .putExtra("package_name", activity.getPackageName()).putExtra("package_label", "日历倒数日");
        if (!open(activity, intent)) appDetails(activity);
        Toast.makeText(activity, "请将“日历倒数日”的省电策略设置为“无限制”", Toast.LENGTH_LONG).show();
    }
    static void requestBatteryExemption(Activity activity) {
        PowerManager power = (PowerManager)activity.getSystemService(Activity.POWER_SERVICE);
        if (power.isIgnoringBatteryOptimizations(activity.getPackageName())) {
            Toast.makeText(activity, "Android电池优化已豁免，小米省电策略仍需单独核对", Toast.LENGTH_LONG).show(); return;
        }
        if (!open(activity, new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:" + activity.getPackageName())))) {
            if (!open(activity, new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))) appDetails(activity);
        }
    }
}
