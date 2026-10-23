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
import com.example.waitwise.adapters.OrderAdapter;
import com.example.waitwise.models.Order;
import com.example.waitwise.repositories.OrderRepository;
import com.example.waitwise.utils.FirebaseManager;

import java.util.List;

public class MyOrdersActivity extends AppCompatActivity implements OrderAdapter.OnOrderClickListener {

    private ProgressBar progressBarOrders;
    private LinearLayout layoutEmptyOrders;
    private RecyclerView recyclerOrders;

    private OrderAdapter orderAdapter;
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

        setContentView(R.layout.activity_my_orders);
        com.example.waitwise.utils.EdgeToEdgeUtils.applyEdgeToEdge(this, findViewById(R.id.ordersRoot));

        orderRepository = new OrderRepository();

        initializeViews();
        setupRecyclerView();
        loadOrders();
    }

    private void initializeViews() {
        progressBarOrders = findViewById(R.id.progressBarOrders);
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

    private void loadOrders() {
        progressBarOrders.setVisibility(View.VISIBLE);
        layoutEmptyOrders.setVisibility(View.GONE);
        recyclerOrders.setVisibility(View.GONE);

        orderRepository.listenToUserOrders(currentUserId, new OrderRepository.OrdersCallback() {
            @Override
            public void onOrdersLoaded(List<Order> orders) {
                progressBarOrders.setVisibility(View.GONE);
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
                progressBarOrders.setVisibility(View.GONE);
                Toast.makeText(MyOrdersActivity.this, "Error loading orders: " + errorMessage, Toast.LENGTH_SHORT).show();
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
    protected void onDestroy() {
        super.onDestroy();
        if (orderRepository != null) {
            orderRepository.detachListener();
        }
    }
}
