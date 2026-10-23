package com.example.waitwise;

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

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.waitwise.activities.CartActivity;
import com.example.waitwise.activities.LoginActivity;
import com.example.waitwise.activities.MyOrdersActivity;
import com.example.waitwise.activities.MyTokensActivity;
import com.example.waitwise.activities.ProfileActivity;
import com.example.waitwise.adapters.FoodAdapter;
import com.example.waitwise.models.CartItem;
import com.example.waitwise.models.FoodItem;
import com.example.waitwise.repositories.CartRepository;
import com.example.waitwise.repositories.MenuRepository;
import com.example.waitwise.utils.FirebaseManager;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements FoodAdapter.OnFoodItemClickListener {

    private MaterialButton btnCart;
    private MaterialButton categoryAll;
    private MaterialButton categoryBreakfast;
    private MaterialButton categoryMeals;
    private MaterialButton categorySnacks;
    private MaterialButton categoryDrinks;

    private EditText searchInput;
    private TextView menuCount;
    private ProgressBar progressBarMenu;
    private LinearLayout layoutEmptyState;
    private TextView tvEmptyTitle;
    private TextView tvEmptySubtitle;
    private MaterialButton btnRetryMenu;
    private RecyclerView recyclerMenu;

    private FoodAdapter foodAdapter;
    private MenuRepository menuRepository;
    private CartRepository cartRepository;

    private final List<FoodItem> allFoodItems = new ArrayList<>();
    private final List<FoodItem> filteredFoodItems = new ArrayList<>();

    private int totalCartCount = 0;
    private String selectedCategory = "All";
    private String searchQuery = "";

    private final int greenColor = android.graphics.Color.rgb(36, 107, 74);
    private final int whiteColor = android.graphics.Color.WHITE;
    private final int textColor = android.graphics.Color.rgb(32, 36, 33);
    private final int borderColor = android.graphics.Color.rgb(231, 232, 227);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Enforce authentication
        if (!FirebaseManager.getInstance().isUserLoggedIn()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        View mainView = findViewById(R.id.main);
        if (mainView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainView, (view, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                view.setPadding(
                        systemBars.left,
                        systemBars.top,
                        systemBars.right,
                        systemBars.bottom
                );
                return insets;
            });
        }

        menuRepository = new MenuRepository();
        cartRepository = new CartRepository();

        initializeViews();
        setupRecyclerView();
        setupCart();
        setupSearch();
        setupCategories();
        setupNavigation();
        loadMenuData();
        listenToUserCart();
    }

    private void initializeViews() {
        btnCart = findViewById(R.id.btnCart);
        searchInput = findViewById(R.id.searchInput);
        menuCount = findViewById(R.id.menuCount);
        progressBarMenu = findViewById(R.id.progressBarMenu);
        layoutEmptyState = findViewById(R.id.layoutEmptyState);
        tvEmptyTitle = findViewById(R.id.tvEmptyTitle);
        tvEmptySubtitle = findViewById(R.id.tvEmptySubtitle);
        btnRetryMenu = findViewById(R.id.btnRetryMenu);
        recyclerMenu = findViewById(R.id.recyclerMenu);

        categoryAll = findViewById(R.id.categoryAll);
        categoryBreakfast = findViewById(R.id.categoryBreakfast);
        categoryMeals = findViewById(R.id.categoryMeals);
        categorySnacks = findViewById(R.id.categorySnacks);
        categoryDrinks = findViewById(R.id.categoryDrinks);

        btnRetryMenu.setOnClickListener(v -> loadMenuData());
    }

    private void setupRecyclerView() {
        foodAdapter = new FoodAdapter(this);
        recyclerMenu.setLayoutManager(new LinearLayoutManager(this));
        recyclerMenu.setAdapter(foodAdapter);
    }

    private void setupCart() {
        btnCart.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, CartActivity.class);
            startActivity(intent);
        });
    }

    private void listenToUserCart() {
        String uid = FirebaseManager.getInstance().getCurrentUserId();
        if (uid != null) {
            cartRepository.listenToUserCart(uid, new CartRepository.CartCallback() {
                @Override
                public void onCartLoaded(List<CartItem> items) {
                    int count = 0;
                    for (CartItem item : items) {
                        count += item.getQuantity();
                    }
                    totalCartCount = count;
                    btnCart.setText("Cart (" + totalCartCount + ")");
                }

                @Override
                public void onError(String errorMessage) {
                    // Ignored or logged
                }
            });
        }
    }

    private void loadMenuData() {
        progressBarMenu.setVisibility(View.VISIBLE);
        layoutEmptyState.setVisibility(View.GONE);
        recyclerMenu.setVisibility(View.GONE);

        menuRepository.listenToMenu(new MenuRepository.MenuCallback() {
            @Override
            public void onMenuLoaded(List<FoodItem> items) {
                progressBarMenu.setVisibility(View.GONE);
                allFoodItems.clear();
                allFoodItems.addAll(items);
                filterMenu();
            }

            @Override
            public void onError(String errorMessage) {
                progressBarMenu.setVisibility(View.GONE);
                showEmptyOrErrorState("Failed to load menu", errorMessage + "\nPlease tap Retry.");
            }
        });
    }

    private void setupSearch() {
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchQuery = s.toString().trim().toLowerCase(Locale.ROOT);
                filterMenu();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void setupCategories() {
        categoryAll.setOnClickListener(view -> selectCategory("All"));
        categoryBreakfast.setOnClickListener(view -> selectCategory("Breakfast"));
        categoryMeals.setOnClickListener(view -> selectCategory("Meals"));
        categorySnacks.setOnClickListener(view -> selectCategory("Snacks"));
        categoryDrinks.setOnClickListener(view -> selectCategory("Drinks"));
    }

    private void selectCategory(String category) {
        selectedCategory = category;
        updateCategoryAppearance();
        filterMenu();
    }

    private void updateCategoryAppearance() {
        MaterialButton[] buttons = {categoryAll, categoryBreakfast, categoryMeals, categorySnacks, categoryDrinks};
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

    private void filterMenu() {
        filteredFoodItems.clear();

        for (FoodItem item : allFoodItems) {
            boolean matchesCategory = selectedCategory.equalsIgnoreCase("All")
                    || item.getCategory().equalsIgnoreCase(selectedCategory);

            String combinedText = (item.getName() + " " + item.getDescription() + " " + item.getCategory())
                    .toLowerCase(Locale.ROOT);

            boolean matchesSearch = searchQuery.isEmpty() || combinedText.contains(searchQuery);

            if (matchesCategory && matchesSearch) {
                filteredFoodItems.add(item);
            }
        }

        foodAdapter.setItems(filteredFoodItems);
        int count = filteredFoodItems.size();
        menuCount.setText(count + (count == 1 ? " item" : " items"));

        if (filteredFoodItems.isEmpty()) {
            if (allFoodItems.isEmpty()) {
                showEmptyOrErrorState("No food items available", "Our kitchen is currently preparing new delicious items. Please check back soon.");
            } else {
                showEmptyOrErrorState("No matches found", "No menu items match your search or category filter.");
            }
        } else {
            layoutEmptyState.setVisibility(View.GONE);
            recyclerMenu.setVisibility(View.VISIBLE);
        }
    }

    private void showEmptyOrErrorState(String title, String subtitle) {
        tvEmptyTitle.setText(title);
        tvEmptySubtitle.setText(subtitle);
        layoutEmptyState.setVisibility(View.VISIBLE);
        recyclerMenu.setVisibility(View.GONE);
    }

    @Override
    public void onAddToCart(FoodItem food) {
        String uid = FirebaseManager.getInstance().getCurrentUserId();
        if (uid == null) {
            Toast.makeText(this, "Please log in to add items to cart", Toast.LENGTH_SHORT).show();
            return;
        }

        cartRepository.addToCart(uid, food, new CartRepository.CartActionCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(MainActivity.this, food.getName() + " added to cart", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String errorMessage) {
                Toast.makeText(MainActivity.this, "Could not add item: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onItemClick(FoodItem food) {
        // Tap on item displays brief info or prompt
        Toast.makeText(this, food.getName() + " - " + food.getDescription(), Toast.LENGTH_SHORT).show();
    }

    private void setupNavigation() {
        findViewById(R.id.navHome).setOnClickListener(view -> {
            // Already on Home
        });

        findViewById(R.id.navOrders).setOnClickListener(view -> {
            startActivity(new Intent(MainActivity.this, MyOrdersActivity.class));
        });

        findViewById(R.id.navTokens).setOnClickListener(view -> {
            startActivity(new Intent(MainActivity.this, MyTokensActivity.class));
        });

        findViewById(R.id.navProfile).setOnClickListener(view -> {
            startActivity(new Intent(MainActivity.this, ProfileActivity.class));
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (menuRepository != null) {
            menuRepository.detachListener();
        }
        if (cartRepository != null) {
            cartRepository.detachListener();
        }
    }
}
