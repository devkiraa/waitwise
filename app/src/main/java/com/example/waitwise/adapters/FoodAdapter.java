package com.example.waitwise.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.waitwise.R;
import com.example.waitwise.models.FoodItem;
import com.example.waitwise.utils.ImageLoader;
import com.example.waitwise.utils.PriceUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.imageview.ShapeableImageView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class FoodAdapter extends RecyclerView.Adapter<FoodAdapter.FoodViewHolder> {

    public interface OnFoodItemClickListener {
        void onAddToCart(FoodItem food);
        void onItemClick(FoodItem food);
    }

    private final List<FoodItem> items = new ArrayList<>();
    private final OnFoodItemClickListener listener;

    public FoodAdapter(OnFoodItemClickListener listener) {
        this.listener = listener;
    }

    public void setItems(List<FoodItem> newItems) {
        this.items.clear();
        if (newItems != null) {
            this.items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public FoodViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_food, parent, false);
        return new FoodViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FoodViewHolder holder, int position) {
        FoodItem item = items.get(position);
        holder.bind(item, listener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class FoodViewHolder extends RecyclerView.ViewHolder {
        private final MaterialCardView cardFoodItem;
        private final ShapeableImageView imgFood;
        private final TextView tvCategoryBadge;
        private final TextView tvUnavailableBadge;
        private final TextView tvFoodName;
        private final TextView tvFoodDescription;
        private final TextView tvFoodPrice;
        private final MaterialButton btnAddToCart;

        FoodViewHolder(@NonNull View itemView) {
            super(itemView);
            cardFoodItem = itemView.findViewById(R.id.cardFoodItem);
            imgFood = itemView.findViewById(R.id.imgFood);
            tvCategoryBadge = itemView.findViewById(R.id.tvCategoryBadge);
            tvUnavailableBadge = itemView.findViewById(R.id.tvUnavailableBadge);
            tvFoodName = itemView.findViewById(R.id.tvFoodName);
            tvFoodDescription = itemView.findViewById(R.id.tvFoodDescription);
            tvFoodPrice = itemView.findViewById(R.id.tvFoodPrice);
            btnAddToCart = itemView.findViewById(R.id.btnAddToCart);
        }

        void bind(FoodItem food, OnFoodItemClickListener listener) {
            tvFoodName.setText(food.getName());
            tvFoodDescription.setText(food.getDescription());
            tvFoodPrice.setText(PriceUtils.formatPrice(food.getPrice()));

            String category = food.getCategory();
            tvCategoryBadge.setText(category.toUpperCase(Locale.ROOT));

            boolean isAvailable = food.isAvailable();
            if (isAvailable) {
                tvUnavailableBadge.setVisibility(View.GONE);
                btnAddToCart.setEnabled(true);
                btnAddToCart.setAlpha(1.0f);
                cardFoodItem.setAlpha(1.0f);
            } else {
                tvUnavailableBadge.setVisibility(View.VISIBLE);
                btnAddToCart.setEnabled(false);
                btnAddToCart.setAlpha(0.4f);
                cardFoodItem.setAlpha(0.6f);
            }

            // Load food image if available
            ImageLoader.getInstance().loadImage(food.getImageUrl(), imgFood, R.drawable.ic_launcher_foreground);

            btnAddToCart.setOnClickListener(v -> {
                if (listener != null && food.isAvailable()) {
                    listener.onAddToCart(food);
                }
            });

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemClick(food);
                }
            });
        }
    }
}
