# ImageLoader

ImageLoader is a simple library that makes it easy to download, display and cache remote images in Android apps. Image download happens off the UI thread and the images are cached with a two-level in-memory/SD card cache.

- [Recent Changes](#recent-changes)
- [Using the Library](#using-the-library)
- [Getting the library](#getting-the-library)
- [Contributing](#contributing)
- [Contributors](#contributors)
- [History](#history)
- [License](#license)

## Recent Changes
The latest stable version, 1.5.9, is [available from Maven Central][maven-stable].

* Merged [pull request #107][iss107] for reported issue "Error with Network urls"
* Fixed flickering when dataset is refreshed even when an animation has been set [#105][iss105]
* Fixed the encoding of the request headers as per [issue #104][iss104]
* Added ability to use ImageLoader for local images, just use the `Uri` path of the file [#106][iss106]
* Recently there has been some issues with Robotium 3.6 failing with following error:

        Test failed to run to completion. Reason: 'Instrumentation run failed due to 'java.lang.IllegalAccessError'.

  If you face this problem open module settings in your IDE and make sure that all dependencies scope beside Robotium are `provided` and only Robotium is `compile`

[maven-stable]: http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.novoda.imageloader%22%20AND%20a%3A%22imageloader-core%22
[iss107]: https://github.com/novoda/ImageLoader/pull/107
[iss106]: https://github.com/novoda/ImageLoader/pull/106
[iss105]: https://github.com/novoda/ImageLoader/pull/105
[iss104]: https://github.com/novoda/ImageLoader/pull/104

## Using the Library
### Overview
The demo project is a good place to start but here are the basic steps:

![Shows steps: 1) normal ListActivity with images, 2) Create ImageTags, setting it on the ImageViews with setTag, and 3) Call load on the ImageLoader and let the ImageLoader library handle the rest][img-overview]
[img-overview]: https://github.com/novoda/ImageLoader/raw/master/extra/documentationImage1.png "General overview"

### In the Application Class
Add the following code to initialise and provide access to the ImageLoader. `SettingsBuilder` gives you some control over the caching and network connections.

    @Override
    public void onCreate() {
        super.onCreate();
        LoaderSettings settings = new SettingsBuilder()
          .withDisconnectOnEveryCall(true).build(this);
        imageManager = new ImageManager(this, settings);
    }

    public static final ImageManager getImageManager() {
        return imageManager;
    }

#### LRU Cache Option
The default cache uses soft references. With a memory-constrained system like Android, space can be reclaimed too often, limiting the performance of the cache. The LRU cache is intended to solve this problem. It's particularly useful if your app displays many small images.

    settings = new SettingsBuilder()
        .withCacheManager(new LruBitmapCache(this)).build(this);
    thumbnailImageLoader = new ImageManager(this, settings);

The `LruBitmapCache` will take 25% of the free memory available for the cache by default. You can customise this with an alternative constructor:

    int PERCENTAGE_OF_CACHE = 50;
    settings = new SettingsBuilder()
        .withCacheManager(new LruBitmapCache(this, PERCENTAGE_OF_CACHE)).build(this);
    thumbnailImageLoader = new ImageManager(this, settings);

#### Additional Settings
ImageLoader uses `UrlConnection` to fetch images. There are two important `UrlConnection` parameters that you might want to change: `connectionTimeout` & `readTimeout`.

    SettingsBuilder builder = new SettingsBuilder();
    Settings settings = builder.withConnectionTimeout(20000)
        .withReadTimeout(30000).build(this);

`connectionTimeout` is the timeout for the initial connection. `readTimeout` is the timeout waiting for data.

### In the Activity, Fragment or Adapter
When you want to load an image into an `ImageView`, you just get the ImageLoader from the Application class (`getImageManager().getLoader()`) and call the `load()` method. Here is how you could use it in a `ListView` with the binder setting the image `URL` in the `ImageView` as a tag:

    /* newInstance(Context context, int defaultImageResource) */
    ImageTagFactory imageTagFactory = newInstance(this, R.drawable.bg_img_loading);
    imageTagFactory.setErrorImageId(R.drawable.bg_img_notfound);

    private ViewBinder getViewBinder() {
        return new ViewBinder() {
            @Override
            public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
                // Build image tag with remote image URL
                ImageTag tag = imageTagFactory.build(cursor.getString(columnIndex));
                ((ImageView) view).setTag(tag);
                imageLoader.load(view);
                return true;
            }
        };
    }

The `ImageTagFactory` configures ImageLoader with the size of the images to display and the loading image to be displayed whilst the real image is being fetched. The ImageLoader will fetch the image from the in-memory cache (if available), from the SD card (if available) or from the network as a last resort.

### Cleaning the SD card cache
If you want ImageLoader to clean up the SD card cache, add the following code in the `onCreate` of the Application class:

    imageManager.getFileManager().clean();

In `SettingsBuilder` you can configure the expiration period (default is 7 days).

### In the AndroidManifest.xml
There are two things you need to add: Permissions and the Service to clean up the SD cache. (Since 1.5.6 the cleanup service is no longer required!)

    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

    <service android:name="com.novoda.ImageLoader.core.service.CacheCleaner" android:exported="true">
        <intent-filter>
            <action android:name="com.novoda.ImageLoader.core.action.CLEAN_CACHE" />
        </intent-filter>
    </service>


### Cached Preview Images (optional)
Cached preview images is a feature designed for when you have a list of items with thumbnail images and you subsequently display a larger version of the same image on some user action. ImageLoader can take the small image from the cache (if available) and use it as the preview image whilst the large image loads.

There are two options for implementing cached preview images: configure the image tag before calling load or configure the `ImageTagFactory`.

    // Image tag after normal settings 
    imageTag.setPreviewHeight(100);
    imageTag.setPreviewHeight(100);
    imageTag.setPreviewUrl(previewUrl);
    imageView.setTag(imageTag);
    getImageManager().getLoader().load(imageView);

    // If small and large image have same URL, configure with the ImageTagFactory
    imageTagFactory = new ImageTagFactory(this, R.drawable.loading);
    imageTagFactory.setErrorImageId(R.drawable.image_not_found);
    imageTagFactory.usePreviewImage(THUMB_IMAGE_SIZE, THUMB_IMAGE_SIZE, true);

    // On bind 
    ImageView imageView = (ImageView) view;
    String url = cursor.getString(columnIndex);
    imageView.setTag(imageTagFactory.build(url));
    MyApplication.getImageManager().getLoader().load(imageView);

### DirectLoader (utility)
ImageLoader contains a utility class for directly downloading a `Bitmap` from a `URL`. This is useful for downloading an image to display in a notification. This does _not_ handle threading for you. **You should do the download inside an `AsyncTask` or `Thread`**.

    Bitmap myImage = new DirectLoader().download(url);

This method will throw an `ImageNotFoundException` if there is no image on the other end of your `URL`.

### Adding an animation
If you want to load a an image using an animation you just have to add an `Animation` object to the `ImageLoader#load()` method

    /* newInstance(Context context, int defaultImageResource) */
    ImageTagFactory imageTagFactory = newInstance(this, R.drawable.bg_img_loading);
    imageTagFactory.setErrorImageId(R.drawable.bg_img_notfound);

    Animation fadeInAnimation = AnimationUtils.loadAnimation(this, R.anim.fade_in);
    imageTagFactory.setAnimation(fadeInAnimation);

## Getting the library
### Using Gradle
### Using Maven
### As a .jar

## Contributing
### Reporting Issues
### Pull Requests
#### Project Structure
#### Requirements
#### Setting Up in Android Studio

## Contributors

## History
### 1.5.8
### 1.5.7
### 1.5.6
### 1.5.5
### 1.5.2
### 1.5.1

## License
Copyright &copy; 2013 [Novoda](http://novoda.com/blog/) Ltd. Released under the [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.html)
