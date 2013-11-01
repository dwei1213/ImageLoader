package com.novoda.imageloader.acceptance;

import android.test.InstrumentationTestCase;
import android.test.suitebuilder.annotation.Suppress;

import com.novoda.imageloader.core.NovodaImageLoader;
import com.novoda.imageloader.core.ImageLoader;
import com.novoda.imageloader.core.LoaderSettings;

public class NovodaImageLoaderInstrumentationTest extends InstrumentationTestCase {
    public NovodaImageLoaderInstrumentationTest(String name) {
        super();
        setName(name);
    }

    @Suppress()
    // XXX No assertions so not running - gives NPE
    public void testCacheImage() {
        LoaderSettings settings = new LoaderSettings();
        ImageLoader defaultImageLoader = NovodaImageLoader.newInstance(getInstrumentation().getTargetContext(), settings);
        defaultImageLoader.cacheImage("http://king.com/img.png", 100, 100);
    }
}
