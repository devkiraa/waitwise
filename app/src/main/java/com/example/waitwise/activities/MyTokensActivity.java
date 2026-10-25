package com.example.waitwise.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.waitwise.R;
import com.example.waitwise.adapters.TokenAdapter;
import com.example.waitwise.models.Order;
import com.example.waitwise.repositories.OrderRepository;
import com.example.waitwise.utils.FirebaseManager;
import com.example.waitwise.utils.PopupUtils;
import com.facebook.shimmer.ShimmerFrameLayout;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class MyTokensActivity extends AppCompatActivity implements TokenAdapter.OnTokenClickListener {

    private ShimmerFrameLayout shimmerTokens;
    private LinearLayout layoutEmptyTokens;
    private RecyclerView recyclerTokens;

    private TokenAdapter tokenAdapter;
    private OrderRepository orderRepository;
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

        setContentView(R.layout.activity_my_tokens);
        com.example.waitwise.utils.EdgeToEdgeUtils.applyEdgeToEdge(this, findViewById(R.id.tokensRoot));

        orderRepository = new OrderRepository();

        initializeViews();
        setupRecyclerView();
        setupNavigation();
        loadTokens();
    }

    private void initializeViews() {
        shimmerTokens = findViewById(R.id.shimmerTokens);
        layoutEmptyTokens = findViewById(R.id.layoutEmptyTokens);
        recyclerTokens = findViewById(R.id.recyclerTokens);

        findViewById(R.id.btnBackTokens).setOnClickListener(v -> finish());
        findViewById(R.id.btnTokensOrderNow).setOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        tokenAdapter = new TokenAdapter(this);
        recyclerTokens.setLayoutManager(new LinearLayoutManager(this));
        recyclerTokens.setAdapter(tokenAdapter);
    }

    private void setupNavigation() {
        View navHome = findViewById(R.id.navHome);
        View navOrders = findViewById(R.id.navOrders);
        View navProfile = findViewById(R.id.navProfile);

        if (navHome != null) {
            navHome.setOnClickListener(v -> {
                Intent intent = new Intent(MyTokensActivity.this, com.example.waitwise.MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
            });
        }
        if (navOrders != null) {
            navOrders.setOnClickListener(v -> {
                startActivity(new Intent(MyTokensActivity.this, MyOrdersActivity.class));
                finish();
            });
        }
        if (navProfile != null) {
            navProfile.setOnClickListener(v -> {
                startActivity(new Intent(MyTokensActivity.this, ProfileActivity.class));
                finish();
            });
        }
    }

    private void loadTokens() {
        if (shimmerTokens != null) {
            shimmerTokens.startShimmer();
            shimmerTokens.setVisibility(View.VISIBLE);
        }
        layoutEmptyTokens.setVisibility(View.GONE);
        recyclerTokens.setVisibility(View.GONE);

        AtomicBoolean loaded = new AtomicBoolean(false);
        Handler handler = new Handler(Looper.getMainLooper());
        Runnable timeoutRunnable = () -> {
            if (loaded.compareAndSet(false, true)) {
                if (shimmerTokens != null) {
                    shimmerTokens.stopShimmer();
                    shimmerTokens.setVisibility(View.GONE);
                }
                layoutEmptyTokens.setVisibility(View.VISIBLE);
                recyclerTokens.setVisibility(View.GONE);
            }
        };
        handler.postDelayed(timeoutRunnable, 3500);

        orderRepository.listenToUserOrders(currentUserId, new OrderRepository.OrdersCallback() {
            @Override
            public void onOrdersLoaded(List<Order> orders) {
                if (loaded.compareAndSet(false, true)) {
                    handler.removeCallbacks(timeoutRunnable);
                }
                if (shimmerTokens != null) {
                    shimmerTokens.stopShimmer();
                    shimmerTokens.setVisibility(View.GONE);
                }
                if (orders.isEmpty()) {
                    layoutEmptyTokens.setVisibility(View.VISIBLE);
                    recyclerTokens.setVisibility(View.GONE);
                } else {
                    layoutEmptyTokens.setVisibility(View.GONE);
                    recyclerTokens.setVisibility(View.VISIBLE);
                    tokenAdapter.setTokens(orders);
                }
            }

            @Override
            public void onError(String errorMessage) {
                if (loaded.compareAndSet(false, true)) {
                    handler.removeCallbacks(timeoutRunnable);
                }
                if (shimmerTokens != null) {
                    shimmerTokens.stopShimmer();
                    shimmerTokens.setVisibility(View.GONE);
                }
                layoutEmptyTokens.setVisibility(View.VISIBLE);
                PopupUtils.showPopup(MyTokensActivity.this, "Error", errorMessage);
            }
        });
    }

    @Override
    public void onTokenClick(Order order) {
        PopupUtils.showPopup(this, "Token #" + order.getTokenNumber(), "Current Status: " + order.getOrderStatus() + "\nPayment Method: " + order.getPaymentMethod());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (orderRepository != null) {
            orderRepository.detachListener();
        }
    }
}
