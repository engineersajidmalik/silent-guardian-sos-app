package services;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Intent;
import android.os.SystemClock;
import android.util.Log;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Toast;

/**
 * Enhanced Accessibility Service for Volume Button Emergency Detection
 *
 * ISSUES FIXED:
 * 1. Missing proper service configuration for key event filtering
 * 2. Incorrect timing logic for 3-press detection
 * 3. Missing event consumption handling
 * 4. Added proper service info configuration
 *
 * HOW IT WORKS:
 * - Monitors volume down key events globally when enabled
 * - Detects 3 presses within 2 seconds
 * - Triggers emergency when pattern detected
 * - Consumes events to prevent system volume changes during emergency activation
 */
public class AccessibilityButtonService extends AccessibilityService {

    private static final String TAG = "AccessibilityButtonService";
    private static final long PRESS_TIME_WINDOW = 2000; // 2 seconds in milliseconds

    // Tracking arrays for precise timing
    private long[] pressTimestamps = new long[3];
    private int currentPressCount = 0;
    private long lastPressTime = 0;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Accessibility Service Created");
        initializeVolumeDownTracking(); // Changed from resetVolumeDownTracking()
    }

    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
        Log.d(TAG, "Accessibility Service Connected - Configuring...");

        // CRITICAL FIX: Proper service configuration for key event filtering
        AccessibilityServiceInfo info = new AccessibilityServiceInfo();

        // We need to monitor window state changes to ensure service stays active
        info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED |
                AccessibilityEvent.TYPE_VIEW_FOCUSED;

        // Generic feedback type
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;

        // CRITICAL FLAGS for volume key detection:
        // FLAG_REQUEST_FILTER_KEY_EVENTS - Allows us to receive key events
        // FLAG_REPORT_VIEW_IDS - Helps with debugging
        // FLAG_RETRIEVE_INTERACTIVE_WINDOWS - Ensures service stays active
        info.flags = AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS |
                AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS |
                AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS;

        // Set notification timeout
        info.notificationTimeout = 100;

        // Apply configuration
        setServiceInfo(info);

        Log.d(TAG, "Service configured with key event filtering enabled");

        // Show confirmation that service is active
        showServiceStatus(true);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // Monitor accessibility events for debugging
        if (event != null) {
            Log.d(TAG, "Accessibility Event: " + event.getEventType());
        }
    }

    @Override
    public void onInterrupt() {
        Log.w(TAG, "Accessibility Service Interrupted");
        showServiceStatus(false);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Accessibility Service Destroyed");
        showServiceStatus(false);
    }

    /**
     * CRITICAL METHOD: This is where volume down detection happens
     * FIXED: Proper event handling and consumption logic
     */
    @Override
    protected boolean onKeyEvent(KeyEvent event) {
        int keyCode = event.getKeyCode();
        int action = event.getAction();

        Log.d(TAG, "Key event received - KeyCode: " + keyCode + ", Action: " + action);

        // Only handle key down events (action = 0)
        if (action == KeyEvent.ACTION_DOWN) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_VOLUME_DOWN:
                    Log.d(TAG, "Volume Down Pressed");
                    return handleVolumeDownPress(); // Return true if pattern detected

                case KeyEvent.KEYCODE_VOLUME_UP:
                    Log.d(TAG, "Volume Up Pressed - Resetting counter");
                    resetVolumeDownCounter(); // Reset on volume up (prevents false positives)
                    break;

                default:
                    // Other keys don't affect our detection
                    break;
            }
        }

        // Return false for events we don't consume
        return super.onKeyEvent(event);
    }

    /**
     * FIXED: Enhanced volume down detection logic with proper timing validation
     */
    private boolean handleVolumeDownPress() {
        long currentTime = SystemClock.elapsedRealtime();

        // Check if this is the first press or if we should reset the counter
        if (currentPressCount == 0 ||
                (currentTime - lastPressTime) > PRESS_TIME_WINDOW) {

            // Reset counter if too much time has passed between presses
            resetVolumeDownCounter();
            pressTimestamps[0] = currentTime;
            currentPressCount = 1;
            Log.d(TAG, "Volume down sequence started - Press: " + currentPressCount);

        } else {
            // Continue existing sequence
            if (currentPressCount < 3) {
                pressTimestamps[currentPressCount] = currentTime;
                currentPressCount++;
                Log.d(TAG, "Volume down sequence continued - Press: " + currentPressCount);

                // Check if we've reached the activation count
                if (currentPressCount >= 3) {
                    // CRITICAL FIX: Verify all presses were within the time window
                    long firstPressTime = pressTimestamps[0];
                    long lastPressTime = pressTimestamps[2];
                    long totalTime = lastPressTime - firstPressTime;

                    Log.d(TAG, "Checking timing - First: " + firstPressTime +
                            ", Last: " + lastPressTime + ", Total: " + totalTime);

                    if (totalTime <= PRESS_TIME_WINDOW) {
                        Log.d(TAG, "✅ Volume down activation pattern detected! " +
                                "Time: " + totalTime + "ms");
                        triggerEmergencyActivation();
                        resetVolumeDownCounter();
                        return true; // CRITICAL: Consume the event to prevent system volume change
                    } else {
                        Log.d(TAG, "❌ Volume down sequence too slow: " + totalTime + "ms");
                        resetVolumeDownCounter();
                    }
                }
            }
        }

        lastPressTime = currentTime;
        return false; // Don't consume the event yet (waiting for more presses)
    }

    /**
     * Reset the volume down detection counter
     */
    private void resetVolumeDownCounter() {
        currentPressCount = 0;
        for (int i = 0; i < pressTimestamps.length; i++) {
            pressTimestamps[i] = 0;
        }
        Log.d(TAG, "Volume down counter reset");
    }

    /**
     * Initialize volume down tracking variables
     */
    private void initializeVolumeDownTracking() {
        resetVolumeDownCounter();
        lastPressTime = 0;
        Log.d(TAG, "Volume down tracking initialized");
    }

    /**
     * Trigger emergency activation
     */
    private void triggerEmergencyActivation() {
        Log.d(TAG, "Triggering emergency activation via volume button");

        // Show immediate visual feedback
        showEmergencyActivation();

        // Start emergency service
        EmergencyService.triggerEmergency(this);

        // Send broadcast to main activity if running
        Intent intent = new Intent("com.silentguardian.VOLUME_EMERGENCY_ACTIVATED");
        intent.putExtra("activation_method", "volume_down");
        sendBroadcast(intent);
    }

    /**
     * Show visual feedback for emergency activation
     */
    private void showEmergencyActivation() {
        try {
            Toast.makeText(this, "🚨 EMERGENCY ACTIVATED - Volume Down Pattern Detected!",
                    Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Log.e(TAG, "Error showing emergency activation toast: " + e.getMessage());
        }
    }

    /**
     * Show service status feedback
     */
    private void showServiceStatus(boolean isRunning) {
        try {
            if (isRunning) {
                Toast.makeText(this, "✓ Silent Guardian Volume Detection Active",
                        Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "⚠ Silent Guardian Volume Detection Stopped",
                        Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error showing service status: " + e.getMessage());
        }
    }

    /**
     * Check if accessibility service is properly enabled in system settings
     * FIXED: More reliable service status checking
     */
    public static boolean isServiceEnabled(android.content.Context context) {
        try {
            int enabled = android.provider.Settings.Secure.getInt(
                    context.getContentResolver(),
                    android.provider.Settings.Secure.ACCESSIBILITY_ENABLED
            );

            if (enabled == 1) {
                String settingValue = android.provider.Settings.Secure.getString(
                        context.getContentResolver(),
                        android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
                );

                if (settingValue != null) {
                    String serviceName = context.getPackageName() +
                            "/services.AccessibilityButtonService";
                    return settingValue.contains(serviceName);
                }
            }
        } catch (android.provider.Settings.SettingNotFoundException e) {
            Log.e(TAG, "Error checking accessibility service status: " + e.getMessage());
        }

        return false;
    }
}