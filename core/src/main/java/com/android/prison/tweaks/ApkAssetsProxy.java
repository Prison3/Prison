package com.android.prison.tweaks;
import com.android.prison.utils.Logger;
import java.lang.reflect.Method;

import com.android.prison.base.ClassInvocationStub;
import com.android.prison.base.MethodHook;
import com.android.prison.base.ProxyMethod;

/**
 * Proxy for ApkAssets to block problematic resource loading
 */
public class ApkAssetsProxy extends ClassInvocationStub {
    public static final String TAG = ApkAssetsProxy.class.getSimpleName();

    private static final String APK_ASSETS_CLASS = "android.content.res.ApkAssets";

    public ApkAssetsProxy() {
        try {
            Class.forName(APK_ASSETS_CLASS);
        } catch (ClassNotFoundException e) {
            Logger.w(TAG, "ApkAssets class not found: " + e.getMessage());
        }
    }

    @Override
    protected Object getWho() {
        return null; // This is a static hook, not an instance hook
    }

    @Override
    protected void inject(Object baseInvocation, Object proxyInvocation) {
        // This is a static hook, no injection needed
    }

    @Override
    public boolean isBadEnv() {
        return false;
    }

    @Override
    protected void onBindMethod() {
        super.onBindMethod();
    }

    @ProxyMethod("loadOverlayFromPath")
    public static class LoadOverlayFromPath extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            String path = (String) args[0];
            
            // Block problematic resource paths
            if (path != null && (path.contains("resource-cache") || 
                                path.contains("@idmap") || 
                                path.contains(".frro") ||
                                path.contains("systemui") ||
                                path.contains("data@resource-cache@"))) {
                Logger.d(TAG, "Blocking problematic overlay path: " + path);
                throw new RuntimeException("Blocked problematic overlay path: " + path);
            }
            
            // For non-problematic paths, proceed normally
            return method.invoke(who, args);
        }
    }

    @ProxyMethod("nativeLoad")
    public static class NativeLoad extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            String path = (String) args[0];
            
            // Block problematic resource paths
            if (path != null && (path.contains("resource-cache") || 
                                path.contains("@idmap") || 
                                path.contains(".frro") ||
                                path.contains("systemui") ||
                                path.contains("data@resource-cache@"))) {
                Logger.d(TAG, "Blocking problematic native load path: " + path);
                throw new RuntimeException("Blocked problematic native load path: " + path);
            }
            
            // For non-problematic paths, proceed normally
            return method.invoke(who, args);
        }
    }
}
