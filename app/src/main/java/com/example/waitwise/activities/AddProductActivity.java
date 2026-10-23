package com.example.waitwise.activities;

import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.waitwise.R;
import com.example.waitwise.models.FoodItem;
import com.example.waitwise.models.User;
import com.example.waitwise.repositories.MenuRepository;
import com.example.waitwise.repositories.UserRepository;
import com.example.waitwise.utils.FirebaseManager;
import com.example.waitwise.utils.ImageLoader;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.materialswitch.MaterialSwitch;

public class AddProductActivity extends AppCompatActivity {

    private TextView tvAddProductTitle;
    private ShapeableImageView imgProductPreview;
    private EditText etProductName;
    private EditText etProductDesc;
    private EditText etProductPrice;
    private AutoCompleteTextView actvCategory;
    private EditText etProductImageUrl;
    private MaterialSwitch switchProductAvailable;
    private ProgressBar progressBarAddProduct;
    private MaterialButton btnSaveProduct;
    private MaterialButton btnCancelProduct;

    private MenuRepository menuRepository;
    private UserRepository userRepository;
    private FoodItem editingFoodItem = null;
    private boolean isSaving = false;

    private static final String[] CATEGORIES = {"Breakfast", "Meals", "Snacks", "Drinks"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Security check
        if (!FirebaseManager.getInstance().isUserLoggedIn()) {
            finish();
            return;
        }

        userRepository = new UserRepository();
        userRepository.getUserRole(FirebaseManager.getInstance().getCurrentUserId(), new UserRepository.RoleCallback() {
            @Override
            public void onRoleLoaded(String role) {
                if (!User.ROLE_ADMIN.equalsIgnoreCase(role)) {
                    Toast.makeText(AddProductActivity.this, "Access denied.", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onError(String errorMessage) {
                finish();
            }
        });

        setContentView(R.layout.activity_add_product);

        menuRepository = new MenuRepository();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            editingFoodItem = getIntent().getSerializableExtra("food_item", FoodItem.class);
        } else {
            editingFoodItem = (FoodItem) getIntent().getSerializableExtra("food_item");
        }

        initializeViews();
        setupCategoryDropdown();
        setupImagePreviewWatcher();

        if (editingFoodItem != null) {
            populateExistingData(editingFoodItem);
        }
    }

    private void initializeViews() {
        tvAddProductTitle = findViewById(R.id.tvAddProductTitle);
        imgProductPreview = findViewById(R.id.imgProductPreview);
        etProductName = findViewById(R.id.etProductName);
        etProductDesc = findViewById(R.id.etProductDesc);
        etProductPrice = findViewById(R.id.etProductPrice);
        actvCategory = findViewById(R.id.actvCategory);
        etProductImageUrl = findViewById(R.id.etProductImageUrl);
        switchProductAvailable = findViewById(R.id.switchProductAvailable);
        progressBarAddProduct = findViewById(R.id.progressBarAddProduct);
        btnSaveProduct = findViewById(R.id.btnSaveProduct);
        btnCancelProduct = findViewById(R.id.btnCancelProduct);

        findViewById(R.id.btnBackAddProduct).setOnClickListener(v -> finish());
        btnCancelProduct.setOnClickListener(v -> finish());
        btnSaveProduct.setOnClickListener(v -> handleSaveProduct());
    }

    private void setupCategoryDropdown() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, CATEGORIES);
        actvCategory.setAdapter(adapter);
        actvCategory.setText(CATEGORIES[0], false);
    }

    private void setupImagePreviewWatcher() {
        etProductImageUrl.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String url = s.toString().trim();
                ImageLoader.getInstance().loadImage(url, imgProductPreview, R.drawable.ic_launcher_foreground);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void populateExistingData(FoodItem item) {
        tvAddProductTitle.setText("Edit Food Product");
        btnSaveProduct.setText("Update Product");

        etProductName.setText(item.getName());
        etProductDesc.setText(item.getDescription());
        etProductPrice.setText(String.valueOf(item.getPrice()));
        actvCategory.setText(item.getCategory(), false);
        etProductImageUrl.setText(item.getImageUrl());
        switchProductAvailable.setChecked(item.isAvailable());

        ImageLoader.getInstance().loadImage(item.getImageUrl(), imgProductPreview, R.drawable.ic_launcher_foreground);
    }

    private void handleSaveProduct() {
        if (isSaving) return;

        String name = etProductName.getText().toString().trim();
        String desc = etProductDesc.getText().toString().trim();
        String priceStr = etProductPrice.getText().toString().trim();
        String category = actvCategory.getText().toString().trim();
        String imageUrl = etProductImageUrl.getText().toString().trim();
        boolean available = switchProductAvailable.isChecked();

        if (TextUtils.isEmpty(name)) {
            etProductName.setError("Product name is required");
            etProductName.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(desc)) {
            etProductDesc.setError("Description is required");
            etProductDesc.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(priceStr)) {
            etProductPrice.setError("Price is required");
            etProductPrice.requestFocus();
            return;
        }

        double price;
        try {
            price = Double.parseDouble(priceStr);
            if (price <= 0) {
                etProductPrice.setError("Price must be greater than zero");
                etProductPrice.requestFocus();
                return;
            }
        } catch (NumberFormatException e) {
            etProductPrice.setError("Invalid price format");
            etProductPrice.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(category)) {
            actvCategory.setError("Please select a category");
            return;
        }

        setSaving(true);

        if (editingFoodItem != null) {
            // Update mode
            editingFoodItem.setName(name);
            editingFoodItem.setDescription(desc);
            editingFoodItem.setPrice(price);
            editingFoodItem.setCategory(category);
            editingFoodItem.setImageUrl(imageUrl);
            editingFoodItem.setAvailable(available);

            menuRepository.updateProduct(editingFoodItem, new MenuRepository.ActionCallback() {
                @Override
                public void onSuccess() {
                    setSaving(false);
                    Toast.makeText(AddProductActivity.this, "Product updated successfully!", Toast.LENGTH_SHORT).show();
                    finish();
                }

                @Override
                public void onError(String errorMessage) {
                    setSaving(false);
                    Toast.makeText(AddProductActivity.this, "Failed to update product: " + errorMessage, Toast.LENGTH_LONG).show();
                }
            });
        } else {
            // Create mode
            FoodItem newProduct = new FoodItem(
                    null,
                    name,
                    desc,
                    price,
                    category,
                    imageUrl,
                    available,
                    System.currentTimeMillis()
            );

            menuRepository.addProduct(newProduct, new MenuRepository.ActionCallback() {
                @Override
                public void onSuccess() {
                    setSaving(false);
                    Toast.makeText(AddProductActivity.this, "Product added to menu successfully!", Toast.LENGTH_SHORT).show();
                    finish();
                }

                @Override
                public void onError(String errorMessage) {
                    setSaving(false);
                    Toast.makeText(AddProductActivity.this, "Failed to add product: " + errorMessage, Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    private void setSaving(boolean saving) {
        isSaving = saving;
        progressBarAddProduct.setVisibility(saving ? View.VISIBLE : View.GONE);
        btnSaveProduct.setEnabled(!saving);
        btnCancelProduct.setEnabled(!saving);
        etProductName.setEnabled(!saving);
        etProductDesc.setEnabled(!saving);
        etProductPrice.setEnabled(!saving);
        actvCategory.setEnabled(!saving);
        etProductImageUrl.setEnabled(!saving);
        switchProductAvailable.setEnabled(!saving);
    }
}
