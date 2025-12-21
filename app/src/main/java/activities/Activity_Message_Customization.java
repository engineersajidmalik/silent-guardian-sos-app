package activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.OnBackPressedCallback;

import utils.BaseActivity;
import com.example.silentguardian.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class Activity_Message_Customization extends BaseActivity {

    // UI Components
    private TextView tvDefaultMessage, tvCharCount, tvMessagePreview, tvPreviewChars;
    private TextInputLayout tilCustomMessage;
    private TextInputEditText etCustomMessage;
    private SwitchMaterial switchUseCustom;
    private RadioGroup rgLocationFormat;
    private RadioButton rbCoordinates, rbGoogleMaps;
    private MaterialButton btnTestMessage;

    // SharedPreferences
    private SharedPreferences sharedPreferences;
    private static final String PREFS_MESSAGE = "MessagePrefs";
    private static final String KEY_CUSTOM_MESSAGE = "custom_message";
    private static final String KEY_USE_CUSTOM = "use_custom";
    private static final String KEY_LOCATION_FORMAT = "location_format";

    // Default message
    private static final String DEFAULT_MESSAGE = "🚨 EMERGENCY! I need help!\n📍 Location: {LOCATION}\n📱 Contact me ASAP!";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message_customization);

        initializeUIComponents();
        loadSavedPreferences();
        setupAllEventListeners();
        highlightCurrentNavItem();
        setupBackButtonHandler();
        updatePreview();
    }

    /**
     * Setup modern back gesture handling
     */
    private void setupBackButtonHandler() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                goBackToMain();
            }
        });
    }

    /**
     * Initialize all UI components
     */
    private void initializeUIComponents() {
        // TextViews
        tvDefaultMessage = findViewById(R.id.tvDefaultMessage);
        tvCharCount = findViewById(R.id.tvCharCount);
        tvMessagePreview = findViewById(R.id.tvMessagePreview);
        tvPreviewChars = findViewById(R.id.tvPreviewChars);

        // Input fields
        tilCustomMessage = findViewById(R.id.tilCustomMessage);
        etCustomMessage = findViewById(R.id.etCustomMessage);

        // Switches and Radio buttons
        switchUseCustom = findViewById(R.id.switchUseCustom);
        rgLocationFormat = findViewById(R.id.rgLocationFormat);
        rbCoordinates = findViewById(R.id.rbCoordinates);
        rbGoogleMaps = findViewById(R.id.rbGoogleMaps);

        // Buttons
        btnTestMessage = findViewById(R.id.btnTestMessage);

        // Bottom Navigation (from BaseActivity)
        findBottomNavViews();

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences(PREFS_MESSAGE, MODE_PRIVATE);

        // Set default message
        tvDefaultMessage.setText(DEFAULT_MESSAGE);
    }

    /**
     * Load saved preferences
     */
    private void loadSavedPreferences() {
        // Load custom message
        String savedMessage = sharedPreferences.getString(KEY_CUSTOM_MESSAGE, DEFAULT_MESSAGE);
        etCustomMessage.setText(savedMessage);

        // Load switch state
        boolean useCustom = sharedPreferences.getBoolean(KEY_USE_CUSTOM, false);
        switchUseCustom.setChecked(useCustom);
        toggleCustomMessageInput(useCustom);

        // Load location format
        String locationFormat = sharedPreferences.getString(KEY_LOCATION_FORMAT, "coordinates");
        if (locationFormat.equals("google_maps")) {
            rbGoogleMaps.setChecked(true);
        } else {
            rbCoordinates.setChecked(true);
        }

        // Update character count
        updateCharacterCount(savedMessage);
    }

    /**
     * Setup all event listeners
     */
    private void setupAllEventListeners() {
        // Custom message text change listener
        etCustomMessage.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Not needed
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Not needed
            }

            @Override
            public void afterTextChanged(Editable s) {
                updateCharacterCount(s.toString());
                saveCustomMessage(s.toString());
                updatePreview();
            }
        });

        // Use custom message switch
        switchUseCustom.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                toggleCustomMessageInput(isChecked);
                saveUseCustomSetting(isChecked);
                updatePreview();
            }
        });

        // Location format radio group
        rgLocationFormat.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                saveLocationFormatSetting();
                updatePreview();
            }
        });

        // Test message button
        btnTestMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                testMessage();
            }
        });

        // Bottom Navigation (from BaseActivity)
        setupBottomNavigation();
    }

    /**
     * Toggle custom message input field
     */
    private void toggleCustomMessageInput(boolean enabled) {
        if (enabled) {
            tilCustomMessage.setAlpha(1.0f);
            etCustomMessage.setEnabled(true);
            etCustomMessage.setFocusable(true);
            etCustomMessage.setFocusableInTouchMode(true);
        } else {
            tilCustomMessage.setAlpha(0.6f);
            etCustomMessage.setEnabled(false);
            etCustomMessage.setFocusable(false);
        }
    }

    /**
     * Update character count
     */
    private void updateCharacterCount(String message) {
        int charCount = message.length();
        int smsParts = calculateSMSParts(charCount);

        tvCharCount.setText(charCount + "/160");

        // Change color if over limit
        if (charCount > 160) {
            tvCharCount.setTextColor(getResources().getColor(R.color.colorError));
        } else {
            tvCharCount.setTextColor(getResources().getColor(R.color.textSecondary));
        }

        // Update preview character count
        tvPreviewChars.setText(smsParts + " SMS (" + charCount + " chars)");
    }

    /**
     * Calculate SMS parts needed
     */
    private int calculateSMSParts(int charCount) {
        if (charCount <= 160) {
            return 1;
        } else {
            // For multi-part SMS (153 chars per part after first part)
            int remainingChars = charCount - 160;
            int additionalParts = (int) Math.ceil(remainingChars / 153.0);
            return 1 + additionalParts;
        }
    }

    /**
     * Update message preview
     */
    private void updatePreview() {
        String previewMessage;

        if (switchUseCustom.isChecked()) {
            previewMessage = etCustomMessage.getText().toString().trim();
        } else {
            previewMessage = DEFAULT_MESSAGE;
        }

        // Replace location placeholder
        String locationText;
        if (rbCoordinates.isChecked()) {
            locationText = "31.5204, 74.3587"; // Example coordinates
        } else {
            locationText = "https://maps.google.com/?q=31.5204,74.3587";
        }

        previewMessage = previewMessage.replace("{LOCATION}", locationText);
        previewMessage = previewMessage.replace("{TIME}", "14:30"); // Example time

        tvMessagePreview.setText(previewMessage);

        // Update character info
        int charCount = previewMessage.length();
        int smsParts = calculateSMSParts(charCount);
        tvPreviewChars.setText(smsParts + " SMS (" + charCount + " chars)");
    }

    /**
     * Test message functionality
     */
    private void testMessage() {
        String message;

        if (switchUseCustom.isChecked()) {
            message = etCustomMessage.getText().toString().trim();
        } else {
            message = DEFAULT_MESSAGE;
        }

        // Replace placeholders with test data
        message = message.replace("{LOCATION}", "Test Location: 31.5204, 74.3587");
        message = message.replace("{TIME}", "Test Time: 14:30");

        // Create intent to test sending (you can modify this to actually send)
        Intent sendIntent = new Intent(Intent.ACTION_SEND);
        sendIntent.setType("text/plain");
        sendIntent.putExtra(Intent.EXTRA_TEXT, message);

        // Check if there's an app that can handle this intent
        if (sendIntent.resolveActivity(getPackageManager()) != null) {
            startActivity(Intent.createChooser(sendIntent, "Test SOS Message"));
            Toast.makeText(this, "Choose an app to test the message", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "No app available to send messages", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Save custom message to SharedPreferences
     */
    private void saveCustomMessage(String message) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_CUSTOM_MESSAGE, message);
        editor.apply();
    }

    /**
     * Save use custom setting to SharedPreferences
     */
    private void saveUseCustomSetting(boolean useCustom) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(KEY_USE_CUSTOM, useCustom);
        editor.apply();
    }

    /**
     * Save location format setting to SharedPreferences
     */
    private void saveLocationFormatSetting() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        String format = rbCoordinates.isChecked() ? "coordinates" : "google_maps";
        editor.putString(KEY_LOCATION_FORMAT, format);
        editor.apply();
    }

    /**
     * Navigate back to main screen
     */
    private void goBackToMain() {
        Intent intent = new Intent(Activity_Message_Customization.this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    /**
     * Highlight current screen in bottom navigation
     */
    private void highlightCurrentNavItem() {
        resetAllBottomNavItems();
        // This screen doesn't have a direct nav item, but we can highlight none or contacts
        // Based on your app flow, you might want to highlight the settings or none
    }

    // ========== OVERRIDE BASEACTIVITY NAVIGATION METHODS ==========

    @Override
    protected void navigateToContacts() {
        Intent intent = new Intent(Activity_Message_Customization.this, Activity_Contacts.class);
        startActivity(intent);
        finish();
    }

    @Override
    protected void navigateToSettings() {
        // Already in settings section, do nothing or go to main settings
        goBackToMain();
    }

    @Override
    protected void navigateToHelp() {
        Intent intent = new Intent(Activity_Message_Customization.this, Activity_Help.class);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        highlightCurrentNavItem();
    }
}