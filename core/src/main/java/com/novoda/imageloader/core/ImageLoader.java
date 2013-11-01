package com.novoda.imageloader.core;

import android.widget.ImageView;

import com.novoda.imageloader.core.cache.CacheManager;
import com.novoda.imageloader.core.file.FileManager;
import com.novoda.imageloader.core.network.NetworkManager;

/**
 * ImageLoader serves as the entry point to the library.
 * <p/>
 * It provides a simple and easy interface to access three fundamental parts of the ImageLoader
 * library: {@link com.novoda.imageloader.core.file.FileManager},
 * {@link com.novoda.imageloader.core.cache.CacheManager, and
 * {@link com.novoda.imageloader.core.network.NetworkManager}}.
 */
public interface ImageLoader {
    /**
     * Returns the {@code CacheManager}.
     *
     * @return cacheManager the {@code CacheManager} returned
     */
    CacheManager getCacheManager();

    /**
     * Returns the {@code FileManager}.
     *
     * @return fileManager the {@code FileManager} returned
     */
    FileManager getFileManager();

    /**
     * Returns the {@code NetworkManager}.
     *
     * @return networkManager the {@code NetworkManager} returned
     */
    NetworkManager getNetworkManager();

    /**
     * Initiates the loading process for the given {@code ImageView}.
     * <p/>
     * {@code imageView.getTag()} should return an object of type {@link com.novoda.imageloader.core.model.ImageTag}.
     *
     * @param imageView the {@code ImageView} with attached {@code ImageTag}
     */
    void load(ImageView imageView);

    /**
     * Loads an image into the cache.
     * <p/>
     * Fetches the image from the network, decoding and caching the retrieved bitmap.
     * If the image is already cached, this method will do nothing.
     * <p/>
     * Does not bind the image to any {@code View}; can be used for pre-fetching images.
     * Runs in the same thread as the calling method; ensure this is not called from the main thread.
     *
     * @param url the URL of the image to be fetched
     * @param width the width of the cached image
     * @param height the height of the cached image
     */
    void cacheImage(String url, int width, int height);

    /**
     * Adds a listener to be notified when an image is loaded.
     *
     * @param listener the {@code OnImageLoadedListener} which will be notified
     */
    void setOnImageLoadedListener(OnImageLoadedListener listener);

    /**
     * Removes the specified listener from the ImageLoader.
     *
     * @param listener the {@code OnImageLoadedListener} to be removed
     */
    void removeOnImageLoadedListener(OnImageLoadedListener listener);
}
