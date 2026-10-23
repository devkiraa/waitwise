package com.example.waitwise.activities;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.waitwise.MainActivity;
import com.example.waitwise.R;
import com.example.waitwise.models.Order;
import com.example.waitwise.utils.PriceUtils;
import com.google.android.material.button.MaterialButton;

public class OrderConfirmationActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_confirmation);

        Order order;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            order = getIntent().getSerializableExtra("order", Order.class);
        } else {
            order = (Order) getIntent().getSerializableExtra("order");
        }

        TextView tvConfirmationToken = findViewById(R.id.tvConfirmationToken);
        TextView tvConfirmationStatus = findViewById(R.id.tvConfirmationStatus);
        TextView tvConfirmationOrderId = findViewById(R.id.tvConfirmationOrderId);
        TextView tvConfirmationPayment = findViewById(R.id.tvConfirmationPayment);
        TextView tvConfirmationSubtotal = findViewById(R.id.tvConfirmationSubtotal);
        MaterialButton btnViewTokens = findViewById(R.id.btnViewTokens);
        MaterialButton btnBackToHome = findViewById(R.id.btnBackToHome);

        if (order != null) {
            tvConfirmationToken.setText("#" + order.getTokenNumber());
            tvConfirmationStatus.setText("STATUS: " + order.getOrderStatus());
            tvConfirmationOrderId.setText("Order ID: " + order.getOrderId());
            tvConfirmationPayment.setText("Payment: " + order.getPaymentMethod() + " (" + order.getPaymentStatus() + ")");
            tvConfirmationSubtotal.setText("Total Amount: " + PriceUtils.formatPrice(order.getSubtotal()));
        }

        btnViewTokens.setOnClickListener(v -> {
            Intent intent = new Intent(OrderConfirmationActivity.this, MyTokensActivity.class);
            startActivity(intent);
            finish();
        });

        btnBackToHome.setOnClickListener(v -> {
            Intent intent = new Intent(OrderConfirmationActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });
    }
}
