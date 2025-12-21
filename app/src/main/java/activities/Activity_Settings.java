package activities;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;

import com.example.silentguardian.R;
import com.google.android.material.switchmaterial.SwitchMaterial;

import receivers.VolumeButtonReceiver;
import utils.BaseActivity;
import utils.DataManager;
import services.VolumeButtonService;
import services.ShakeDetectionService; // New import

public class Activity_Settings extends BaseActivity {

    // UI Components
    private SwitchMaterial switchNotifications, switchLocationUpdates, switchAppLock, switchVolumeButton;
    private SwitchMaterial switchShakeDetection; // New switch
    private SwitchMaterial switchSendSMS; // Only SMS toggle remains
    private TextView tvAppVersion;
    private View sosMessageSetting, resetLayout;
    private View layoutShakeSensitivity; // New layout for sensitivity settings

    // DataManager
    private DataManager dataManager;

    // Keys
    private static final String KEY_SMS_ENABLED = "sms_enabled";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        dataManager = new DataManager(this);

        initializeUIComponents();
        loadSavedPreferences();
        setupAllEventListeners();
        highlightCurrentNavItem();
        setupBackButtonHandler();
        loadAppVersion();
    }

    private void setupBackButtonHandler() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                goBackToMain();
            }
        });
    }

    private void initializeUIComponents() {
        // Settings switches
        switchNotifications = findViewById(R.id.switchNotifications);
        switchLocationUpdates = findViewById(R.id.switchLocationUpdates);
        switchAppLock = findViewById(R.id.switchAppLock);
        switchVolumeButton = findViewById(R.id.switchVolumeButton); // Updated ID
        switchShakeDetection = findViewById(R.id.switchShakeDetection); // New switch

        // SMS toggle only
        switchSendSMS = findViewById(R.id.switchSendSMS);

        // Other
        tvAppVersion = findViewById(R.id.tvAppVersion);

        // Clickable layouts
        sosMessageSetting = findViewById(R.id.sosMessageSetting);
        resetLayout = findViewById(R.id.layoutReset);
        layoutShakeSensitivity = findViewById(R.id.layoutShakeSensitivity); // New

        // Bottom Navigation (from BaseActivity)
        findBottomNavViews();
    }

    private void loadSavedPreferences() {
        // Settings
        switchNotifications.setChecked(dataManager.getNotificationSetting());
        switchLocationUpdates.setChecked(dataManager.getLocationUpdatesSetting());
        switchAppLock.setChecked(dataManager.getAppLockSetting());
        switchVolumeButton.setChecked(dataManager.getVolumeButtonEnabled()); // Updated method name

        // Shake detection (NEW)
        switchShakeDetection.setChecked(dataManager.getShakeDetectionEnabled());

        // Update sensitivity layout visibility
        updateShakeSensitivityVisibility();

        // SMS enabled
        boolean smsEnabled = dataManager.getBoolean(KEY_SMS_ENABLED, true);
        switchSendSMS.setChecked(smsEnabled);
    }

    private void setupAllEventListeners() {
        // Notifications
        switchNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
            dataManager.saveNotificationSetting(isChecked);
            Toast.makeText(this,
                    "Notifications " + (isChecked ? "enabled" : "disabled"),
                    Toast.LENGTH_SHORT).show();
        });

        // Location updates
        switchLocationUpdates.setOnCheckedChangeListener((buttonView, isChecked) -> {
            dataManager.saveLocationUpdatesSetting(isChecked);
            Toast.makeText(this,
                    "Location updates " + (isChecked ? "enabled" : "disabled"),
                    Toast.LENGTH_SHORT).show();
        });

        // App lock
        switchAppLock.setOnCheckedChangeListener((buttonView, isChecked) -> {
            dataManager.saveAppLockSetting(isChecked);
            if (isChecked) showAppLockSetupDialog();
            else Toast.makeText(this, "App lock disabled", Toast.LENGTH_SHORT).show();
        });

        // Volume button detection
        switchVolumeButton.setOnCheckedChangeListener((buttonView, isChecked) -> {
            dataManager.saveVolumeButtonEnabled(isChecked); // Updated method name

            if (isChecked) {
                showVolumeButtonInstructions();
                Toast.makeText(this, "Volume button trigger enabled", Toast.LENGTH_SHORT).show();

                // Start the volume button monitoring service
                VolumeButtonService.startService(this);
            } else {
                Toast.makeText(this, "Volume button trigger disabled", Toast.LENGTH_SHORT).show();

                // Stop the volume button monitoring service
                VolumeButtonService.stopService(this);
            }
        });

        // Shake Detection (NEW)
        switchShakeDetection.setOnCheckedChangeListener((buttonView, isChecked) -> {
            dataManager.saveShakeDetectionEnabled(isChecked);

            // Update sensitivity layout visibility
            updateShakeSensitivityVisibility();

            if (isChecked) {
                showShakeDetectionInstructions();
                Toast.makeText(this, "Shake detection enabled", Toast.LENGTH_SHORT).show();

                // Start the shake detection service
                ShakeDetectionService.startService(this);
            } else {
                Toast.makeText(this, "Shake detection disabled", Toast.LENGTH_SHORT).show();

                // Stop the shake detection service
                ShakeDetectionService.stopService(this);
            }
        });

        // Shake sensitivity settings (NEW)
        layoutShakeSensitivity.setOnClickListener(v -> {
            showShakeSensitivityDialog();
        });

        // Send SMS automatically
        switchSendSMS.setOnCheckedChangeListener((buttonView, isChecked) -> {
            dataManager.saveBoolean(KEY_SMS_ENABLED, isChecked);
            Toast.makeText(this,
                    "SMS " + (isChecked ? "enabled" : "disabled"),
                    Toast.LENGTH_SHORT).show();
        });

        // SOS message customization
        sosMessageSetting.setOnClickListener(v -> {
            Intent intent = new Intent(this, Activity_Message_Customization.class);
            startActivity(intent);
        });

        // Reset all
        resetLayout.setOnClickListener(v -> showResetConfirmationDialog());

        // Bottom Nav
        setupBottomNavigation();
    }

    private void updateShakeSensitivityVisibility() {
        boolean isShakeEnabled = switchShakeDetection.isChecked();
        layoutShakeSensitivity.setVisibility(isShakeEnabled ? View.VISIBLE : View.GONE);
    }

    private void showVolumeButtonInstructions() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("How to Use Volume Button Trigger");
        builder.setMessage("🚨 EMERGENCY TRIGGER:\n\n" +
                "1. Enable Accessibility Service (one-time setup)\n" +
                "2. Press VOLUME DOWN button 3 times quickly\n" +
                "3. Works even when screen is OFF\n\n" +
                "Setup Steps:\n" +
                "1. Go to Phone Settings → Accessibility\n" +
                "2. Find 'Silent Guardian'\n" +
                "3. Turn ON the service\n\n" +
                "Test: Press volume down 3x quickly after enabling");
        builder.setPositiveButton("OPEN SETTINGS", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Open accessibility settings
                Intent intent = new Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS);
                startActivity(intent);
            }
        });
        builder.setNegativeButton("LATER", null);
        builder.show();
    }

    private void showShakeDetectionInstructions() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("How to Use Shake Detection");
        builder.setMessage("📱 SHAKE TO TRIGGER EMERGENCY:\n\n" +
                "1. Enable Shake Detection\n" +
                "2. Shake phone vigorously 2-3 times\n" +
                "3. Works when screen is ON or OFF\n" +
                "4. No Accessibility setup needed\n\n" +
                "Advantages:\n" +
                "• Works while debugging/connected to PC\n" +
                "• No system conflicts with other apps\n" +
                "• Can adjust sensitivity\n" +
                "• Low battery usage\n\n" +
                "Test: Shake phone 2-3 times quickly");
        builder.setPositiveButton("GOT IT", null);
        builder.setNeutralButton("ADJUST SENSITIVITY", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                showShakeSensitivityDialog();
            }
        });
        builder.show();
    }

    private void showShakeSensitivityDialog() {
        String[] sensitivityOptions = {"Low", "Medium", "High"};
        int currentSensitivity = dataManager.getShakeSensitivity(); // 0=Low, 1=Medium, 2=High
        if (currentSensitivity < 0 || currentSensitivity > 2) currentSensitivity = 1; // Default to Medium

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Shake Sensitivity");
        builder.setMessage("Choose how sensitive shake detection should be:\n\n" +
                "• LOW: Needs strong shaking (fewer false alarms)\n" +
                "• MEDIUM: Normal shaking (recommended)\n" +
                "• HIGH: Light shaking triggers easily");

        builder.setSingleChoiceItems(sensitivityOptions, currentSensitivity, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dataManager.saveShakeSensitivity(which);
                Toast.makeText(Activity_Settings.this,
                        "Sensitivity set to: " + sensitivityOptions[which],
                        Toast.LENGTH_SHORT).show();
                updateSensitivityText();
                dialog.dismiss();
            }
        });

        builder.setNegativeButton("CANCEL", null);
        builder.show();
    }

    private void updateSensitivityText() {
        int sensitivity = dataManager.getShakeSensitivity();
        String[] sensitivityOptions = {"Low", "Medium", "High"};
        TextView tvSensitivity = findViewById(R.id.tvShakeSensitivity);
        if (tvSensitivity != null) {
            String text = sensitivityOptions[sensitivity];
            tvSensitivity.setText(text);
        }
    }

    private void showAppLockSetupDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("App Lock");
        builder.setMessage("App lock feature will be implemented later. For now, this switch only saves setting.");
        builder.setPositiveButton("OK", null);
        builder.show();
    }

    private void showResetConfirmationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Reset All Settings");
        builder.setMessage("Are you sure you want to reset all settings to default? This cannot be undone.");
        builder.setPositiveButton("RESET", (dialog, which) -> resetAllSettings());
        builder.setNegativeButton("CANCEL", null);
        builder.show();
    }

    private void resetAllSettings() {
        dataManager.clearAllData(); // clears contacts/settings/data

        // Reset volume button counter
        VolumeButtonReceiver.resetManual();

        // Reload defaults
        loadSavedPreferences();

        Toast.makeText(this, "All settings have been reset", Toast.LENGTH_SHORT).show();
    }

    private void loadAppVersion() {
        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            tvAppVersion.setText(pInfo.versionName);
        } catch (PackageManager.NameNotFoundException e) {
            tvAppVersion.setText("1.0.0");
        }
    }

    private void goBackToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    private void highlightCurrentNavItem() {
        resetAllBottomNavItems();
        if (navSettings != null) highlightBottomNavItem(navSettings);
    }

    @Override
    protected void navigateToContacts() {
        Intent intent = new Intent(this, Activity_Contacts.class);
        startActivity(intent);
        finish();
    }

    @Override
    protected void navigateToSettings() {
        // already here
    }

    @Override
    protected void navigateToHelp() {
        Intent intent = new Intent(this, Activity_Help.class);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        highlightCurrentNavItem();
        // Update sensitivity text
        updateSensitivityText();
    }
}