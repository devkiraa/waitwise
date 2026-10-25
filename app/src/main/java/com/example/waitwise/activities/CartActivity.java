package com.example.waitwise.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.waitwise.R;
import com.example.waitwise.adapters.CartAdapter;
import com.example.waitwise.models.CartItem;
import com.example.waitwise.models.FoodItem;
import com.example.waitwise.repositories.CartRepository;
import com.example.waitwise.repositories.MenuRepository;
import com.example.waitwise.utils.EdgeToEdgeUtils;
import com.example.waitwise.utils.FirebaseManager;
import com.example.waitwise.utils.PopupUtils;
import com.example.waitwise.utils.PriceUtils;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class CartActivity extends AppCompatActivity implements CartAdapter.CartItemListener {

    private ShimmerFrameLayout shimmerCart;
    private LinearLayout layoutEmptyCart;
    private RecyclerView recyclerCart;
    private MaterialCardView cardCheckoutSummary;
    private TextView tvCartSubtotal;
    private MaterialButton btnProceedCheckout;
    private MaterialButton btnClearCart;

    private CartAdapter cartAdapter;
    private CartRepository cartRepository;
    private MenuRepository menuRepository;

    private final List<CartItem> cartItems = new ArrayList<>();
    private final Map<String, FoodItem> menuMap = new HashMap<>();
    private double currentSubtotal = 0.0;
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

        setContentView(R.layout.activity_cart);
        EdgeToEdgeUtils.applyEdgeToEdge(this, findViewById(R.id.cartRoot));

        cartRepository = new CartRepository();
        menuRepository = new MenuRepository();

        initializeViews();
        setupRecyclerView();
        loadMenuAndCart();
    }

    private void initializeViews() {
        shimmerCart = findViewById(R.id.shimmerCart);
        layoutEmptyCart = findViewById(R.id.layoutEmptyCart);
        recyclerCart = findViewById(R.id.recyclerCart);
        cardCheckoutSummary = findViewById(R.id.cardCheckoutSummary);
        tvCartSubtotal = findViewById(R.id.tvCartSubtotal);
        btnProceedCheckout = findViewById(R.id.btnProceedCheckout);
        btnClearCart = findViewById(R.id.btnClearCart);

        findViewById(R.id.btnBackCart).setOnClickListener(v -> finish());
        findViewById(R.id.btnExploreMenu).setOnClickListener(v -> finish());

        btnClearCart.setOnClickListener(v -> {
            if (cartItems.isEmpty()) return;
            cartRepository.clearCart(currentUserId, new CartRepository.CartActionCallback() {
                @Override
                public void onSuccess() {
                    PopupUtils.showPopup(CartActivity.this, "Cart Cleared", "All items have been removed from your cart.");
                }

                @Override
                public void onError(String errorMessage) {
                    PopupUtils.showPopup(CartActivity.this, "Error", "Failed to clear cart: " + errorMessage);
                }
            });
        });

        btnProceedCheckout.setOnClickListener(v -> {
            if (cartItems.isEmpty()) {
                PopupUtils.showPopup(this, "Empty Cart", "Your cart is currently empty.");
                return;
            }
            Intent intent = new Intent(CartActivity.this, CheckoutActivity.class);
            startActivity(intent);
        });
    }

    private void setupRecyclerView() {
        cartAdapter = new CartAdapter(this);
        recyclerCart.setLayoutManager(new LinearLayoutManager(this));
        recyclerCart.setAdapter(cartAdapter);
    }

    private void loadMenuAndCart() {
        if (shimmerCart != null) {
            shimmerCart.startShimmer();
            shimmerCart.setVisibility(View.VISIBLE);
        }

        AtomicBoolean loaded = new AtomicBoolean(false);
        Handler handler = new Handler(Looper.getMainLooper());
        Runnable timeoutRunnable = () -> {
            if (loaded.compareAndSet(false, true)) {
                if (shimmerCart != null) {
                    shimmerCart.stopShimmer();
                    shimmerCart.setVisibility(View.GONE);
                }
                updateUI();
            }
        };
        handler.postDelayed(timeoutRunnable, 3500);

        menuRepository.listenToMenu(new MenuRepository.MenuCallback() {
            @Override
            public void onMenuLoaded(List<FoodItem> items) {
                menuMap.clear();
                for (FoodItem item : items) {
                    menuMap.put(item.getId(), item);
                }

                cartRepository.listenToUserCart(currentUserId, new CartRepository.CartCallback() {
                    @Override
                    public void onCartLoaded(List<CartItem> items) {
                        if (loaded.compareAndSet(false, true)) {
                            handler.removeCallbacks(timeoutRunnable);
                        }
                        if (shimmerCart != null) {
                            shimmerCart.stopShimmer();
                            shimmerCart.setVisibility(View.GONE);
                        }
                        cartItems.clear();

                        double subtotal = 0.0;
                        for (CartItem ci : items) {
                            FoodItem food = menuMap.get(ci.getFoodId());
                            if (food != null) {
                                ci.setFoodItem(food);
                                cartItems.add(ci);
                                double lineTotal = PriceUtils.calculateLineTotal(food.getPrice(), ci.getQuantity());
                                subtotal = PriceUtils.add(subtotal, lineTotal);
                            }
                        }

                        currentSubtotal = subtotal;
                        updateUI();
                    }

                    @Override
                    public void onError(String errorMessage) {
                        if (loaded.compareAndSet(false, true)) {
                            handler.removeCallbacks(timeoutRunnable);
                        }
                        if (shimmerCart != null) {
                            shimmerCart.stopShimmer();
                            shimmerCart.setVisibility(View.GONE);
                        }
                        updateUI();
                        PopupUtils.showPopup(CartActivity.this, "Cart Error", errorMessage);
                    }
                });
            }

            @Override
            public void onError(String errorMessage) {
                if (loaded.compareAndSet(false, true)) {
                    handler.removeCallbacks(timeoutRunnable);
                }
                if (shimmerCart != null) {
                    shimmerCart.stopShimmer();
                    shimmerCart.setVisibility(View.GONE);
                }
                updateUI();
                PopupUtils.showPopup(CartActivity.this, "Menu Error", errorMessage);
            }
        });
    }

    private void updateUI() {
        cartAdapter.setItems(cartItems);
        tvCartSubtotal.setText(PriceUtils.formatPrice(currentSubtotal));

        if (cartItems.isEmpty()) {
            layoutEmptyCart.setVisibility(View.VISIBLE);
            recyclerCart.setVisibility(View.GONE);
            cardCheckoutSummary.setVisibility(View.GONE);
            btnClearCart.setVisibility(View.GONE);
        } else {
            layoutEmptyCart.setVisibility(View.GONE);
            recyclerCart.setVisibility(View.VISIBLE);
            cardCheckoutSummary.setVisibility(View.VISIBLE);
            btnClearCart.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onIncreaseQuantity(CartItem item) {
        cartRepository.updateQuantity(currentUserId, item.getFoodId(), item.getQuantity() + 1, null);
    }

    @Override
    public void onDecreaseQuantity(CartItem item) {
        int newQty = item.getQuantity() - 1;
        cartRepository.updateQuantity(currentUserId, item.getFoodId(), newQty, null);
    }

    @Override
    public void onRemoveItem(CartItem item) {
        cartRepository.removeFromCart(currentUserId, item.getFoodId(), null);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cartRepository != null) {
            cartRepository.detachListener();
        }
        if (menuRepository != null) {
            menuRepository.detachListener();
        }
    }
}
