package com.android.prison.system;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import com.android.prison.utils.Logger;
import androidx.core.app.NotificationCompat;

import com.android.prison.core.PrisonCore;
import com.android.prison.utils.BuildCompat;

/**
 * Enhanced DaemonService with proper foreground service support and error handling.
 * Fixed for Android 14+ compatibility and proper notification setup.
 */
public class DaemonService extends Service {
    public static final String TAG = DaemonService.class.getSimpleName();
    private static final int NOTIFY_ID = PrisonCore.getPackageName().hashCode();
    private static final String CHANNEL_ID = "prison_daemon_channel";
    private static final String CHANNEL_NAME = "Prison Daemon Service";
    private static final String CHANNEL_DESCRIPTION = "Keeps Prison core services running";

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Logger.d(TAG, "DaemonService onCreate");
        
        // Create notification channel for Android 8.0+
        if (BuildCompat.isOreo()) {
            createNotificationChannel();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Logger.d(TAG, "DaemonService onStartCommand");
        
        try {
            // Start the inner service
            Intent innerIntent = new Intent(this, DaemonInnerService.class);
            startService(innerIntent);
            
            // Start foreground service for Android 8.0+
            if (BuildCompat.isOreo()) {
                if (!startForegroundService()) {
                    Logger.w(TAG, "Failed to start foreground service, falling back to regular service");
                    return START_STICKY;
                }
            }
            
            Logger.d(TAG, "DaemonService started successfully");
            return START_STICKY;
            
        } catch (Exception e) {
            Logger.e(TAG, "Error starting DaemonService: " + e.getMessage(), e);
            // Return START_STICKY to allow the system to restart the service
            return START_STICKY;
        }
    }

    @Override
    public void onDestroy() {
        Logger.d(TAG, "DaemonService onDestroy");
        super.onDestroy();
    }

    /**
     * Create notification channel for Android 8.0+
     */
    private void createNotificationChannel() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_LOW
                );
                channel.setDescription(CHANNEL_DESCRIPTION);
                channel.setShowBadge(false);
                channel.setSound(null, null);
                channel.enableVibration(false);
                
                NotificationManager notificationManager = getSystemService(NotificationManager.class);
                if (notificationManager != null) {
                    notificationManager.createNotificationChannel(channel);
                    Logger.d(TAG, "Notification channel created successfully");
                }
            }
        } catch (Exception e) {
            Logger.e(TAG, "Failed to create notification channel: " + e.getMessage(), e);
        }
    }

    /**
     * Start foreground service with proper notification
     */
    private boolean startForegroundService() {
        try {
            Notification notification = createNotification();
            if (notification != null) {
                startForeground(NOTIFY_ID, notification);
                Logger.d(TAG, "Foreground service started successfully");
                return true;
            } else {
                Logger.e(TAG, "Failed to create notification");
                return false;
            }
        } catch (Exception e) {
            Logger.e(TAG, "Failed to start foreground service: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * Create proper notification for foreground service
     */
    private Notification createNotification() {
        try {
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Prison Core")
                .setContentText("Core services are running")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .setAutoCancel(false);
            
            return builder.build();
        } catch (Exception e) {
            Logger.e(TAG, "Failed to create notification: " + e.getMessage(), e);
            return null;
        }
    }

    /**
     * Inner service to handle notification cancellation
     */
    public static class DaemonInnerService extends Service {
        @Override
        public void onCreate() {
            Logger.i(TAG, "DaemonInnerService -> onCreate");
            super.onCreate();
        }

        @Override
        public int onStartCommand(Intent intent, int flags, int startId) {
            Logger.i(TAG, "DaemonInnerService -> onStartCommand");
            
            try {
                // Cancel the notification from the main service
                NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                if (nm != null) {
                    nm.cancel(NOTIFY_ID);
                    Logger.d(TAG, "Notification cancelled successfully");
                }
                
                // Stop this inner service
                stopSelf();
                return START_NOT_STICKY;
                
            } catch (Exception e) {
                Logger.e(TAG, "Error in DaemonInnerService: " + e.getMessage(), e);
                stopSelf();
                return START_NOT_STICKY;
            }
        }

        @Override
        public IBinder onBind(Intent intent) {
            return null;
        }

        @Override
        public void onDestroy() {
            Logger.i(TAG, "DaemonInnerService -> onDestroy");
            super.onDestroy();
        }
    }
}
