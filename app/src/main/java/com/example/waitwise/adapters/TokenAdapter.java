package com.example.waitwise.adapters;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.waitwise.R;
import com.example.waitwise.models.Order;
import com.example.waitwise.models.OrderItem;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TokenAdapter extends RecyclerView.Adapter<TokenAdapter.TokenViewHolder> {

    public interface OnTokenClickListener {
        void onTokenClick(Order order);
    }

    private final List<Order> tokens = new ArrayList<>();
    private final OnTokenClickListener listener;
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());

    public TokenAdapter(OnTokenClickListener listener) {
        this.listener = listener;
    }

    public void setTokens(List<Order> newTokens) {
        this.tokens.clear();
        if (newTokens != null) {
            this.tokens.addAll(newTokens);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TokenViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_token, parent, false);
        return new TokenViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TokenViewHolder holder, int position) {
        Order order = tokens.get(position);
        holder.bind(order, listener, timeFormat);
    }

    @Override
    public int getItemCount() {
        return tokens.size();
    }

    static class TokenViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvTokenReadyBanner;
        private final TextView tvTokenNumber;
        private final TextView tvTokenStatusBadge;
        private final TextView tvTokenItems;
        private final TextView tvTokenTime;

        TokenViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTokenReadyBanner = itemView.findViewById(R.id.tvTokenReadyBanner);
            tvTokenNumber = itemView.findViewById(R.id.tvTokenNumber);
            tvTokenStatusBadge = itemView.findViewById(R.id.tvTokenStatusBadge);
            tvTokenItems = itemView.findViewById(R.id.tvTokenItems);
            tvTokenTime = itemView.findViewById(R.id.tvTokenTime);
        }

        void bind(Order order, OnTokenClickListener listener, SimpleDateFormat timeFormat) {
            tvTokenNumber.setText("#" + order.getTokenNumber());
            tvTokenStatusBadge.setText(order.getOrderStatus());

            // Banner visibility for READY status
            boolean isReady = Order.STATUS_READY.equalsIgnoreCase(order.getOrderStatus());
            tvTokenReadyBanner.setVisibility(isReady ? View.VISIBLE : View.GONE);

            // Time string
            String timeStr = order.getCreatedAt() > 0 ? timeFormat.format(new Date(order.getCreatedAt())) : "";
            tvTokenTime.setText("Ordered at " + timeStr + " • " + order.getPaymentMethod());

            // Items breakdown
            StringBuilder sb = new StringBuilder();
            Map<String, OrderItem> items = order.getItems();
            if (items != null && !items.isEmpty()) {
                int i = 0;
                for (OrderItem item : items.values()) {
                    if (i > 0) sb.append(", ");
                    sb.append(item.getName()).append(" × ").append(item.getQuantity());
                    i++;
                }
            } else {
                sb.append("Details unavailable");
            }
            tvTokenItems.setText(sb.toString());

            // Status Badge styling
            applyStatusStyle(tvTokenStatusBadge, order.getOrderStatus());

            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onTokenClick(order);
            });
        }

        private void applyStatusStyle(TextView badge, String status) {
            int bgColor;
            int textColor;

            switch (status != null ? status : "") {
                case Order.STATUS_READY:
                    bgColor = Color.parseColor("#E8F5E9");
                    textColor = Color.parseColor("#2E7D32");
                    break;
                case Order.STATUS_PREPARING:
                case Order.STATUS_ACCEPTED:
                    bgColor = Color.parseColor("#FFF3E0");
                    textColor = Color.parseColor("#E65100");
                    break;
                case Order.STATUS_CANCELLED:
                    bgColor = Color.parseColor("#FFEBEE");
                    textColor = Color.parseColor("#C62828");
                    break;
                case Order.STATUS_COLLECTED:
                    bgColor = Color.parseColor("#ECEFF1");
                    textColor = Color.parseColor("#546E7A");
                    break;
                case Order.STATUS_PLACED:
                default:
                    bgColor = Color.parseColor("#E8EFE8");
                    textColor = Color.parseColor("#246B4A");
                    break;
            }

            GradientDrawable shape = new GradientDrawable();
            shape.setShape(GradientDrawable.RECTANGLE);
            shape.setCornerRadius(16);
            shape.setColor(bgColor);
            badge.setBackground(shape);
            badge.setTextColor(textColor);
        }
    }
}
