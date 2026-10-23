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
import com.example.waitwise.utils.PriceUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.OrderViewHolder> {

    public interface OnOrderClickListener {
        void onOrderClick(Order order);
    }

    private final List<Order> orders = new ArrayList<>();
    private final OnOrderClickListener listener;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault());

    public OrderAdapter(OnOrderClickListener listener) {
        this.listener = listener;
    }

    public void setOrders(List<Order> newOrders) {
        this.orders.clear();
        if (newOrders != null) {
            this.orders.addAll(newOrders);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_order, parent, false);
        return new OrderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderViewHolder holder, int position) {
        Order order = orders.get(position);
        holder.bind(order, listener, dateFormat);
    }

    @Override
    public int getItemCount() {
        return orders.size();
    }

    static class OrderViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvOrderTokenBadge;
        private final TextView tvOrderStatusBadge;
        private final TextView tvOrderDate;
        private final TextView tvOrderId;
        private final TextView tvOrderItemsSummary;
        private final TextView tvOrderPaymentDetails;
        private final TextView tvOrderTotal;

        OrderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvOrderTokenBadge = itemView.findViewById(R.id.tvOrderTokenBadge);
            tvOrderStatusBadge = itemView.findViewById(R.id.tvOrderStatusBadge);
            tvOrderDate = itemView.findViewById(R.id.tvOrderDate);
            tvOrderId = itemView.findViewById(R.id.tvOrderId);
            tvOrderItemsSummary = itemView.findViewById(R.id.tvOrderItemsSummary);
            tvOrderPaymentDetails = itemView.findViewById(R.id.tvOrderPaymentDetails);
            tvOrderTotal = itemView.findViewById(R.id.tvOrderTotal);
        }

        void bind(Order order, OnOrderClickListener listener, SimpleDateFormat dateFormat) {
            tvOrderTokenBadge.setText("TOKEN #" + order.getTokenNumber());
            tvOrderStatusBadge.setText(order.getOrderStatus());

            if (order.getCreatedAt() > 0) {
                tvOrderDate.setText(dateFormat.format(new Date(order.getCreatedAt())));
            } else {
                tvOrderDate.setText("-");
            }

            tvOrderId.setText("Order ID: " + order.getOrderId());
            tvOrderPaymentDetails.setText(order.getPaymentMethod() + " (" + order.getPaymentStatus() + ")");
            tvOrderTotal.setText(PriceUtils.formatPrice(order.getSubtotal()));

            // Build items text
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
                sb.append("No items listed");
            }
            tvOrderItemsSummary.setText(sb.toString());

            // Status Badge styling
            applyStatusStyle(tvOrderStatusBadge, order.getOrderStatus());

            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onOrderClick(order);
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
