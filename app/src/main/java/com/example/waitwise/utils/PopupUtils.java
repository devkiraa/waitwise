package com.example.waitwise.utils;

import android.content.Context;
import androidx.appcompat.app.AlertDialog;

public class PopupUtils {

    public static void showPopup(Context context, String title, String message) {
        if (context == null) return;
        try {
            new AlertDialog.Builder(context)
                    .setTitle(title)
                    .setMessage(message)
                    .setPositiveButton("OK", null)
                    .show();
        } catch (Exception ignored) {
            // Activity finished or destroyed
        }
    }
}
