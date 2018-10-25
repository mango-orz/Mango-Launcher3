/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.launcher3;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ProviderInfo;
import android.os.Looper;
import android.os.Process;
import android.util.Log;

import com.android.launcher3.compat.LauncherAppsCompat;
import com.android.launcher3.compat.PackageInstallerCompat;
import com.android.launcher3.compat.UserManagerCompat;
import com.android.launcher3.config.FeatureFlags;
import com.android.launcher3.model.ClassifyModel;
import com.android.launcher3.notification.NotificationListener;
import com.android.launcher3.util.ConfigMonitor;
import com.android.launcher3.util.Preconditions;
import com.android.launcher3.util.SettingsObserver;

import java.util.List;
import java.util.concurrent.ExecutionException;

import static com.android.launcher3.ui.settings.SettingsActivity.NOTIFICATION_BADGING;

public class LauncherAppState {

    public static final String ACTION_FORCE_ROLOAD = "force-reload-launcher";

    // We do not need any synchronization for this variable as its only written on UI thread.
    private static LauncherAppState INSTANCE;

    private final Context mContext;
    private final LauncherModel mModel;
    private final ClassifyModel mClassifyModel;
    private final IconCache mIconCache;
    private final WidgetPreviewLoader mWidgetCache;
    private final InvariantDeviceProfile mInvariantDeviceProfile;
    private final SettingsObserver mNotificationBadgingObserver;
    /** static for newInstance failed */
    private static LauncherProvider mLauncherProvider;

    public static LauncherAppState getInstance(final Context context) {
        if (INSTANCE == null) {
            if (Looper.myLooper() == Looper.getMainLooper()) {
                INSTANCE = new LauncherAppState(context.getApplicationContext());
            } else {
                try {
                    return new MainThreadExecutor().submit(() -> LauncherAppState.getInstance(context)).get();
                } catch (InterruptedException | ExecutionException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return INSTANCE;
    }

    public static LauncherAppState getInstanceNoCreate() {
        return INSTANCE;
    }

    public Context getContext() {
        return mContext;
    }

    private LauncherAppState(Context context) {
        if (getLocalProvider(context) == null) {
            throw new RuntimeException(
                    "Initializing LauncherAppState in the absence of LauncherProvider");
        }
        Log.v(Launcher.TAG, "LauncherAppState initiated");
        Preconditions.assertUIThread();
        mContext = context;

        mInvariantDeviceProfile = new InvariantDeviceProfile(mContext);
        mIconCache = new IconCache(mContext, mInvariantDeviceProfile);
        mWidgetCache = new WidgetPreviewLoader(mContext, mIconCache);
        mClassifyModel = new ClassifyModel(this);
        mModel = new LauncherModel(this, mIconCache, AppFilter.newInstance(mContext));

        LauncherAppsCompat.getInstance(mContext).addOnAppsChangedCallback(mModel);

        // Register intent receivers
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_LOCALE_CHANGED);
        // For handling managed profiles
        filter.addAction(Intent.ACTION_MANAGED_PROFILE_ADDED);
        filter.addAction(Intent.ACTION_MANAGED_PROFILE_REMOVED);
        filter.addAction(Intent.ACTION_MANAGED_PROFILE_AVAILABLE);
        filter.addAction(Intent.ACTION_MANAGED_PROFILE_UNAVAILABLE);
        filter.addAction(Intent.ACTION_MANAGED_PROFILE_UNLOCKED);

        if (FeatureFlags.IS_DOGFOOD_BUILD) {
            filter.addAction(ACTION_FORCE_ROLOAD);
        }

        mClassifyModel.init(mContext);

        mContext.registerReceiver(mModel, filter);
        UserManagerCompat.getInstance(mContext).enableAndResetCache();
        new ConfigMonitor(mContext).register();

        if (!mContext.getResources().getBoolean(R.bool.notification_badging_enabled)) {
            mNotificationBadgingObserver = null;
        } else {
            // Register an observer to rebind the notification listener when badging is re-enabled.
            mNotificationBadgingObserver = new SettingsObserver.Secure(
                    mContext.getContentResolver()) {
                @Override
                public void onSettingChanged(boolean isNotificationBadgingEnabled) {
                    if (isNotificationBadgingEnabled) {
                        NotificationListener.requestRebind(new ComponentName(
                                mContext, NotificationListener.class));
                    }
                }
            };
            mNotificationBadgingObserver.register(NOTIFICATION_BADGING);
        }
    }

    /**
     * Call from Application.onTerminate(), which is not guaranteed to ever be called.
     */
    public void onTerminate() {
        mContext.unregisterReceiver(mModel);
        final LauncherAppsCompat launcherApps = LauncherAppsCompat.getInstance(mContext);
        launcherApps.removeOnAppsChangedCallback(mModel);
        PackageInstallerCompat.getInstance(mContext).onStop();
        if (mNotificationBadgingObserver != null) {
            mNotificationBadgingObserver.unregister();
        }
    }

    LauncherModel setLauncher(Launcher launcher) {
        getLocalProvider(mContext).setLauncherProviderChangeListener(launcher);
        mModel.initialize(launcher);
        return mModel;
    }

    public IconCache getIconCache() {
        return mIconCache;
    }

    public LauncherModel getModel() {
        return mModel;
    }

    public ClassifyModel getClassifyModel() {
        return mClassifyModel;
    }

    public WidgetPreviewLoader getWidgetCache() {
        return mWidgetCache;
    }

    public InvariantDeviceProfile getInvariantDeviceProfile() {
        return mInvariantDeviceProfile;
    }

    /**
     * Shorthand for {@link #getInvariantDeviceProfile()}
     */
    public static InvariantDeviceProfile getIDP(Context context) {
        return LauncherAppState.getInstance(context).getInvariantDeviceProfile();
    }

    @SuppressLint("Recycle")
    private static LauncherProvider getLocalProvider(Context context) {
        if (mLauncherProvider == null) {
            try (ContentProviderClient cl = context.getContentResolver()
                    .acquireContentProviderClient(LauncherProvider.AUTHORITY)) {
                mLauncherProvider = (LauncherProvider) cl.getLocalContentProvider();
            } catch (Exception e) {
                // new Instance when exception
                mLauncherProvider = new LauncherProvider();
            }
        }
        return mLauncherProvider;
    }
}
