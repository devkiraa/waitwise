package com.example.waitwise.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.waitwise.R;
import com.example.waitwise.models.CartItem;
import com.example.waitwise.models.FoodItem;
import com.example.waitwise.utils.PriceUtils;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class CartAdapter extends RecyclerView.Adapter<CartAdapter.CartViewHolder> {

    public interface CartItemListener {
        void onIncreaseQuantity(CartItem item);
        void onDecreaseQuantity(CartItem item);
        void onRemoveItem(CartItem item);
    }

    private final List<CartItem> items = new ArrayList<>();
    private final CartItemListener listener;

    public CartAdapter(CartItemListener listener) {
        this.listener = listener;
    }

    public void setItems(List<CartItem> newItems) {
        this.items.clear();
        if (newItems != null) {
            this.items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CartViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_cart, parent, false);
        return new CartViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CartViewHolder holder, int position) {
        CartItem cartItem = items.get(position);
        holder.bind(cartItem, listener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class CartViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvCartFoodName;
        private final TextView tvCartUnitPrice;
        private final TextView tvCartLineTotal;
        private final TextView tvQuantity;
        private final MaterialButton btnMinus;
        private final MaterialButton btnPlus;
        private final MaterialButton btnRemoveItem;

        CartViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCartFoodName = itemView.findViewById(R.id.tvCartFoodName);
            tvCartUnitPrice = itemView.findViewById(R.id.tvCartUnitPrice);
            tvCartLineTotal = itemView.findViewById(R.id.tvCartLineTotal);
            tvQuantity = itemView.findViewById(R.id.tvQuantity);
            btnMinus = itemView.findViewById(R.id.btnMinus);
            btnPlus = itemView.findViewById(R.id.btnPlus);
            btnRemoveItem = itemView.findViewById(R.id.btnRemoveItem);
        }

        void bind(CartItem cartItem, CartItemListener listener) {
            FoodItem food = cartItem.getFoodItem();
            String name = food != null ? food.getName() : "Item (" + cartItem.getFoodId() + ")";
            double unitPrice = food != null ? food.getPrice() : 0.0;
            double lineTotal = PriceUtils.calculateLineTotal(unitPrice, cartItem.getQuantity());

            tvCartFoodName.setText(name);
            tvCartUnitPrice.setText(PriceUtils.formatPrice(unitPrice) + " each");
            tvCartLineTotal.setText(PriceUtils.formatPrice(lineTotal));
            tvQuantity.setText(String.valueOf(cartItem.getQuantity()));

            btnMinus.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDecreaseQuantity(cartItem);
                }
            });

            btnPlus.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onIncreaseQuantity(cartItem);
                }
            });

            btnRemoveItem.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onRemoveItem(cartItem);
                }
            });
        }
    }
}
