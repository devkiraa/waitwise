package com.example.waitwise;

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
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.widget.NestedScrollView;
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
import com.example.waitwise.repositories.FavoritesRepository;
import com.example.waitwise.repositories.MenuRepository;
import com.example.waitwise.utils.EdgeToEdgeUtils;
import com.example.waitwise.utils.FcmTokenManager;
import com.example.waitwise.utils.FirebaseManager;
import com.example.waitwise.utils.InAppNotificationManager;
import com.example.waitwise.utils.PopupUtils;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainActivity extends AppCompatActivity implements FoodAdapter.OnFoodItemClickListener {

    private MaterialButton btnCart;
    private MaterialButton categoryAll;
    private MaterialButton categoryFavorites;
    private MaterialButton categoryBreakfast;
    private MaterialButton categoryMeals;
    private MaterialButton categorySnacks;
    private MaterialButton categoryDrinks;

    private EditText searchInput;
    private TextView menuCount;
    private ShimmerFrameLayout shimmerMenu;
    private LinearLayout layoutEmptyState;
    private TextView tvEmptyTitle;
    private TextView tvEmptySubtitle;
    private MaterialButton btnRetryMenu;
    private RecyclerView recyclerMenu;
    private NestedScrollView nestedScrollViewMain;

    private FoodAdapter foodAdapter;
    private MenuRepository menuRepository;
    private CartRepository cartRepository;
    private FavoritesRepository favoritesRepository;

    private final List<FoodItem> allFoodItems = new ArrayList<>();
    private final List<FoodItem> filteredFoodItems = new ArrayList<>();
    private final Set<String> userFavoriteFoodIds = new HashSet<>();

    private int totalCartCount = 0;
    private String selectedCategory = "All";
    private String searchQuery = "";

    private static final int PAGE_SIZE = 5;
    private int displayedCount = 5;

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

        setContentView(R.layout.activity_main);
        EdgeToEdgeUtils.applyEdgeToEdge(this, findViewById(R.id.main));

        menuRepository = new MenuRepository();
        cartRepository = new CartRepository();
        favoritesRepository = new FavoritesRepository();

        FcmTokenManager.syncFcmToken(FirebaseManager.getInstance().getCurrentUserId());
        FcmTokenManager.requestNotificationPermission(this);

        initializeViews();
        setupRecyclerView();
        setupCart();
        setupSearch();
        setupCategories();
        setupNavigation();
        loadMenuData();
        listenToUserCart();
        listenToUserFavorites();
    }

    private void initializeViews() {
        btnCart = findViewById(R.id.btnCart);
        searchInput = findViewById(R.id.searchInput);
        menuCount = findViewById(R.id.menuCount);
        shimmerMenu = findViewById(R.id.shimmerMenu);
        layoutEmptyState = findViewById(R.id.layoutEmptyState);
        tvEmptyTitle = findViewById(R.id.tvEmptyTitle);
        tvEmptySubtitle = findViewById(R.id.tvEmptySubtitle);
        btnRetryMenu = findViewById(R.id.btnRetryMenu);
        recyclerMenu = findViewById(R.id.recyclerMenu);
        nestedScrollViewMain = findViewById(R.id.nestedScrollViewMain);

        categoryAll = findViewById(R.id.categoryAll);
        categoryFavorites = findViewById(R.id.categoryFavorites);
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

        if (nestedScrollViewMain != null) {
            nestedScrollViewMain.setOnScrollChangeListener((NestedScrollView.OnScrollChangeListener) (v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
                if (scrollY >= (v.getChildAt(0).getMeasuredHeight() - v.getMeasuredHeight() - 250)) {
                    loadNextPage();
                }
            });
        }
    }

    private void loadNextPage() {
        if (filteredFoodItems.isEmpty()) return;
        if (displayedCount < filteredFoodItems.size()) {
            displayedCount = Math.min(displayedCount + PAGE_SIZE, filteredFoodItems.size());
            foodAdapter.setItems(new ArrayList<>(filteredFoodItems.subList(0, displayedCount)));
            int total = filteredFoodItems.size();
            menuCount.setText(displayedCount + " of " + total + (total == 1 ? " item" : " items"));
        }
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

    private void listenToUserFavorites() {
        String uid = FirebaseManager.getInstance().getCurrentUserId();
        if (uid != null) {
            favoritesRepository.listenToUserFavorites(uid, new FavoritesRepository.FavoritesCallback() {
                @Override
                public void onFavoritesLoaded(Set<String> favoriteFoodIds) {
                    userFavoriteFoodIds.clear();
                    userFavoriteFoodIds.addAll(favoriteFoodIds);
                    foodAdapter.setFavorites(userFavoriteFoodIds);
                    if ("Favorites".equalsIgnoreCase(selectedCategory)) {
                        filterMenu();
                    }
                }

                @Override
                public void onError(String errorMessage) {
                    // Ignored
                }
            });
        }
    }

    private void loadMenuData() {
        if (shimmerMenu != null) {
            shimmerMenu.startShimmer();
            shimmerMenu.setVisibility(View.VISIBLE);
        }
        layoutEmptyState.setVisibility(View.GONE);
        recyclerMenu.setVisibility(View.GONE);

        AtomicBoolean loaded = new AtomicBoolean(false);
        Handler handler = new Handler(Looper.getMainLooper());
        Runnable timeoutRunnable = () -> {
            if (loaded.compareAndSet(false, true)) {
                if (shimmerMenu != null) {
                    shimmerMenu.stopShimmer();
                    shimmerMenu.setVisibility(View.GONE);
                }
                if (allFoodItems.isEmpty()) {
                    showEmptyOrErrorState("No food items available", "Our kitchen is currently preparing new delicious items. Please check back soon.");
                }
            }
        };
        handler.postDelayed(timeoutRunnable, 3500);

        menuRepository.listenToMenu(new MenuRepository.MenuCallback() {
            @Override
            public void onMenuLoaded(List<FoodItem> items) {
                if (loaded.compareAndSet(false, true)) {
                    handler.removeCallbacks(timeoutRunnable);
                }
                if (shimmerMenu != null) {
                    shimmerMenu.stopShimmer();
                    shimmerMenu.setVisibility(View.GONE);
                }
                allFoodItems.clear();
                allFoodItems.addAll(items);
                filterMenu();
            }

            @Override
            public void onError(String errorMessage) {
                if (loaded.compareAndSet(false, true)) {
                    handler.removeCallbacks(timeoutRunnable);
                }
                if (shimmerMenu != null) {
                    shimmerMenu.stopShimmer();
                    shimmerMenu.setVisibility(View.GONE);
                }
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
        categoryFavorites.setOnClickListener(view -> selectCategory("Favorites"));
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
        MaterialButton[] buttons = {categoryAll, categoryFavorites, categoryBreakfast, categoryMeals, categorySnacks, categoryDrinks};
        String[] categories = {"All", "Favorites", "Breakfast", "Meals", "Snacks", "Drinks"};

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
            boolean matchesCategory;
            if ("Favorites".equalsIgnoreCase(selectedCategory)) {
                matchesCategory = userFavoriteFoodIds.contains(item.getId());
            } else {
                matchesCategory = selectedCategory.equalsIgnoreCase("All")
                        || item.getCategory().equalsIgnoreCase(selectedCategory);
            }

            String combinedText = (item.getName() + " " + item.getDescription() + " " + item.getCategory())
                    .toLowerCase(Locale.ROOT);

            boolean matchesSearch = searchQuery.isEmpty() || combinedText.contains(searchQuery);

            if (matchesCategory && matchesSearch) {
                filteredFoodItems.add(item);
            }
        }

        displayedCount = Math.min(PAGE_SIZE, filteredFoodItems.size());
        int total = filteredFoodItems.size();

        if (filteredFoodItems.isEmpty()) {
            foodAdapter.setItems(new ArrayList<>());
            menuCount.setText("0 items");
            if ("Favorites".equalsIgnoreCase(selectedCategory)) {
                showEmptyOrErrorState("No favorite items", "Tap the heart icon on any food item to add it to your favorites.");
            } else if (allFoodItems.isEmpty()) {
                showEmptyOrErrorState("No food items available", "Our kitchen is currently preparing new delicious items. Please check back soon.");
            } else {
                showEmptyOrErrorState("No matches found", "No menu items match your search or category filter.");
            }
        } else {
            foodAdapter.setItems(new ArrayList<>(filteredFoodItems.subList(0, displayedCount)));
            menuCount.setText(displayedCount + " of " + total + (total == 1 ? " item" : " items"));
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
            PopupUtils.showPopup(this, "Login Required", "Please log in to add items to cart.");
            return;
        }

        cartRepository.addToCart(uid, food, new CartRepository.CartActionCallback() {
            @Override
            public void onSuccess() {
                PopupUtils.showPopup(MainActivity.this, "Item Added", food.getName() + " was added to your cart.");
            }

            @Override
            public void onError(String errorMessage) {
                PopupUtils.showPopup(MainActivity.this, "Cart Error", "Could not add item: " + errorMessage);
            }
        });
    }

    @Override
    public void onItemClick(FoodItem food) {
        // Tap on item displays info popup
        PopupUtils.showPopup(this, food.getName(), food.getDescription());
    }

    @Override
    public void onToggleFavorite(FoodItem food, boolean isFavorite) {
        String uid = FirebaseManager.getInstance().getCurrentUserId();
        if (uid == null) {
            PopupUtils.showPopup(this, "Login Required", "Please log in to save favorite items.");
            return;
        }

        if (isFavorite) {
            userFavoriteFoodIds.add(food.getId());
            PopupUtils.showPopup(this, "Favorites", food.getName() + " added to favorites ♥");
        } else {
            userFavoriteFoodIds.remove(food.getId());
            PopupUtils.showPopup(this, "Favorites", food.getName() + " removed from favorites.");
        }
        foodAdapter.setFavorites(userFavoriteFoodIds);
        if ("Favorites".equalsIgnoreCase(selectedCategory)) {
            filterMenu();
        }

        favoritesRepository.toggleFavorite(uid, food.getId(), isFavorite);
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
    protected void onResume() {
        super.onResume();
        String uid = FirebaseManager.getInstance().getCurrentUserId();
        if (uid != null) {
            InAppNotificationManager.getInstance().startListeningForOrderUpdates(this, uid);
            InAppNotificationManager.getInstance().setUiListener(notification -> {
                Snackbar snackbar = Snackbar.make(findViewById(R.id.main),
                        notification.getTitle() + ": " + notification.getMessage(),
                        Snackbar.LENGTH_LONG);
                snackbar.setAction("VIEW TOKEN", v -> {
                    Intent intent = new Intent(MainActivity.this, MyTokensActivity.class);
                    intent.putExtra("orderId", notification.getOrderId());
                    startActivity(intent);
                });
                snackbar.show();
            });
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        InAppNotificationManager.getInstance().clearUiListener();
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
        if (favoritesRepository != null) {
            favoritesRepository.detachListener();
        }
    }
}
