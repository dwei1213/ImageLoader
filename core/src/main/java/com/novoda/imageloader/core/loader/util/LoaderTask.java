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
package com.novoda.imageloader.core.loader.util;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.ContactsContract;
import android.view.animation.Animation;
import android.widget.ImageView;

import com.novoda.imageloader.core.LoaderSettings;
import com.novoda.imageloader.core.OnImageLoadedListener;
import com.novoda.imageloader.core.exception.ImageNotFoundException;
import com.novoda.imageloader.core.model.ImageTag;
import com.novoda.imageloader.core.model.ImageWrapper;

import java.io.File;
import java.io.InputStream;
import java.lang.ref.WeakReference;

public class LoaderTask extends AsyncTask<String, Void, Bitmap> {

    private final LoaderSettings loaderSettings;
    private final WeakReference<OnImageLoadedListener> onImageLoadedListener;
    private String url;
    private boolean saveScaledImage;
    private boolean useCacheOnly;
    private int width;
    private int height;
    private int notFoundResourceId;
    private ImageView imageView;
    private Context context;
    private File imageFile;
    private Animation animation;

    public LoaderTask(ImageWrapper imageWrapper, LoaderSettings loaderSettings) {
        this(imageWrapper, loaderSettings, null);
    }

    public LoaderTask(ImageWrapper imageWrapper, LoaderSettings loaderSettings, WeakReference<OnImageLoadedListener> onImageLoadedListener) {
        this.loaderSettings = loaderSettings;
        this.onImageLoadedListener = onImageLoadedListener;
        if (imageWrapper != null) {
            extractWrapperData(imageWrapper);
        }
    }

    @Override
    protected Bitmap doInBackground(String... args) {
        if (isCancelled()) {
            return null;
        }

        if (url == null || url.length() <= 0 || url.equals("_url_error")) {
            return getNotFoundImage(context);
        }

        if (!imageFile.exists()) {
            if (useCacheOnly) {
                return null;
            }
            Uri uri = Uri.parse(url);
            if (isContactPhoto(uri)) {
                return getContactPhoto(uri);
            } else if (isFromFileSystem(uri)) {
                return getLocalImage(uri);
            } else {
                return getNetworkImage(imageFile, uri);
            }
        }
        return getImageFromFile(imageFile);
    }

    private boolean isContactPhoto(Uri uri) {
        return uri.toString().startsWith("content://com.android.contacts/");
    }

    private Bitmap getContactPhoto(Uri uri) {
        InputStream photoDataStream = ContactsContract.Contacts.openContactPhotoInputStream(context.getContentResolver(), uri);
        Bitmap photo = BitmapFactory.decodeStream(photoDataStream);
        return photo;
    }

    private boolean isFromFileSystem(Uri uri) {
        return uri.getScheme() != null ? uri.getScheme().equalsIgnoreCase(ContentResolver.SCHEME_FILE) : true;
    }

    private Bitmap getLocalImage(Uri uri) {
        File image = new File(uri.getPath());
        if (image.exists()) {
            return getImageFromFile(image);
        } else {
            return getNotFoundImage(context);
        }
    }

    private Bitmap getNetworkImage(File imageFile, Uri uri) {
        try {
            loaderSettings.getNetworkManager().retrieveImage(uri.toString(), imageFile);
        } catch (ImageNotFoundException inf) {
            return getNotFoundImage(context);
        }
        if (hasImageViewUrlChanged()) {
            return null;
        }
        return getImageFromFile(imageFile);
    }

    private Bitmap getImageFromFile(File imageFile) {
        Bitmap b;
        if (loaderSettings.isAlwaysUseOriginalSize()) {
            b = loaderSettings.getBitmapUtil().decodeFile(imageFile, width, height);
        } else {
            b = loaderSettings.getBitmapUtil().decodeFileAndScale(imageFile, width, height, loaderSettings.isAllowUpsampling());
        }

        if (b == null) {
            // decoding failed
            return b;
        }

        if (saveScaledImage) {
            saveScaledImage(imageFile, b);
        }
        loaderSettings.getCacheManager().put(url, b);
        return b;
    }

    private void extractWrapperData(ImageWrapper imageWrapper) {
        url = imageWrapper.getUrl();
        width = imageWrapper.getWidth();
        height = imageWrapper.getHeight();
        notFoundResourceId = imageWrapper.getNotFoundResourceId();
        useCacheOnly = imageWrapper.isUseCacheOnly();
        imageView = imageWrapper.getImageView();
        context = imageWrapper.getContext();
        imageFile = getImageFile(imageWrapper);
        animation = imageView.getAnimation();
    }

    private void saveScaledImage(File imageFile, Bitmap b) {
        loaderSettings.getFileManager().saveBitmap(imageFile.getAbsolutePath(), b, width, height);
    }

    private File getImageFile(ImageWrapper imageWrapper) {
        File imageFile = null;
        if (imageWrapper.isSaveThumbnail()) {
            imageFile = loaderSettings.getFileManager().getFile(url, width, height);
        }
        if (imageFile == null || !imageFile.exists()) {
            imageFile = loaderSettings.getFileManager().getFile(url);
            if (imageWrapper.isSaveThumbnail()) {
                saveScaledImage = true;
            }
        }
        return imageFile;
    }

    private boolean hasImageViewUrlChanged() {
        if (url == null) {
            return false;
        } else {
            return !url.equals(((ImageTag) imageView.getTag()).getUrl());
        }
    }

    @Override
    protected void onPostExecute(Bitmap bitmap) {
        if (bitmap == null) {
            return;
        }
        if (isCancelled()) {
            bitmap.recycle();
            return;
        }
        if (!hasImageViewUrlChanged()) {
            listenerCallback();
            imageView.setImageBitmap(bitmap);
            stopExistingAnimation();
            if (animation != null) {
                imageView.startAnimation(animation);
            }
        }
    }

    private void stopExistingAnimation() {
        Animation old = imageView.getAnimation();
        if (old != null && !old.hasEnded()) {
            old.cancel();
        }
    }

    private void listenerCallback() {
        if (onImageLoadedListener != null && onImageLoadedListener.get() != null) {
            onImageLoadedListener.get().onImageLoaded(imageView);
        }
    }

    private Bitmap getNotFoundImage(Context c) {
        String key = "resource" + notFoundResourceId + width + height;
        Bitmap b = loaderSettings.getResCacheManager().get(key);
        if (b != null) {
            return b;
        }
        if (loaderSettings.isAlwaysUseOriginalSize()) {
            b = loaderSettings.getBitmapUtil().decodeResourceBitmap(c, width, height, notFoundResourceId);
        } else {
            b = loaderSettings.getBitmapUtil().decodeResourceBitmapAndScale(c, width, height, notFoundResourceId, loaderSettings.isAllowUpsampling());
        }
        loaderSettings.getResCacheManager().put(key, b);
        return b;
    }

    public String getUrl() {
        return url;
    }

}
