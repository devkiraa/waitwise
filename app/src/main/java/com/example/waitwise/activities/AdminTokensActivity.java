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

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.waitwise.R;
import com.example.waitwise.adapters.AdminTokenAdapter;
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

public class AdminTokensActivity extends AppCompatActivity implements AdminTokenAdapter.OnTokenActionListener {

    private TextView tvAdminTokensCount;
    private EditText etSearchAdminTokens;
    private ShimmerFrameLayout shimmerAdminTokens;
    private LinearLayout layoutEmptyAdminTokens;
    private RecyclerView recyclerAdminTokens;

    private MaterialButton filterTokensActive;
    private MaterialButton filterTokensReady;
    private MaterialButton filterTokensAll;

    private AdminTokenAdapter adapter;
    private OrderRepository orderRepository;
    private UserRepository userRepository;

    private final List<Order> allOrders = new ArrayList<>();
    private final List<Order> filteredTokens = new ArrayList<>();
    private String selectedFilter = "ACTIVE";
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
                    PopupUtils.showPopup(AdminTokensActivity.this, "Access Denied", "Administrator access required.");
                    finish();
                }
            }

            @Override
            public void onError(String errorMessage) {
                finish();
            }
        });

        setContentView(R.layout.activity_admin_tokens);
        EdgeToEdgeUtils.applyEdgeToEdge(this, findViewById(R.id.adminTokensRoot));

        orderRepository = new OrderRepository();

        initializeViews();
        setupRecyclerView();
        setupFilters();
        loadTokens();
    }

    private void initializeViews() {
        tvAdminTokensCount = findViewById(R.id.tvAdminTokensCount);
        etSearchAdminTokens = findViewById(R.id.etSearchAdminTokens);
        shimmerAdminTokens = findViewById(R.id.shimmerAdminTokens);
        layoutEmptyAdminTokens = findViewById(R.id.layoutEmptyAdminTokens);
        recyclerAdminTokens = findViewById(R.id.recyclerAdminTokens);

        filterTokensActive = findViewById(R.id.filterTokensActive);
        filterTokensReady = findViewById(R.id.filterTokensReady);
        filterTokensAll = findViewById(R.id.filterTokensAll);

        if (etSearchAdminTokens != null) {
            etSearchAdminTokens.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    searchQuery = s.toString().trim().toLowerCase(Locale.ROOT);
                    filterTokensList();
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
        }

        findViewById(R.id.btnBackAdminTokens).setOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        adapter = new AdminTokenAdapter(this);
        recyclerAdminTokens.setLayoutManager(new LinearLayoutManager(this));
        recyclerAdminTokens.setAdapter(adapter);
    }

    private void setupFilters() {
        filterTokensActive.setOnClickListener(v -> setFilter("ACTIVE"));
        filterTokensReady.setOnClickListener(v -> setFilter("READY"));
        filterTokensAll.setOnClickListener(v -> setFilter("ALL"));
    }

    private void setFilter(String filter) {
        selectedFilter = filter;
        updateFilterAppearance();
        filterTokensList();
    }

    private void updateFilterAppearance() {
        MaterialButton[] buttons = {filterTokensActive, filterTokensReady, filterTokensAll};
        String[] filterKeys = {"ACTIVE", "READY", "ALL"};

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

    private void loadTokens() {
        if (shimmerAdminTokens != null) {
            shimmerAdminTokens.startShimmer();
            shimmerAdminTokens.setVisibility(View.VISIBLE);
        }
        layoutEmptyAdminTokens.setVisibility(View.GONE);
        recyclerAdminTokens.setVisibility(View.GONE);

        AtomicBoolean loaded = new AtomicBoolean(false);
        Handler handler = new Handler(Looper.getMainLooper());
        Runnable timeoutRunnable = () -> {
            if (loaded.compareAndSet(false, true)) {
                if (shimmerAdminTokens != null) {
                    shimmerAdminTokens.stopShimmer();
                    shimmerAdminTokens.setVisibility(View.GONE);
                }
                layoutEmptyAdminTokens.setVisibility(View.VISIBLE);
            }
        };
        handler.postDelayed(timeoutRunnable, 3500);

        orderRepository.listenToAllOrders(new OrderRepository.OrdersCallback() {
            @Override
            public void onOrdersLoaded(List<Order> orders) {
                if (loaded.compareAndSet(false, true)) {
                    handler.removeCallbacks(timeoutRunnable);
                }
                if (shimmerAdminTokens != null) {
                    shimmerAdminTokens.stopShimmer();
                    shimmerAdminTokens.setVisibility(View.GONE);
                }
                allOrders.clear();
                allOrders.addAll(orders);
                filterTokensList();
            }

            @Override
            public void onError(String errorMessage) {
                if (loaded.compareAndSet(false, true)) {
                    handler.removeCallbacks(timeoutRunnable);
                }
                if (shimmerAdminTokens != null) {
                    shimmerAdminTokens.stopShimmer();
                    shimmerAdminTokens.setVisibility(View.GONE);
                }
                layoutEmptyAdminTokens.setVisibility(View.VISIBLE);
                PopupUtils.showPopup(AdminTokensActivity.this, "Error", errorMessage);
            }
        });
    }

    private void filterTokensList() {
        filteredTokens.clear();

        for (Order order : allOrders) {
            String status = order.getOrderStatus();
            boolean matchesStatus = false;

            switch (selectedFilter) {
                case "ACTIVE":
                    matchesStatus = Order.STATUS_PLACED.equalsIgnoreCase(status)
                            || Order.STATUS_ACCEPTED.equalsIgnoreCase(status)
                            || Order.STATUS_PREPARING.equalsIgnoreCase(status);
                    break;
                case "READY":
                    matchesStatus = Order.STATUS_READY.equalsIgnoreCase(status);
                    break;
                case "ALL":
                    matchesStatus = true;
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
            String combinedText = (tokenStr + " #" + tokenStr + " " + orderIdStr + " " + itemNamesSb).toLowerCase(Locale.ROOT);

            boolean matchesSearch = searchQuery.isEmpty() || combinedText.contains(searchQuery);

            if (matchesStatus && matchesSearch) {
                filteredTokens.add(order);
            }
        }

        adapter.setTokens(filteredTokens);
        int count = filteredTokens.size();
        tvAdminTokensCount.setText(count + (count == 1 ? " token" : " tokens"));

        if (filteredTokens.isEmpty()) {
            layoutEmptyAdminTokens.setVisibility(View.VISIBLE);
            recyclerAdminTokens.setVisibility(View.GONE);
        } else {
            layoutEmptyAdminTokens.setVisibility(View.GONE);
            recyclerAdminTokens.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onUpdateStatus(Order order, String nextStatus) {
        orderRepository.updateOrderStatus(order.getOrderId(), nextStatus, new OrderRepository.ActionCallback() {
            @Override
            public void onSuccess() {
                PopupUtils.showPopup(AdminTokensActivity.this, "Token Updated", "Token #" + order.getTokenNumber() + " updated to " + nextStatus);
            }

            @Override
            public void onError(String errorMessage) {
                PopupUtils.showPopup(AdminTokensActivity.this, "Update Error", errorMessage);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (orderRepository != null) orderRepository.detachListener();
    }
}
