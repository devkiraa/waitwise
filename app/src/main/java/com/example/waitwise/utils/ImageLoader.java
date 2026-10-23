package com.example.waitwise.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.LruCache;
import android.widget.ImageView;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ImageLoader {

    private static ImageLoader instance;
    private final LruCache<String, Bitmap> memoryCache;
    private final ExecutorService executorService;
    private final Handler mainHandler;

    private ImageLoader() {
        int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        int cacheSize = maxMemory / 8; // Use 1/8th of available memory
        memoryCache = new LruCache<String, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                return bitmap.getByteCount() / 1024;
            }
        };
        executorService = Executors.newFixedThreadPool(4);
        mainHandler = new Handler(Looper.getMainLooper());
    }

    public static synchronized ImageLoader getInstance() {
        if (instance == null) {
            instance = new ImageLoader();
        }
        return instance;
    }

    public void loadImage(String imageUrl, ImageView imageView, int fallbackPlaceholderRes) {
        if (imageView == null) return;

        if (imageUrl == null || imageUrl.trim().isEmpty()) {
            if (fallbackPlaceholderRes != 0) {
                imageView.setImageResource(fallbackPlaceholderRes);
            }
            return;
        }

        Bitmap cached = memoryCache.get(imageUrl);
        if (cached != null) {
            imageView.setImageBitmap(cached);
            return;
        }

        if (fallbackPlaceholderRes != 0) {
            imageView.setImageResource(fallbackPlaceholderRes);
        }

        imageView.setTag(imageUrl);

        executorService.execute(() -> {
            Bitmap bitmap = downloadBitmap(imageUrl);
            if (bitmap != null) {
                memoryCache.put(imageUrl, bitmap);
                mainHandler.post(() -> {
                    Object tag = imageView.getTag();
                    if (imageUrl.equals(tag)) {
                        imageView.setImageBitmap(bitmap);
                    }
                });
            }
        });
    }

    private Bitmap downloadBitmap(String urlStr) {
        HttpURLConnection connection = null;
        InputStream inputStream = null;
        try {
            URL url = new URL(urlStr);
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(8000);
            connection.setReadTimeout(8000);
            connection.setDoInput(true);
            connection.connect();

            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                inputStream = connection.getInputStream();
                return BitmapFactory.decodeStream(inputStream);
            }
        } catch (Exception ignored) {
            // Log or fallback
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (Exception ignored) {
                }
            }
            if (connection != null) {
                connection.disconnect();
            }
        }
        return null;
    }
}
