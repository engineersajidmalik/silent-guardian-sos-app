package utils;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

public class DataManager {

    private static final String PREFS_CONTACTS = "SilentGuardianContacts";
    private static final String PREFS_SETTINGS = "SettingsPrefs";
    private static final String PREFS_DATA = "SilentGuardianData";

    // Contacts
    private static final String KEY_CONTACTS = "emergency_contacts";

    // SOS Message
    private static final String KEY_SOS_MESSAGE = "sos_message";
    private static final String KEY_SOS_DELAY = "sos_delay";

    // Settings switches
    private static final String KEY_NOTIFICATIONS = "notifications";
    private static final String KEY_LOCATION_UPDATES = "location_updates";
    private static final String KEY_APP_LOCK = "app_lock";

    // Emergency triggers
    private static final String KEY_STEALTH_MODE = "stealth_mode";
    private static final String KEY_VOLUME_BUTTON_ENABLED = "volume_button_enabled"; // Renamed for clarity
    private static final String KEY_SHAKE_DETECTION_ENABLED = "shake_detection_enabled"; // NEW
    private static final String KEY_SHAKE_SENSITIVITY = "shake_sensitivity"; // NEW: 0=Low, 1=Medium, 2=High

    // Backward compatibility key (for old apps using power button key)
    private static final String KEY_POWER_ENABLED_LEGACY = "power_enabled";

    private SharedPreferences contactsPrefs;
    private SharedPreferences settingsPrefs;
    private SharedPreferences dataPrefs;

    public DataManager(Context context) {
        contactsPrefs = context.getSharedPreferences(PREFS_CONTACTS, Context.MODE_PRIVATE);
        settingsPrefs = context.getSharedPreferences(PREFS_SETTINGS, Context.MODE_PRIVATE);
        dataPrefs = context.getSharedPreferences(PREFS_DATA, Context.MODE_PRIVATE);
    }

    // ========== CONTACTS ==========

    public Set<String> getContactsSet() {
        return contactsPrefs.getStringSet(KEY_CONTACTS, new HashSet<String>());
    }

    public void saveContactsSet(Set<String> contactsSet) {
        contactsPrefs.edit().putStringSet(KEY_CONTACTS, new HashSet<>(contactsSet)).apply();
    }

    public void addContact(String name, String phone) {
        Set<String> contactsSet = getContactsSet();
        contactsSet.add(name + "|" + phone);
        saveContactsSet(contactsSet);
    }

    public void updateContact(String oldName, String oldPhone, String newName, String newPhone) {
        Set<String> contactsSet = getContactsSet();
        contactsSet.remove(oldName + "|" + oldPhone);
        contactsSet.add(newName + "|" + newPhone);
        saveContactsSet(contactsSet);
    }

    public void deleteContact(String name, String phone) {
        Set<String> contactsSet = getContactsSet();
        contactsSet.remove(name + "|" + phone);
        saveContactsSet(contactsSet);
    }

    public int getContactCount() {
        return getContactsSet().size();
    }

    public boolean isMaxContactsReached() {
        return getContactCount() >= 3;
    }

    // ========== SOS MESSAGE ==========

    public void saveSOSMessage(String message) {
        dataPrefs.edit().putString(KEY_SOS_MESSAGE, message).apply();
    }

    public String getSOSMessage() {
        return dataPrefs.getString(KEY_SOS_MESSAGE,
                "🚨 EMERGENCY! I need help!\n📍 Location: {LOCATION}\n📱 Contact me ASAP!");
    }

    // ========== SETTINGS ==========

    public void saveSOSDelay(int delayIndex) {
        settingsPrefs.edit().putInt(KEY_SOS_DELAY, delayIndex).apply();
    }

    public int getSOSDelay() {
        // Default to 2 (5 seconds) for safety confirmation
        return settingsPrefs.getInt(KEY_SOS_DELAY, 2); // 2 = 5 seconds
    }

    public void saveNotificationSetting(boolean enabled) {
        settingsPrefs.edit().putBoolean(KEY_NOTIFICATIONS, enabled).apply();
    }

    public boolean getNotificationSetting() {
        return settingsPrefs.getBoolean(KEY_NOTIFICATIONS, true);
    }

    public void saveLocationUpdatesSetting(boolean enabled) {
        settingsPrefs.edit().putBoolean(KEY_LOCATION_UPDATES, enabled).apply();
    }

    public boolean getLocationUpdatesSetting() {
        return settingsPrefs.getBoolean(KEY_LOCATION_UPDATES, true);
    }

    public void saveAppLockSetting(boolean enabled) {
        settingsPrefs.edit().putBoolean(KEY_APP_LOCK, enabled).apply();
    }

    public boolean getAppLockSetting() {
        return settingsPrefs.getBoolean(KEY_APP_LOCK, false);
    }

    // ========== EMERGENCY TRIGGERS ==========

    public void saveStealthMode(boolean enabled) {
        dataPrefs.edit().putBoolean(KEY_STEALTH_MODE, enabled).apply();
    }

    public boolean getStealthMode() {
        return dataPrefs.getBoolean(KEY_STEALTH_MODE, false);
    }

    // ========== VOLUME BUTTON TRIGGER ==========

    /**
     * Get volume button trigger enabled status
     * Supports backward compatibility with old "power_enabled" key
     */
    public boolean getVolumeButtonEnabled() {
        // First try the new key
        if (dataPrefs.contains(KEY_VOLUME_BUTTON_ENABLED)) {
            return dataPrefs.getBoolean(KEY_VOLUME_BUTTON_ENABLED, false);
        }
        // Fallback to old key for backward compatibility
        else if (dataPrefs.contains(KEY_POWER_ENABLED_LEGACY)) {
            boolean legacyValue = dataPrefs.getBoolean(KEY_POWER_ENABLED_LEGACY, false);
            // Migrate to new key
            saveVolumeButtonEnabled(legacyValue);
            // Remove old key
            dataPrefs.edit().remove(KEY_POWER_ENABLED_LEGACY).apply();
            return legacyValue;
        }
        // Default value
        return false;
    }

    /**
     * Save volume button trigger enabled status
     */
    public void saveVolumeButtonEnabled(boolean enabled) {
        dataPrefs.edit().putBoolean(KEY_VOLUME_BUTTON_ENABLED, enabled).apply();
    }

    /**
     * Backward compatibility method (for old code)
     */
    public boolean getPowerButtonEnabled() {
        return getVolumeButtonEnabled();
    }

    /**
     * Backward compatibility method (for old code)
     */
    public void savePowerButtonEnabled(boolean enabled) {
        saveVolumeButtonEnabled(enabled);
    }

    // ========== SHAKE DETECTION TRIGGER (NEW) ==========

    /**
     * Get shake detection enabled status
     */
    public boolean getShakeDetectionEnabled() {
        return dataPrefs.getBoolean(KEY_SHAKE_DETECTION_ENABLED, false);
    }

    /**
     * Save shake detection enabled status
     */
    public void saveShakeDetectionEnabled(boolean enabled) {
        dataPrefs.edit().putBoolean(KEY_SHAKE_DETECTION_ENABLED, enabled).apply();
    }

    /**
     * Get shake sensitivity (0=Low, 1=Medium, 2=High)
     */
    public int getShakeSensitivity() {
        return dataPrefs.getInt(KEY_SHAKE_SENSITIVITY, 1); // Default to Medium
    }

    /**
     * Save shake sensitivity (0=Low, 1=Medium, 2=High)
     */
    public void saveShakeSensitivity(int sensitivity) {
        // Validate and clamp value
        if (sensitivity < 0) sensitivity = 0;
        if (sensitivity > 2) sensitivity = 2;
        dataPrefs.edit().putInt(KEY_SHAKE_SENSITIVITY, sensitivity).apply();
    }

    /**
     * Get shake sensitivity as string
     */
    public String getShakeSensitivityString() {
        int sensitivity = getShakeSensitivity();
        switch (sensitivity) {
            case 0: return "Low";
            case 1: return "Medium";
            case 2: return "High";
            default: return "Medium";
        }
    }

    // ========== GENERAL STORAGE HELPERS ==========

    public void saveString(String key, String value) {
        dataPrefs.edit().putString(key, value).apply();
    }

    public String getString(String key, String defaultValue) {
        return dataPrefs.getString(key, defaultValue);
    }

    public void saveInt(String key, int value) {
        dataPrefs.edit().putInt(key, value).apply();
    }

    public int getInt(String key, int defaultValue) {
        return dataPrefs.getInt(key, defaultValue);
    }

    public void saveLong(String key, long value) {
        dataPrefs.edit().putLong(key, value).apply();
    }

    public long getLong(String key, long defaultValue) {
        return dataPrefs.getLong(key, defaultValue);
    }

    public void saveBoolean(String key, boolean value) {
        dataPrefs.edit().putBoolean(key, value).apply();
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        return dataPrefs.getBoolean(key, defaultValue);
    }

    public String[] getContactNames() {
        Set<String> contactsSet = getContactsSet();
        String[] names = new String[3];
        int index = 0;

        for (String contactData : contactsSet) {
            if (index >= 3) break;
            String[] parts = contactData.split("\\|");
            if (parts.length == 2) {
                names[index] = parts[0];
                index++;
            }
        }
        return names;
    }

    public String[] getContactPhones() {
        Set<String> contactsSet = getContactsSet();
        String[] phones = new String[3];
        int index = 0;

        for (String contactData : contactsSet) {
            if (index >= 3) break;
            String[] parts = contactData.split("\\|");
            if (parts.length == 2) {
                phones[index] = parts[1];
                index++;
            }
        }
        return phones;
    }

    public void saveSetting(String key, boolean value) {
        dataPrefs.edit().putBoolean(key, value).apply();
    }

    public boolean getSetting(String key, boolean defaultValue) {
        return dataPrefs.getBoolean(key, defaultValue);
    }

    // ========== CLEAR DATA ==========

    public void clearAllData() {
        contactsPrefs.edit().clear().apply();
        settingsPrefs.edit().clear().apply();
        dataPrefs.edit().clear().apply();
    }

    // ========== UTIL ==========

    public int getSOSDelayInMillis() {
        // Default to 5 seconds (5000ms) for safety
        int delayIndex = getSOSDelay();
        switch (delayIndex) {
            case 0: return 0;      // Immediate
            case 1: return 3000;   // 3 seconds
            case 2: return 5000;   // 5 seconds (default)
            case 3: return 10000;  // 10 seconds
            default: return 5000;  // Default to 5 seconds
        }
    }

    public String getUserName() {
        return getString("user_name", "User");
    }

    public void saveUserName(String name) {
        saveString("user_name", name);
    }

    public String getEmergencyMessage() {
        return getSOSMessage();
    }

    // Optional: For test phone number
    public String getTestPhoneNumber() {
        return getString("test_phone_number", "");
    }

    public void saveTestPhoneNumber(String phone) {
        saveString("test_phone_number", phone);
    }

    // ========== MIGRATION METHODS ==========

    /**
     * Migrate from old key names to new ones
     */
    public void migrateOldSettings() {
        // Check if migration needed (old key exists, new key doesn't)
        if (dataPrefs.contains(KEY_POWER_ENABLED_LEGACY) &&
                !dataPrefs.contains(KEY_VOLUME_BUTTON_ENABLED)) {

            boolean oldValue = dataPrefs.getBoolean(KEY_POWER_ENABLED_LEGACY, false);
            saveVolumeButtonEnabled(oldValue);

            // Remove old key
            dataPrefs.edit().remove(KEY_POWER_ENABLED_LEGACY).apply();
        }
    }
}