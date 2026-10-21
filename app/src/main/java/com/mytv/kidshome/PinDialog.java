package com.mytv.kidshome;

import android.app.Activity;
import android.app.AlertDialog;
import android.text.InputType;
import android.widget.EditText;
import android.widget.Toast;

/** Small numeric-PIN prompts shared by the kids and parent screens. */
final class PinDialog {

    interface OnPin {
        void onPin(String pin);
    }

    private PinDialog() {
    }

    static void ask(Activity activity, int titleRes, OnPin onPin) {
        EditText input = new EditText(activity);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        new AlertDialog.Builder(activity)
                .setTitle(titleRes)
                .setView(input)
                .setPositiveButton(android.R.string.ok, (d, w) -> onPin.onPin(input.getText().toString()))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
        input.requestFocus();
    }

    /** Runs {@code onSuccess} only if the parent enters the correct PIN. */
    static void verify(Activity activity, AppStore store, Runnable onSuccess) {
        ask(activity, R.string.enter_pin, pin -> {
            if (store.checkPin(pin)) {
                onSuccess.run();
            } else {
                Toast.makeText(activity, R.string.wrong_pin, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
