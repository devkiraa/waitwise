package com.example.waitwise.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.example.waitwise.MainActivity;
import com.example.waitwise.R;
import com.example.waitwise.models.User;
import com.example.waitwise.repositories.UserRepository;
import com.example.waitwise.utils.EdgeToEdgeUtils;
import com.example.waitwise.utils.FcmTokenManager;
import com.example.waitwise.utils.FirebaseManager;

public class SplashActivity extends AppCompatActivity {

    private UserRepository userRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        EdgeToEdgeUtils.applyEdgeToEdge(this, findViewById(R.id.splashRoot));

        userRepository = new UserRepository();

        new Handler(Looper.getMainLooper()).postDelayed(this::checkAuthAndNavigate, 1200);
    }

    private void checkAuthAndNavigate() {
        if (isFinishing() || isDestroyed()) return;

        if (FirebaseManager.getInstance().isUserLoggedIn()) {
            String uid = FirebaseManager.getInstance().getCurrentUserId();
            FcmTokenManager.syncFcmToken(uid);

            userRepository.getUserRole(uid, new UserRepository.RoleCallback() {
                @Override
                public void onRoleLoaded(String role) {
                    if (isFinishing() || isDestroyed()) return;
                    if (User.ROLE_ADMIN.equalsIgnoreCase(role)) {
                        navigateTo(AdminDashboardActivity.class);
                    } else {
                        navigateTo(MainActivity.class);
                    }
                }

                @Override
                public void onError(String errorMessage) {
                    if (isFinishing() || isDestroyed()) return;
                    navigateTo(MainActivity.class);
                }
            });
        } else {
            navigateTo(LoginActivity.class);
        }
    }

    private void navigateTo(Class<?> targetActivity) {
        Intent intent = new Intent(SplashActivity.this, targetActivity);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
