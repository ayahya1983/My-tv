package com.mytv.kidshome;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Installed-app discovery plus the parent's settings (allowed apps and PIN). */
final class AppStore {

    static final class AppEntry {
        final String packageName;
        final CharSequence label;
        final Drawable image;

        AppEntry(String packageName, CharSequence label, Drawable image) {
            this.packageName = packageName;
            this.label = label;
            this.image = image;
        }
    }

    /** Shown on first run if installed; parents can change the list at any time. */
    private static final String[] KID_FRIENDLY_DEFAULTS = {
            "com.google.android.youtube.tvkids", // YouTube Kids
            "com.netflix.ninja",                 // Netflix (use a Kids profile)
            "com.disney.disneyplus",             // Disney+
            "org.pbskids.video",                 // PBS KIDS Video
    };

    private static final String PREFS = "kids_home";
    private static final String KEY_ALLOWED = "allowed";
    private static final String KEY_PIN = "pin";
    private static final String DEFAULT_PIN = "1234";

    private final Context context;
    private final PackageManager pm;
    private final SharedPreferences prefs;

    AppStore(Context context) {
        this.context = context;
        this.pm = context.getPackageManager();
        this.prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    /** Every app that can be opened from a TV launcher, sorted by name, excluding this one. */
    List<AppEntry> launchableApps() {
        Map<String, ResolveInfo> byPackage = new LinkedHashMap<>();
        for (String category : new String[]{Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER}) {
            Intent intent = new Intent(Intent.ACTION_MAIN).addCategory(category);
            for (ResolveInfo ri : pm.queryIntentActivities(intent, 0)) {
                String pkg = ri.activityInfo.packageName;
                if (!pkg.equals(context.getPackageName()) && !byPackage.containsKey(pkg)) {
                    byPackage.put(pkg, ri);
                }
            }
        }
        List<AppEntry> apps = new ArrayList<>();
        for (ResolveInfo ri : byPackage.values()) {
            Drawable image = ri.activityInfo.loadBanner(pm);
            if (image == null) image = ri.activityInfo.applicationInfo.loadBanner(pm);
            if (image == null) image = ri.loadIcon(pm);
            apps.add(new AppEntry(ri.activityInfo.packageName, ri.loadLabel(pm), image));
        }
        Collator collator = Collator.getInstance();
        Collections.sort(apps, (a, b) -> collator.compare(a.label.toString(), b.label.toString()));
        return apps;
    }

    /** Installed apps the parent has approved, in name order. */
    List<AppEntry> allowedApps() {
        Set<String> allowed = allowedPackages();
        List<AppEntry> result = new ArrayList<>();
        for (AppEntry app : launchableApps()) {
            if (allowed.contains(app.packageName)) result.add(app);
        }
        return result;
    }

    Set<String> allowedPackages() {
        Set<String> stored = prefs.getStringSet(KEY_ALLOWED, null);
        if (stored != null) return new HashSet<>(stored);
        Set<String> defaults = new HashSet<>();
        for (String pkg : KID_FRIENDLY_DEFAULTS) {
            if (pm.getLaunchIntentForPackage(pkg) != null || pm.getLeanbackLaunchIntentForPackage(pkg) != null) {
                defaults.add(pkg);
            }
        }
        return defaults;
    }

    void setAllowedPackages(Set<String> packages) {
        prefs.edit().putStringSet(KEY_ALLOWED, new HashSet<>(packages)).apply();
    }

    boolean checkPin(String pin) {
        return prefs.getString(KEY_PIN, DEFAULT_PIN).equals(pin);
    }

    void setPin(String pin) {
        prefs.edit().putString(KEY_PIN, pin).apply();
    }

    Intent launchIntent(String packageName) {
        Intent intent = pm.getLeanbackLaunchIntentForPackage(packageName);
        if (intent == null) intent = pm.getLaunchIntentForPackage(packageName);
        if (intent != null) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return intent;
    }
}
