package com.mytv.kidshome;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** PIN-protected screen where a parent picks which apps appear on the kids' home screen. */
public class ParentActivity extends Activity {

    private AppStore store;
    private ListView list;
    private List<AppStore.AppEntry> apps;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parent);
        store = new AppStore(this);
        list = findViewById(R.id.apps);

        apps = store.launchableApps();
        List<CharSequence> labels = new ArrayList<>();
        for (AppStore.AppEntry app : apps) labels.add(app.label);
        list.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_multiple_choice, labels));

        Set<String> allowed = store.allowedPackages();
        for (int i = 0; i < apps.size(); i++) {
            list.setItemChecked(i, allowed.contains(apps.get(i).packageName));
        }

        findViewById(R.id.save).setOnClickListener(v -> save());
        findViewById(R.id.change_pin).setOnClickListener(v ->
                PinDialog.ask(this, R.string.new_pin, pin -> {
                    if (pin.matches("\\d{4,8}")) {
                        store.setPin(pin);
                        Toast.makeText(this, R.string.pin_changed, Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, R.string.pin_invalid, Toast.LENGTH_SHORT).show();
                    }
                }));
        findViewById(R.id.tv_settings).setOnClickListener(v ->
                startActivity(new Intent(Settings.ACTION_SETTINGS)));
    }

    private void save() {
        Set<String> allowed = new HashSet<>();
        for (int i = 0; i < apps.size(); i++) {
            if (list.isItemChecked(i)) allowed.add(apps.get(i).packageName);
        }
        store.setAllowedPackages(allowed);
        Toast.makeText(this, R.string.saved, Toast.LENGTH_SHORT).show();
        finish();
    }
}
