package com.mytv.kidshome;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Calendar;
import java.util.List;

/** The kids' home screen: big, colourful tiles for parent-approved apps only. No feeds, no ads. */
public class HomeActivity extends Activity {

    private static final int[] TILE_COLORS = {
            0xFFFF6B6B, 0xFFFFA94D, 0xFF4ECDC4, 0xFF5B8DEF, 0xFFA78BFA, 0xFF34D399,
    };

    private AppStore store;
    private GridLayout grid;
    private TextView empty;
    private TextView greeting;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        store = new AppStore(this);
        grid = findViewById(R.id.grid);
        empty = findViewById(R.id.empty);
        greeting = findViewById(R.id.greeting);
        findViewById(R.id.parents).setOnClickListener(v ->
                PinDialog.verify(this, store, () -> startActivity(new Intent(this, ParentActivity.class))));
    }

    @Override
    protected void onResume() {
        super.onResume();
        greeting.setText(greetingForNow());
        showApps();
    }

    @Override
    public void onBackPressed() {
        // This is the home screen; there is nowhere to go back to.
    }

    private void showApps() {
        grid.removeAllViews();
        List<AppStore.AppEntry> apps = store.allowedApps();
        empty.setVisibility(apps.isEmpty() ? View.VISIBLE : View.GONE);

        int columns = grid.getColumnCount();
        int margin = dp(14);
        int available = getResources().getDisplayMetrics().widthPixels - dp(48) * 2 - dp(12) * 2;
        int tileWidth = available / columns - margin * 2;

        for (int i = 0; i < apps.size(); i++) {
            grid.addView(makeTile(apps.get(i), TILE_COLORS[i % TILE_COLORS.length], tileWidth, margin));
        }
        if (grid.getChildCount() > 0) grid.getChildAt(0).requestFocus();
    }

    private View makeTile(AppStore.AppEntry app, int color, int width, int margin) {
        View tile = getLayoutInflater().inflate(R.layout.item_tile, grid, false);
        GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
        lp.width = width;
        lp.setMargins(margin, margin, margin, margin);
        tile.setLayoutParams(lp);

        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(20));
        bg.setColor(color);
        tile.setBackground(bg);

        ((ImageView) tile.findViewById(R.id.icon)).setImageDrawable(app.image);
        ((TextView) tile.findViewById(R.id.label)).setText(app.label);

        tile.setOnFocusChangeListener((v, focused) -> {
            bg.setStroke(focused ? dp(5) : 0, Color.WHITE);
            float scale = focused ? 1.1f : 1f;
            v.animate().scaleX(scale).scaleY(scale).translationZ(focused ? dp(8) : 0).setDuration(150).start();
        });
        tile.setOnClickListener(v -> launch(app.packageName));
        return tile;
    }

    private void launch(String packageName) {
        Intent intent = store.launchIntent(packageName);
        try {
            if (intent == null) throw new ActivityNotFoundException(packageName);
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, R.string.cannot_open, Toast.LENGTH_SHORT).show();
        }
    }

    private int greetingForNow() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (hour < 12) return R.string.greeting_morning;
        if (hour < 18) return R.string.greeting_afternoon;
        return R.string.greeting_evening;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
