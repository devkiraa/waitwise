package com.example.waitwise.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.waitwise.MainActivity;
import com.example.waitwise.R;
import com.example.waitwise.models.FoodItem;
import com.example.waitwise.models.Order;
import com.example.waitwise.models.User;
import com.example.waitwise.repositories.MenuRepository;
import com.example.waitwise.repositories.OrderRepository;
import com.example.waitwise.repositories.UserRepository;
import com.example.waitwise.utils.FirebaseManager;
import com.google.android.material.button.MaterialButton;

import java.util.List;

public class AdminDashboardActivity extends AppCompatActivity {

    private TextView tvTotalProducts;
    private TextView tvAvailabilityRatio;
    private TextView tvUnavailableProducts;
    private TextView tvPendingOrders;
    private TextView tvReadyOrders;
    private ProgressBar progressBarDashboard;

    private MenuRepository menuRepository;
    private OrderRepository orderRepository;
    private UserRepository userRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Security check 1: Must be logged in
        if (!FirebaseManager.getInstance().isUserLoggedIn()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        userRepository = new UserRepository();

        // Security check 2: Must have administrator role
        String uid = FirebaseManager.getInstance().getCurrentUserId();
        userRepository.getUserRole(uid, new UserRepository.RoleCallback() {
            @Override
            public void onRoleLoaded(String role) {
                if (!User.ROLE_ADMIN.equalsIgnoreCase(role)) {
                    Toast.makeText(AdminDashboardActivity.this, "Access denied. Administrator privileges required.", Toast.LENGTH_LONG).show();
                    Intent intent = new Intent(AdminDashboardActivity.this, MainActivity.class);
                    startActivity(intent);
                    finish();
                }
            }

            @Override
            public void onError(String errorMessage) {
                Toast.makeText(AdminDashboardActivity.this, "Security verification failed: " + errorMessage, Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(AdminDashboardActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        });

        setContentView(R.layout.activity_admin_dashboard);
        com.example.waitwise.utils.EdgeToEdgeUtils.applyEdgeToEdge(this, findViewById(R.id.adminDashboardRoot));

        menuRepository = new MenuRepository();
        orderRepository = new OrderRepository();

        initializeViews();
        loadDashboardData();
    }

    private void initializeViews() {
        tvTotalProducts = findViewById(R.id.tvTotalProducts);
        tvAvailabilityRatio = findViewById(R.id.tvAvailabilityRatio);
        tvUnavailableProducts = findViewById(R.id.tvUnavailableProducts);
        tvPendingOrders = findViewById(R.id.tvPendingOrders);
        tvReadyOrders = findViewById(R.id.tvReadyOrders);
        progressBarDashboard = findViewById(R.id.progressBarDashboard);

        MaterialButton btnManageProducts = findViewById(R.id.btnManageProducts);
        MaterialButton btnAddProduct = findViewById(R.id.btnAddProduct);
        MaterialButton btnManageOrders = findViewById(R.id.btnManageOrders);
        MaterialButton btnManageTokens = findViewById(R.id.btnManageTokens);
        MaterialButton btnSwitchCustomerView = findViewById(R.id.btnSwitchCustomerView);
        MaterialButton btnAdminLogout = findViewById(R.id.btnAdminLogout);

        btnManageProducts.setOnClickListener(v -> {
            startActivity(new Intent(AdminDashboardActivity.this, ManageProductsActivity.class));
        });

        btnAddProduct.setOnClickListener(v -> {
            startActivity(new Intent(AdminDashboardActivity.this, AddProductActivity.class));
        });

        btnManageOrders.setOnClickListener(v -> {
            startActivity(new Intent(AdminDashboardActivity.this, AdminOrdersActivity.class));
        });

        btnManageTokens.setOnClickListener(v -> {
            startActivity(new Intent(AdminDashboardActivity.this, AdminTokensActivity.class));
        });

        btnSwitchCustomerView.setOnClickListener(v -> {
            startActivity(new Intent(AdminDashboardActivity.this, MainActivity.class));
        });

        btnAdminLogout.setOnClickListener(v -> confirmLogout());
    }

    private void loadDashboardData() {
        progressBarDashboard.setVisibility(View.VISIBLE);

        // 1. Live Menu Statistics
        menuRepository.listenToMenu(new MenuRepository.MenuCallback() {
            @Override
            public void onMenuLoaded(List<FoodItem> items) {
                int total = items.size();
                int available = 0;
                int unavailable = 0;

                for (FoodItem item : items) {
                    if (item.isAvailable()) {
                        available++;
                    } else {
                        unavailable++;
                    }
                }

                tvTotalProducts.setText(String.valueOf(total));
                tvAvailabilityRatio.setText(available + " available in menu");
                tvUnavailableProducts.setText(String.valueOf(unavailable));
                progressBarDashboard.setVisibility(View.GONE);
            }

            @Override
            public void onError(String errorMessage) {
                progressBarDashboard.setVisibility(View.GONE);
                Toast.makeText(AdminDashboardActivity.this, "Menu stats error: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });

        // 2. Live Order Statistics
        orderRepository.listenToAllOrders(new OrderRepository.OrdersCallback() {
            @Override
            public void onOrdersLoaded(List<Order> orders) {
                int inPrep = 0;
                int ready = 0;

                for (Order order : orders) {
                    String status = order.getOrderStatus();
                    if (Order.STATUS_PLACED.equalsIgnoreCase(status)
                            || Order.STATUS_ACCEPTED.equalsIgnoreCase(status)
                            || Order.STATUS_PREPARING.equalsIgnoreCase(status)) {
                        inPrep++;
                    } else if (Order.STATUS_READY.equalsIgnoreCase(status)) {
                        ready++;
                    }
                }

                tvPendingOrders.setText(String.valueOf(inPrep));
                tvReadyOrders.setText(String.valueOf(ready));
            }

            @Override
            public void onError(String errorMessage) {
                // Non-blocking
            }
        });
    }

    private void confirmLogout() {
        new AlertDialog.Builder(this)
                .setTitle("Administrator Logout")
                .setMessage("Are you sure you want to end your administrator session?")
                .setPositiveButton("Log Out", (dialog, which) -> {
                    FirebaseManager.getInstance().signOut();
                    Intent intent = new Intent(AdminDashboardActivity.this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (menuRepository != null) menuRepository.detachListener();
        if (orderRepository != null) orderRepository.detachListener();
    }
}
