package com.example.waitwise.activities;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.waitwise.R;
import com.example.waitwise.adapters.AdminOrderAdapter;
import com.example.waitwise.models.Order;
import com.example.waitwise.models.User;
import com.example.waitwise.repositories.OrderRepository;
import com.example.waitwise.repositories.UserRepository;
import com.example.waitwise.utils.FirebaseManager;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class AdminOrdersActivity extends AppCompatActivity implements AdminOrderAdapter.OnOrderAdminActionListener {

    private TextView tvAdminOrdersCount;
    private ProgressBar progressBarAdminOrders;
    private LinearLayout layoutEmptyAdminOrders;
    private RecyclerView recyclerAdminOrders;

    private MaterialButton filterOrdersAll;
    private MaterialButton filterOrdersActive;
    private MaterialButton filterOrdersReady;
    private MaterialButton filterOrdersCompleted;

    private AdminOrderAdapter adapter;
    private OrderRepository orderRepository;
    private UserRepository userRepository;

    private final List<Order> allOrders = new ArrayList<>();
    private final List<Order> filteredOrders = new ArrayList<>();
    private String selectedFilter = "ALL";

    private final int greenColor = android.graphics.Color.rgb(36, 107, 74);
    private final int whiteColor = android.graphics.Color.WHITE;
    private final int textColor = android.graphics.Color.rgb(32, 36, 33);
    private final int borderColor = android.graphics.Color.rgb(231, 232, 227);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Security check
        if (!FirebaseManager.getInstance().isUserLoggedIn()) {
            finish();
            return;
        }

        userRepository = new UserRepository();
        userRepository.getUserRole(FirebaseManager.getInstance().getCurrentUserId(), new UserRepository.RoleCallback() {
            @Override
            public void onRoleLoaded(String role) {
                if (!User.ROLE_ADMIN.equalsIgnoreCase(role)) {
                    Toast.makeText(AdminOrdersActivity.this, "Access denied.", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onError(String errorMessage) {
                finish();
            }
        });

        setContentView(R.layout.activity_admin_orders);

        orderRepository = new OrderRepository();

        initializeViews();
        setupRecyclerView();
        setupFilters();
        loadOrders();
    }

    private void initializeViews() {
        tvAdminOrdersCount = findViewById(R.id.tvAdminOrdersCount);
        progressBarAdminOrders = findViewById(R.id.progressBarAdminOrders);
        layoutEmptyAdminOrders = findViewById(R.id.layoutEmptyAdminOrders);
        recyclerAdminOrders = findViewById(R.id.recyclerAdminOrders);

        filterOrdersAll = findViewById(R.id.filterOrdersAll);
        filterOrdersActive = findViewById(R.id.filterOrdersActive);
        filterOrdersReady = findViewById(R.id.filterOrdersReady);
        filterOrdersCompleted = findViewById(R.id.filterOrdersCompleted);

        findViewById(R.id.btnBackAdminOrders).setOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        adapter = new AdminOrderAdapter(this);
        recyclerAdminOrders.setLayoutManager(new LinearLayoutManager(this));
        recyclerAdminOrders.setAdapter(adapter);
    }

    private void setupFilters() {
        filterOrdersAll.setOnClickListener(v -> setFilter("ALL"));
        filterOrdersActive.setOnClickListener(v -> setFilter("ACTIVE"));
        filterOrdersReady.setOnClickListener(v -> setFilter("READY"));
        filterOrdersCompleted.setOnClickListener(v -> setFilter("COMPLETED"));
    }

    private void setFilter(String filter) {
        selectedFilter = filter;
        updateFilterAppearance();
        filterOrdersList();
    }

    private void updateFilterAppearance() {
        MaterialButton[] buttons = {filterOrdersAll, filterOrdersActive, filterOrdersReady, filterOrdersCompleted};
        String[] filterKeys = {"ALL", "ACTIVE", "READY", "COMPLETED"};

        for (int i = 0; i < buttons.length; i++) {
            boolean selected = filterKeys[i].equals(selectedFilter);
            if (selected) {
                buttons[i].setBackgroundTintList(ColorStateList.valueOf(greenColor));
                buttons[i].setTextColor(whiteColor);
                buttons[i].setStrokeColor(ColorStateList.valueOf(greenColor));
            } else {
                buttons[i].setBackgroundTintList(ColorStateList.valueOf(whiteColor));
                buttons[i].setTextColor(textColor);
                buttons[i].setStrokeColor(ColorStateList.valueOf(borderColor));
            }
        }
    }

    private void loadOrders() {
        progressBarAdminOrders.setVisibility(View.VISIBLE);
        layoutEmptyAdminOrders.setVisibility(View.GONE);
        recyclerAdminOrders.setVisibility(View.GONE);

        orderRepository.listenToAllOrders(new OrderRepository.OrdersCallback() {
            @Override
            public void onOrdersLoaded(List<Order> orders) {
                progressBarAdminOrders.setVisibility(View.GONE);
                allOrders.clear();
                allOrders.addAll(orders);
                filterOrdersList();
            }

            @Override
            public void onError(String errorMessage) {
                progressBarAdminOrders.setVisibility(View.GONE);
                Toast.makeText(AdminOrdersActivity.this, "Error: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void filterOrdersList() {
        filteredOrders.clear();

        for (Order order : allOrders) {
            String status = order.getOrderStatus();
            boolean match = false;

            switch (selectedFilter) {
                case "ALL":
                    match = true;
                    break;
                case "ACTIVE":
                    match = Order.STATUS_PLACED.equalsIgnoreCase(status)
                            || Order.STATUS_ACCEPTED.equalsIgnoreCase(status)
                            || Order.STATUS_PREPARING.equalsIgnoreCase(status);
                    break;
                case "READY":
                    match = Order.STATUS_READY.equalsIgnoreCase(status);
                    break;
                case "COMPLETED":
                    match = Order.STATUS_COLLECTED.equalsIgnoreCase(status)
                            || Order.STATUS_CANCELLED.equalsIgnoreCase(status);
                    break;
            }

            if (match) {
                filteredOrders.add(order);
            }
        }

        adapter.setOrders(filteredOrders);
        int count = filteredOrders.size();
        tvAdminOrdersCount.setText(count + (count == 1 ? " order" : " orders"));

        if (filteredOrders.isEmpty()) {
            layoutEmptyAdminOrders.setVisibility(View.VISIBLE);
            recyclerAdminOrders.setVisibility(View.GONE);
        } else {
            layoutEmptyAdminOrders.setVisibility(View.GONE);
            recyclerAdminOrders.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onAdvanceStatus(Order order, String nextStatus) {
        orderRepository.updateOrderStatus(order.getOrderId(), nextStatus, new OrderRepository.ActionCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(AdminOrdersActivity.this, "Token #" + order.getTokenNumber() + " updated to " + nextStatus, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String errorMessage) {
                Toast.makeText(AdminOrdersActivity.this, "Update error: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onCancelOrder(Order order) {
        new AlertDialog.Builder(this)
                .setTitle("Cancel Order")
                .setMessage("Are you sure you want to cancel Order #" + order.getOrderId() + " (Token #" + order.getTokenNumber() + ")?")
                .setPositiveButton("Yes, Cancel", (dialog, which) -> {
                    orderRepository.updateOrderStatus(order.getOrderId(), Order.STATUS_CANCELLED, new OrderRepository.ActionCallback() {
                        @Override
                        public void onSuccess() {
                            Toast.makeText(AdminOrdersActivity.this, "Order cancelled.", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onError(String errorMessage) {
                            Toast.makeText(AdminOrdersActivity.this, "Failed: " + errorMessage, Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("No", null)
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (orderRepository != null) orderRepository.detachListener();
    }
}
