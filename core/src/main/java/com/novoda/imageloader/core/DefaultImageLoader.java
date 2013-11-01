/**
 * Copyright 2012 Novoda Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.novoda.imageloader.core;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.widget.ImageView;

import com.novoda.imageloader.core.cache.CacheManager;
import com.novoda.imageloader.core.exception.ImageNotFoundException;
import com.novoda.imageloader.core.file.FileManager;
import com.novoda.imageloader.core.loader.Loader;
import com.novoda.imageloader.core.network.NetworkManager;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

/**
 * A default implementation of {@code ImageLoader}.
 * <p/>
 * A {@code DefaultImageLoader} instance can be instantiated at the application
 * level and used statically across the application.
 * <p/>
 * The {@code Manifest.permission.WRITE_EXTERNAL_STORAGE} and {@code Manifest.permission.INTERNET}
 * are currently necessary for the imageLoader library to work properly.
 * <p/>
 * {@see Manifest#WRITE_EXTERNAL_STORAGE}
 * {@see Manifest#INTERNET}
 */
public class DefaultImageLoader implements ImageLoader {
    private final LoaderSettings loaderSettings;
    private final Map<Integer, WeakReference<OnImageLoadedListener>> onImageLoadedListeners;

    /**
     * Returns a new instance of the {@code DefaultImageLoader}.
     *
     * @param loaderSettings the {@link com.novoda.imageloader.core.LoaderSettings}
     *                       used to customise the returned {@link com.novoda.imageloader.core.ImageLoader}
     * @return imageLoader the newly constructed {@code ImageLoader}
     */
    public static ImageLoader newInstance(LoaderSettings loaderSettings) {
        return newInstance(null, loaderSettings);
    }

    /**
     * Returns a new instance of the {@code DefaultImageLoader}.
     *
     * @param context        the {@link android.app.Application} context
     * @param loaderSettings the {@link com.novoda.imageloader.core.LoaderSettings}
     *                       used to customise the returned {@link com.novoda.imageloader.core.ImageLoader}
     * @return imageLoader the newly constructed {@code ImageLoader}
     */
    public static ImageLoader newInstance(Context context, LoaderSettings loaderSettings) {
        return new DefaultImageLoader(context, loaderSettings);
    }

    private DefaultImageLoader(Context context, LoaderSettings loaderSettings) {
        if (context != null) {
            verifyPermissions(context);
        }
        this.loaderSettings = loaderSettings;
        onImageLoadedListeners = new HashMap<Integer, WeakReference<OnImageLoadedListener>>();
    }

    private void verifyPermissions(Context context) {
        verifyPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        verifyPermission(context, Manifest.permission.INTERNET);
    }

    private void verifyPermission(Context c, String permission) {
        int p = c.getPackageManager().checkPermission(permission, c.getPackageName());
        if (p == PackageManager.PERMISSION_DENIED) {
            throw new RuntimeException("ImageLoader : please add the permission " + permission + " to the manifest");
        }
    }

    @Override
    public CacheManager getCacheManager() {
        return loaderSettings.getCacheManager();
    }

    @Override
    public FileManager getFileManager() {
        return loaderSettings.getFileManager();
    }

    @Override
    public NetworkManager getNetworkManager() {
        return loaderSettings.getNetworkManager();
    }

    @Override
    public void load(ImageView imageView) {
        loaderSettings.getLoader().load(imageView);
    }

    @Override
    public void setOnImageLoadedListener(OnImageLoadedListener listener) {
        onImageLoadedListeners.put(listener.hashCode(), new WeakReference<OnImageLoadedListener>(listener));
        loaderSettings.getLoader().setLoadListener(onImageLoadedListeners.get(listener.hashCode()));
    }

    @Override
    public void removeOnImageLoadedListener(OnImageLoadedListener listener) {
        onImageLoadedListeners.remove(listener.hashCode());
    }

    /**
     * Runs in the same thread as the calling method; ensure this is not called from the main thread.
     */
    @Override
    public void cacheImage(String url, int width, int height) {
        Bitmap bm = loaderSettings.getCacheManager().get(url, width, height);
        if (bm == null) {
            try {
                File imageFile = loaderSettings.getFileManager().getFile(url, width, height);
                if (!imageFile.exists()) {
                    loaderSettings.getNetworkManager().retrieveImage(url, imageFile);
                }
                Bitmap b;
                if (loaderSettings.isAlwaysUseOriginalSize()) {
                    b = loaderSettings.getBitmapUtil().decodeFile(imageFile, width, height);
                } else {
                    b = loaderSettings.getBitmapUtil().decodeFileAndScale(imageFile, width, height, loaderSettings.isAllowUpsampling());
                }
                if (b == null) {
                    // decode failed
                    loaderSettings.getCacheManager().put(url, b);
                }
            } catch (ImageNotFoundException inf) {
                // no-op
                inf.printStackTrace();
            }
        }
    }

    /**
     * Use {@link #load(android.widget.ImageView)}
     *
     * @return loader
     * @deprecated in 1.5.11-SNAPSHOT
     */
    @Deprecated
    public Loader getLoader() {
        return loaderSettings.getLoader();
    }

    /**
     * Use {@link #removeOnImageLoadedListener(OnImageLoadedListener)}
     *
     * @param listener
     * @deprecated in 1.5.11-SNAPSHOT
     */
    @Deprecated
    public void unRegisterOnImageLoadedListener(OnImageLoadedListener listener) {
        onImageLoadedListeners.remove(listener.hashCode());
    }
}
