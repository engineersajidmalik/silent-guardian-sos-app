package services;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.example.silentguardian.R;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import android.os.Handler;

import utils.DataManager;

/**
 * Main emergency service triggered by power button, SOS, etc.
 * Executes emergency actions (SMS sending).
 *
 * ENHANCED: Added better integration with volume button detection
 * Now provides immediate feedback when triggered from any method
 */
public class EmergencyService extends Service {

    private static final String CHANNEL_ID = "EmergencyServiceChannel";
    private static final int NOTIFICATION_ID = 1003;

    // Matches Activity_Settings.java
    private static final String KEY_SMS_ENABLED = "sms_enabled";

    private DataManager dataManager;
    private FusedLocationProviderClient fusedLocationClient;

    @Override
    public void onCreate() {
        super.onCreate();
        dataManager = new DataManager(this);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        createNotificationChannel();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(NOTIFICATION_ID, createNotification());

        String action = (intent != null) ? intent.getAction() : null;
        String triggerMethod = (intent != null) ? intent.getStringExtra("trigger_method") : "unknown";

        Log.d("EmergencyService", "Emergency triggered via: " + triggerMethod);

        if (action == null || "TRIGGER_EMERGENCY".equals(action)) {
            executeEmergencyActions(triggerMethod);
        } else if ("SEND_TEST_SMS".equals(action)) {
            handleTestEmergency();
        }

        return START_STICKY;
    }

    private void handleTestEmergency() {
        Toast.makeText(this, "📱 Test Emergency Mode", Toast.LENGTH_SHORT).show();
        SMSService.sendTestMessage(this, dataManager.getString("test_phone_number", ""));
    }

    /**
     * Execute all emergency actions with trigger method feedback
     */
    private void executeEmergencyActions(String triggerMethod) {
        // ENHANCED: Provide immediate feedback based on trigger method
        showTriggerFeedback(triggerMethod);

        // 1) Acquire location first
        getCurrentLocationForEmergency();

        // 2) After short wait, send SMS if enabled
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            // ✅ Read SMS switch from DataManager
            boolean smsEnabled = dataManager.getBoolean(KEY_SMS_ENABLED, true);

            // ✅ SMS (automatic) - only if enabled
            if (smsEnabled) {
                SMSService.startEmergencySMS(this);
                Toast.makeText(this, "✓ Emergency SMS sent! (" + triggerMethod + ")", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "SMS alerts are disabled in settings", Toast.LENGTH_LONG).show();
            }

            // 3) Optional: continuous tracking
            if (dataManager.getLocationUpdatesSetting()) {
                LocationService.startLocationTracking(this);
            }

            stopSelf();

        }, 1500); // Reduced delay for faster response
    }

    /**
     * ENHANCED: Show feedback based on how emergency was triggered
     */
    private void showTriggerFeedback(String triggerMethod) {
        String message;
        switch (triggerMethod) {
            case "volume_down":
                message = "🚨 Volume Button Emergency Activated!";
                break;
            case "shake":
                message = "🚨 Shake Emergency Activated!";
                break;
            case "sos_button":
                message = "🚨 SOS Button Emergency Activated!";
                break;
            default:
                message = "🚨 Emergency Activated!";
        }

        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();

        // Add vibration feedback
        try {
            android.os.Vibrator vibrator = (android.os.Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null && vibrator.hasVibrator()) {
                // Vibrate pattern: 3 short vibrations
                long[] pattern = {0, 200, 100, 200, 100, 200};
                vibrator.vibrate(pattern, -1);
            }
        } catch (Exception e) {
            Log.e("EmergencyService", "Vibration error: " + e.getMessage());
        }
    }

    /**
     * Get current location for emergency
     */
    private void getCurrentLocationForEmergency() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Location permission not granted", Toast.LENGTH_SHORT).show();
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        saveEmergencyLocation(location);
                    } else {
                        requestNewLocationForEmergency();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Location error: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    /**
     * Request a fresh location (single update)
     */
    private void requestNewLocationForEmergency() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        LocationRequest locationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(5000)
                .setFastestInterval(2000)
                .setNumUpdates(1);

        LocationCallback locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult != null && locationResult.getLastLocation() != null) {
                    saveEmergencyLocation(locationResult.getLastLocation());
                }
            }
        };

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());

        new Handler(Looper.getMainLooper()).postDelayed(() ->
                fusedLocationClient.removeLocationUpdates(locationCallback), 10000);
    }

    /**
     * Save emergency location to DataManager
     */
    private void saveEmergencyLocation(Location location) {
        String locationText = String.format("Lat: %.6f, Lng: %.6f",
                location.getLatitude(), location.getLongitude());

        dataManager.saveString("last_location", locationText);
        dataManager.saveString("last_location_time", String.valueOf(System.currentTimeMillis()));
        dataManager.saveString("emergency_location", locationText);
    }

    /**
     * Notification channel (Android 8+)
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Emergency Service Channel",
                    NotificationManager.IMPORTANCE_HIGH
            );
            serviceChannel.setDescription("Emergency alert service");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(serviceChannel);
        }
    }

    /**
     * Foreground notification
     */
    private Notification createNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("🚨 Silent Guardian")
                .setContentText("Emergency service active")
                .setSmallIcon(R.drawable.ic_notifications_24dp)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setOngoing(true)
                .build();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Toast.makeText(this, "Emergency service stopped", Toast.LENGTH_SHORT).show();
    }

    /**
     * ENHANCED: Trigger emergency service with trigger method tracking
     */
    public static void triggerEmergency(Context context, String triggerMethod) {
        Intent serviceIntent = new Intent(context, EmergencyService.class);
        serviceIntent.setAction("TRIGGER_EMERGENCY");
        serviceIntent.putExtra("trigger_method", triggerMethod);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent);
        } else {
            context.startService(serviceIntent);
        }
    }

    /**
     * Original method for backward compatibility
     */
    public static void triggerEmergency(Context context) {
        triggerEmergency(context, "unknown");
    }
}