package com.example.waitwise.activities;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.waitwise.R;
import com.example.waitwise.adapters.AdminProductAdapter;
import com.example.waitwise.models.FoodItem;
import com.example.waitwise.models.User;
import com.example.waitwise.repositories.MenuRepository;
import com.example.waitwise.repositories.UserRepository;
import com.example.waitwise.utils.EdgeToEdgeUtils;
import com.example.waitwise.utils.FirebaseManager;
import com.example.waitwise.utils.PopupUtils;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

public class ManageProductsActivity extends AppCompatActivity implements AdminProductAdapter.OnProductAdminListener {

    private EditText etSearchAdminProducts;
    private TextView tvAdminProductCount;
    private ShimmerFrameLayout shimmerManageProducts;
    private LinearLayout layoutEmptyAdminProducts;
    private RecyclerView recyclerAdminProducts;

    private MaterialButton catAdminAll;
    private MaterialButton catAdminBreakfast;
    private MaterialButton catAdminMeals;
    private MaterialButton catAdminSnacks;
    private MaterialButton catAdminDrinks;

    private AdminProductAdapter adapter;
    private MenuRepository menuRepository;
    private UserRepository userRepository;

    private final List<FoodItem> allProducts = new ArrayList<>();
    private final List<FoodItem> filteredProducts = new ArrayList<>();

    private String selectedCategory = "All";
    private String searchQuery = "";

    private final int greenColor = android.graphics.Color.rgb(36, 107, 74);
    private final int whiteColor = android.graphics.Color.WHITE;
    private final int textColor = android.graphics.Color.rgb(32, 36, 33);
    private final int borderColor = android.graphics.Color.rgb(231, 232, 227);

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
                    PopupUtils.showPopup(ManageProductsActivity.this, "Access Denied", "Administrator access required.");
                    finish();
                }
            }

            @Override
            public void onError(String errorMessage) {
                finish();
            }
        });

        setContentView(R.layout.activity_manage_products);
        EdgeToEdgeUtils.applyEdgeToEdge(this, findViewById(R.id.manageProductsRoot));

        menuRepository = new MenuRepository();

        initializeViews();
        setupRecyclerView();
        setupSearch();
        setupCategories();
        loadProducts();
    }

    private void initializeViews() {
        etSearchAdminProducts = findViewById(R.id.etSearchAdminProducts);
        tvAdminProductCount = findViewById(R.id.tvAdminProductCount);
        shimmerManageProducts = findViewById(R.id.shimmerManageProducts);
        layoutEmptyAdminProducts = findViewById(R.id.layoutEmptyAdminProducts);
        recyclerAdminProducts = findViewById(R.id.recyclerAdminProducts);

        catAdminAll = findViewById(R.id.catAdminAll);
        catAdminBreakfast = findViewById(R.id.catAdminBreakfast);
        catAdminMeals = findViewById(R.id.catAdminMeals);
        catAdminSnacks = findViewById(R.id.catAdminSnacks);
        catAdminDrinks = findViewById(R.id.catAdminDrinks);

        findViewById(R.id.btnBackManageProducts).setOnClickListener(v -> finish());

        View.OnClickListener openAdd = v -> startActivity(new Intent(ManageProductsActivity.this, AddProductActivity.class));
        findViewById(R.id.btnAddProductTop).setOnClickListener(openAdd);
        findViewById(R.id.btnEmptyAddProduct).setOnClickListener(openAdd);
    }

    private void setupRecyclerView() {
        adapter = new AdminProductAdapter(this);
        recyclerAdminProducts.setLayoutManager(new LinearLayoutManager(this));
        recyclerAdminProducts.setAdapter(adapter);
    }

    private void setupSearch() {
        etSearchAdminProducts.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchQuery = s.toString().trim().toLowerCase(Locale.ROOT);
                filterProducts();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void setupCategories() {
        catAdminAll.setOnClickListener(v -> selectCategory("All"));
        catAdminBreakfast.setOnClickListener(v -> selectCategory("Breakfast"));
        catAdminMeals.setOnClickListener(v -> selectCategory("Meals"));
        catAdminSnacks.setOnClickListener(v -> selectCategory("Snacks"));
        catAdminDrinks.setOnClickListener(v -> selectCategory("Drinks"));
    }

    private void selectCategory(String category) {
        selectedCategory = category;
        updateCategoryAppearance();
        filterProducts();
    }

    private void updateCategoryAppearance() {
        MaterialButton[] buttons = {catAdminAll, catAdminBreakfast, catAdminMeals, catAdminSnacks, catAdminDrinks};
        String[] categories = {"All", "Breakfast", "Meals", "Snacks", "Drinks"};

        for (int i = 0; i < buttons.length; i++) {
            boolean selected = categories[i].equalsIgnoreCase(selectedCategory);
            if (selected) {
                buttons[i].setBackgroundTintList(ColorStateList.valueOf(greenColor));
                buttons[i].setTextColor(whiteColor);
                buttons[i].setStrokeColor(ColorStateList.valueOf(greenColor));
            } else {
                buttons[i].setBackgroundTintList(ColorStateList.valueOf(whiteColor));
                buttons[i].setTextColor(textColor);
                buttons[i].setStrokeColor(ColorStateList.valueOf(borderColor));
            }
        }
    }

    private void loadProducts() {
        if (shimmerManageProducts != null) {
            shimmerManageProducts.startShimmer();
            shimmerManageProducts.setVisibility(View.VISIBLE);
        }
        layoutEmptyAdminProducts.setVisibility(View.GONE);
        recyclerAdminProducts.setVisibility(View.GONE);

        AtomicBoolean loaded = new AtomicBoolean(false);
        Handler handler = new Handler(Looper.getMainLooper());
        Runnable timeoutRunnable = () -> {
            if (loaded.compareAndSet(false, true)) {
                if (shimmerManageProducts != null) {
                    shimmerManageProducts.stopShimmer();
                    shimmerManageProducts.setVisibility(View.GONE);
                }
                layoutEmptyAdminProducts.setVisibility(View.VISIBLE);
            }
        };
        handler.postDelayed(timeoutRunnable, 3500);

        menuRepository.listenToMenu(new MenuRepository.MenuCallback() {
            @Override
            public void onMenuLoaded(List<FoodItem> items) {
                if (loaded.compareAndSet(false, true)) {
                    handler.removeCallbacks(timeoutRunnable);
                }
                if (shimmerManageProducts != null) {
                    shimmerManageProducts.stopShimmer();
                    shimmerManageProducts.setVisibility(View.GONE);
                }
                allProducts.clear();
                allProducts.addAll(items);
                filterProducts();
            }

            @Override
            public void onError(String errorMessage) {
                if (loaded.compareAndSet(false, true)) {
                    handler.removeCallbacks(timeoutRunnable);
                }
                if (shimmerManageProducts != null) {
                    shimmerManageProducts.stopShimmer();
                    shimmerManageProducts.setVisibility(View.GONE);
                }
                layoutEmptyAdminProducts.setVisibility(View.VISIBLE);
                PopupUtils.showPopup(ManageProductsActivity.this, "Menu Error", errorMessage);
            }
        });
    }

    private void filterProducts() {
        filteredProducts.clear();

        for (FoodItem item : allProducts) {
            boolean matchesCategory = selectedCategory.equalsIgnoreCase("All")
                    || item.getCategory().equalsIgnoreCase(selectedCategory);

            String combinedText = (item.getName() + " " + item.getDescription() + " " + item.getCategory())
                    .toLowerCase(Locale.ROOT);

            boolean matchesSearch = searchQuery.isEmpty() || combinedText.contains(searchQuery);

            if (matchesCategory && matchesSearch) {
                filteredProducts.add(item);
            }
        }

        adapter.setItems(filteredProducts);
        int count = filteredProducts.size();
        tvAdminProductCount.setText(count + (count == 1 ? " product" : " products"));

        if (filteredProducts.isEmpty()) {
            layoutEmptyAdminProducts.setVisibility(View.VISIBLE);
            recyclerAdminProducts.setVisibility(View.GONE);
        } else {
            layoutEmptyAdminProducts.setVisibility(View.GONE);
            recyclerAdminProducts.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onEdit(FoodItem item) {
        Intent intent = new Intent(this, AddProductActivity.class);
        intent.putExtra("food_item", item);
        startActivity(intent);
    }

    @Override
    public void onDelete(FoodItem item) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Product")
                .setMessage("Are you sure you want to delete '" + item.getName() + "' from the menu? Historical orders will not be affected.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    menuRepository.deleteProduct(item.getId(), new MenuRepository.ActionCallback() {
                        @Override
                        public void onSuccess() {
                            PopupUtils.showPopup(ManageProductsActivity.this, "Product Deleted", "'" + item.getName() + "' was removed from the menu.");
                        }

                        @Override
                        public void onError(String errorMessage) {
                            PopupUtils.showPopup(ManageProductsActivity.this, "Delete Error", errorMessage);
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onToggleAvailability(FoodItem item, boolean newAvailable) {
        menuRepository.toggleProductAvailability(item.getId(), newAvailable, new MenuRepository.ActionCallback() {
            @Override
            public void onSuccess() {
                String status = newAvailable ? "available for ordering" : "marked as unavailable/sold out";
                PopupUtils.showPopup(ManageProductsActivity.this, "Stock Updated", item.getName() + " is now " + status + ".");
            }

            @Override
            public void onError(String errorMessage) {
                PopupUtils.showPopup(ManageProductsActivity.this, "Update Error", errorMessage);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (menuRepository != null) menuRepository.detachListener();
    }
}
