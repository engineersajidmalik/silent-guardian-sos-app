package receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;

import services.EmergencyService;
import utils.DataManager;

public class VolumeButtonReceiver extends BroadcastReceiver {
    private static final String TAG = "VolumeButtonReceiver";

    // Timing for 3 presses (within 2 seconds)
    private static final long PRESS_TIME_WINDOW = 2000; // 2 seconds

    // Track press count and timestamps
    private static long[] pressTimes = new long[3];
    private static int pressCount = 0;

    // Handler for resetting count
    private static Handler resetHandler = new Handler();
    private static Runnable resetRunnable = new Runnable() {
        @Override
        public void run() {
            pressCount = 0;
        }
    };

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!"android.intent.action.MEDIA_BUTTON".equals(intent.getAction())) {
            return;
        }

        // Check if volume button feature is enabled
        DataManager dataManager = new DataManager(context);
        if (!dataManager.getPowerButtonEnabled()) {
            return;
        }

        // Get the key event
        android.view.KeyEvent event = intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
        if (event == null) {
            return;
        }

        // Check if it's a volume down press (not release)
        if (event.getKeyCode() == android.view.KeyEvent.KEYCODE_VOLUME_DOWN &&
                event.getAction() == android.view.KeyEvent.ACTION_DOWN) {

            handleVolumeButtonPress(context);
        }
    }

    private void handleVolumeButtonPress(Context context) {
        long currentTime = SystemClock.elapsedRealtime();

        // Reset old presses
        if (pressCount > 0 && (currentTime - pressTimes[0]) > PRESS_TIME_WINDOW) {
            pressCount = 0;
        }

        // Store this press time
        if (pressCount < 3) {
            pressTimes[pressCount] = currentTime;
            pressCount++;
        }

        // Cancel any pending reset
        resetHandler.removeCallbacks(resetRunnable);

        // Check if we have 3 presses within time window
        if (pressCount == 3) {
            long timeBetweenFirstAndLast = pressTimes[2] - pressTimes[0];

            if (timeBetweenFirstAndLast <= PRESS_TIME_WINDOW) {
                // SUCCESS! 3 presses detected
                triggerEmergency(context);

                // Reset immediately
                pressCount = 0;
                return;
            } else {
                // Too slow, keep only the last 2 presses
                pressTimes[0] = pressTimes[1];
                pressTimes[1] = pressTimes[2];
                pressCount = 2;
            }
        }

        // Schedule reset if user stops pressing
        resetHandler.postDelayed(resetRunnable, PRESS_TIME_WINDOW);
    }

    private void triggerEmergency(Context context) {
        Log.d(TAG, "Volume button emergency triggered!");

        // Show quick feedback
        Toast.makeText(context, "🚨 Emergency triggered via volume button!",
                Toast.LENGTH_SHORT).show();

        // Start emergency service
        EmergencyService.triggerEmergency(context);
    }

    // Manual reset for testing
    public static void resetManual() {
        pressCount = 0;
        resetHandler.removeCallbacks(resetRunnable);
    }
}