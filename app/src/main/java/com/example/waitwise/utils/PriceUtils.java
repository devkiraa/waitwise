package com.example.waitwise.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

public class PriceUtils {

    private PriceUtils() {
        // Utility class
    }

    /**
     * Formats a monetary amount with the Indian Rupee symbol (₹).
     * e.g., 60.0 -> "₹60", 60.5 -> "₹60.50"
     */
    public static String formatPrice(double amount) {
        BigDecimal bd = BigDecimal.valueOf(amount).setScale(2, RoundingMode.HALF_UP);
        if (bd.remainder(BigDecimal.ONE).compareTo(BigDecimal.ZERO) == 0) {
            return String.format(Locale.getDefault(), "₹%d", bd.longValue());
        } else {
            return String.format(Locale.getDefault(), "₹%.2f", bd.doubleValue());
        }
    }

    /**
     * Calculates line total = unitPrice * quantity using BigDecimal to prevent rounding issues.
     */
    public static double calculateLineTotal(double unitPrice, int quantity) {
        if (quantity <= 0 || unitPrice < 0) {
            return 0.0;
        }
        BigDecimal priceBd = BigDecimal.valueOf(unitPrice);
        BigDecimal qtyBd = BigDecimal.valueOf(quantity);
        return priceBd.multiply(qtyBd).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    /**
     * Adds two currency amounts accurately.
     */
    public static double add(double a, double b) {
        BigDecimal b1 = BigDecimal.valueOf(a);
        BigDecimal b2 = BigDecimal.valueOf(b);
        return b1.add(b2).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
