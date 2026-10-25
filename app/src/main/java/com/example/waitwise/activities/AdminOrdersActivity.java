package com.example.waitwise.activities;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.waitwise.R;
import com.example.waitwise.adapters.AdminOrderAdapter;
import com.example.waitwise.models.Order;
import com.example.waitwise.models.OrderItem;
import com.example.waitwise.models.User;
import com.example.waitwise.repositories.OrderRepository;
import com.example.waitwise.repositories.UserRepository;
import com.example.waitwise.utils.EdgeToEdgeUtils;
import com.example.waitwise.utils.FirebaseManager;
import com.example.waitwise.utils.PopupUtils;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

public class AdminOrdersActivity extends AppCompatActivity implements AdminOrderAdapter.OnOrderAdminActionListener {

    private TextView tvAdminOrdersCount;
    private EditText etSearchAdminOrders;
    private ShimmerFrameLayout shimmerAdminOrders;
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
    private String searchQuery = "";

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
                    PopupUtils.showPopup(AdminOrdersActivity.this, "Access Denied", "Administrator access required.");
                    finish();
                }
            }

            @Override
            public void onError(String errorMessage) {
                finish();
            }
        });

        setContentView(R.layout.activity_admin_orders);
        EdgeToEdgeUtils.applyEdgeToEdge(this, findViewById(R.id.adminOrdersRoot));

        orderRepository = new OrderRepository();

        initializeViews();
        setupRecyclerView();
        setupFilters();
        loadOrders();
    }

    private void initializeViews() {
        tvAdminOrdersCount = findViewById(R.id.tvAdminOrdersCount);
        etSearchAdminOrders = findViewById(R.id.etSearchAdminOrders);
        shimmerAdminOrders = findViewById(R.id.shimmerAdminOrders);
        layoutEmptyAdminOrders = findViewById(R.id.layoutEmptyAdminOrders);
        recyclerAdminOrders = findViewById(R.id.recyclerAdminOrders);

        filterOrdersAll = findViewById(R.id.filterOrdersAll);
        filterOrdersActive = findViewById(R.id.filterOrdersActive);
        filterOrdersReady = findViewById(R.id.filterOrdersReady);
        filterOrdersCompleted = findViewById(R.id.filterOrdersCompleted);

        if (etSearchAdminOrders != null) {
            etSearchAdminOrders.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    searchQuery = s.toString().trim().toLowerCase(Locale.ROOT);
                    filterOrdersList();
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
        }

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
        if (shimmerAdminOrders != null) {
            shimmerAdminOrders.startShimmer();
            shimmerAdminOrders.setVisibility(View.VISIBLE);
        }
        layoutEmptyAdminOrders.setVisibility(View.GONE);
        recyclerAdminOrders.setVisibility(View.GONE);

        AtomicBoolean loaded = new AtomicBoolean(false);
        Handler handler = new Handler(Looper.getMainLooper());
        Runnable timeoutRunnable = () -> {
            if (loaded.compareAndSet(false, true)) {
                if (shimmerAdminOrders != null) {
                    shimmerAdminOrders.stopShimmer();
                    shimmerAdminOrders.setVisibility(View.GONE);
                }
                layoutEmptyAdminOrders.setVisibility(View.VISIBLE);
            }
        };
        handler.postDelayed(timeoutRunnable, 3500);

        orderRepository.listenToAllOrders(new OrderRepository.OrdersCallback() {
            @Override
            public void onOrdersLoaded(List<Order> orders) {
                if (loaded.compareAndSet(false, true)) {
                    handler.removeCallbacks(timeoutRunnable);
                }
                if (shimmerAdminOrders != null) {
                    shimmerAdminOrders.stopShimmer();
                    shimmerAdminOrders.setVisibility(View.GONE);
                }
                allOrders.clear();
                allOrders.addAll(orders);
                filterOrdersList();
            }

            @Override
            public void onError(String errorMessage) {
                if (loaded.compareAndSet(false, true)) {
                    handler.removeCallbacks(timeoutRunnable);
                }
                if (shimmerAdminOrders != null) {
                    shimmerAdminOrders.stopShimmer();
                    shimmerAdminOrders.setVisibility(View.GONE);
                }
                layoutEmptyAdminOrders.setVisibility(View.VISIBLE);
                PopupUtils.showPopup(AdminOrdersActivity.this, "Error", errorMessage);
            }
        });
    }

    private void filterOrdersList() {
        filteredOrders.clear();

        for (Order order : allOrders) {
            String status = order.getOrderStatus();
            boolean matchesStatus = false;

            switch (selectedFilter) {
                case "ALL":
                    matchesStatus = true;
                    break;
                case "ACTIVE":
                    matchesStatus = Order.STATUS_PLACED.equalsIgnoreCase(status)
                            || Order.STATUS_ACCEPTED.equalsIgnoreCase(status)
                            || Order.STATUS_PREPARING.equalsIgnoreCase(status);
                    break;
                case "READY":
                    matchesStatus = Order.STATUS_READY.equalsIgnoreCase(status);
                    break;
                case "COMPLETED":
                    matchesStatus = Order.STATUS_COLLECTED.equalsIgnoreCase(status)
                            || Order.STATUS_CANCELLED.equalsIgnoreCase(status);
                    break;
            }

            String tokenStr = String.valueOf(order.getTokenNumber());
            String orderIdStr = order.getOrderId() != null ? order.getOrderId() : "";

            StringBuilder itemNamesSb = new StringBuilder();
            if (order.getItems() != null) {
                for (OrderItem item : order.getItems().values()) {
                    if (item != null) itemNamesSb.append(item.getName()).append(" ");
                }
            }
            String combinedText = (tokenStr + " #" + tokenStr + " " + orderIdStr + " " + itemNamesSb.toString()).toLowerCase(Locale.ROOT);

            boolean matchesSearch = searchQuery.isEmpty() || combinedText.contains(searchQuery);

            if (matchesStatus && matchesSearch) {
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
                PopupUtils.showPopup(AdminOrdersActivity.this, "Status Updated", "Token #" + order.getTokenNumber() + " status is now " + nextStatus);
            }

            @Override
            public void onError(String errorMessage) {
                PopupUtils.showPopup(AdminOrdersActivity.this, "Update Error", errorMessage);
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
                            PopupUtils.showPopup(AdminOrdersActivity.this, "Order Cancelled", "Token #" + order.getTokenNumber() + " was cancelled.");
                        }

                        @Override
                        public void onError(String errorMessage) {
                            PopupUtils.showPopup(AdminOrdersActivity.this, "Cancellation Error", errorMessage);
                        }
                    });
                })
                .setNegativeButton("No", null)
                .show();
    }

    @Override
    public void onUpdatePrepTime(Order order, int minutes) {
        orderRepository.updateEstimatedPrepTime(order.getOrderId(), minutes, new OrderRepository.ActionCallback() {
            @Override
            public void onSuccess() {
                String msg = minutes > 0
                        ? "Prep time set to " + minutes + " mins for Token #" + order.getTokenNumber()
                        : "Prep time cleared for Token #" + order.getTokenNumber();
                PopupUtils.showPopup(AdminOrdersActivity.this, "Prep Time Set", msg);
            }

            @Override
            public void onError(String errorMessage) {
                PopupUtils.showPopup(AdminOrdersActivity.this, "Prep Time Error", errorMessage);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (orderRepository != null) orderRepository.detachListener();
    }
}
