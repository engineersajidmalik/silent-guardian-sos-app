package services;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.IBinder;
import android.os.Vibrator;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import utils.DataManager;
import com.example.silentguardian.R;

public class ShakeDetectionService extends Service implements SensorEventListener {

    private static final String TAG = "ShakeDetectionService";
    private static final int NOTIFICATION_ID = 102;
    private static final String CHANNEL_ID = "silentguardian_channel";

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private DataManager dataManager;
    private Vibrator vibrator;

    // Shake detection variables - INCREASED THRESHOLDS FOR STRONGER SHAKES
    private float lastX, lastY, lastZ;
    private long lastUpdate;
    private static final int BASE_SHAKE_THRESHOLD = 2000; // INCREASED from 800
    private static final int TIME_THRESHOLD = 100;
    private int shakeCount = 0;
    private long lastShakeTime = 0;
    private static final int SHAKE_RESET_TIME = 2000;

    // Anti-spam variables
    private long lastEmergencyTriggerTime = 0;
    private static final int EMERGENCY_COOLDOWN = 10000; // 10 seconds cooldown between triggers

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Service created");

        dataManager = new DataManager(this);
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            Log.d(TAG, "Accelerometer: " + (accelerometer != null ? "Found" : "Not found"));
        }

        // Register broadcast receiver for cancellation
        IntentFilter filter = new IntentFilter();
        filter.addAction("SHAKE_EMERGENCY_CANCELLED");
        registerReceiver(cancelReceiver, filter);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Service started");

        // Create notification channel if not exists
        createNotificationChannel();

        // Start as foreground service
        startForegroundService();

        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
            Log.d(TAG, "Accelerometer registered");
        } else {
            Log.e(TAG, "No accelerometer available");
            stopSelf();
        }

        return START_STICKY;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                NotificationChannel channel = new NotificationChannel(
                        CHANNEL_ID,
                        "Silent Guardian",
                        NotificationManager.IMPORTANCE_LOW
                );
                channel.setDescription("Emergency alerts and background services");
                channel.setShowBadge(false);

                NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                manager.createNotificationChannel(channel);
                Log.d(TAG, "Notification channel created in service");
            } catch (Exception e) {
                Log.e(TAG, "Error creating notification channel: " + e.getMessage());
            }
        }
    }

    private void startForegroundService() {
        try {
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle("Silent Guardian")
                    .setContentText("Shake detection is active")
                    .setSmallIcon(R.drawable.ic_notifications_24dp)
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .setOngoing(true);

            startForeground(NOTIFICATION_ID, builder.build());
            Log.d(TAG, "Foreground service started successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error starting foreground service: " + e.getMessage());
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            long currentTime = System.currentTimeMillis();

            if ((currentTime - lastUpdate) > TIME_THRESHOLD) {
                long timeDifference = currentTime - lastUpdate;
                lastUpdate = currentTime;

                float x = event.values[0];
                float y = event.values[1];
                float z = event.values[2];

                // Calculate shake intensity - MORE AGGRESSIVE FILTERING
                float deltaX = Math.abs(x - lastX);
                float deltaY = Math.abs(y - lastY);
                float deltaZ = Math.abs(z - lastZ);

                // Use maximum delta for stronger shake detection
                float maxDelta = Math.max(Math.max(deltaX, deltaY), deltaZ);
                float speed = (maxDelta / timeDifference) * 10000;

                Log.d(TAG, "Shake speed calculated: " + speed + " (Max delta: " + maxDelta + ")");

                // Get sensitivity setting - ADJUSTED FOR STRONGER SHAKES
                int sensitivity = dataManager.getShakeSensitivity();
                int adjustedThreshold = BASE_SHAKE_THRESHOLD;

                switch (sensitivity) {
                    case 0: // Low - Needs VERY strong shaking (emergency level)
                        adjustedThreshold = 3500;
                        break;
                    case 1: // Medium - Needs strong shaking (recommended)
                        adjustedThreshold = 2500;
                        break;
                    case 2: // High - Needs moderate shaking
                        adjustedThreshold = 1800;
                        break;
                }

                Log.d(TAG, "Threshold: " + adjustedThreshold + " (Sensitivity: " + sensitivity + ")");

                if (speed > adjustedThreshold) {
                    long now = System.currentTimeMillis();

                    // Reset if too much time passed
                    if ((now - lastShakeTime) > SHAKE_RESET_TIME) {
                        shakeCount = 0;
                    }

                    shakeCount++;
                    lastShakeTime = now;

                    Log.d(TAG, "Strong shake detected! Count: " + shakeCount + ", Speed: " + speed);

                    // VIBRATE ON EACH STRONG SHAKE DETECTION
                    vibrateOnShake();

                    if (shakeCount >= 2) { // Need 2 strong shakes
                        // Check cooldown to prevent spam
                        if (System.currentTimeMillis() - lastEmergencyTriggerTime > EMERGENCY_COOLDOWN) {
                            Log.d(TAG, "✅ Strong shakes detected, triggering emergency...");
                            triggerEmergency();
                            lastEmergencyTriggerTime = System.currentTimeMillis();
                        } else {
                            Log.d(TAG, "Emergency trigger cooldown active, skipping");
                        }
                        shakeCount = 0;
                    }
                }

                lastX = x;
                lastY = y;
                lastZ = z;
            }
        }
    }

    /**
     * Vibrate when strong shake is detected
     */
    private void vibrateOnShake() {
        try {
            if (vibrator != null && vibrator.hasVibrator()) {
                // Short vibration for feedback
                vibrator.vibrate(150); // 150ms vibration for stronger feedback
                Log.d(TAG, "Vibrated on strong shake detection");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error vibrating: " + e.getMessage());
        }
    }

    private void triggerEmergency() {
        Log.d(TAG, "Emergency triggered by strong shake! Sending broadcast to MainActivity...");

        // Send broadcast to MainActivity to show confirmation dialog
        Intent broadcastIntent = new Intent("SHAKE_EMERGENCY_DETECTED");
        sendBroadcast(broadcastIntent);

        // Show notification that strong shake was detected
        showShakeDetectedNotification();

        Log.d(TAG, "Broadcast sent for shake emergency confirmation");
    }

    private void showShakeDetectedNotification() {
        try {
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle("Silent Guardian - Strong Shake Detected!")
                    .setContentText("Emergency confirmation required")
                    .setSmallIcon(R.drawable.ic_notifications_24dp)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true);

            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) {
                manager.notify(103, builder.build());
                Log.d(TAG, "Shake detection notification shown");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error showing shake notification: " + e.getMessage());
        }
    }

    /**
     * Broadcast receiver to handle cancellation from MainActivity
     */
    private BroadcastReceiver cancelReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("SHAKE_EMERGENCY_CANCELLED".equals(action)) {
                Log.d(TAG, "Shake emergency cancelled by user, resetting shake count");
                shakeCount = 0;
                lastShakeTime = 0;
            }
        }
    };

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Not used
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "Service destroyed");

        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }

        // Unregister broadcast receiver
        try {
            unregisterReceiver(cancelReceiver);
        } catch (Exception e) {
            Log.e(TAG, "Error unregistering receiver: " + e.getMessage());
        }

        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    // Static methods
    public static void startService(Context context) {
        try {
            Log.d(TAG, "Starting service...");
            Intent serviceIntent = new Intent(context, ShakeDetectionService.class);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent);
                Log.d(TAG, "startForegroundService called");
            } else {
                context.startService(serviceIntent);
                Log.d(TAG, "startService called");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error starting service: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void stopService(Context context) {
        try {
            Log.d(TAG, "Stopping service...");
            Intent serviceIntent = new Intent(context, ShakeDetectionService.class);
            context.stopService(serviceIntent);
        } catch (Exception e) {
            Log.e(TAG, "Error stopping service: " + e.getMessage());
        }
    }
}