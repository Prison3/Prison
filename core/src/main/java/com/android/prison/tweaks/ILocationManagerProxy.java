package com.android.prison.tweaks;

import android.content.Context;
import android.location.LocationManager;
import android.os.IInterface;
import com.android.prison.utils.Logger;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;

import com.android.prison.base.MethodHook;
import com.android.prison.interfaces.android.location.BRILocationManagerStub;
import com.android.prison.interfaces.android.location.provider.BRProviderProperties;
import com.android.prison.interfaces.android.os.BRServiceManager;
import com.android.prison.base.PActivityThread;
import com.android.prison.manager.PLocationManager;
import com.android.prison.base.BinderInvocationStub;
import com.android.prison.base.ProxyMethod;
import com.android.prison.utils.MethodParameterUtils;

public class ILocationManagerProxy extends BinderInvocationStub {
    public static final String TAG = ILocationManagerProxy.class.getSimpleName();

    public ILocationManagerProxy() {
        super(BRServiceManager.get().getService(Context.LOCATION_SERVICE));
    }

    @Override
    protected Object getWho() {
        return BRILocationManagerStub.get().asInterface(BRServiceManager.get().getService(Context.LOCATION_SERVICE));
    }

    @Override
    protected void inject(Object baseInvocation, Object proxyInvocation) {
        replaceSystemService(Context.LOCATION_SERVICE);
    }

    @Override
    public boolean isBadEnv() {
        return false;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
//        Logger.d(TAG, "call: " + method.getName());
        MethodParameterUtils.replaceFirstAppPkg(args);
        
        // Check if this is a Google Play Services process trying to access location
        String packageName = PActivityThread.getAppPackageName();
        if (packageName != null && packageName.equals("com.google.android.gms")) {
            // For Google Play Services, return null for location requests to prevent crashes
            if (method.getName().equals("getLastLocation") || 
                method.getName().equals("getLastKnownLocation") ||
                method.getName().equals("requestLocationUpdates")) {
                Logger.w(TAG, "Blocking location request from Google Play Services to prevent crash");
                return null;
            }
        }
        
        return super.invoke(proxy, method, args);
    }

    @ProxyMethod("registerGnssStatusCallback")
    public static class RegisterGnssStatusCallback extends MethodHook {

        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            // todo
            return true;
        }
    }

    @ProxyMethod("getLastLocation")
    public static class GetLastLocation extends MethodHook {

        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            if (PLocationManager.isFakeLocationEnable()) {
                return PLocationManager.get().getLocation(PActivityThread.getUserId(), PActivityThread.getAppPackageName()).convert2SystemLocation();
            }
            
            // Handle permission issues gracefully
            try {
                return method.invoke(who, args);
            } catch (Exception e) {
                if (e.getCause() instanceof SecurityException) {
                    Logger.w(TAG, "Location permission denied, returning null for getLastLocation");
                    return null;
                }
                throw e;
            }
        }
    }

    @ProxyMethod("getLastKnownLocation")
    public static class GetLastKnownLocation extends MethodHook {

        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            if (PLocationManager.isFakeLocationEnable()) {
                return PLocationManager.get().getLocation(PActivityThread.getUserId(), PActivityThread.getAppPackageName()).convert2SystemLocation();
            }
            
            // Handle permission issues gracefully
            try {
                return method.invoke(who, args);
            } catch (Exception e) {
                if (e.getCause() instanceof SecurityException) {
                    Logger.w(TAG, "Location permission denied, returning null for getLastKnownLocation");
                    return null;
                }
                throw e;
            }
        }
    }

    @ProxyMethod("requestLocationUpdates")
    public static class RequestLocationUpdates extends MethodHook {

        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            if (PLocationManager.isFakeLocationEnable()) {
                if (args[1] instanceof IInterface) {
                    IInterface listener = (IInterface) args[1];
                    PLocationManager.get().requestLocationUpdates(listener.asBinder());
                    return 0;
                }
            }
            
            // Handle permission issues gracefully
            try {
                return method.invoke(who, args);
            } catch (Exception e) {
                if (e.getCause() instanceof SecurityException) {
                    Logger.w(TAG, "Location permission denied for requestLocationUpdates, returning 0");
                    return 0;
                }
                throw e;
            }
        }
    }

    @ProxyMethod("removeUpdates")
    public static class RemoveUpdates extends MethodHook {

        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            if (args[0] instanceof IInterface) {
                IInterface listener = (IInterface) args[0];
                PLocationManager.get().removeUpdates(listener.asBinder());
                return 0;
            }
            return method.invoke(who, args);
        }
    }

    @ProxyMethod("getProviderProperties")
    public static class GetProviderProperties extends MethodHook {

        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Object providerProperties = method.invoke(who, args);
            if (PLocationManager.isFakeLocationEnable()) {
                BRProviderProperties.get(providerProperties)._set_mHasNetworkRequirement(false);
                if (PLocationManager.get().getCell(PActivityThread.getUserId(), PActivityThread.getAppPackageName()) == null) {
                    BRProviderProperties.get(providerProperties)._set_mHasCellRequirement(false);
                }
            }
            return method.invoke(who, args);
        }
    }

    @ProxyMethod("removeGpsStatusListener")
    public static class RemoveGpsStatusListener extends MethodHook {

        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            // todo
            return 0;
        }
    }

    @ProxyMethod("getBestProvider")
    public static class GetBestProvider extends MethodHook {

        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            if (PLocationManager.isFakeLocationEnable()) {
                return LocationManager.GPS_PROVIDER;
            }
            return method.invoke(who, args);
        }
    }

    @ProxyMethod("getAllProviders")
    public static class GetAllProviders extends MethodHook {

        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            return Arrays.asList(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER);
        }
    }

    @ProxyMethod("isProviderEnabledForUser")
    public static class isProviderEnabledForUser extends MethodHook {

        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            String provider = (String) args[0];
            return Objects.equals(provider, LocationManager.GPS_PROVIDER);
        }
    }

    @ProxyMethod("setExtraLocationControllerPackageEnabled")
    public static class setExtraLocationControllerPackageEnabled extends MethodHook {

        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            return 0;
        }
    }
}
