package com.example.waitwise.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.waitwise.MainActivity;
import com.example.waitwise.R;
import com.example.waitwise.models.User;
import com.example.waitwise.repositories.UserRepository;
import com.example.waitwise.utils.EdgeToEdgeUtils;
import com.example.waitwise.utils.FcmTokenManager;
import com.example.waitwise.utils.FirebaseManager;
import com.example.waitwise.utils.PopupUtils;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.concurrent.atomic.AtomicBoolean;

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail;
    private EditText etPassword;
    private ShimmerFrameLayout shimmerLogin;
    private MaterialButton btnLogin;
    private UserRepository userRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        EdgeToEdgeUtils.applyEdgeToEdge(this, findViewById(R.id.loginRoot));

        userRepository = new UserRepository();

        // If already logged in, check role and route appropriately
        if (FirebaseManager.getInstance().isUserLoggedIn()) {
            initializeViews();
            setLoading(true);
            routeBasedOnRole(FirebaseManager.getInstance().getCurrentUserId());
            return;
        }

        initializeViews();
    }

    private void initializeViews() {
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        shimmerLogin = findViewById(R.id.shimmerLogin);
        btnLogin = findViewById(R.id.btnLogin);

        TextView tvForgotPassword = findViewById(R.id.tvForgotPassword);
        TextView tvGoToRegister = findViewById(R.id.tvGoToRegister);

        btnLogin.setOnClickListener(v -> performLogin());

        tvGoToRegister.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
        });

        tvForgotPassword.setOnClickListener(v -> showForgotPasswordDialog());
    }

    private void performLogin() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Email is required");
            etEmail.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Enter a valid email address");
            etEmail.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Password is required");
            etPassword.requestFocus();
            return;
        }

        setLoading(true);

        FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser fbUser = FirebaseAuth.getInstance().getCurrentUser();
                        if (fbUser != null) {
                            routeBasedOnRole(fbUser.getUid());
                        } else {
                            setLoading(false);
                            goToCustomerHome();
                        }
                    } else {
                        setLoading(false);
                        String errorMsg = task.getException() != null
                                ? task.getException().getMessage()
                                : "Authentication failed.";
                        PopupUtils.showPopup(LoginActivity.this, "Sign In Failed", errorMsg);
                    }
                });
    }

    private void routeBasedOnRole(String uid) {
        if (isFinishing() || isDestroyed()) return;

        FcmTokenManager.syncFcmToken(uid);

        FirebaseUser fbUser = FirebaseAuth.getInstance().getCurrentUser();
        if (fbUser != null) {
            String name = !TextUtils.isEmpty(fbUser.getDisplayName())
                    ? fbUser.getDisplayName()
                    : (fbUser.getEmail() != null && fbUser.getEmail().contains("@") ? fbUser.getEmail().split("@")[0] : "Customer");
            String email = fbUser.getEmail() != null ? fbUser.getEmail() : "";
            userRepository.getUserProfile(uid, new UserRepository.UserCallback() {
                @Override
                public void onUserLoaded(User user) {
                    if (TextUtils.isEmpty(user.getName()) || TextUtils.isEmpty(user.getEmail())) {
                        user.setName(!TextUtils.isEmpty(user.getName()) ? user.getName() : name);
                        user.setEmail(!TextUtils.isEmpty(user.getEmail()) ? user.getEmail() : email);
                        userRepository.saveUserProfile(user, null);
                    }
                }

                @Override
                public void onError(String errorMessage) {
                    User newUser = new User(uid, name, email, User.ROLE_CUSTOMER, System.currentTimeMillis());
                    userRepository.saveUserProfile(newUser, null);
                }
            });
        }

        AtomicBoolean routed = new AtomicBoolean(false);
        Handler handler = new Handler(Looper.getMainLooper());

        Runnable safetyTimeout = () -> {
            if (routed.compareAndSet(false, true)) {
                if (!isFinishing() && !isDestroyed()) {
                    setLoading(false);
                    goToCustomerHome();
                }
            }
        };

        // Safety fallback after 3 seconds
        handler.postDelayed(safetyTimeout, 3000);

        userRepository.getUserRole(uid, new UserRepository.RoleCallback() {
            @Override
            public void onRoleLoaded(String role) {
                if (routed.compareAndSet(false, true)) {
                    handler.removeCallbacks(safetyTimeout);
                    if (!isFinishing() && !isDestroyed()) {
                        setLoading(false);
                        if (User.ROLE_ADMIN.equalsIgnoreCase(role)) {
                            Toast.makeText(LoginActivity.this, "Administrator access granted.", Toast.LENGTH_SHORT).show();
                            goToAdminDashboard();
                        } else {
                            Toast.makeText(LoginActivity.this, "Welcome back!", Toast.LENGTH_SHORT).show();
                            goToCustomerHome();
                        }
                    }
                }
            }

            @Override
            public void onError(String errorMessage) {
                if (routed.compareAndSet(false, true)) {
                    handler.removeCallbacks(safetyTimeout);
                    if (!isFinishing() && !isDestroyed()) {
                        setLoading(false);
                        goToCustomerHome();
                    }
                }
            }
        });
    }

    private void showForgotPasswordDialog() {
        final EditText resetEmailInput = new EditText(this);
        resetEmailInput.setHint("Enter your registered email");
        resetEmailInput.setInputType(android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);

        int paddingPx = (int) (16 * getResources().getDisplayMetrics().density);
        resetEmailInput.setPadding(paddingPx, paddingPx, paddingPx, paddingPx);

        new AlertDialog.Builder(this)
                .setTitle("Reset Password")
                .setMessage("Enter your email address to receive password reset instructions.")
                .setView(resetEmailInput)
                .setPositiveButton("Send Reset Link", (dialog, which) -> {
                    String email = resetEmailInput.getText().toString().trim();
                    if (!TextUtils.isEmpty(email) && Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                        FirebaseAuth.getInstance().sendPasswordResetEmail(email)
                                .addOnSuccessListener(aVoid ->
                                        Toast.makeText(LoginActivity.this, "Reset link sent to your email.", Toast.LENGTH_LONG).show())
                                .addOnFailureListener(e ->
                                        Toast.makeText(LoginActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
                    } else {
                        Toast.makeText(LoginActivity.this, "Please provide a valid email.", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void setLoading(boolean isLoading) {
        if (shimmerLogin != null) {
            shimmerLogin.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            if (isLoading) shimmerLogin.startShimmer();
            else shimmerLogin.stopShimmer();
        }
        if (btnLogin != null) {
            btnLogin.setVisibility(isLoading ? View.GONE : View.VISIBLE);
            btnLogin.setEnabled(!isLoading);
        }
        if (etEmail != null) {
            etEmail.setEnabled(!isLoading);
        }
        if (etPassword != null) {
            etPassword.setEnabled(!isLoading);
        }
    }

    private void goToCustomerHome() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void goToAdminDashboard() {
        Intent intent = new Intent(this, AdminDashboardActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
