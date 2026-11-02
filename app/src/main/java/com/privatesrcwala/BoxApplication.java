package com.privatesrcwala;

import android.app.Application;
import android.content.Context;
import android.util.Log;
import java.io.File;
import top.Vspace.blackbox.BlackBoxCore;
import top.Vspace.blackbox.app.configuration.AppLifecycleCallback;
import top.Vspace.blackbox.app.configuration.ClientConfiguration;

public class BoxApplication extends Application {
    private static final String TAG = "BoxApplication";

    static {
        try {
            System.loadLibrary("privatesrcwala");
        } catch (UnsatisfiedLinkError error) {
            Log.e(TAG, "Native app library unavailable", error);
        }
    }

    public static native String getSdkKey();

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        try {
            BlackBoxCore.get().doAttachBaseContext(base, new ClientConfiguration() {
                @Override
                public String getHostPackageName() {
                    return base.getPackageName();
                }

                @Override
                public boolean setHideRoot() {
                    return true;
                }

                @Override
                public boolean isEnableDaemonService() {
                    return false;
                }

                @Override
                public boolean requestInstallPackage(File file) {
                    return false;
                }
            });
        } catch (Throwable error) {
            Log.e(TAG, "Tcore initialization skipped", error);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            BlackBoxCore.get().doCreate();
            BlackBoxCore.get().addAppLifecycleCallback(new AppLifecycleCallback() {
                @Override
                public void beforeApplicationOnCreate(String packageName, String processName, Application application, int userId) {
                    // Game-specific native modules are loaded only by the container process.
                }
            });
        } catch (Throwable error) {
            Log.e(TAG, "Tcore runtime initialization skipped", error);
        }
    }
}
