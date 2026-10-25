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
import com.google.firebase.auth.UserProfileChangeRequest;

public class RegisterActivity extends AppCompatActivity {

    private EditText etRegName;
    private EditText etRegEmail;
    private EditText etRegPassword;
    private EditText etRegConfirmPassword;
    private ShimmerFrameLayout shimmerRegister;
    private MaterialButton btnRegister;
    private UserRepository userRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        EdgeToEdgeUtils.applyEdgeToEdge(this, findViewById(R.id.registerRoot));

        userRepository = new UserRepository();

        etRegName = findViewById(R.id.etRegName);
        etRegEmail = findViewById(R.id.etRegEmail);
        etRegPassword = findViewById(R.id.etRegPassword);
        etRegConfirmPassword = findViewById(R.id.etRegConfirmPassword);
        shimmerRegister = findViewById(R.id.shimmerRegister);
        btnRegister = findViewById(R.id.btnRegister);
        TextView tvGoToLogin = findViewById(R.id.tvGoToLogin);

        btnRegister.setOnClickListener(v -> performRegistration());
        tvGoToLogin.setOnClickListener(v -> finish());
    }

    private void performRegistration() {
        String name = etRegName.getText().toString().trim();
        String email = etRegEmail.getText().toString().trim();
        String password = etRegPassword.getText().toString().trim();
        String confirmPassword = etRegConfirmPassword.getText().toString().trim();

        if (TextUtils.isEmpty(name)) {
            etRegName.setError("Name is required");
            etRegName.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(email)) {
            etRegEmail.setError("Email is required");
            etRegEmail.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etRegEmail.setError("Enter a valid email address");
            etRegEmail.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password) || password.length() < 6) {
            etRegPassword.setError("Password must be at least 6 characters");
            etRegPassword.requestFocus();
            return;
        }

        if (!password.equals(confirmPassword)) {
            etRegConfirmPassword.setError("Passwords do not match");
            etRegConfirmPassword.requestFocus();
            return;
        }

        setLoading(true);

        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser fbUser = FirebaseAuth.getInstance().getCurrentUser();
                        if (fbUser != null) {
                            String uid = fbUser.getUid();

                            // 1. Update Firebase Auth Display Name directly
                            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                    .setDisplayName(name)
                                    .build();
                            fbUser.updateProfile(profileUpdates);

                            // 2. Persist user record in Realtime Database as customer role
                            User user = new User(uid, name, email, User.ROLE_CUSTOMER, System.currentTimeMillis());
                            userRepository.saveUserProfile(user, null);
                            FcmTokenManager.syncFcmToken(uid);
                        }

                        // 3. Dismiss loader, notify user, and log them into MainActivity
                        setLoading(false);
                        PopupUtils.showPopup(RegisterActivity.this, "Welcome to WaitWise", "Account created successfully! Welcome, " + name);
                        goToMainActivity();
                    } else {
                        setLoading(false);
                        String errorMsg = task.getException() != null
                                ? task.getException().getMessage()
                                : "Registration failed.";
                        PopupUtils.showPopup(RegisterActivity.this, "Registration Failed", errorMsg);
                    }
                });
    }

    private void setLoading(boolean isLoading) {
        if (shimmerRegister != null) {
            shimmerRegister.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            if (isLoading) shimmerRegister.startShimmer();
            else shimmerRegister.stopShimmer();
        }
        if (btnRegister != null) {
            btnRegister.setVisibility(isLoading ? View.GONE : View.VISIBLE);
            btnRegister.setEnabled(!isLoading);
        }
        etRegName.setEnabled(!isLoading);
        etRegEmail.setEnabled(!isLoading);
        etRegPassword.setEnabled(!isLoading);
        etRegConfirmPassword.setEnabled(!isLoading);
    }

    private void goToMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
