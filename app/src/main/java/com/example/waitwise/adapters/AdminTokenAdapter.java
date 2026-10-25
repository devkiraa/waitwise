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
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AdminTokenAdapter extends RecyclerView.Adapter<AdminTokenAdapter.AdminTokenViewHolder> {

    public interface OnTokenActionListener {
        void onUpdateStatus(Order order, String nextStatus);
    }

    private final List<Order> tokens = new ArrayList<>();
    private final OnTokenActionListener listener;
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());

    public AdminTokenAdapter(OnTokenActionListener listener) {
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
    public AdminTokenViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_token, parent, false);
        return new AdminTokenViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AdminTokenViewHolder holder, int position) {
        Order order = tokens.get(position);
        holder.bind(order, listener, timeFormat);
    }

    @Override
    public int getItemCount() {
        return tokens.size();
    }

    static class AdminTokenViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvAdminTokenLargeNum;
        private final TextView tvAdminTokenStatusPill;
        private final TextView tvAdminTokenItemsList;
        private final TextView tvAdminTokenTimeSub;
        private final MaterialButton btnTokenAction;

        AdminTokenViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAdminTokenLargeNum = itemView.findViewById(R.id.tvAdminTokenLargeNum);
            tvAdminTokenStatusPill = itemView.findViewById(R.id.tvAdminTokenStatusPill);
            tvAdminTokenItemsList = itemView.findViewById(R.id.tvAdminTokenItemsList);
            tvAdminTokenTimeSub = itemView.findViewById(R.id.tvAdminTokenTimeSub);
            btnTokenAction = itemView.findViewById(R.id.btnTokenAction);
        }

        void bind(Order order, OnTokenActionListener listener, SimpleDateFormat timeFormat) {
            tvAdminTokenLargeNum.setText("#" + order.getTokenNumber());
            tvAdminTokenStatusPill.setText(order.getOrderStatus());

            String timeStr = order.getCreatedAt() > 0 ? timeFormat.format(new Date(order.getCreatedAt())) : "";
            tvAdminTokenTimeSub.setText(timeStr + " • " + order.getPaymentMethod() + " (" + order.getPaymentStatus() + ")");

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
                sb.append("Order details unavailable");
            }
            tvAdminTokenItemsList.setText(sb.toString());

            applyStatusStyle(tvAdminTokenStatusPill, order.getOrderStatus());

            String currentStatus = order.getOrderStatus();
            String targetStatus = null;
            String buttonLabel = null;

            if (Order.STATUS_READY.equalsIgnoreCase(currentStatus)) {
                targetStatus = Order.STATUS_COLLECTED;
                buttonLabel = "Mark Collected";
            } else if (Order.STATUS_PLACED.equalsIgnoreCase(currentStatus)
                    || Order.STATUS_ACCEPTED.equalsIgnoreCase(currentStatus)
                    || Order.STATUS_PREPARING.equalsIgnoreCase(currentStatus)) {
                targetStatus = Order.STATUS_READY;
                buttonLabel = "Mark Ready for Pickup";
            }

            if (targetStatus != null) {
                btnTokenAction.setVisibility(View.VISIBLE);
                btnTokenAction.setText(buttonLabel);
                final String finalTarget = targetStatus;
                btnTokenAction.setOnClickListener(v -> {
                    if (listener != null) listener.onUpdateStatus(order, finalTarget);
                });
            } else {
                btnTokenAction.setVisibility(View.GONE);
            }
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
            shape.setCornerRadius(12);
            shape.setColor(bgColor);
            badge.setBackground(shape);
            badge.setTextColor(textColor);
        }
    }
}
