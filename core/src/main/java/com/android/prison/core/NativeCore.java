package com.android.prison.core;

import android.content.Context;
import android.os.Process;

import androidx.annotation.Keep;

import java.io.File;
import java.util.List;

import dalvik.system.DexFile;

import com.android.prison.base.PActivityThread;
import com.android.prison.utils.Logger;
import com.android.prison.utils.DexFileCompat;

import com.android.prison.system.JarManager;

/**
 * Native core interface for Prison framework.
 * Provides JNI bridge to native library (libprison.so) and handles UID spoofing,
 * path redirection, and DEX loading.
 */
public class NativeCore {
    private static final String TAG = NativeCore.class.getSimpleName();
    
    // Package name constants
    private static final String PACKAGE_MICROG = "com.google.android.gms";
    private static final String PACKAGE_WEBVIEW = "com.google.android.webview";
    
    // System settings class names
    private static final String CLASS_SETTINGS = "Settings";
    private static final String CLASS_FEATURE_FLAG = "FeatureFlag";
    
    // System settings method prefixes
    private static final String[] SETTINGS_METHOD_PREFIXES = {
        "getString", "getInt", "getLong", "getFloat"
    };

    static {
        System.loadLibrary("prison");
    }

    /**
     * Enables native I/O hooks for file system redirection.
     * API level is obtained automatically from native layer.
     * 
     * @param context Android context for accessing system services
     * @param packageName Package name of the virtualized application
     */
    public static native void installHooks(Context context, String packageName);

    /**
     * Adds an I/O redirection rule.
     * 
     * @param targetPath Original path to redirect from
     * @param relocatePath Target path to redirect to
     */
    public static native void addIORule(String targetPath, String relocatePath);

    /**
     * Disables Android Hidden API restrictions (Android 9.0+).
     * 
     * @return true if successful, false otherwise
     */
    public static native boolean disableHiddenApi();

    /**
     * Disables resource loading restrictions.
     * 
     * @return true if successful, false otherwise
     */
    public static native boolean disableResourceLoading();

    /**
     * Spoofs the calling UID for virtualized applications.
     * Called from native layer via JNI to handle UID validation issues.
     * 
     * @param origCallingUid Original calling UID from system
     * @return Spoofed UID appropriate for the virtualized environment
     */
    @Keep
    public static int getCallingUid(int origCallingUid) {
        try {
            // System UIDs should remain unchanged
            if (isSystemUid(origCallingUid)) {
                return origCallingUid;
            }
            
            // Non-user app UIDs should remain unchanged
            if (origCallingUid > Process.LAST_APPLICATION_UID) {
                return origCallingUid;
            }

            // Handle host app UID spoofing
            if (origCallingUid == PrisonCore.getUid()) {
                return handleHostUidSpoofing();
            }
            
            return origCallingUid;
        } catch (Exception e) {
            Logger.e(TAG, "Error in getCallingUid: " + e.getMessage(), e);
            // Return a safe fallback UID
            return Process.myUid();
        }
    }

    /**
     * Checks if the UID is a system UID.
     */
    private static boolean isSystemUid(int uid) {
        return uid > 0 && uid < Process.FIRST_APPLICATION_UID;
    }

    /**
     * Handles UID spoofing for host app calls.
     */
    private static int handleHostUidSpoofing() {
        String appPackageName = PActivityThread.getAppPackageName();
        
        // Handle microG package
        if (PACKAGE_MICROG.equals(appPackageName)) {
            // microG handling (currently no special action needed)
        }
        
        // Handle WebView package
        // WebView's Process.myUid() is not hooked, so special handling is needed
        if (PACKAGE_WEBVIEW.equals(appPackageName)) {
            return Process.myUid();
        }
        
        // Try to get calling bound UID for sandboxed environments
        int callingBUid = getCallingBoundUid();
        if (callingBUid > 0 && callingBUid < Process.LAST_APPLICATION_UID) {
            return callingBUid;
        }
        
        // Check for system settings access to prevent UID mismatch
        int settingsUid = checkSystemSettingsAccess();
        if (settingsUid > 0) {
            return settingsUid;
        }
        
        // Fallback to host UID
        return PrisonCore.getUid();
    }

    /**
     * Gets the calling bound UID for sandboxed environments.
     */
    private static int getCallingBoundUid() {
        try {
            return PActivityThread.getCallingBoundUid();
        } catch (Exception e) {
            Logger.w(TAG, "Error getting calling BUid: " + e.getMessage());
            return -1;
        }
    }

    /**
     * Checks if the current call is accessing system settings and returns appropriate UID.
     */
    private static int checkSystemSettingsAccess() {
        try {
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            for (StackTraceElement element : stackTrace) {
                String className = element.getClassName();
                String methodName = element.getMethodName();
                
                // Check if this is a system settings access that might cause UID issues
                if (isSystemSettingsClass(className) && isSettingsMethod(methodName)) {
                    Logger.d(TAG, "System settings access detected, using system UID to prevent mismatch");
                    return Process.SYSTEM_UID; // UID 1000
                }
            }
        } catch (Exception e) {
            Logger.w(TAG, "Error analyzing stack trace for UID resolution: " + e.getMessage());
        }
        return -1;
    }

    /**
     * Checks if the class name indicates system settings access.
     */
    private static boolean isSystemSettingsClass(String className) {
        return className.contains(CLASS_SETTINGS) || className.contains(CLASS_FEATURE_FLAG);
    }

    /**
     * Checks if the method name indicates a settings getter method.
     */
    private static boolean isSettingsMethod(String methodName) {
        for (String prefix : SETTINGS_METHOD_PREFIXES) {
            if (methodName.contains(prefix)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Redirects a path string using IOCore.
     * Called from native layer via JNI.
     * 
     * @param path Original path
     * @return Redirected path
     */
    @Keep
    public static String redirectPath(String path) {
        return IOCore.get().redirectPath(path);
    }

    /**
     * Redirects a File path using IOCore.
     * Called from native layer via JNI.
     * 
     * @param path Original file path
     * @return Redirected file path
     */
    @Keep
    public static File redirectPath(File path) {
        return IOCore.get().redirectPath(path);
    }

    /**
     * Loads an empty DEX file and returns its cookies.
     * Used for certain system calls that require DEX cookies.
     * 
     * @return Array of DEX cookies, or empty array if loading fails
     */
    @Keep
    public static long[] loadEmptyDex() {
        try {
            File emptyJar = JarManager.getInstance().getEmptyJar();
            if (emptyJar == null) {
                Logger.w(TAG, "Empty JAR not available, attempting sync initialization");
                JarManager.getInstance().initializeSync();
                emptyJar = JarManager.getInstance().getEmptyJar();
            }
            
            if (emptyJar == null || !emptyJar.exists()) {
                Logger.e(TAG, "Empty JAR file not found or invalid");
                return new long[0];
            }
            
            DexFile dexFile = new DexFile(emptyJar);
            List<Long> cookies = DexFileCompat.getCookies(dexFile);
            
            if (cookies == null || cookies.isEmpty()) {
                Logger.w(TAG, "No cookies found in empty DEX");
                return new long[0];
            }
            
            long[] result = new long[cookies.size()];
            for (int i = 0; i < cookies.size(); i++) {
                result[i] = cookies.get(i);
            }
            
            Logger.d(TAG, "Successfully loaded empty DEX with " + cookies.size() + " cookies");
            return result;
        } catch (Exception e) {
            Logger.e(TAG, "Failed to load empty DEX", e);
            return new long[0];
        }
    }
}
