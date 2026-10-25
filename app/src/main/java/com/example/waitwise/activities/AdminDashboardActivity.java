package com.example.waitwise.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.waitwise.MainActivity;
import com.example.waitwise.R;
import com.example.waitwise.models.FoodItem;
import com.example.waitwise.models.Order;
import com.example.waitwise.models.OrderItem;
import com.example.waitwise.models.User;
import com.example.waitwise.repositories.MenuRepository;
import com.example.waitwise.repositories.OrderRepository;
import com.example.waitwise.repositories.UserRepository;
import com.example.waitwise.utils.FcmTokenManager;
import com.example.waitwise.utils.FirebaseManager;
import com.example.waitwise.utils.PopupUtils;
import com.example.waitwise.utils.PriceUtils;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.material.button.MaterialButton;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class AdminDashboardActivity extends AppCompatActivity {

    private TextView tvTotalProducts;
    private TextView tvAvailabilityRatio;
    private TextView tvUnavailableProducts;
    private TextView tvPendingOrders;
    private TextView tvReadyOrders;
    private TextView tvCompletedOrdersToday;
    private TextView tvOrdersPlacedToday;
    private TextView tvGrossSalesToday;
    private TextView tvPopularItemToday;
    private ShimmerFrameLayout shimmerAdminDashboard;

    private MenuRepository menuRepository;
    private OrderRepository orderRepository;
    private UserRepository userRepository;

    private final List<Order> cachedOrders = new ArrayList<>();
    private long selectedExportStartMs = 0;
    private String selectedExportLabel = "Today";

    private final ActivityResultLauncher<String> createDocumentLauncher = registerForActivityResult(
            new ActivityResultContracts.CreateDocument("text/csv"),
            uri -> {
                if (uri != null) {
                    exportOrdersToCsvUri(uri);
                }
            }
    );

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
                    PopupUtils.showPopup(AdminDashboardActivity.this, "Access Denied", "Administrator privileges required.");
                    Intent intent = new Intent(AdminDashboardActivity.this, MainActivity.class);
                    startActivity(intent);
                    finish();
                }
            }

            @Override
            public void onError(String errorMessage) {
                PopupUtils.showPopup(AdminDashboardActivity.this, "Security Error", "Security verification failed: " + errorMessage);
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
        tvCompletedOrdersToday = findViewById(R.id.tvCompletedOrdersToday);
        tvOrdersPlacedToday = findViewById(R.id.tvOrdersPlacedToday);
        tvGrossSalesToday = findViewById(R.id.tvGrossSalesToday);
        tvPopularItemToday = findViewById(R.id.tvPopularItemToday);
        shimmerAdminDashboard = findViewById(R.id.shimmerAdminDashboard);

        MaterialButton btnManageProducts = findViewById(R.id.btnManageProducts);
        MaterialButton btnAddProduct = findViewById(R.id.btnAddProduct);
        MaterialButton btnManageOrders = findViewById(R.id.btnManageOrders);
        MaterialButton btnManageTokens = findViewById(R.id.btnManageTokens);
        MaterialButton btnExportSalesReport = findViewById(R.id.btnExportSalesReport);
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

        btnExportSalesReport.setOnClickListener(v -> showExportDateRangeDialog());

        btnSwitchCustomerView.setOnClickListener(v -> {
            startActivity(new Intent(AdminDashboardActivity.this, MainActivity.class));
        });

        btnAdminLogout.setOnClickListener(v -> confirmLogout());
    }

    private void loadDashboardData() {
        if (shimmerAdminDashboard != null) {
            shimmerAdminDashboard.startShimmer();
            shimmerAdminDashboard.setVisibility(View.VISIBLE);
        }

        AtomicBoolean loaded = new AtomicBoolean(false);
        Handler handler = new Handler(Looper.getMainLooper());
        Runnable timeoutRunnable = () -> {
            if (loaded.compareAndSet(false, true)) {
                if (shimmerAdminDashboard != null) {
                    shimmerAdminDashboard.stopShimmer();
                    shimmerAdminDashboard.setVisibility(View.GONE);
                }
            }
        };
        handler.postDelayed(timeoutRunnable, 3500);

        // 1. Live Menu Statistics
        menuRepository.listenToMenu(new MenuRepository.MenuCallback() {
            @Override
            public void onMenuLoaded(List<FoodItem> items) {
                if (loaded.compareAndSet(false, true)) {
                    handler.removeCallbacks(timeoutRunnable);
                }
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
                if (shimmerAdminDashboard != null) {
                    shimmerAdminDashboard.stopShimmer();
                    shimmerAdminDashboard.setVisibility(View.GONE);
                }
            }

            @Override
            public void onError(String errorMessage) {
                if (loaded.compareAndSet(false, true)) {
                    handler.removeCallbacks(timeoutRunnable);
                }
                if (shimmerAdminDashboard != null) {
                    shimmerAdminDashboard.stopShimmer();
                    shimmerAdminDashboard.setVisibility(View.GONE);
                }
                PopupUtils.showPopup(AdminDashboardActivity.this, "Menu Stats Error", errorMessage);
            }
        });

        // 2. Live Order Statistics
        orderRepository.listenToAllOrders(new OrderRepository.OrdersCallback() {
            @Override
            public void onOrdersLoaded(List<Order> orders) {
                cachedOrders.clear();
                cachedOrders.addAll(orders);

                int inPrep = 0;
                int ready = 0;

                // Today Start Timestamp (Local Timezone)
                Calendar cal = Calendar.getInstance();
                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);
                long startOfTodayMs = cal.getTimeInMillis();

                int ordersPlacedTodayCount = 0;
                int completedOrdersTodayCount = 0;
                double grossSalesToday = 0.0;
                Map<String, Integer> itemFrequencyMap = new HashMap<>();

                for (Order order : orders) {
                    String status = order.getOrderStatus();
                    if (Order.STATUS_PLACED.equalsIgnoreCase(status)
                            || Order.STATUS_ACCEPTED.equalsIgnoreCase(status)
                            || Order.STATUS_PREPARING.equalsIgnoreCase(status)) {
                        inPrep++;
                    } else if (Order.STATUS_READY.equalsIgnoreCase(status)) {
                        ready++;
                    }

                    if (order.getCreatedAt() >= startOfTodayMs) {
                        ordersPlacedTodayCount++;
                        if (Order.STATUS_COLLECTED.equalsIgnoreCase(status)) {
                            completedOrdersTodayCount++;
                            grossSalesToday = PriceUtils.add(grossSalesToday, order.getSubtotal());

                            // Item popularity
                            if (order.getItems() != null) {
                                for (OrderItem item : order.getItems().values()) {
                                    if (item != null && item.getName() != null) {
                                        Integer currentQtyObj = itemFrequencyMap.get(item.getName());
                                        int currentQty = currentQtyObj != null ? currentQtyObj : 0;
                                        itemFrequencyMap.put(item.getName(), currentQty + item.getQuantity());
                                    }
                                }
                            }
                        }
                    }
                }

                tvPendingOrders.setText(String.valueOf(inPrep));
                tvReadyOrders.setText(String.valueOf(ready));
                tvCompletedOrdersToday.setText(String.valueOf(completedOrdersTodayCount));
                tvOrdersPlacedToday.setText(ordersPlacedTodayCount + " total placed today");
                tvGrossSalesToday.setText(PriceUtils.formatPrice(grossSalesToday));

                // Determine top item
                String topItemName = "None";
                int maxQty = 0;
                for (Map.Entry<String, Integer> entry : itemFrequencyMap.entrySet()) {
                    if (entry.getValue() > maxQty) {
                        maxQty = entry.getValue();
                        topItemName = entry.getKey();
                    }
                }
                tvPopularItemToday.setText("Top Dish: " + topItemName + (maxQty > 0 ? " (" + maxQty + ")" : ""));
            }

            @Override
            public void onError(String errorMessage) {
                // Non-blocking
            }
        });
    }

    private void showExportDateRangeDialog() {
        if (cachedOrders.isEmpty()) {
            PopupUtils.showPopup(this, "Export Sales Report", "No orders available to export.");
            return;
        }

        String[] ranges = {"Today", "Last 7 Days", "Last 30 Days", "All Time"};
        new AlertDialog.Builder(this)
                .setTitle("Select Date Range for CSV Export")
                .setItems(ranges, (dialog, which) -> {
                    Calendar cal = Calendar.getInstance();
                    long now = System.currentTimeMillis();

                    switch (which) {
                        case 0: // Today
                            cal.set(Calendar.HOUR_OF_DAY, 0);
                            cal.set(Calendar.MINUTE, 0);
                            cal.set(Calendar.SECOND, 0);
                            cal.set(Calendar.MILLISECOND, 0);
                            selectedExportStartMs = cal.getTimeInMillis();
                            selectedExportLabel = "Today";
                            break;
                        case 1: // Last 7 Days
                            selectedExportStartMs = now - (7L * 24 * 60 * 60 * 1000L);
                            selectedExportLabel = "Last7Days";
                            break;
                        case 2: // Last 30 Days
                            selectedExportStartMs = now - (30L * 24 * 60 * 60 * 1000L);
                            selectedExportLabel = "Last30Days";
                            break;
                        case 3: // All Time
                        default:
                            selectedExportStartMs = 0;
                            selectedExportLabel = "AllTime";
                            break;
                    }

                    String filename = "WaitWise_Sales_" + selectedExportLabel + "_" + System.currentTimeMillis() + ".csv";
                    createDocumentLauncher.launch(filename);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void exportOrdersToCsvUri(Uri uri) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        List<Order> exportList = new ArrayList<>();
        double totalSales = 0.0;

        for (Order order : cachedOrders) {
            if (order.getCreatedAt() >= selectedExportStartMs) {
                exportList.add(order);
                if (Order.STATUS_COLLECTED.equalsIgnoreCase(order.getOrderStatus())) {
                    totalSales = PriceUtils.add(totalSales, order.getSubtotal());
                }
            }
        }

        if (exportList.isEmpty()) {
            PopupUtils.showPopup(this, "CSV Export", "No orders found in the selected date range.");
            return;
        }

        StringBuilder csvBuilder = new StringBuilder();
        csvBuilder.append("\"Order ID\",\"Token Number\",\"Created Date\",\"Order Status\",\"Payment Method\",\"Payment Status\",\"Total (INR)\"\n");

        for (Order order : exportList) {
            String dateStr = order.getCreatedAt() > 0 ? sdf.format(new Date(order.getCreatedAt())) : "N/A";
            csvBuilder.append(escapeCsv(order.getOrderId())).append(",")
                    .append(order.getTokenNumber()).append(",")
                    .append(escapeCsv(dateStr)).append(",")
                    .append(escapeCsv(order.getOrderStatus())).append(",")
                    .append(escapeCsv(order.getPaymentMethod())).append(",")
                    .append(escapeCsv(order.getPaymentStatus())).append(",")
                    .append(order.getSubtotal()).append("\n");
        }

        csvBuilder.append("\n");
        csvBuilder.append("\"\",\"\",\"\",\"\",\"Summary Completed Sales:\",\"\",").append(totalSales).append("\n");

        try (OutputStream outputStream = getContentResolver().openOutputStream(uri)) {
            if (outputStream != null) {
                outputStream.write(csvBuilder.toString().getBytes(StandardCharsets.UTF_8));
                outputStream.flush();
                PopupUtils.showPopup(this, "CSV Export Success", "Sales report exported successfully!");
            } else {
                PopupUtils.showPopup(this, "CSV Export Failed", "Failed to open destination file.");
            }
        } catch (Exception e) {
            PopupUtils.showPopup(this, "CSV Export Error", e.getMessage());
        }
    }

    private static String escapeCsv(String input) {
        if (input == null) return "\"\"";
        String escaped = input.replace("\"", "\"\"");
        if (escaped.contains(",") || escaped.contains("\"") || escaped.contains("\n") || escaped.contains("\r")) {
            return "\"" + escaped + "\"";
        }
        return "\"" + escaped + "\"";
    }

    private void confirmLogout() {
        new AlertDialog.Builder(this)
                .setTitle("Administrator Logout")
                .setMessage("Are you sure you want to end your administrator session?")
                .setPositiveButton("Log Out", (dialog, which) -> {
                    FcmTokenManager.clearFcmToken(FirebaseManager.getInstance().getCurrentUserId());
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
