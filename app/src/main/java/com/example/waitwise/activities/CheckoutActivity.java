package com.example.waitwise.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.waitwise.R;
import com.example.waitwise.models.CartItem;
import com.example.waitwise.models.FoodItem;
import com.example.waitwise.models.Order;
import com.example.waitwise.models.OrderItem;
import com.example.waitwise.repositories.CartRepository;
import com.example.waitwise.repositories.MenuRepository;
import com.example.waitwise.repositories.OrderRepository;
import com.example.waitwise.utils.FirebaseManager;
import com.example.waitwise.utils.PopupUtils;
import com.example.waitwise.utils.PriceUtils;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CheckoutActivity extends AppCompatActivity {

    private LinearLayout layoutOrderItemsContainer;
    private TextView tvCheckoutSubtotal;
    private RadioGroup radioGroupPayment;
    private RadioButton rbUpi;
    private ShimmerFrameLayout shimmerCheckout;
    private MaterialButton btnPlaceOrder;

    private CartRepository cartRepository;
    private MenuRepository menuRepository;
    private OrderRepository orderRepository;

    private String currentUserId;
    private final List<CartItem> cartItems = new ArrayList<>();
    private final Map<String, FoodItem> menuMap = new HashMap<>();
    private double calculatedSubtotal = 0.0;
    private boolean isPlacingOrder = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        currentUserId = FirebaseManager.getInstance().getCurrentUserId();
        if (currentUserId == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_checkout);
        com.example.waitwise.utils.EdgeToEdgeUtils.applyEdgeToEdge(this, findViewById(R.id.checkoutRoot));

        cartRepository = new CartRepository();
        menuRepository = new MenuRepository();
        orderRepository = new OrderRepository();

        initializeViews();
        loadOrderData();
    }

    private void initializeViews() {
        layoutOrderItemsContainer = findViewById(R.id.layoutOrderItemsContainer);
        tvCheckoutSubtotal = findViewById(R.id.tvCheckoutSubtotal);
        radioGroupPayment = findViewById(R.id.radioGroupPayment);
        rbUpi = findViewById(R.id.rbUpi);
        shimmerCheckout = findViewById(R.id.shimmerCheckout);
        btnPlaceOrder = findViewById(R.id.btnPlaceOrder);

        findViewById(R.id.btnBackCheckout).setOnClickListener(v -> finish());

        radioGroupPayment.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbUpi) {
                showMockPaymentInfo("UPI Demonstration",
                        "This is a demonstration interface for project presentation. No UPI transactions will be executed.");
            }
        });

        btnPlaceOrder.setOnClickListener(v -> handlePlaceOrder());
    }

    private void showMockPaymentInfo(String title, String message) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("Understood", null)
                .show();
    }

    private void loadOrderData() {
        if (shimmerCheckout != null) {
            shimmerCheckout.startShimmer();
            shimmerCheckout.setVisibility(View.VISIBLE);
        }
        btnPlaceOrder.setEnabled(false);

        menuRepository.listenToMenu(new MenuRepository.MenuCallback() {
            @Override
            public void onMenuLoaded(List<FoodItem> items) {
                menuMap.clear();
                for (FoodItem fi : items) {
                    menuMap.put(fi.getId(), fi);
                }

                cartRepository.listenToUserCart(currentUserId, new CartRepository.CartCallback() {
                    @Override
                    public void onCartLoaded(List<CartItem> items) {
                        if (shimmerCheckout != null) {
                            shimmerCheckout.stopShimmer();
                            shimmerCheckout.setVisibility(View.GONE);
                        }
                        btnPlaceOrder.setEnabled(true);

                        cartItems.clear();
                        cartItems.addAll(items);
                        renderSummary();
                    }

                    @Override
                    public void onError(String errorMessage) {
                        if (shimmerCheckout != null) {
                            shimmerCheckout.stopShimmer();
                            shimmerCheckout.setVisibility(View.GONE);
                        }
                        PopupUtils.showPopup(CheckoutActivity.this, "Cart Error", errorMessage);
                    }
                });
            }

            @Override
            public void onError(String errorMessage) {
                if (shimmerCheckout != null) {
                    shimmerCheckout.stopShimmer();
                    shimmerCheckout.setVisibility(View.GONE);
                }
                PopupUtils.showPopup(CheckoutActivity.this, "Menu Error", errorMessage);
            }
        });
    }

    private void renderSummary() {
        layoutOrderItemsContainer.removeAllViews();
        calculatedSubtotal = 0.0;

        if (cartItems.isEmpty()) {
            PopupUtils.showPopup(this, "Empty Cart", "Your cart is currently empty.");
            finish();
            return;
        }

        for (CartItem ci : cartItems) {
            FoodItem food = menuMap.get(ci.getFoodId());
            if (food != null) {
                double lineTotal = PriceUtils.calculateLineTotal(food.getPrice(), ci.getQuantity());
                calculatedSubtotal = PriceUtils.add(calculatedSubtotal, lineTotal);

                LinearLayout row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setPadding(0, 6, 0, 6);

                TextView tvNameQty = new TextView(this);
                tvNameQty.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));
                tvNameQty.setText(food.getName() + " × " + ci.getQuantity());
                tvNameQty.setTextColor(getColor(R.color.ww_text_primary));
                tvNameQty.setTextSize(14);

                TextView tvPrice = new TextView(this);
                tvPrice.setText(PriceUtils.formatPrice(lineTotal));
                tvPrice.setTextColor(getColor(R.color.ww_primary));
                tvPrice.setTextSize(14);

                row.addView(tvNameQty);
                row.addView(tvPrice);
                layoutOrderItemsContainer.addView(row);
            }
        }

        tvCheckoutSubtotal.setText(PriceUtils.formatPrice(calculatedSubtotal));
    }

    private void handlePlaceOrder() {
        if (isPlacingOrder) return;

        if (cartItems.isEmpty()) {
            PopupUtils.showPopup(this, "Empty Cart", "Cannot place order: Cart is empty.");
            return;
        }

        // Validate food availability and verify prices
        Map<String, OrderItem> orderItemsMap = new HashMap<>();
        double verifiedSubtotal = 0.0;

        for (CartItem ci : cartItems) {
            FoodItem food = menuMap.get(ci.getFoodId());
            if (food == null) {
                PopupUtils.showPopup(this, "Item Missing", "Item " + ci.getFoodId() + " no longer exists in menu.");
                return;
            }
            if (!food.isAvailable()) {
                PopupUtils.showPopup(this, "Item Unavailable", food.getName() + " is currently unavailable. Please remove it from cart.");
                return;
            }

            double lineTotal = PriceUtils.calculateLineTotal(food.getPrice(), ci.getQuantity());
            verifiedSubtotal = PriceUtils.add(verifiedSubtotal, lineTotal);

            OrderItem orderItem = new OrderItem(
                    food.getId(),
                    food.getName(),
                    food.getPrice(),
                    ci.getQuantity(),
                    lineTotal
            );
            orderItemsMap.put(food.getId(), orderItem);
        }

        String paymentMethod = Order.PAYMENT_CASH;
        if (rbUpi != null && rbUpi.isChecked()) {
            paymentMethod = Order.PAYMENT_UPI_DEMO;
        }

        isPlacingOrder = true;
        if (shimmerCheckout != null) {
            shimmerCheckout.startShimmer();
            shimmerCheckout.setVisibility(View.VISIBLE);
        }
        btnPlaceOrder.setEnabled(false);

        Order newOrder = new Order();
        newOrder.setUserId(currentUserId);
        newOrder.setItems(orderItemsMap);
        newOrder.setSubtotal(verifiedSubtotal);
        newOrder.setPaymentMethod(paymentMethod);
        newOrder.setPaymentStatus(Order.PAYMENT_STATUS_PENDING);
        newOrder.setOrderStatus(Order.STATUS_PLACED);
        newOrder.setCreatedAt(System.currentTimeMillis());

        orderRepository.placeOrder(newOrder, new OrderRepository.PlaceOrderCallback() {
            @Override
            public void onSuccess(Order createdOrder) {
                // Clear user cart only after successful order creation
                cartRepository.clearCart(currentUserId, null);

                isPlacingOrder = false;
                if (shimmerCheckout != null) {
                    shimmerCheckout.stopShimmer();
                    shimmerCheckout.setVisibility(View.GONE);
                }

                Intent intent = new Intent(CheckoutActivity.this, OrderConfirmationActivity.class);
                intent.putExtra("order", createdOrder);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
            }

            @Override
            public void onError(String errorMessage) {
                isPlacingOrder = false;
                if (shimmerCheckout != null) {
                    shimmerCheckout.stopShimmer();
                    shimmerCheckout.setVisibility(View.GONE);
                }
                btnPlaceOrder.setEnabled(true);
                PopupUtils.showPopup(CheckoutActivity.this, "Order Failed", errorMessage);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cartRepository != null) cartRepository.detachListener();
        if (menuRepository != null) menuRepository.detachListener();
    }
}
