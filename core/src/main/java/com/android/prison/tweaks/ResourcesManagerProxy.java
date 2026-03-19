package com.android.prison.tweaks;
import com.android.prison.utils.Logger;
import java.lang.reflect.Method;

import com.android.prison.base.ClassInvocationStub;
import com.android.prison.base.MethodHook;
import com.android.prison.base.ProxyMethod;

/**
 * Proxy for ResourcesManager to block problematic resource loading
 */
public class ResourcesManagerProxy extends ClassInvocationStub {
    public static final String TAG = ResourcesManagerProxy.class.getSimpleName();

    private static final String RESOURCES_MANAGER_CLASS = "android.app.ResourcesManager";

    public ResourcesManagerProxy() {
        try {
            Class.forName(RESOURCES_MANAGER_CLASS);
        } catch (ClassNotFoundException e) {
            Logger.w(TAG, "ResourcesManager class not found: " + e.getMessage());
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

    @ProxyMethod("loadApkAssets")
    public static class LoadApkAssets extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            String path = (String) args[0];
            
            // Block problematic resource paths
            if (path != null && (path.contains("resource-cache") || 
                                path.contains("@idmap") || 
                                path.contains(".frro") ||
                                path.contains("systemui") ||
                                path.contains("data@resource-cache@"))) {
                Logger.d(TAG, "Blocking problematic ApkAssets load: " + path);
                // Return null or throw exception to prevent loading
                return null;
            }
            
            // For non-problematic paths, proceed normally
            return method.invoke(who, args);
        }
    }

    @ProxyMethod("loadOverlayFromPath")
    public static class LoadOverlayFromPath extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            String path = (String) args[0];
            
            // Block problematic overlay paths
            if (path != null && (path.contains("resource-cache") || 
                                path.contains("@idmap") || 
                                path.contains(".frro") ||
                                path.contains("systemui") ||
                                path.contains("data@resource-cache@"))) {
                Logger.d(TAG, "Blocking problematic overlay path: " + path);
                // Return null to prevent loading
                return null;
            }
            
            // For non-problematic paths, proceed normally
            return method.invoke(who, args);
        }
    }
}
