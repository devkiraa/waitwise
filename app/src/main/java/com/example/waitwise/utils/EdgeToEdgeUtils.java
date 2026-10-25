package com.example.waitwise.utils;

import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

public class EdgeToEdgeUtils {

    private EdgeToEdgeUtils() {
        // Utility class
    }

    /**
     * Enables edge-to-edge display, sets dark status bar/navigation bar icons for light backgrounds,
     * and applies system bar insets (status bar, navigation bar, display cutouts) padding to the
     * provided root view so content stays within safe viewing bounds.
     */
    public static void applyEdgeToEdge(@NonNull AppCompatActivity activity, View rootView) {
        EdgeToEdge.enable(activity);

        // Ensure system status bar icons (time, battery, wifi, notifications) are dark and crisp on light backgrounds
        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(activity.getWindow(), activity.getWindow().getDecorView());
        if (controller != null) {
            controller.setAppearanceLightStatusBars(true);
            controller.setAppearanceLightNavigationBars(true);
        }

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
