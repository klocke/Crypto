package com.example.crypto;

import android.content.Context;
import android.widget.Toast;

/**
 * Created by Tobias on 29.04.16.
 */
public final class Message {

    private Message() {
        throw new IllegalStateException("No instances.");
    }

    private static Toast currToast;

    public static void show(Context context, String text) {
        cancel();
        currToast = Toast.makeText(context, text, Toast.LENGTH_SHORT);
        currToast.show();
    }

    public static void cancel() {
        if (currToast != null) {
            currToast.cancel();
        }
    }

}
