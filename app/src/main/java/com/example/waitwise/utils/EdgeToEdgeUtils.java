package com.example.waitwise.utils;

import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class EdgeToEdgeUtils {

    private EdgeToEdgeUtils() {
        // Utility class
    }

    /**
     * Enables edge-to-edge display and applies system bar insets (status bar, navigation bar,
     * and display cutouts) padding to the provided root view so content stays within safe viewing bounds.
     */
    public static void applyEdgeToEdge(@NonNull AppCompatActivity activity, View rootView) {
        EdgeToEdge.enable(activity);

        if (rootView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(rootView, (view, insets) -> {
                Insets systemBars = insets.getInsets(
                        WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout()
                );
                view.setPadding(
                        systemBars.left,
                        systemBars.top,
                        systemBars.right,
                        systemBars.bottom
                );
                return insets;
            });
        }
    }
}
