package com.example.waitwise.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.LinearLayout;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.waitwise.R;
import com.example.waitwise.adapters.OrderAdapter;
import com.example.waitwise.models.Order;
import com.example.waitwise.models.OrderItem;
import com.example.waitwise.models.FoodItem;
import com.example.waitwise.repositories.CartRepository;
import com.example.waitwise.repositories.MenuRepository;
import com.example.waitwise.repositories.OrderRepository;
import com.example.waitwise.utils.FirebaseManager;
import com.example.waitwise.utils.PopupUtils;
import com.facebook.shimmer.ShimmerFrameLayout;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class MyOrdersActivity extends AppCompatActivity implements OrderAdapter.OnOrderClickListener {

    private ShimmerFrameLayout shimmerOrders;
    private LinearLayout layoutEmptyOrders;
    private RecyclerView recyclerOrders;

    private OrderAdapter orderAdapter;
    private OrderRepository orderRepository;
    private MenuRepository menuRepository;
    private CartRepository cartRepository;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        currentUserId = FirebaseManager.getInstance().getCurrentUserId();
        if (currentUserId == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_my_orders);
        com.example.waitwise.utils.EdgeToEdgeUtils.applyEdgeToEdge(this, findViewById(R.id.ordersRoot));

        orderRepository = new OrderRepository();
        menuRepository = new MenuRepository();
        cartRepository = new CartRepository();

        initializeViews();
        setupRecyclerView();
        setupNavigation();
        loadOrders();
    }

    private void initializeViews() {
        shimmerOrders = findViewById(R.id.shimmerOrders);
        layoutEmptyOrders = findViewById(R.id.layoutEmptyOrders);
        recyclerOrders = findViewById(R.id.recyclerOrders);

        findViewById(R.id.btnBackOrders).setOnClickListener(v -> finish());
        findViewById(R.id.btnOrdersExplore).setOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        orderAdapter = new OrderAdapter(this);
        recyclerOrders.setLayoutManager(new LinearLayoutManager(this));
        recyclerOrders.setAdapter(orderAdapter);
    }

    private void setupNavigation() {
        View navHome = findViewById(R.id.navHome);
        View navTokens = findViewById(R.id.navTokens);
        View navProfile = findViewById(R.id.navProfile);

        if (navHome != null) {
            navHome.setOnClickListener(v -> {
                Intent intent = new Intent(MyOrdersActivity.this, com.example.waitwise.MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
            });
        }
        if (navTokens != null) {
            navTokens.setOnClickListener(v -> {
                startActivity(new Intent(MyOrdersActivity.this, MyTokensActivity.class));
                finish();
            });
        }
        if (navProfile != null) {
            navProfile.setOnClickListener(v -> {
                startActivity(new Intent(MyOrdersActivity.this, ProfileActivity.class));
                finish();
            });
        }
    }

    private void loadOrders() {
        if (shimmerOrders != null) {
            shimmerOrders.startShimmer();
            shimmerOrders.setVisibility(View.VISIBLE);
        }
        layoutEmptyOrders.setVisibility(View.GONE);
        recyclerOrders.setVisibility(View.GONE);

        AtomicBoolean loaded = new AtomicBoolean(false);
        Handler handler = new Handler(Looper.getMainLooper());
        Runnable timeoutRunnable = () -> {
            if (loaded.compareAndSet(false, true)) {
                if (shimmerOrders != null) {
                    shimmerOrders.stopShimmer();
                    shimmerOrders.setVisibility(View.GONE);
                }
                layoutEmptyOrders.setVisibility(View.VISIBLE);
                recyclerOrders.setVisibility(View.GONE);
            }
        };
        handler.postDelayed(timeoutRunnable, 3500);

        orderRepository.listenToUserOrders(currentUserId, new OrderRepository.OrdersCallback() {
            @Override
            public void onOrdersLoaded(List<Order> orders) {
                if (loaded.compareAndSet(false, true)) {
                    handler.removeCallbacks(timeoutRunnable);
                }
                if (shimmerOrders != null) {
                    shimmerOrders.stopShimmer();
                    shimmerOrders.setVisibility(View.GONE);
                }
                if (orders.isEmpty()) {
                    layoutEmptyOrders.setVisibility(View.VISIBLE);
                    recyclerOrders.setVisibility(View.GONE);
                } else {
                    layoutEmptyOrders.setVisibility(View.GONE);
                    recyclerOrders.setVisibility(View.VISIBLE);
                    orderAdapter.setOrders(orders);
                }
            }

            @Override
            public void onError(String errorMessage) {
                if (loaded.compareAndSet(false, true)) {
                    handler.removeCallbacks(timeoutRunnable);
                }
                if (shimmerOrders != null) {
                    shimmerOrders.stopShimmer();
                    shimmerOrders.setVisibility(View.GONE);
                }
                layoutEmptyOrders.setVisibility(View.VISIBLE);
                PopupUtils.showPopup(MyOrdersActivity.this, "Error", errorMessage);
            }
        });
    }

    @Override
    public void onOrderClick(Order order) {
        // Tapping order navigates to tokens or displays details
        Intent intent = new Intent(this, MyTokensActivity.class);
        startActivity(intent);
    }

    @Override
    public void onCancelOrder(Order order) {
        if (order == null || order.getOrderId() == null) return;

        new AlertDialog.Builder(this)
                .setTitle("Cancel Order")
                .setMessage("Are you sure you want to cancel Token #" + order.getTokenNumber() + "? The kitchen will be notified.")
                .setPositiveButton("Yes, Cancel Order", (dialog, which) -> {
                    orderRepository.updateOrderStatus(order.getOrderId(), Order.STATUS_CANCELLED, new OrderRepository.ActionCallback() {
                        @Override
                        public void onSuccess() {
                            PopupUtils.showPopup(MyOrdersActivity.this, "Order Cancelled", "Token #" + order.getTokenNumber() + " has been cancelled.");
                        }

                        @Override
                        public void onError(String errorMessage) {
                            PopupUtils.showPopup(MyOrdersActivity.this, "Cancellation Error", errorMessage);
                        }
                    });
                })
                .setNegativeButton("Keep Order", null)
                .show();
    }

    @Override
    public void onReorder(Order order) {
        if (order == null || order.getItems() == null || order.getItems().isEmpty()) {
            PopupUtils.showPopup(this, "Reorder Error", "Order has no items to reorder.");
            return;
        }

        if (menuRepository == null) menuRepository = new MenuRepository();
        if (cartRepository == null) cartRepository = new CartRepository();

        menuRepository.getMenuOnce(new MenuRepository.MenuCallback() {
            @Override
            public void onMenuLoaded(List<FoodItem> currentMenu) {
                Map<String, FoodItem> currentMenuMap = new HashMap<>();
                for (FoodItem fi : currentMenu) {
                    currentMenuMap.put(fi.getId(), fi);
                }

                int addedCount = 0;
                int skippedCount = 0;

                for (OrderItem originalItem : order.getItems().values()) {
                    FoodItem currentFood = currentMenuMap.get(originalItem.getFoodId());
                    if (currentFood != null && currentFood.isAvailable()) {
                        int qty = originalItem.getQuantity() > 0 ? originalItem.getQuantity() : 1;
                        for (int q = 0; q < qty; q++) {
                            cartRepository.addToCart(currentUserId, currentFood, null);
                        }
                        addedCount++;
                    } else {
                        skippedCount++;
                    }
                }

                if (addedCount > 0) {
                    String msg = skippedCount > 0
                            ? addedCount + " item(s) added to cart. " + skippedCount + " item(s) were unavailable."
                            : "Items added to cart successfully.";
                    PopupUtils.showPopup(MyOrdersActivity.this, "Reorder Success", msg);

                    Intent intent = new Intent(MyOrdersActivity.this, CartActivity.class);
                    startActivity(intent);
                } else {
                    PopupUtils.showPopup(MyOrdersActivity.this, "Reorder Unavailable", "None of the items from this order are currently available in the menu.");
                }
            }

            @Override
            public void onError(String errorMessage) {
                PopupUtils.showPopup(MyOrdersActivity.this, "Reorder Error", errorMessage);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (orderRepository != null) {
            orderRepository.detachListener();
        }
        if (menuRepository != null) {
            menuRepository.detachListener();
        }
        if (cartRepository != null) {
            cartRepository.detachListener();
        }
    }
}
