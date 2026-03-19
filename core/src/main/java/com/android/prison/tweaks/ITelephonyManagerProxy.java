package com.android.prison.tweaks;

import android.content.Context;
import android.os.IBinder;
import java.lang.reflect.Method;
import java.util.List;

import com.android.prison.base.MethodHook;
import com.android.prison.core.PrisonCore;
import com.android.prison.interfaces.android.os.BRServiceManager;
import com.android.prison.interfaces.com.android.internal.telephony.BRITelephonyStub;
import com.android.prison.base.PActivityThread;
import com.android.prison.entity.PCell;
import com.android.prison.manager.PLocationManager;
import com.android.prison.base.BinderInvocationStub;
import com.android.prison.base.ProxyMethod;
import com.android.prison.utils.Logger;
import com.android.prison.utils.Md5Utils;

public class ITelephonyManagerProxy extends BinderInvocationStub {
    public static final String TAG = ITelephonyManagerProxy.class.getSimpleName();

    public ITelephonyManagerProxy() {
        super(BRServiceManager.get().getService(Context.TELEPHONY_SERVICE));
    }

    @Override
    protected Object getWho() {
        IBinder telephony = BRServiceManager.get().getService(Context.TELEPHONY_SERVICE);
        return BRITelephonyStub.get().asInterface(telephony);
    }

    @Override
    protected void inject(Object baseInvocation, Object proxyInvocation) {
        replaceSystemService(Context.TELEPHONY_SERVICE);
    }

    @Override
    public boolean isBadEnv() {
        return false;
    }

    @ProxyMethod("getDeviceId")
    public static class GetDeviceId extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
//                MethodParameterUtils.replaceFirstAppPkg(args);
//                return method.invoke(who, args);
            Object original = method.invoke(who, args);
            if (original == null) return null;
            return Md5Utils.md5(PrisonCore.getPackageName());
        }
    }

    @ProxyMethod("getImeiForSlot")
    public static class getImeiForSlot extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
//                MethodParameterUtils.replaceFirstAppPkg(args);
//                return method.invoke(who, args);
            Object original = method.invoke(who, args);
            if (original == null) return null;
            return Md5Utils.md5(PrisonCore.getPackageName());
        }
    }

    @ProxyMethod("getMeidForSlot")
    public static class GetMeidForSlot extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
//                MethodParameterUtils.replaceFirstAppPkg(args);
//                return method.invoke(who, args);
            Object original = method.invoke(who, args);
            if (original == null) return null;
            return Md5Utils.md5(PrisonCore.getPackageName());
        }
    }

    @ProxyMethod("isUserDataEnabled")
    public static class IsUserDataEnabled extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            return true;
        }
    }


    @ProxyMethod("getLine1NumberForDisplay")
    public static class getLine1NumberForDisplay extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            return null;
        }
    }

    @ProxyMethod("getSubscriberId")
    public static class GetSubscriberId extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Object original = method.invoke(who, args);
            if (original == null) return null;
            return Md5Utils.md5(PrisonCore.getPackageName());
        }
    }

    @ProxyMethod("getDeviceIdWithFeature")
    public static class GetDeviceIdWithFeature extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Object original = method.invoke(who, args);
            if (original == null) return null;
            return Md5Utils.md5(PrisonCore.getPackageName());
        }
    }

    @ProxyMethod("getCellLocation")
    public static class GetCellLocation extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Logger.d(TAG, "getCellLocation");
            if (PLocationManager.isFakeLocationEnable()) {
                PCell cell = PLocationManager.get().getCell(PActivityThread.getUserId(), PActivityThread.getAppPackageName());
                if (cell != null) {
                    // TODO Transfer PCell to CdmaCellLocation/GsmCellLocation
                    return null;
                }
            }
            return method.invoke(who, args);
        }
    }

    @ProxyMethod("getAllCellInfo")
    public static class GetAllCellInfo extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            if (PLocationManager.isFakeLocationEnable()) {
                List<PCell> cell = PLocationManager.get().getAllCell(PActivityThread.getUserId(), PActivityThread.getAppPackageName());
                // TODO Transfer PCell to CdmaCellLocation/GsmCellLocation
                return cell;
            }
            try {
                return method.invoke(who, args);
            } catch (Throwable e) {
                return null;
            }
        }
    }

    @ProxyMethod("getNetworkOperator")
    public static class GetNetworkOperator extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Logger.d(TAG, "getNetworkOperator");
            return method.invoke(who, args);
        }
    }

    @ProxyMethod("getNetworkTypeForSubscriber")
    public static class GetNetworkTypeForSubscriber extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                return method.invoke(who, args);
            } catch (Throwable e) {
                return 0;
            }
        }
    }

    @ProxyMethod("getNeighboringCellInfo")
    public static class GetNeighboringCellInfo extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Logger.d(TAG, "getNeighboringCellInfo");
            if (PLocationManager.isFakeLocationEnable()) {
                List<PCell> cell = PLocationManager.get().getNeighboringCell(PActivityThread.getUserId(), PActivityThread.getAppPackageName());
                // TODO Transfer PCell to CdmaCellLocation/GsmCellLocation
                return null;
            }
            return method.invoke(who, args);
        }
    }
}
