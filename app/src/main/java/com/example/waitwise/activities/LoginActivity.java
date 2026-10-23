package com.example.waitwise.activities;

import android.content.Intent;
import android.os.Bundle;
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
import com.example.waitwise.utils.FirebaseManager;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail;
    private EditText etPassword;
    private ProgressBar progressBarLogin;
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
        progressBarLogin = findViewById(R.id.progressBarLogin);
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
                        Toast.makeText(LoginActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void routeBasedOnRole(String uid) {
        userRepository.getUserRole(uid, new UserRepository.RoleCallback() {
            @Override
            public void onRoleLoaded(String role) {
                setLoading(false);
                if (User.ROLE_ADMIN.equalsIgnoreCase(role)) {
                    Toast.makeText(LoginActivity.this, "Administrator access granted.", Toast.LENGTH_SHORT).show();
                    goToAdminDashboard();
                } else {
                    Toast.makeText(LoginActivity.this, "Welcome back!", Toast.LENGTH_SHORT).show();
                    goToCustomerHome();
                }
            }

            @Override
            public void onError(String errorMessage) {
                // In case of network/permission issue, default safely to customer screen
                setLoading(false);
                goToCustomerHome();
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
        progressBarLogin.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnLogin.setEnabled(!isLoading);
        etEmail.setEnabled(!isLoading);
        etPassword.setEnabled(!isLoading);
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
