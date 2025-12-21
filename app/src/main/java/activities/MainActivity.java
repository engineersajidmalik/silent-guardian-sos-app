package activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.util.Log;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;

import services.AccessibilityButtonService;
import utils.BaseActivity;
import utils.DataManager;
import services.EmergencyService;
import services.LocationService;
import services.SMSService;
import services.VolumeButtonService;
import services.ShakeDetectionService;

import com.example.silentguardian.R;
import com.google.android.material.switchmaterial.SwitchMaterial;

/**
 * Main Activity for Silent Guardian SOS App
 *
 * ENHANCED: Added volume button detection integration
 * Now properly handles volume button service and provides user feedback
 */
public class MainActivity extends BaseActivity {

    // UI Components
    private SwitchMaterial switchStealth, switchVolume, switchShake;
    private TextView tvStealthStatus, tvGpsStatus, tvContactsStatus, tvNetworkStatus, tvVolumeStatus; // ADDED: Volume status
    private View sosContainer, cardGps, cardContacts, cardNetwork, cardVolume; // ADDED: Volume card

    // App State
    private boolean isStealthMode = false;
    private boolean isVolumeButtonEnabled = false;
    private boolean isShakeDetectionEnabled = false;

    // Data Manager
    private DataManager dataManager;

    // Permission request codes
    private static final int SMS_PERMISSION_REQUEST_CODE = 100;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 101;
    private static final int EMERGENCY_LOCATION_REQUEST_CODE = 102;

    // Broadcast Receivers
    private ShakeEmergencyReceiver shakeEmergencyReceiver;
    private VolumeEmergencyReceiver volumeEmergencyReceiver; // ADDED: Volume emergency receiver

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // CRITICAL: Create notification channel before any services start
        createNotificationChannel();

        // Initialize broadcast receivers
        shakeEmergencyReceiver = new ShakeEmergencyReceiver();
        volumeEmergencyReceiver = new VolumeEmergencyReceiver(); // ADDED

        initializeUIComponents();
        loadSavedSettings();
        setupAllEventListeners();
        checkRequiredPermissions();
        updateAllUI();

        // Initialize DataManager
        dataManager = new DataManager(this);

        // Migrate old settings if needed
        dataManager.migrateOldSettings();

        // ENHANCED: Check accessibility service status and update UI
        updateVolumeButtonStatus();

        // Start volume button service if enabled
        if (dataManager.getVolumeButtonEnabled()) {
            VolumeButtonService.startService(this);
        }

        // Start shake detection service if enabled
        if (dataManager.getShakeDetectionEnabled()) {
            ShakeDetectionService.startService(this);
        }

        // Don't highlight any bottom nav in MainActivity
        resetAllBottomNavItems();
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    @Override
    protected void onResume() {
        super.onResume();
        updateAllUI();
        resetAllBottomNavItems();

        // Register broadcast receivers
        IntentFilter shakeFilter = new IntentFilter();
        shakeFilter.addAction("SHAKE_EMERGENCY_DETECTED");
        shakeFilter.addAction("SHAKE_EMERGENCY_CANCELLED");
        registerReceiver(shakeEmergencyReceiver, shakeFilter);

        // ADDED: Register volume emergency receiver
        IntentFilter volumeFilter = new IntentFilter();
        volumeFilter.addAction("com.silentguardian.VOLUME_EMERGENCY_ACTIVATED");
        registerReceiver(volumeEmergencyReceiver, volumeFilter);

        Log.d("MainActivity", "Broadcast receivers registered");

        // ENHANCED: Check accessibility service status when returning from settings
        updateVolumeButtonStatus();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Unregister broadcast receivers
        try {
            unregisterReceiver(shakeEmergencyReceiver);
            unregisterReceiver(volumeEmergencyReceiver); // ADDED
            Log.d("MainActivity", "Broadcast receivers unregistered");
        } catch (Exception e) {
            Log.e("MainActivity", "Error unregistering receivers: " + e.getMessage());
        }
    }

    /**
     * ADDED: Broadcast Receiver for volume button emergency detection
     */
    private class VolumeEmergencyReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d("VolumeEmergencyReceiver", "Received action: " + action);

            if ("com.silentguardian.VOLUME_EMERGENCY_ACTIVATED".equals(action)) {
                String method = intent.getStringExtra("activation_method");
                if ("volume_down".equals(method)) {
                    // Show immediate feedback for volume button emergency
                    showVolumeEmergencyFeedback();
                }
            }
        }
    }

    /**
     * ADDED: Show feedback when volume button emergency is detected
     */
    private void showVolumeEmergencyFeedback() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // Vibrate for feedback
                vibrateOnEmergency();

                // Update SOS button visual feedback
                updateSOSButton(true);

                // Show quick confirmation
                Toast.makeText(MainActivity.this, "🚨 Emergency activated via Volume Button!", Toast.LENGTH_SHORT).show();

                // Reset SOS button after delay
                new Handler().postDelayed(() -> {
                    updateSOSButton(false);
                }, 2000);
            }
        });
    }

    /**
     * Broadcast Receiver for shake detection
     */
    private class ShakeEmergencyReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d("ShakeEmergencyReceiver", "Received action: " + action);

            if ("SHAKE_EMERGENCY_DETECTED".equals(action)) {
                // Show confirmation dialog for shake detection
                showShakeEmergencyConfirmationDialog();
            } else if ("SHAKE_EMERGENCY_CANCELLED".equals(action)) {
                // Shake emergency was cancelled (do nothing special)
                Log.d("ShakeEmergencyReceiver", "Shake emergency cancelled by user");
            }
        }
    }

    /**
     * Show emergency confirmation dialog for shake detection
     */
    private void showShakeEmergencyConfirmationDialog() {
        // Check if activity is still active
        if (isFinishing() || isDestroyed()) {
            Log.e("MainActivity", "Activity not active, cannot show dialog");
            return;
        }

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                View dialogView = getLayoutInflater().inflate(R.layout.dialog_emergency_confirmation, null);
                builder.setView(dialogView);

                AlertDialog emergencyDialog = builder.create();
                emergencyDialog.setCancelable(false);
                emergencyDialog.setCanceledOnTouchOutside(false);

                TextView dialogCountdown = dialogView.findViewById(R.id.dialogCountdown);
                TextView dialogDetails = dialogView.findViewById(R.id.dialogDetails);
                MaterialButton btnCancel = dialogView.findViewById(R.id.btnCancel);
                MaterialButton btnSendNow = dialogView.findViewById(R.id.btnSendNow);

                int contactCount = dataManager.getContactCount();
                dialogDetails.setText("SHAKE DETECTED!\n\nThis will:\n• Send SMS to " + contactCount + " contact" + (contactCount > 1 ? "s" : "") +
                        "\n• Share your location\n• Activate emergency mode");

                // Get SOS delay from settings (default 5 seconds)
                int delaySeconds = dataManager.getSOSDelayInMillis() / 1000;
                if (delaySeconds <= 0) delaySeconds = 5;

                final int[] secondsLeft = {delaySeconds};
                btnSendNow.setText("SEND NOW (" + secondsLeft[0] + ")");

                final CountDownTimer dialogTimer = new CountDownTimer(delaySeconds * 1000L, 1000) {
                    @Override
                    public void onTick(long millisUntilFinished) {
                        secondsLeft[0] = (int) (millisUntilFinished / 1000);
                        dialogCountdown.setText("Sending in: " + secondsLeft[0] + " second" + (secondsLeft[0] > 1 ? "s" : "") + "...");
                        btnSendNow.setText("SEND NOW (" + secondsLeft[0] + ")");
                        if (secondsLeft[0] <= 3) {
                            dialogCountdown.setTextColor(ContextCompat.getColor(MainActivity.this, android.R.color.holo_red_dark));
                        }
                    }

                    @Override
                    public void onFinish() {
                        emergencyDialog.dismiss();
                        triggerShakeEmergency();
                    }
                };

                dialogTimer.start();

                btnCancel.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dialogTimer.cancel();
                        emergencyDialog.dismiss();
                        Toast.makeText(MainActivity.this, "Shake emergency cancelled", Toast.LENGTH_SHORT).show();

                        // Send broadcast to service to reset shake count
                        Intent cancelIntent = new Intent("SHAKE_EMERGENCY_CANCELLED");
                        sendBroadcast(cancelIntent);
                    }
                });

                btnSendNow.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dialogTimer.cancel();
                        emergencyDialog.dismiss();
                        triggerShakeEmergency();
                    }
                });

                emergencyDialog.show();
                Window window = emergencyDialog.getWindow();
                if (window != null) {
                    window.setBackgroundDrawableResource(android.R.color.transparent);
                    window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                }

                Log.d("MainActivity", "Shake emergency confirmation dialog shown");
            }
        });
    }

    /**
     * Trigger emergency from shake detection
     */
    private void triggerShakeEmergency() {
        Log.d("MainActivity", "Triggering emergency from shake detection");

        // VIBRATE when emergency is confirmed (longer vibration)
        vibrateOnEmergency();

        // Update SOS button visual feedback
        updateSOSButton(true);

        // Get current location before emergency
        getCurrentLocationBeforeEmergency();

        // Start Emergency Service
        EmergencyService.triggerEmergency(this);

        // Show quick confirmation
        Toast.makeText(this, "🚨 Emergency alert sent! (Shake)", Toast.LENGTH_SHORT).show();

        // Show detailed confirmation after 1 second
        new Handler().postDelayed(() -> {
            showEmergencySentConfirmation("Shake detected - ");
        }, 1000);

        // Reset SOS button after delay
        new Handler().postDelayed(() -> {
            updateSOSButton(false);
        }, 2000);
    }

    /**
     * Vibrate when emergency is confirmed
     */
    private void vibrateOnEmergency() {
        try {
            android.os.Vibrator vibrator = (android.os.Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null && vibrator.hasVibrator()) {
                // Pattern: wait 0ms, vibrate 500ms, wait 200ms, vibrate 300ms
                long[] pattern = {0, 500, 200, 300};
                vibrator.vibrate(pattern, -1); // -1 means don't repeat
                Log.d("MainActivity", "Emergency vibration triggered");
            }
        } catch (Exception e) {
            Log.e("MainActivity", "Error vibrating: " + e.getMessage());
        }
    }

    /**
     * Show confirmation after emergency is sent (with custom prefix)
     */
    private void showEmergencySentConfirmation(String prefix) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("🚨 EMERGENCY ALERT SENT!");

        String lastLocation = LocationService.getLastKnownLocation(this);
        int contactCount = dataManager.getContactCount();

        String message = prefix + "Help is on the way!\n\n" +
                "✓ SMS sent to " + contactCount + " emergency contact" + (contactCount > 1 ? "s" : "") + "\n" +
                "✓ Location shared: " + lastLocation + "\n" +
                "✓ Stay calm and find a safe place\n\n" +
                "Emergency services have been notified.";

        builder.setMessage(message);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.setCancelable(false);
        builder.show();
    }

    /**
     * CRITICAL: Create notification channel for Android Oreo+
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                CharSequence name = "Silent Guardian";
                String description = "Emergency alerts and background services";
                int importance = NotificationManager.IMPORTANCE_LOW;

                NotificationChannel channel = new NotificationChannel("silentguardian_channel", name, importance);
                channel.setDescription(description);
                channel.setShowBadge(false);

                NotificationManager notificationManager = getSystemService(NotificationManager.class);
                notificationManager.createNotificationChannel(channel);

                Log.d("MainActivity", "Notification channel created successfully");
            } catch (Exception e) {
                Log.e("MainActivity", "Error creating notification channel: " + e.getMessage());
            }
        }
    }

    /**
     * Initialize all UI components
     * FIXED: Added missing volume button UI components initialization
     */
    private void initializeUIComponents() {
        // Initialize switches
        switchStealth = findViewById(R.id.switchStealth);
        switchVolume = findViewById(R.id.switchVolume);
        switchShake = findViewById(R.id.switchShake);

        // Initialize text views
        tvStealthStatus = findViewById(R.id.tvStealthStatus);
        tvGpsStatus = findViewById(R.id.tvGpsStatus);
        tvContactsStatus = findViewById(R.id.tvContactsStatus);
        tvNetworkStatus = findViewById(R.id.tvNetworkStatus);

        // Initialize clickable views
        sosContainer = findViewById(R.id.sosContainer);
        cardGps = findViewById(R.id.cardGps);
        cardContacts = findViewById(R.id.cardContacts);
        cardNetwork = findViewById(R.id.cardNetwork);

        // Initialize bottom nav (from BaseActivity)
        findBottomNavViews();
    }

    /**
     * Load saved settings from DataManager
     */
    private void loadSavedSettings() {
        dataManager = new DataManager(this);

        isStealthMode = dataManager.getStealthMode();
        isVolumeButtonEnabled = dataManager.getVolumeButtonEnabled();
        isShakeDetectionEnabled = dataManager.getShakeDetectionEnabled();

        // Apply to switches
        switchStealth.setChecked(isStealthMode);
        switchVolume.setChecked(isVolumeButtonEnabled);
        switchShake.setChecked(isShakeDetectionEnabled);

        updateStealthStatusText();
    }

    /**
     * Setup all event listeners
     * FIXED: Added missing volume button card click handler
     */
    private void setupAllEventListeners() {
        // SOS Button Click - Show emergency confirmation dialog
        sosContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Add haptic feedback
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    sosContainer.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK);
                }

                checkPermissionsAndStartEmergency();
            }
        });

        // SOS Button Long Click - Test Mode
        sosContainer.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                // Add haptic feedback
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    sosContainer.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                }

                showTestEmergencyDialog();
                return true;
            }
        });

        // Stealth Mode Switch
        switchStealth.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                handleStealthToggle(isChecked);
            }
        });

        // Volume Button Switch
        switchVolume.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                handleVolumeButtonToggle(isChecked);
            }
        });

        // Shake Detection Switch
        switchShake.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                handleShakeDetectionToggle(isChecked);
            }
        });

        // GPS Card Click
        cardGps.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestLocationPermission();
            }
        });

        // Contacts Card Click
        cardContacts.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openContactsScreen();
            }
        });

        // Network Card Click
        cardNetwork.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestSMSPermission();
            }
        });

        // ADDED: Volume Button Card Click Handler
        initializeVolumeButtonCard();

        // Bottom Navigation
        setupBottomNavigation();
    }

    /**
     * Check and request permissions
     */
    private void checkRequiredPermissions() {
        // Check notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        200);
            }
        }

        // Check SMS permission
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.SEND_SMS}, SMS_PERMISSION_REQUEST_CODE);
        }

        // Check Location permission
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    /**
     * Update all UI elements
     * FIXED: Added volume button status to updateAllUI()
     */
    private void updateAllUI() {
        updateStealthStatusText();
        updateGpsStatus();
        updateContactsStatus();
        updateNetworkStatus();
        updateVolumeButtonStatus(); // ADDED: Update volume button status
        updateSOSButton(false);
    }

    // ========== EMERGENCY FUNCTIONS ==========

    private void checkPermissionsAndStartEmergency() {
        if (dataManager.getContactCount() == 0) {
            Toast.makeText(this, "No emergency contacts added!", Toast.LENGTH_LONG).show();
            openContactsScreen();
            return;
        }

        // Check SMS permission
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "SMS permission required for emergency alerts", Toast.LENGTH_LONG).show();
            requestSMSPermission();
            return;
        }

        // Check Location permission
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            showLocationPermissionDialogForEmergency();
            return;
        }

        showEmergencyConfirmationDialog();
    }

    private void showLocationPermissionDialogForEmergency() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Location Required for Emergency");
        builder.setMessage("Silent Guardian needs your location to:\n\n" +
                "📍 Send your exact coordinates to emergency contacts\n" +
                "📍 Help rescuers find you quickly\n" +
                "📍 Provide accurate location information\n\n" +
                "Without location, contacts won't know where you are!");
        builder.setPositiveButton("GRANT LOCATION", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        EMERGENCY_LOCATION_REQUEST_CODE);
            }
        });
        builder.setNegativeButton("SEND WITHOUT LOCATION", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                showEmergencyConfirmationDialog();
            }
        });
        builder.setCancelable(false);
        builder.show();
    }

    private void showEmergencyConfirmationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_emergency_confirmation, null);
        builder.setView(dialogView);

        AlertDialog emergencyDialog = builder.create();
        emergencyDialog.setCancelable(false);
        emergencyDialog.setCanceledOnTouchOutside(false);

        TextView dialogCountdown = dialogView.findViewById(R.id.dialogCountdown);
        TextView dialogDetails = dialogView.findViewById(R.id.dialogDetails);
        MaterialButton btnCancel = dialogView.findViewById(R.id.btnCancel);
        MaterialButton btnSendNow = dialogView.findViewById(R.id.btnSendNow);

        int contactCount = dataManager.getContactCount();
        dialogDetails.setText("This will:\n• Send SMS to " + contactCount + " contact" + (contactCount > 1 ? "s" : "") +
                "\n• Share your location\n• Activate emergency mode");

        int delaySeconds = dataManager.getSOSDelayInMillis() / 1000;
        if (delaySeconds <= 0) delaySeconds = 5;

        final int[] secondsLeft = {delaySeconds};
        btnSendNow.setText("SEND NOW (" + secondsLeft[0] + ")");

        final CountDownTimer dialogTimer = new CountDownTimer(delaySeconds * 1000L, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                secondsLeft[0] = (int) (millisUntilFinished / 1000);
                dialogCountdown.setText("Sending in: " + secondsLeft[0] + " second" + (secondsLeft[0] > 1 ? "s" : "") + "...");
                btnSendNow.setText("SEND NOW (" + secondsLeft[0] + ")");
                if (secondsLeft[0] <= 3) {
                    dialogCountdown.setTextColor(ContextCompat.getColor(MainActivity.this, android.R.color.holo_red_dark));
                }
            }

            @Override
            public void onFinish() {
                emergencyDialog.dismiss();
                triggerEmergency();
            }
        };

        dialogTimer.start();

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialogTimer.cancel();
                emergencyDialog.dismiss();
                Toast.makeText(MainActivity.this, "Emergency cancelled", Toast.LENGTH_SHORT).show();
            }
        });

        btnSendNow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialogTimer.cancel();
                emergencyDialog.dismiss();
                triggerEmergency();
            }
        });

        emergencyDialog.show();
        Window window = emergencyDialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(android.R.color.transparent);
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    private void triggerEmergency() {
        updateSOSButton(true);
        getCurrentLocationBeforeEmergency();
        EmergencyService.triggerEmergency(this);
        Toast.makeText(this, "🚨 Emergency alert sent!", Toast.LENGTH_SHORT).show();

        new Handler().postDelayed(() -> {
            showEmergencySentConfirmation("");
        }, 1000);

        new Handler().postDelayed(() -> {
            updateSOSButton(false);
        }, 2000);
    }

    private void getCurrentLocationBeforeEmergency() {
        try {
            LocationService.getSingleLocationUpdate(this);
        } catch (Exception e) {
            Toast.makeText(this, "Note: Location unavailable - " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void showTestEmergencyDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Test Emergency Mode");
        builder.setMessage("Send a TEST alert to your emergency contacts?");
        builder.setPositiveButton("SEND TEST", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                sendTestEmergency();
            }
        });
        builder.setNegativeButton("CANCEL", null);
        builder.show();
    }

    private void sendTestEmergency() {
        if (dataManager.getContactCount() == 0) {
            Toast.makeText(this, "No emergency contacts to test with", Toast.LENGTH_LONG).show();
            return;
        }

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "SMS permission needed for test", Toast.LENGTH_LONG).show();
            return;
        }

        SMSService.sendTestMessage(this, dataManager.getString("test_phone_number", ""));
        Toast.makeText(this, "Test alert sent to contacts", Toast.LENGTH_SHORT).show();
    }

    // ========== SETTINGS HANDLERS ==========

    private void handleStealthToggle(boolean isEnabled) {
        isStealthMode = isEnabled;
        dataManager.saveStealthMode(isStealthMode);
        updateStealthStatusText();
        Toast.makeText(this, "Stealth Mode: " + (isEnabled ? "ON" : "OFF"), Toast.LENGTH_SHORT).show();
    }

    private void handleVolumeButtonToggle(boolean isEnabled) {
        isVolumeButtonEnabled = isEnabled;
        dataManager.saveVolumeButtonEnabled(isVolumeButtonEnabled);

        if (isEnabled) {
            VolumeButtonService.startService(this);
            Toast.makeText(this, "Volume button trigger ON", Toast.LENGTH_SHORT).show();
        } else {
            VolumeButtonService.stopService(this);
            Toast.makeText(this, "Volume button trigger OFF", Toast.LENGTH_SHORT).show();
        }
    }

    private void handleShakeDetectionToggle(boolean isEnabled) {
        isShakeDetectionEnabled = isEnabled;
        dataManager.saveShakeDetectionEnabled(isShakeDetectionEnabled);

        if (isEnabled) {
            try {
                ShakeDetectionService.startService(this);
                Toast.makeText(this, "Shake detection ON", Toast.LENGTH_SHORT).show();
                showShakeDetectionInstructions();
            } catch (Exception e) {
                Log.e("MainActivity", "Error starting shake service: " + e.getMessage());
                Toast.makeText(this, "Error starting shake detection: " + e.getMessage(), Toast.LENGTH_LONG).show();
                // Rollback the switch if service fails
                switchShake.setChecked(false);
                isShakeDetectionEnabled = false;
                dataManager.saveShakeDetectionEnabled(false);
            }
        } else {
            try {
                ShakeDetectionService.stopService(this);
                Toast.makeText(this, "Shake detection OFF", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Log.e("MainActivity", "Error stopping shake service: " + e.getMessage());
            }
        }
    }

    private void showShakeDetectionInstructions() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("How to Use Shake Detection");
        builder.setMessage("📱 SHAKE TO TRIGGER EMERGENCY:\n\n" +
                "1. Enable Shake Detection in settings\n" +
                "2. Shake phone vigorously 2-3 times\n" +
                "3. 5-second confirmation dialog will appear\n" +
                "4. Works even when screen is OFF\n" +
                "5. Cancel within 5 seconds to prevent false activation\n\n" +
                "Tips:\n" +
                "• Test with gentle shakes first\n" +
                "• Adjust sensitivity in Settings\n" +
                "• Battery usage is minimal");
        builder.setPositiveButton("GOT IT", null);
        builder.show();
    }

    // ========== UI UPDATE METHODS ==========

    private void updateStealthStatusText() {
        tvStealthStatus.setText(isStealthMode ? "ACTIVE" : "INACTIVE");
    }

    private void updateGpsStatus() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            tvGpsStatus.setText("GPS: READY");
        } else {
            tvGpsStatus.setText("GPS: PERMISSION NEEDED");
        }
    }

    private void updateContactsStatus() {
        int contactCount = dataManager.getContactCount();
        String statusText;

        if (contactCount == 0) {
            statusText = "Contacts: NONE (Add in Settings)";
        } else if (contactCount == 1) {
            statusText = "Contacts: 1 READY";
        } else {
            statusText = "Contacts: " + contactCount + " READY";
        }

        tvContactsStatus.setText(statusText);
    }

    private void updateNetworkStatus() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
            tvNetworkStatus.setText("SMS: READY");
        } else {
            tvNetworkStatus.setText("SMS: PERMISSION NEEDED");
        }
    }

    private void updateSOSButton(boolean isActive) {
        if (isActive) {
            sosContainer.setBackgroundResource(R.drawable.bg_sos_active);
            sosContainer.animate()
                    .scaleX(1.1f)
                    .scaleY(1.1f)
                    .setDuration(500)
                    .withEndAction(() -> sosContainer.animate()
                            .scaleX(1.0f)
                            .scaleY(1.0f)
                            .setDuration(500)
                            .start())
                    .start();
        } else {
            sosContainer.setBackgroundResource(R.drawable.bg_sos_inner);
            sosContainer.setScaleX(1.0f);
            sosContainer.setScaleY(1.0f);
        }
    }

    // ========== PERMISSION HANDLING ==========

    private void requestLocationPermission() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Location Permission Needed");
        builder.setMessage("Silent Guardian needs location access to:\n" +
                "• Share your location during emergencies\n" +
                "• Provide accurate coordinates to contacts\n" +
                "• Enable GPS-based safety features");
        builder.setPositiveButton("GRANT", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
            }
        });
        builder.setNegativeButton("LATER", null);
        builder.show();
    }

    private void requestSMSPermission() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("SMS Permission Needed");
        builder.setMessage("Silent Guardian needs SMS access to:\n" +
                "• Send emergency alerts to contacts\n" +
                "• Notify help during critical situations\n" +
                "• Save lives in emergencies");
        builder.setPositiveButton("GRANT", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.SEND_SMS}, SMS_PERMISSION_REQUEST_CODE);
            }
        });
        builder.setNegativeButton("LATER", null);
        builder.show();
    }

    // ========== NAVIGATION METHODS ==========

    private void openContactsScreen() {
        Intent intent = new Intent(MainActivity.this, Activity_Contacts.class);
        startActivity(intent);
    }

    @Override
    protected void navigateToContacts() {
        Intent intent = new Intent(MainActivity.this, Activity_Contacts.class);
        startActivity(intent);
    }

    @Override
    protected void navigateToSettings() {
        Intent intent = new Intent(MainActivity.this, Activity_Settings.class);
        startActivity(intent);
    }

    @Override
    protected void navigateToHelp() {
        Intent intent = new Intent(MainActivity.this, Activity_Help.class);
        startActivity(intent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (requestCode == SMS_PERMISSION_REQUEST_CODE) {
                Toast.makeText(this, "SMS permission granted", Toast.LENGTH_SHORT).show();
                updateNetworkStatus();
            } else if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
                Toast.makeText(this, "Location permission granted", Toast.LENGTH_SHORT).show();
                updateGpsStatus();
            } else if (requestCode == EMERGENCY_LOCATION_REQUEST_CODE) {
                Toast.makeText(this, "Location permission granted. Starting emergency...", Toast.LENGTH_SHORT).show();
                showEmergencyConfirmationDialog();
            } else if (requestCode == 200) { // Notification permission
                Toast.makeText(this, "Notification permission granted", Toast.LENGTH_SHORT).show();
            }
        } else {
            if (requestCode == EMERGENCY_LOCATION_REQUEST_CODE) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Emergency Without Location");
                builder.setMessage("You denied location permission. Send emergency alert WITHOUT location?");
                builder.setPositiveButton("SEND ANYWAY", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        showEmergencyConfirmationDialog();
                    }
                });
                builder.setNegativeButton("CANCEL", null);
                builder.show();
            } else {
                Toast.makeText(this, "Permission denied. Some features may not work.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * ADDED: Update volume button status display
     */
    private void updateVolumeButtonStatus() {
        boolean isEnabled = AccessibilityButtonService.isServiceEnabled(this);

        if (tvVolumeStatus != null) {
            if (isEnabled) {
                tvVolumeStatus.setText("Volume Button: ACTIVE");
                tvVolumeStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark));
            } else {
                tvVolumeStatus.setText("Volume Button: DISABLED (Enable in Settings)");
                tvVolumeStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark));
            }
        }
    }

    /**
     * ADDED: Initialize volume button card click handler
     */
    private void initializeVolumeButtonCard() {
        if (cardVolume != null) {
            cardVolume.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    openAccessibilitySettings();
                }
            });
        }
    }

    /**
     * ADDED: Open accessibility settings
     */
    private void openAccessibilitySettings() {
        try {
            Intent intent = new Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS);
            startActivity(intent);
            Toast.makeText(this, "Enable 'Silent Guardian Accessibility Service'", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "Could not open accessibility settings", Toast.LENGTH_SHORT).show();
        }
    }

}
