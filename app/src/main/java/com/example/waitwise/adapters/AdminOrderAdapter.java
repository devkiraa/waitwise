package com.example.waitwise.adapters;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.example.waitwise.R;
import com.example.waitwise.models.Order;
import com.example.waitwise.models.OrderItem;
import com.example.waitwise.utils.PriceUtils;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AdminOrderAdapter extends RecyclerView.Adapter<AdminOrderAdapter.AdminOrderViewHolder> {

    public interface OnOrderAdminActionListener {
        void onAdvanceStatus(Order order, String nextStatus);
        void onCancelOrder(Order order);
        void onUpdatePrepTime(Order order, int minutes);
    }

    private final List<Order> orders = new ArrayList<>();
    private final OnOrderAdminActionListener listener;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault());

    public AdminOrderAdapter(OnOrderAdminActionListener listener) {
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
    public AdminOrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_order, parent, false);
        return new AdminOrderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AdminOrderViewHolder holder, int position) {
        Order order = orders.get(position);
        holder.bind(order, listener, dateFormat);
    }

    @Override
    public int getItemCount() {
        return orders.size();
    }

    static class AdminOrderViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvAdminOrderToken;
        private final TextView tvAdminOrderStatus;
        private final TextView tvAdminOrderMeta;
        private final TextView tvAdminOrderPrepTime;
        private final TextView tvAdminOrderItems;
        private final TextView tvAdminOrderPayment;
        private final TextView tvAdminOrderTotal;
        private final LinearLayout layoutAdminOrderActions;
        private final MaterialButton btnAdvanceStatus;
        private final MaterialButton btnSetPrepTime;
        private final MaterialButton btnCancelOrder;

        AdminOrderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAdminOrderToken = itemView.findViewById(R.id.tvAdminOrderToken);
            tvAdminOrderStatus = itemView.findViewById(R.id.tvAdminOrderStatus);
            tvAdminOrderMeta = itemView.findViewById(R.id.tvAdminOrderMeta);
            tvAdminOrderPrepTime = itemView.findViewById(R.id.tvAdminOrderPrepTime);
            tvAdminOrderItems = itemView.findViewById(R.id.tvAdminOrderItems);
            tvAdminOrderPayment = itemView.findViewById(R.id.tvAdminOrderPayment);
            tvAdminOrderTotal = itemView.findViewById(R.id.tvAdminOrderTotal);
            layoutAdminOrderActions = itemView.findViewById(R.id.layoutAdminOrderActions);
            btnAdvanceStatus = itemView.findViewById(R.id.btnAdvanceStatus);
            btnSetPrepTime = itemView.findViewById(R.id.btnSetPrepTime);
            btnCancelOrder = itemView.findViewById(R.id.btnCancelOrder);
        }

        void bind(Order order, OnOrderAdminActionListener listener, SimpleDateFormat dateFormat) {
            tvAdminOrderToken.setText("TOKEN #" + order.getTokenNumber());
            tvAdminOrderStatus.setText(order.getOrderStatus());

            String timeStr = order.getCreatedAt() > 0 ? dateFormat.format(new Date(order.getCreatedAt())) : "";
            tvAdminOrderMeta.setText("Order #" + order.getOrderId() + " • " + timeStr);

            String estStr = order.getFormattedPrepTimeEstimate();
            if (estStr != null && !estStr.isEmpty()) {
                tvAdminOrderPrepTime.setVisibility(View.VISIBLE);
                tvAdminOrderPrepTime.setText(estStr);
            } else {
                tvAdminOrderPrepTime.setVisibility(View.GONE);
            }

            tvAdminOrderPayment.setText(order.getPaymentMethod() + " (" + order.getPaymentStatus() + ")");
            tvAdminOrderTotal.setText(PriceUtils.formatPrice(order.getSubtotal()));

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
            tvAdminOrderItems.setText(sb.toString());

            applyStatusStyle(tvAdminOrderStatus, order.getOrderStatus());

            // Determine status advancement
            String currentStatus = order.getOrderStatus();
            String nextStatus = null;
            String actionLabel = null;

            switch (currentStatus != null ? currentStatus : "") {
                case Order.STATUS_PLACED:
                    nextStatus = Order.STATUS_ACCEPTED;
                    actionLabel = "Accept Order";
                    break;
                case Order.STATUS_ACCEPTED:
                    nextStatus = Order.STATUS_PREPARING;
                    actionLabel = "Start Prep";
                    break;
                case Order.STATUS_PREPARING:
                    nextStatus = Order.STATUS_READY;
                    actionLabel = "Mark Ready";
                    break;
                case Order.STATUS_READY:
                    nextStatus = Order.STATUS_COLLECTED;
                    actionLabel = "Mark Collected";
                    break;
                case Order.STATUS_COLLECTED:
                case Order.STATUS_CANCELLED:
                default:
                    nextStatus = null;
                    break;
            }

            if (nextStatus != null) {
                layoutAdminOrderActions.setVisibility(View.VISIBLE);
                btnAdvanceStatus.setText(actionLabel);

                final String targetStatus = nextStatus;
                btnAdvanceStatus.setOnClickListener(v -> {
                    if (listener != null) listener.onAdvanceStatus(order, targetStatus);
                });

                btnSetPrepTime.setOnClickListener(v -> {
                    String[] options = {"5 Minutes", "10 Minutes", "15 Minutes", "20 Minutes", "Clear Estimate"};
                    int[] minutes = {5, 10, 15, 20, 0};

                    new AlertDialog.Builder(v.getContext())
                            .setTitle("Set Estimated Prep Time")
                            .setItems(options, (dialog, which) -> {
                                if (listener != null) {
                                    listener.onUpdatePrepTime(order, minutes[which]);
                                }
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
                });

                // Can cancel if not yet collected or cancelled
                btnCancelOrder.setVisibility(View.VISIBLE);
                btnCancelOrder.setOnClickListener(v -> {
                    if (listener != null) listener.onCancelOrder(order);
                });
            } else {
                layoutAdminOrderActions.setVisibility(View.GONE);
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
