package services;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import utils.DataManager;

/**
 * Location Service - Gets and tracks user location, integrates with DataManager and BootReceiver
 */
public class LocationService extends Service {

    private static final String CHANNEL_ID = "LocationServiceChannel";
    private static final int NOTIFICATION_ID = 1001;
    private static final String TRACKING_STATE_PREF = "SilentGuardianPrefs";
    private static final String TRACKING_STATE_KEY = "location_tracking_active";

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private DataManager dataManager;
    private SharedPreferences trackingPrefs;

    @Override
    public void onCreate() {
        super.onCreate();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        dataManager = new DataManager(this);
        trackingPrefs = getSharedPreferences(TRACKING_STATE_PREF, Context.MODE_PRIVATE);
        createNotificationChannel();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(NOTIFICATION_ID, createNotification());

        if (intent != null) {
            String action = intent.getAction();
            if ("START_TRACKING".equals(action)) {
                startLocationUpdates();
            } else if ("STOP_TRACKING".equals(action)) {
                stopLocationUpdates();
                stopSelf();
            } else if ("GET_SINGLE_LOCATION".equals(action)) {
                getSingleLocation();
            }
        } else {
            // If no action specified, check if we should continue tracking from previous state
            if (wasTrackingActiveBefore()) {
                startLocationUpdates();
            }
        }

        return START_STICKY;
    }

    /**
     * Start continuous location updates
     */
    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Location permission needed", Toast.LENGTH_SHORT).show();
            saveTrackingState(false);
            stopSelf();
            return;
        }

        // Check if location updates are enabled in settings
        if (!dataManager.getLocationUpdatesSetting()) {
            Toast.makeText(this, "Location updates disabled in settings", Toast.LENGTH_SHORT).show();
            saveTrackingState(false);
            stopSelf();
            return;
        }

        LocationRequest locationRequest = LocationRequest.create()
                .setInterval(10000) // 10 seconds
                .setFastestInterval(5000) // 5 seconds
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    // Save latest location to DataManager
                    saveLocation(location);
                }
            }

            @Override
            public void onLocationAvailability(com.google.android.gms.location.LocationAvailability locationAvailability) {
                if (!locationAvailability.isLocationAvailable()) {
                    // Location services became unavailable
                    Toast.makeText(LocationService.this, "Location services unavailable", Toast.LENGTH_SHORT).show();
                }
            }
        };

        fusedLocationClient.requestLocationUpdates(locationRequest,
                locationCallback, Looper.getMainLooper());

        // Save tracking state for BootReceiver
        saveTrackingState(true);

        // Update notification with active status
        updateNotification(true);

        Toast.makeText(this, "Location tracking started", Toast.LENGTH_SHORT).show();
    }

    /**
     * Get a single location update
     */
    private void getSingleLocation() {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Location permission needed", Toast.LENGTH_SHORT).show();
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        saveLocation(location);
                        Toast.makeText(this, "Location updated", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "No location available", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to get location: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Stop location updates
     */
    private void stopLocationUpdates() {
        if (locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
            locationCallback = null;
        }

        // Save tracking state for BootReceiver
        saveTrackingState(false);

        // Update notification with stopped status
        updateNotification(false);

        Toast.makeText(this, "Location tracking stopped", Toast.LENGTH_SHORT).show();
    }

    /**
     * Save location to DataManager
     */
    private void saveLocation(Location location) {
        String locationText = String.format("Lat: %.6f, Lng: %.6f",
                location.getLatitude(), location.getLongitude());

        // Save to DataManager
        dataManager.saveString("last_location", locationText);
        dataManager.saveLong("last_location_time", System.currentTimeMillis());

        // Also save accuracy and other details if available
        if (location.hasAccuracy()) {
            dataManager.saveString("last_location_accuracy", String.format("%.1f meters", location.getAccuracy()));
        }

        if (location.hasSpeed()) {
            dataManager.saveString("last_location_speed", String.format("%.1f m/s", location.getSpeed()));
        }

        // Log location update
        logLocationUpdate(locationText);
    }

    /**
     * Log location updates for debugging
     */
    private void logLocationUpdate(String locationText) {
        String currentLogs = dataManager.getString("location_logs", "");
        String timestamp = android.text.format.DateFormat.format("HH:mm:ss", System.currentTimeMillis()).toString();
        String newLog = timestamp + ": " + locationText;

        // Keep only last 20 location logs
        String[] logs = (newLog + "\n" + currentLogs).split("\n");
        StringBuilder finalLogs = new StringBuilder();
        int count = Math.min(logs.length, 20);

        for (int i = 0; i < count; i++) {
            finalLogs.append(logs[i]);
            if (i < count - 1) {
                finalLogs.append("\n");
            }
        }

        dataManager.saveString("location_logs", finalLogs.toString());
    }

    /**
     * Save tracking state for BootReceiver
     */
    private void saveTrackingState(boolean isActive) {
        trackingPrefs.edit().putBoolean(TRACKING_STATE_KEY, isActive).apply();

        // Also save timestamp
        if (isActive) {
            trackingPrefs.edit().putLong("tracking_start_time", System.currentTimeMillis()).apply();
        } else {
            trackingPrefs.edit().remove("tracking_start_time").apply();
        }
    }

    /**
     * Check if tracking was active before (for BootReceiver)
     */
    private boolean wasTrackingActiveBefore() {
        return trackingPrefs.getBoolean(TRACKING_STATE_KEY, false);
    }

    /**
     * Get current location once
     */
    public String getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return "Location permission not granted";
        }

        try {
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(location -> {
                        if (location != null) {
                            saveLocation(location);
                        }
                    });

            String lastLocation = dataManager.getString("last_location", "Location unavailable");
            return lastLocation;

        } catch (Exception e) {
            return "Error getting location: " + e.getMessage();
        }
    }

    /**
     * Create notification channel (Android 8.0+)
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Location Service Channel",
                    NotificationManager.IMPORTANCE_LOW
            );
            serviceChannel.setDescription("Tracks location for emergency alerts");
            serviceChannel.setShowBadge(false);
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }

    /**
     * Create foreground service notification
     */
    private Notification createNotification() {
        String trackingStatus = wasTrackingActiveBefore() ? "Active" : "Inactive";

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Silent Guardian - Location")
                .setContentText("Tracking: " + trackingStatus)
                .setSmallIcon(android.R.drawable.ic_dialog_map)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setShowWhen(false);

        return builder.build();
    }

    /**
     * Update notification with current status
     */
    private void updateNotification(boolean isActive) {
        String trackingStatus = isActive ? "Active" : "Inactive";

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Silent Guardian - Location")
                .setContentText("Tracking: " + trackingStatus)
                .setSmallIcon(android.R.drawable.ic_dialog_map)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(isActive)
                .setOnlyAlertOnce(true)
                .setShowWhen(false);

        NotificationManager manager = getSystemService(NotificationManager.class);
        manager.notify(NOTIFICATION_ID, builder.build());
    }

    @Override
    public void onDestroy() {
        stopLocationUpdates();
        super.onDestroy();
    }

    /**
     * Helper method to start location tracking
     */
    public static void startLocationTracking(Context context) {
        Intent serviceIntent = new Intent(context, LocationService.class);
        serviceIntent.setAction("START_TRACKING");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent);
        } else {
            context.startService(serviceIntent);
        }
    }

    /**
     * Helper method to stop location tracking
     */
    public static void stopLocationTracking(Context context) {
        Intent serviceIntent = new Intent(context, LocationService.class);
        serviceIntent.setAction("STOP_TRACKING");
        context.startService(serviceIntent);
    }

    /**
     * Get single location update
     */
    public static void getSingleLocationUpdate(Context context) {
        Intent serviceIntent = new Intent(context, LocationService.class);
        serviceIntent.setAction("GET_SINGLE_LOCATION");
        context.startService(serviceIntent);
    }

    /**
     * Get last known location using DataManager
     */
    public static String getLastKnownLocation(Context context) {
        DataManager dataManager = new DataManager(context);
        return dataManager.getString("last_location", "No location data available");
    }

    /**
     * Get location logs for debugging
     */
    public static String getLocationLogs(Context context) {
        DataManager dataManager = new DataManager(context);
        return dataManager.getString("location_logs", "No location logs available");
    }

    /**
     * Clear location logs
     */
    public static void clearLocationLogs(Context context) {
        DataManager dataManager = new DataManager(context);
        dataManager.saveString("location_logs", "");
    }

    /**
     * Check if location tracking is currently active
     */
    public static boolean isLocationTrackingActive(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(TRACKING_STATE_PREF, Context.MODE_PRIVATE);
        return prefs.getBoolean(TRACKING_STATE_KEY, false);
    }

    /**
     * Get tracking duration in minutes
     */
    public static long getTrackingDuration(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(TRACKING_STATE_PREF, Context.MODE_PRIVATE);
        long startTime = prefs.getLong("tracking_start_time", 0);

        if (startTime == 0) {
            return 0;
        }

        return (System.currentTimeMillis() - startTime) / (1000 * 60); // Convert to minutes
    }
}