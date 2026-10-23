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
import com.example.waitwise.models.FoodItem;
import com.example.waitwise.utils.ImageLoader;
import com.example.waitwise.utils.PriceUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.materialswitch.MaterialSwitch;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AdminProductAdapter extends RecyclerView.Adapter<AdminProductAdapter.AdminProductViewHolder> {

    public interface OnProductAdminListener {
        void onEdit(FoodItem item);
        void onDelete(FoodItem item);
        void onToggleAvailability(FoodItem item, boolean newAvailable);
    }

    private final List<FoodItem> items = new ArrayList<>();
    private final OnProductAdminListener listener;

    public AdminProductAdapter(OnProductAdminListener listener) {
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
    public AdminProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_product, parent, false);
        return new AdminProductViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AdminProductViewHolder holder, int position) {
        FoodItem item = items.get(position);
        holder.bind(item, listener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class AdminProductViewHolder extends RecyclerView.ViewHolder {
        private final ShapeableImageView imgAdminProduct;
        private final TextView tvAdminProductCategory;
        private final TextView tvAdminProductStatus;
        private final TextView tvAdminProductName;
        private final TextView tvAdminProductDesc;
        private final TextView tvAdminProductPrice;
        private final MaterialSwitch switchAdminAvailable;
        private final MaterialButton btnEditAdminProduct;
        private final MaterialButton btnDeleteAdminProduct;

        AdminProductViewHolder(@NonNull View itemView) {
            super(itemView);
            imgAdminProduct = itemView.findViewById(R.id.imgAdminProduct);
            tvAdminProductCategory = itemView.findViewById(R.id.tvAdminProductCategory);
            tvAdminProductStatus = itemView.findViewById(R.id.tvAdminProductStatus);
            tvAdminProductName = itemView.findViewById(R.id.tvAdminProductName);
            tvAdminProductDesc = itemView.findViewById(R.id.tvAdminProductDesc);
            tvAdminProductPrice = itemView.findViewById(R.id.tvAdminProductPrice);
            switchAdminAvailable = itemView.findViewById(R.id.switchAdminAvailable);
            btnEditAdminProduct = itemView.findViewById(R.id.btnEditAdminProduct);
            btnDeleteAdminProduct = itemView.findViewById(R.id.btnDeleteAdminProduct);
        }

        void bind(FoodItem food, OnProductAdminListener listener) {
            tvAdminProductName.setText(food.getName());
            tvAdminProductDesc.setText(food.getDescription());
            tvAdminProductPrice.setText(PriceUtils.formatPrice(food.getPrice()));
            tvAdminProductCategory.setText(food.getCategory().toUpperCase(Locale.ROOT));

            boolean available = food.isAvailable();
            // Prevent recursive listener trigger while binding
            switchAdminAvailable.setOnCheckedChangeListener(null);
            switchAdminAvailable.setChecked(available);

            applyStatusStyle(tvAdminProductStatus, available);

            ImageLoader.getInstance().loadImage(food.getImageUrl(), imgAdminProduct, R.drawable.ic_launcher_foreground);

            switchAdminAvailable.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (listener != null) {
                    listener.onToggleAvailability(food, isChecked);
                }
            });

            btnEditAdminProduct.setOnClickListener(v -> {
                if (listener != null) listener.onEdit(food);
            });

            btnDeleteAdminProduct.setOnClickListener(v -> {
                if (listener != null) listener.onDelete(food);
            });
        }

        private void applyStatusStyle(TextView badge, boolean available) {
            int bgColor = available ? Color.parseColor("#E8EFE8") : Color.parseColor("#FFEBEE");
            int textColor = available ? Color.parseColor("#246B4A") : Color.parseColor("#C62828");

            badge.setText(available ? "AVAILABLE" : "UNAVAILABLE");

            GradientDrawable shape = new GradientDrawable();
            shape.setShape(GradientDrawable.RECTANGLE);
            shape.setCornerRadius(12);
            shape.setColor(bgColor);
            badge.setBackground(shape);
            badge.setTextColor(textColor);
        }
    }
}
