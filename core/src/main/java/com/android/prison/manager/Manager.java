package com.android.prison.manager;

import android.os.IBinder;
import android.os.IInterface;
import com.android.prison.utils.Logger;
import java.lang.reflect.ParameterizedType;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import com.android.prison.system.SystemServer;
import com.android.prison.utils.Reflector;

/**
 * Created by Prison on 2022/3/23.
 */
public abstract class Manager<Service extends IInterface> {
    public static final String TAG = Manager.class.getSimpleName();

    private Service mService;
    private final AtomicBoolean mServiceCreationFailed = new AtomicBoolean(false);
    private long mLastRetryTime = 0;
    private long mLastServiceCreationTime = 0;
    private static final long RETRY_TIMEOUT_MS = 2000; // Shorter timeout
    private static final long MIN_SERVICE_CREATION_INTERVAL_MS = 50; // Shorter interval
    
    // Global service failure tracking to prevent cascade failures
    private static final AtomicInteger globalServiceFailureCount = new AtomicInteger(0);
    private static final long GLOBAL_FAILURE_RESET_INTERVAL_MS = 30000; // 30 seconds
    private static long lastGlobalFailureReset = 0;

    protected abstract String getServiceName();

    public Service getService() {
        // If service creation previously failed and we're within retry timeout, return null
        if (mServiceCreationFailed.get()) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - mLastRetryTime < RETRY_TIMEOUT_MS) {
                Logger.d(TAG, "Skipping service creation for " + getServiceName() + " due to recent failure");
                return null;
            }
            // Reset the flag after timeout
            mServiceCreationFailed.set(false);
        }

        if (mService != null && mService.asBinder().pingBinder() && mService.asBinder().isBinderAlive()) {
            return mService;
        }

        // Rate limiting: don't create services too frequently
        long currentTime = System.currentTimeMillis();
        if (currentTime - mLastServiceCreationTime < MIN_SERVICE_CREATION_INTERVAL_MS) {
            Logger.d(TAG, "Rate limiting service creation for " + getServiceName());
            return mService; // Return existing service (might be null)
        }

        try {
            IBinder binder = SystemServer.get().getService(getServiceName());
            if (binder == null) {
                Logger.w(TAG, "Failed to get binder for service: " + getServiceName());
                markServiceCreationFailed();
                return null;
            }
            
            // Check if binder is alive before proceeding
            if (!binder.isBinderAlive()) {
                Logger.w(TAG, "Binder is not alive for service: " + getServiceName());
                markServiceCreationFailed();
                return null;
            }
            
            String stubClassName = getTClass().getName() + "$Stub";
            Logger.d(TAG, "Creating service for: " + stubClassName);
            
            mService = Reflector.on(stubClassName).method("asInterface", IBinder.class)
                    .call(binder);
            
            if (mService != null) {
                // Additional health check
                try {
                    if (!mService.asBinder().isBinderAlive()) {
                        Logger.w(TAG, "Service binder is not alive after creation: " + getServiceName());
                        mService = null;
                        markServiceCreationFailed();
                        return null;
                    }
                } catch (Exception e) {
                    Logger.w(TAG, "Error checking service binder health: " + getServiceName(), e);
                    mService = null;
                    markServiceCreationFailed();
                    return null;
                }
                
                final Service serviceRef = mService; // Capture reference for death recipient
                try {
                    serviceRef.asBinder().linkToDeath(new IBinder.DeathRecipient() {
                        @Override
                        public void binderDied() {
                            try {
                                if (serviceRef != null && serviceRef.asBinder() != null) {
                                    serviceRef.asBinder().unlinkToDeath(this, 0);
                                }
                            } catch (Exception e) {
                                Logger.w(TAG, "Error unlinking death recipient for " + getServiceName(), e);
                            }
                            mService = null;
                            Logger.w(TAG, "Service died: " + getServiceName());
                        }
                    }, 0);
                } catch (Exception e) {
                    Logger.w(TAG, "Error linking death recipient for " + getServiceName(), e);
                    // Continue anyway, the service might still work
                }
                
                Logger.d(TAG, "Successfully created service: " + getServiceName());
                mServiceCreationFailed.set(false); // Reset failure flag on success
                mLastServiceCreationTime = currentTime; // Update creation time
            } else {
                Logger.w(TAG, "Failed to create service instance for: " + getServiceName());
                markServiceCreationFailed();
            }
            
            return mService;
        } catch (Throwable e) {
            Logger.e(TAG, "Error creating service for " + getServiceName(), e);
            markServiceCreationFailed();
            return null;
        }
    }

    private void markServiceCreationFailed() {
        mServiceCreationFailed.set(true);
        mLastRetryTime = System.currentTimeMillis();
    }
    
    /**
     * Clear the cached service - call this when the service dies
     */
    public void clearServiceCache() {
        mService = null;
        Logger.d(TAG, "Cleared service cache for " + getServiceName());
    }
    
    /**
     * Check if the service is healthy and available
     */
    public boolean isServiceHealthy() {
        if (mService == null) {
            return false;
        }
        try {
            return mService.asBinder().pingBinder() && mService.asBinder().isBinderAlive();
        } catch (Exception e) {
            Logger.w(TAG, "Service health check failed for " + getServiceName(), e);
            return false;
        }
    }

    private Class<Service> getTClass() {
        return (Class<Service>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
    }
}
