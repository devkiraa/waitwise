package com.example.waitwise.activities;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

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
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ManageProductsActivity extends AppCompatActivity implements AdminProductAdapter.OnProductAdminListener {

    private EditText etSearchAdminProducts;
    private TextView tvAdminProductCount;
    private ProgressBar progressBarManageProducts;
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
                    Toast.makeText(ManageProductsActivity.this, "Access denied.", Toast.LENGTH_SHORT).show();
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
        progressBarManageProducts = findViewById(R.id.progressBarManageProducts);
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
        progressBarManageProducts.setVisibility(View.VISIBLE);
        layoutEmptyAdminProducts.setVisibility(View.GONE);
        recyclerAdminProducts.setVisibility(View.GONE);

        menuRepository.listenToMenu(new MenuRepository.MenuCallback() {
            @Override
            public void onMenuLoaded(List<FoodItem> items) {
                progressBarManageProducts.setVisibility(View.GONE);
                allProducts.clear();
                allProducts.addAll(items);
                filterProducts();
            }

            @Override
            public void onError(String errorMessage) {
                progressBarManageProducts.setVisibility(View.GONE);
                Toast.makeText(ManageProductsActivity.this, "Error loading menu: " + errorMessage, Toast.LENGTH_SHORT).show();
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
                            Toast.makeText(ManageProductsActivity.this, "Product removed from menu.", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onError(String errorMessage) {
                            Toast.makeText(ManageProductsActivity.this, "Failed to delete: " + errorMessage, Toast.LENGTH_SHORT).show();
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
                String status = newAvailable ? "available" : "unavailable";
                Toast.makeText(ManageProductsActivity.this, item.getName() + " is now " + status, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String errorMessage) {
                Toast.makeText(ManageProductsActivity.this, "Update failed: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (menuRepository != null) menuRepository.detachListener();
    }
}
