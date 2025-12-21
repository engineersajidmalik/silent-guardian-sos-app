package services;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.Toast;

import com.example.silentguardian.R;

import utils.DataManager;

public class VolumeButtonService extends Service {
    private static final String TAG = "VolumeButtonService";
    private static final String CHANNEL_ID = "VolumeButtonServiceChannel";
    private static final int NOTIFICATION_ID = 102;

    // Timing for 3 presses
    private static final long PRESS_TIME_WINDOW = 2000; // 2 seconds

    // Track presses
    private long[] pressTimes = new long[3];
    private int pressCount = 0;
    private Handler resetHandler = new Handler();

    // Broadcast receiver for button presses
    private BroadcastReceiver buttonReceiver;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "VolumeButtonService created");

        createNotificationChannel();
        setupButtonReceiver();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Volume Button Service",
                    NotificationManager.IMPORTANCE_LOW
            );
            serviceChannel.setDescription("Monitors volume button for emergency");

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private void setupButtonReceiver() {
        buttonReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                DataManager dataManager = new DataManager(VolumeButtonService.this);
                if (!dataManager.getPowerButtonEnabled()) {
                    return;
                }

                // Check for media button events
                if (Intent.ACTION_MEDIA_BUTTON.equals(intent.getAction())) {
                    KeyEvent event = intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
                    if (event != null && event.getAction() == KeyEvent.ACTION_DOWN) {
                        int keyCode = event.getKeyCode();

                        // Check for volume down button
                        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
                            Log.d(TAG, "Volume down button detected");
                            handleVolumeButtonPress();
                        }
                    }
                }
            }
        };

        // Register receiver for media button events
        IntentFilter filter = new IntentFilter(Intent.ACTION_MEDIA_BUTTON);
        filter.setPriority(1000); // High priority

        try {
            registerReceiver(buttonReceiver, filter);
        } catch (Exception e) {
            Log.e(TAG, "Failed to register button receiver: " + e.getMessage());
        }
    }

    private void handleVolumeButtonPress() {
        long currentTime = SystemClock.elapsedRealtime();

        // Reset old presses
        if (pressCount > 0 && (currentTime - pressTimes[0]) > PRESS_TIME_WINDOW) {
            pressCount = 0;
        }

        // Store this press time
        if (pressCount < 3) {
            pressTimes[pressCount] = currentTime;
            pressCount++;
            Log.d(TAG, "Press count: " + pressCount);
        }

        // Cancel any pending reset
        resetHandler.removeCallbacks(resetRunnable);

        // Check if we have 3 presses within time window
        if (pressCount == 3) {
            long timeBetweenFirstAndLast = pressTimes[2] - pressTimes[0];

            if (timeBetweenFirstAndLast <= PRESS_TIME_WINDOW) {
                // SUCCESS! 3 presses detected
                Log.d(TAG, "3 presses detected within " + timeBetweenFirstAndLast + "ms");
                triggerEmergency();

                // Reset immediately
                pressCount = 0;
                return;
            } else {
                // Too slow, keep only the last 2 presses
                Log.d(TAG, "Too slow: " + timeBetweenFirstAndLast + "ms");
                pressTimes[0] = pressTimes[1];
                pressTimes[1] = pressTimes[2];
                pressCount = 2;
            }
        }

        // Schedule reset if user stops pressing
        resetHandler.postDelayed(resetRunnable, PRESS_TIME_WINDOW);
    }

    private Runnable resetRunnable = new Runnable() {
        @Override
        public void run() {
            pressCount = 0;
            Log.d(TAG, "Press count reset");
        }
    };

    private void triggerEmergency() {
        Log.d(TAG, "Volume button emergency triggered!");

        // Show quick feedback
        Toast.makeText(this, "🚨 Emergency triggered via volume button!",
                Toast.LENGTH_SHORT).show();

        // Start emergency service
        EmergencyService.triggerEmergency(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Start foreground service with notification
        startForeground(NOTIFICATION_ID, createNotification());

        Log.d(TAG, "VolumeButtonService started");

        // Keep service running
        return START_STICKY;
    }

    private Notification createNotification() {
        // Create notification
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return new Notification.Builder(this, CHANNEL_ID)
                    .setContentTitle("Silent Guardian")
                    .setContentText("Volume button monitoring active")
                    .setSmallIcon(R.drawable.ic_notifications_24dp)
                    .setPriority(Notification.PRIORITY_LOW)
                    .build();
        } else {
            return new Notification.Builder(this)
                    .setContentTitle("Silent Guardian")
                    .setContentText("Volume button monitoring active")
                    .setSmallIcon(R.drawable.ic_notifications_24dp)
                    .setPriority(Notification.PRIORITY_LOW)
                    .build();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "VolumeButtonService destroyed");

        // Unregister receiver
        if (buttonReceiver != null) {
            try {
                unregisterReceiver(buttonReceiver);
            } catch (Exception e) {
                Log.e(TAG, "Error unregistering receiver: " + e.getMessage());
            }
        }
    }

    // Start the service
    public static void startService(Context context) {
        Intent serviceIntent = new Intent(context, VolumeButtonService.class);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent);
        } else {
            context.startService(serviceIntent);
        }
    }

    // Stop the service
    public static void stopService(Context context) {
        Intent serviceIntent = new Intent(context, VolumeButtonService.class);
        context.stopService(serviceIntent);
    }
}