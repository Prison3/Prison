package com.android.prison.tweaks;

import android.app.AppOpsManager;
import android.content.Context;
import android.os.IBinder;

import com.android.prison.base.MethodHook;
import com.android.prison.core.PrisonCore;
import com.android.prison.interfaces.android.app.BRAppOpsManager;
import com.android.prison.interfaces.android.os.BRServiceManager;
import com.android.prison.interfaces.com.android.internal.app.BRIAppOpsServiceStub;
import com.android.prison.base.BinderInvocationStub;
import com.android.prison.base.ProxyMethod;
import com.android.prison.utils.Logger;
import com.android.prison.utils.MethodParameterUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;


public class IAppOpsManagerProxy extends BinderInvocationStub {
    public IAppOpsManagerProxy() {
        super(BRServiceManager.get().getService(Context.APP_OPS_SERVICE));
    }

    @Override
    protected Object getWho() {
        IBinder call = BRServiceManager.get().getService(Context.APP_OPS_SERVICE);
        return BRIAppOpsServiceStub.get().asInterface(call);
    }

    @Override
    protected void inject(Object baseInvocation, Object proxyInvocation) {
        if (BRAppOpsManager.get(null)._check_mService() != null) {
            AppOpsManager appOpsManager = (AppOpsManager) PrisonCore.getContext().getSystemService(Context.APP_OPS_SERVICE);
            try {
                BRAppOpsManager.get(appOpsManager)._set_mService(getProxyInvocation());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        replaceSystemService(Context.APP_OPS_SERVICE);
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        String methodName = method.getName();

        // For all check, note, and start operations, return MODE_ALLOWED directly
        // without calling the system to avoid UID mismatch errors
        if (methodName.startsWith("check") ||
                methodName.startsWith("note") ||
                methodName.startsWith("start")) {
            Logger.d(TAG, "AppOps invoke: Bypassing system for " + methodName + ", allowing operation");
            return buildAllowedReturnValue(method, whoOrNull(), args);
        }

        // For finish operations, just return null without calling system
        if (methodName.startsWith("finish")) {
            Logger.d(TAG, "AppOps invoke: Bypassing system for " + methodName);
            return null;
        }

        // For other operations, try to call system with error handling
        try {
            MethodParameterUtils.replaceFirstAppPkg(args);
            MethodParameterUtils.replaceLastUid(args);
            return super.invoke(proxy, method, args);
        } catch (SecurityException e) {
            // Handle SecurityException for UID/package mismatches
            Logger.w(TAG, "AppOps invoke: SecurityException caught for " + methodName + ", allowing operation", e);
            return buildAllowedReturnValue(method, whoOrNull(), args);
        } catch (Exception e) {
            Logger.e(TAG, "AppOps invoke: Error in method " + methodName, e);
            // Return MODE_ALLOWED as fallback to prevent crashes
            return buildAllowedReturnValue(method, whoOrNull(), args);
        }
    }

    @Override
    public boolean isBadEnv() {
        return false;
    }

    @ProxyMethod("noteProxyOperation")
    public static class NoteProxyOperation extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            return buildAllowedReturnValue(method, who, args);
        }
    }

    @ProxyMethod("checkPackage")
    public static class CheckPackage extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            // todo
            return AppOpsManager.MODE_ALLOWED;
        }
    }

    @ProxyMethod("checkOperation")
    public static class CheckOperation extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            // Always return MODE_ALLOWED to bypass UID mismatch errors
            // The system's checkOperation causes SecurityException when package/uid don't match
            Logger.d(TAG, "AppOps CheckOperation: Bypassing system check, allowing operation");
            return AppOpsManager.MODE_ALLOWED;
        }
    }

    // Hook for checkOperationForDevice - this is called on Android 12+ and shown in stack trace
    @ProxyMethod("checkOperationForDevice")
    public static class CheckOperationForDevice extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            // Always return MODE_ALLOWED to bypass UID mismatch errors
            Logger.d(TAG, "AppOps CheckOperationForDevice: Bypassing system check, allowing operation");
            return AppOpsManager.MODE_ALLOWED;
        }
    }

    @ProxyMethod("noteOperation")
    public static class NoteOperation extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            // Always return MODE_ALLOWED to bypass UID mismatch errors
            Logger.d(TAG, "AppOps NoteOperation: Bypassing system check, allowing operation");
            return AppOpsManager.MODE_ALLOWED;
        }
    }

    @ProxyMethod("checkOpNoThrow")
    public static class CheckOpNoThrow extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            // Always return MODE_ALLOWED to bypass UID mismatch errors
            Logger.d(TAG, "AppOps CheckOpNoThrow: Bypassing system check, allowing operation");
            return AppOpsManager.MODE_ALLOWED;
        }
    }

    // Android 12+: startOp/startOpNoThrow gate long-running operations like recording
    @ProxyMethod("startOp")
    public static class StartOp extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            // Always return MODE_ALLOWED to bypass UID mismatch errors
            Logger.d(TAG, "AppOps StartOp: Bypassing system check, allowing operation");
            return AppOpsManager.MODE_ALLOWED;
        }
    }

    @ProxyMethod("startOpNoThrow")
    public static class StartOpNoThrow extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            // Always return MODE_ALLOWED to bypass UID mismatch errors
            Logger.d(TAG, "AppOps StartOpNoThrow: Bypassing system check, allowing operation");
            return AppOpsManager.MODE_ALLOWED;
        }
    }

    // No-op finish so recording sessions don't error out
    @ProxyMethod("finishOp")
    public static class FinishOp extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                int op = (int) args[0];
                String name = getOpPublicName(op);
                if (name != null && isMediaStorageOrAudioOp(name)) {
                    Logger.d(TAG, "AppOps FinishOp: Finishing operation: " + name);
                }
            } catch (Throwable ignored) {
            }
            return null;
        }
    }

    // Specific handler for RECORD_AUDIO operations
    @ProxyMethod("noteOp")
    public static class NoteOp extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                int op = (int) args[0];
                String name = getOpPublicName(op);
                if (name != null && (name.contains("RECORD_AUDIO") || name.contains("AUDIO") || name.contains("MICROPHONE"))) {
                    Logger.d(TAG, "AppOps NoteOp: Allowing RECORD_AUDIO operation: " + name);
                    return AppOpsManager.MODE_ALLOWED;
                }
            } catch (Throwable ignored) {
            }
            return method.invoke(who, args);
        }
    }

    // Specific handler for RECORD_AUDIO operations with package name
    @ProxyMethod("noteOpNoThrow")
    public static class NoteOpNoThrow extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                int op = (int) args[0];
                String name = getOpPublicName(op);
                if (name != null && (name.contains("RECORD_AUDIO") || name.contains("AUDIO") || name.contains("MICROPHONE"))) {
                    Logger.d(TAG, "AppOps NoteOpNoThrow: Allowing RECORD_AUDIO operation: " + name);
                    return AppOpsManager.MODE_ALLOWED;
                }
            } catch (Throwable ignored) {
            }
            return method.invoke(who, args);
        }
    }

    private static boolean isMediaStorageOrAudioOp(String opPublicNameOrStr) {
        if (opPublicNameOrStr == null) return false;
        // Accept both public names and OPSTR strings
        String n = opPublicNameOrStr.toUpperCase();
        return n.contains("READ_MEDIA")
                || n.contains("READ_EXTERNAL_STORAGE")
                || n.contains("RECORD_AUDIO")
                || n.contains("CAPTURE_AUDIO_OUTPUT")
                || n.contains("MODIFY_AUDIO_SETTINGS")
                || n.contains("AUDIO")
                || n.contains("MICROPHONE")
                || n.contains("FOREGROUND_SERVICE")
                || n.contains("SYSTEM_ALERT_WINDOW")
                || n.contains("WRITE_SETTINGS")
                || n.contains("ACCESS_FINE_LOCATION")
                || n.contains("ACCESS_COARSE_LOCATION")
                || n.contains("CAMERA")
                || n.contains("BODY_SENSORS")
                || n.contains("BLUETOOTH_SCAN")
                || n.contains("BLUETOOTH_CONNECT")
                || n.contains("BLUETOOTH_ADVERTISE")
                || n.contains("NEARBY_WIFI_DEVICES")
                || n.contains("POST_NOTIFICATIONS");
    }

    private static String getOpPublicName(int op) {
        try {
            // AppOpsManager.opToPublicName was added in API 29
            java.lang.reflect.Method m = AppOpsManager.class.getMethod("opToPublicName", int.class);
            Object name = m.invoke(null, op);
            return name != null ? name.toString() : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private Object whoOrNull() {
        try {
            return getWho();
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Object buildAllowedReturnValue(Method method, Object who, Object[] args) throws Throwable {
        Class<?> rt = method.getReturnType();
        if (rt == void.class) return null;
        if (rt == int.class || rt == Integer.class) return AppOpsManager.MODE_ALLOWED;
        if (rt == boolean.class || rt == Boolean.class) return true;

        // Android 14+ AppOps note* can return android.app.SyncNotedAppOp (not an int).
        if ("android.app.SyncNotedAppOp".equals(rt.getName())) {
            // Prefer returning a correctly-typed object from framework to satisfy callers.
            Object original = null;
            if (who != null) {
                try {
                    original = method.invoke(who, args);
                } catch (Throwable ignored) {
                    original = null;
                }
            }
            Object allowed = (original != null) ? tryForceAllowedSyncNotedAppOp(original) : null;
            if (allowed != null) return allowed;
            return createAllowedSyncNotedAppOpFallback();
        }

        // Safe-ish default for reference return types when we can't forge a meaningful value.
        return null;
    }

    private static Object tryForceAllowedSyncNotedAppOp(Object syncNotedAppOp) {
        try {
            // Try common field names across Android releases.
            if (setIntFieldIfPresent(syncNotedAppOp, "mOpMode", AppOpsManager.MODE_ALLOWED)) return syncNotedAppOp;
            if (setIntFieldIfPresent(syncNotedAppOp, "mMode", AppOpsManager.MODE_ALLOWED)) return syncNotedAppOp;
            if (setIntFieldIfPresent(syncNotedAppOp, "opMode", AppOpsManager.MODE_ALLOWED)) return syncNotedAppOp;
            if (setIntFieldIfPresent(syncNotedAppOp, "mode", AppOpsManager.MODE_ALLOWED)) return syncNotedAppOp;
            return syncNotedAppOp;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static boolean setIntFieldIfPresent(Object obj, String fieldName, int value) {
        try {
            Field f = obj.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            f.setInt(obj, value);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static Object createAllowedSyncNotedAppOpFallback() {
        try {
            Class<?> cls = Class.forName("android.app.SyncNotedAppOp");
            // Try to instantiate with the "best effort" constructor:
            // we fill all ints with MODE_ALLOWED for the first int, others 0; objects null.
            Constructor<?>[] ctors = cls.getDeclaredConstructors();
            if (ctors == null || ctors.length == 0) return null;
            Constructor<?> c = ctors[0];
            c.setAccessible(true);
            Class<?>[] pts = c.getParameterTypes();
            Object[] params = new Object[pts.length];
            boolean firstIntSet = false;
            for (int i = 0; i < pts.length; i++) {
                Class<?> p = pts[i];
                if (p == int.class || p == Integer.class) {
                    if (!firstIntSet) {
                        params[i] = AppOpsManager.MODE_ALLOWED;
                        firstIntSet = true;
                    } else {
                        params[i] = 0;
                    }
                } else if (p == long.class || p == Long.class) {
                    params[i] = 0L;
                } else if (p == boolean.class || p == Boolean.class) {
                    params[i] = false;
                } else {
                    params[i] = null;
                }
            }
            Object obj = c.newInstance(params);
            // Best-effort ensure mode becomes allowed if field exists.
            tryForceAllowedSyncNotedAppOp(obj);
            return obj;
        } catch (Throwable ignored) {
            return null;
        }
    }
}
