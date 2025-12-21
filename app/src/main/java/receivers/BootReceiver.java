package receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.Toast;
import services.LocationService;
import utils.DataManager;

/**
 * BootReceiver - Restarts necessary services when device boots up
 *
 * This receiver will:
 * 1. Check if location tracking was active before reboot
 * 2. Restart location service if needed
 * 3. Restart other emergency monitoring services
 */
public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) {
            return;
        }

        String action = intent.getAction();

        if (Intent.ACTION_BOOT_COMPLETED.equals(action) ||
                "android.intent.action.QUICKBOOT_POWERON".equals(action) ||
                Intent.ACTION_LOCKED_BOOT_COMPLETED.equals(action)) {

            handleBootCompleted(context);
        }
    }

    /**
     * Handle device boot completion
     */
    private void handleBootCompleted(Context context) {
        DataManager dataManager = new DataManager(context);

        // Check if location tracking was active before reboot
        boolean wasLocationTrackingActive = wasLocationTrackingActive(context);
        boolean locationUpdatesEnabled = dataManager.getLocationUpdatesSetting();

        // Check if emergency triggers were enabled
        boolean powerButtonEnabled = dataManager.getPowerButtonEnabled();

        // Restart location service if it was active and setting is enabled
        if (wasLocationTrackingActive && locationUpdatesEnabled) {
            restartLocationService(context);
        }

        // Show toast notification (optional, can be removed for stealth mode)
        if (dataManager.getStealthMode()) {
            // Silent restart for stealth mode
        } else {
            Toast.makeText(context, "Silent Guardian services restarted", Toast.LENGTH_SHORT).show();
        }

        // Log boot completion
        logBootEvent(context);
    }

    /**
     * Check if location tracking was active before reboot
     * We check SharedPreferences for last known state
     */
    private boolean wasLocationTrackingActive(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("SilentGuardianPrefs", Context.MODE_PRIVATE);

        // Check if we have recent location data (within last 5 minutes)
        long lastLocationTime = prefs.getLong("last_location_time", 0);
        long currentTime = System.currentTimeMillis();

        // If we had location data within 5 minutes before reboot, assume tracking was active
        if (lastLocationTime > 0 && (currentTime - lastLocationTime) < 5 * 60 * 1000) {
            return true;
        }

        // Also check if we have a specific flag for active tracking
        return prefs.getBoolean("location_tracking_active", false);
    }

    /**
     * Restart the location service
     */
    private void restartLocationService(Context context) {
        try {
            LocationService.startLocationTracking(context);

            // Update tracking flag
            SharedPreferences prefs = context.getSharedPreferences("SilentGuardianPrefs", Context.MODE_PRIVATE);
            prefs.edit().putBoolean("location_tracking_active", true).apply();

        } catch (Exception e) {
            // Log error but don't crash
            e.printStackTrace();
        }
    }

    /**
     * Log boot event for debugging
     */
    private void logBootEvent(Context context) {
        DataManager dataManager = new DataManager(context);

        String currentLogs = dataManager.getString("boot_logs", "");
        String timestamp = android.text.format.DateFormat.format("yyyy-MM-dd HH:mm:ss", System.currentTimeMillis()).toString();
        String newLog = "Device booted: " + timestamp;

        // Keep only last 10 boot logs
        String[] logs = (newLog + "\n" + currentLogs).split("\n");
        StringBuilder finalLogs = new StringBuilder();
        int count = Math.min(logs.length, 10);

        for (int i = 0; i < count; i++) {
            finalLogs.append(logs[i]);
            if (i < count - 1) {
                finalLogs.append("\n");
            }
        }

        dataManager.saveString("boot_logs", finalLogs.toString());
    }

    /**
     * Helper method to save location tracking state
     * Call this when starting/stopping location service
     */
    public static void saveLocationTrackingState(Context context, boolean isActive) {
        SharedPreferences prefs = context.getSharedPreferences("SilentGuardianPrefs", Context.MODE_PRIVATE);
        prefs.edit().putBoolean("location_tracking_active", isActive).apply();
    }

    /**
     * Helper method to get boot logs
     */
    public static String getBootLogs(Context context) {
        DataManager dataManager = new DataManager(context);
        return dataManager.getString("boot_logs", "No boot logs available");
    }

    /**
     * Clear boot logs
     */
    public static void clearBootLogs(Context context) {
        DataManager dataManager = new DataManager(context);
        dataManager.saveString("boot_logs", "");
    }
}