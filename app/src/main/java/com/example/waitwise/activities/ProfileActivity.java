package com.example.waitwise.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.waitwise.R;
import com.example.waitwise.models.User;
import com.example.waitwise.repositories.UserRepository;
import com.example.waitwise.utils.FirebaseManager;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

import java.util.Locale;

public class ProfileActivity extends AppCompatActivity {

    private TextView tvAvatarInitial;
    private TextView tvProfileName;
    private TextView tvProfileEmail;
    private TextView tvProfileRoleBadge;
    private EditText etEditName;
    private ProgressBar progressBarProfile;
    private MaterialButton btnUpdateProfile;
    private MaterialButton btnLogout;

    private View dividerAdmin;
    private TextView tvProfileAdminConsole;

    private UserRepository userRepository;
    private String currentUserId;
    private User currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        currentUserId = FirebaseManager.getInstance().getCurrentUserId();
        if (currentUserId == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_profile);
        com.example.waitwise.utils.EdgeToEdgeUtils.applyEdgeToEdge(this, findViewById(R.id.profileRoot));

        userRepository = new UserRepository();

        initializeViews();
        loadProfile();
    }

    private void initializeViews() {
        tvAvatarInitial = findViewById(R.id.tvAvatarInitial);
        tvProfileName = findViewById(R.id.tvProfileName);
        tvProfileEmail = findViewById(R.id.tvProfileEmail);
        tvProfileRoleBadge = findViewById(R.id.tvProfileRoleBadge);
        etEditName = findViewById(R.id.etEditName);
        progressBarProfile = findViewById(R.id.progressBarProfile);
        btnUpdateProfile = findViewById(R.id.btnUpdateProfile);
        btnLogout = findViewById(R.id.btnLogout);

        dividerAdmin = findViewById(R.id.dividerAdmin);
        tvProfileAdminConsole = findViewById(R.id.tvProfileAdminConsole);

        findViewById(R.id.btnBackProfile).setOnClickListener(v -> finish());

        findViewById(R.id.tvProfileMyOrders).setOnClickListener(v -> {
            startActivity(new Intent(ProfileActivity.this, MyOrdersActivity.class));
        });

        findViewById(R.id.tvProfileMyTokens).setOnClickListener(v -> {
            startActivity(new Intent(ProfileActivity.this, MyTokensActivity.class));
        });

        tvProfileAdminConsole.setOnClickListener(v -> {
            startActivity(new Intent(ProfileActivity.this, AdminDashboardActivity.class));
        });

        btnUpdateProfile.setOnClickListener(v -> handleUpdateName());

        btnLogout.setOnClickListener(v -> confirmLogout());
    }

    private void loadProfile() {
        String email = FirebaseManager.getInstance().getCurrentUserEmail();
        FirebaseUser fbUser = FirebaseManager.getInstance().getCurrentUser();
        String initialName = fbUser != null && !TextUtils.isEmpty(fbUser.getDisplayName())
                ? fbUser.getDisplayName()
                : (email != null && email.contains("@") ? email.split("@")[0] : "Customer");

        displayUserData(initialName, email != null ? email : "", User.ROLE_CUSTOMER);

        progressBarProfile.setVisibility(View.VISIBLE);

        userRepository.getUserProfile(currentUserId, new UserRepository.UserCallback() {
            @Override
            public void onUserLoaded(User user) {
                progressBarProfile.setVisibility(View.GONE);
                currentUser = user;
                displayUserData(user.getName(), user.getEmail(), user.getRole());
            }

            @Override
            public void onError(String errorMessage) {
                progressBarProfile.setVisibility(View.GONE);
            }
        });
    }

    private void displayUserData(String name, String email, String role) {
        tvProfileName.setText(name);
        tvProfileEmail.setText(email);
        etEditName.setText(name);

        String displayRole = (role != null ? role : User.ROLE_CUSTOMER).toUpperCase(Locale.ROOT);
        tvProfileRoleBadge.setText(displayRole);

        boolean isAdmin = User.ROLE_ADMIN.equalsIgnoreCase(role);
        if (dividerAdmin != null) dividerAdmin.setVisibility(isAdmin ? View.VISIBLE : View.GONE);
        if (tvProfileAdminConsole != null) tvProfileAdminConsole.setVisibility(isAdmin ? View.VISIBLE : View.GONE);

        if (!TextUtils.isEmpty(name)) {
            tvAvatarInitial.setText(name.substring(0, 1).toUpperCase(Locale.ROOT));
        } else {
            tvAvatarInitial.setText("U");
        }
    }

    private void handleUpdateName() {
        String newName = etEditName.getText().toString().trim();
        if (TextUtils.isEmpty(newName)) {
            etEditName.setError("Name cannot be empty");
            etEditName.requestFocus();
            return;
        }

        progressBarProfile.setVisibility(View.VISIBLE);
        btnUpdateProfile.setEnabled(false);

        FirebaseUser fbUser = FirebaseManager.getInstance().getCurrentUser();
        if (fbUser != null) {
            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                    .setDisplayName(newName)
                    .build();
            fbUser.updateProfile(profileUpdates);
        }

        String existingRole = currentUser != null ? currentUser.getRole() : User.ROLE_CUSTOMER;
        if (currentUser == null) {
            currentUser = new User(currentUserId, newName, FirebaseManager.getInstance().getCurrentUserEmail(), existingRole, System.currentTimeMillis());
        } else {
            currentUser.setName(newName);
        }

        userRepository.saveUserProfile(currentUser, new UserRepository.ActionCallback() {
            @Override
            public void onSuccess() {
                progressBarProfile.setVisibility(View.GONE);
                btnUpdateProfile.setEnabled(true);
                displayUserData(newName, currentUser.getEmail(), currentUser.getRole());
                Toast.makeText(ProfileActivity.this, "Profile updated successfully!", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String errorMessage) {
                progressBarProfile.setVisibility(View.GONE);
                btnUpdateProfile.setEnabled(true);
                displayUserData(newName, currentUser.getEmail(), currentUser.getRole());
                Toast.makeText(ProfileActivity.this, "Saved locally. Notice: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void confirmLogout() {
        new AlertDialog.Builder(this)
                .setTitle("Log Out")
                .setMessage("Are you sure you want to log out of WaitWise?")
                .setPositiveButton("Log Out", (dialog, which) -> {
                    FirebaseManager.getInstance().signOut();
                    Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
