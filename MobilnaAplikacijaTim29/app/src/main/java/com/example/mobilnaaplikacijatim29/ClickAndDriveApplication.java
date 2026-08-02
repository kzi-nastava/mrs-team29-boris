package com.example.mobilnaaplikacijatim29;

import android.app.Application;
import android.content.SharedPreferences;

import org.osmdroid.config.Configuration;
import org.osmdroid.config.IConfigurationProvider;

import java.io.File;

public class ClickAndDriveApplication extends Application {

    private static final String OSM_USER_AGENT =
            "ClickAndDrive-Team29/1.0 (Android; com.example.mobilnaaplikacijatim29)";

    @Override
    public void onCreate() {
        super.onCreate();

        SharedPreferences preferences = getSharedPreferences(
                "osmdroid_configuration",
                MODE_PRIVATE
        );

        IConfigurationProvider configuration = Configuration.getInstance();
        configuration.load(getApplicationContext(), preferences);
        configuration.setUserAgentValue(OSM_USER_AGENT);

        // A new app-owned cache avoids showing 403 responses cached before
        // the identifying User-Agent was configured correctly.
        File basePath = new File(getCacheDir(), "osmdroid-v3");
        configuration.setOsmdroidBasePath(basePath);
        configuration.setOsmdroidTileCache(new File(basePath, "tiles"));
        configuration.save(getApplicationContext(), preferences);
    }
}
