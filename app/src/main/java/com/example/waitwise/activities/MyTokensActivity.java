package com.example.waitwise.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.waitwise.R;
import com.example.waitwise.adapters.TokenAdapter;
import com.example.waitwise.models.Order;
import com.example.waitwise.repositories.OrderRepository;
import com.example.waitwise.utils.FirebaseManager;

import java.util.List;

public class MyTokensActivity extends AppCompatActivity implements TokenAdapter.OnTokenClickListener {

    private ProgressBar progressBarTokens;
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
        loadTokens();
    }

    private void initializeViews() {
        progressBarTokens = findViewById(R.id.progressBarTokens);
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

    private void loadTokens() {
        progressBarTokens.setVisibility(View.VISIBLE);
        layoutEmptyTokens.setVisibility(View.GONE);
        recyclerTokens.setVisibility(View.GONE);

        orderRepository.listenToUserOrders(currentUserId, new OrderRepository.OrdersCallback() {
            @Override
            public void onOrdersLoaded(List<Order> orders) {
                progressBarTokens.setVisibility(View.GONE);
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
                progressBarTokens.setVisibility(View.GONE);
                Toast.makeText(MyTokensActivity.this, "Error: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onTokenClick(Order order) {
        Toast.makeText(this, "Token #" + order.getTokenNumber() + " is currently " + order.getOrderStatus(), Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (orderRepository != null) {
            orderRepository.detachListener();
        }
    }
}
