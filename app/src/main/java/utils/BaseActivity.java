package utils;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.example.silentguardian.R;

/**
 * BaseActivity - Contains common bottom navigation functionality and theme management
 * All activities with bottom nav should extend this class
 */
public class BaseActivity extends AppCompatActivity {

    // Bottom Navigation components - PROTECTED so child classes can access
    protected LinearLayout navContacts;
    protected LinearLayout navSettings;
    protected LinearLayout navHelp;

    // Theme SharedPreferences key
    private static final String PREFS_SETTINGS = "SettingsPrefs";
    private static final String KEY_THEME = "theme";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Apply theme BEFORE setContentView (IMPORTANT!)
        applyTheme();

        // Child classes will set their own content views
    }

    // ========== THEME MANAGEMENT METHODS ==========

    /**
     * Apply saved theme from SharedPreferences
     * Call this BEFORE setContentView() in child activities
     */
    private void applyTheme() {
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_SETTINGS, MODE_PRIVATE);
        int themeIndex = sharedPreferences.getInt(KEY_THEME, 2); // Default: System

        // Apply theme using AppCompatDelegate
        switch (themeIndex) {
            case 0: // Light
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case 1: // Dark
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            case 2: // System Default
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
            default:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        }
    }

    /**
     * Get current theme index from SharedPreferences
     * @return 0=Light, 1=Dark, 2=System
     */
    protected int getCurrentTheme() {
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_SETTINGS, MODE_PRIVATE);
        return sharedPreferences.getInt(KEY_THEME, 2); // Default to System
    }

    /**
     * Save theme preference and restart activity
     * Call this when user selects a theme
     * @param themeIndex 0=Light, 1=Dark, 2=System
     */
    protected void saveAndApplyTheme(int themeIndex) {
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_SETTINGS, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(KEY_THEME, themeIndex);
        editor.apply();

        // Apply immediately
        switch (themeIndex) {
            case 0: // Light
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case 1: // Dark
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            case 2: // System Default
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
        }

        // Recreate activity to apply theme changes
        recreate();
    }

    // ========== BOTTOM NAV HIGHLIGHTING METHODS ==========

    /**
     * Highlight a specific bottom navigation item
     * @param navItem The LinearLayout nav item to highlight
     */
    protected void highlightBottomNavItem(LinearLayout navItem) {
        if (navItem == null || navItem.getChildCount() < 2) {
            return;
        }

        // Get ImageView (child 0) and TextView (child 1) from LinearLayout
        View iconView = navItem.getChildAt(0);
        View textView = navItem.getChildAt(1);

        // Set selected background
        navItem.setBackgroundResource(R.drawable.bottom_nav_item_selected);

        // Change icon color to primary color
        if (iconView instanceof ImageView) {
            ImageView icon = (ImageView) iconView;
            icon.setColorFilter(getResources().getColor(R.color.colorPrimary));
        }

        // Change text color to primary color
        if (textView instanceof TextView) {
            TextView text = (TextView) textView;
            text.setTextColor(getResources().getColor(R.color.colorPrimary));
        }
    }

    /**
     * Reset a bottom navigation item to default state
     * @param navItem The LinearLayout nav item to reset
     */
    protected void resetBottomNavItem(LinearLayout navItem) {
        if (navItem == null || navItem.getChildCount() < 2) {
            return;
        }

        // Get ImageView (child 0) and TextView (child 1) from LinearLayout
        View iconView = navItem.getChildAt(0);
        View textView = navItem.getChildAt(1);

        // Set default background
        navItem.setBackgroundResource(R.drawable.bottom_nav_item_default);

        // Reset icon color to appropriate color based on theme
        if (iconView instanceof ImageView) {
            ImageView icon = (ImageView) iconView;
            // Use theme-aware color instead of hardcoded white
            icon.setColorFilter(getResources().getColor(android.R.color.white));
        }

        // Reset text color
        if (textView instanceof TextView) {
            TextView text = (TextView) textView;
            text.setTextColor(getResources().getColor(android.R.color.white));
        }
    }

    /**
     * Reset all bottom navigation items to default state
     */
    protected void resetAllBottomNavItems() {
        // Reset Contacts nav item
        if (navContacts != null) {
            resetBottomNavItem(navContacts);
        }

        // Reset Settings nav item
        if (navSettings != null) {
            resetBottomNavItem(navSettings);
        }

        // Reset Help nav item
        if (navHelp != null) {
            resetBottomNavItem(navHelp);
        }
    }

    /**
     * Set up bottom navigation with common click listeners
     * Call this in child activities after initializing navContacts, navSettings, navHelp
     */
    protected void setupBottomNavigation() {
        // Contacts navigation
        if (navContacts != null) {
            navContacts.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    navigateToContacts();
                }
            });
        }

        // Settings navigation
        if (navSettings != null) {
            navSettings.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    navigateToSettings();
                }
            });
        }

        // Help navigation
        if (navHelp != null) {
            navHelp.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    navigateToHelp();
                }
            });
        }
    }

    // ========== NAVIGATION METHODS (to be overridden by child classes) ==========

    /**
     * Navigate to Contacts activity
     * Override this in child classes with specific implementation
     */
    protected void navigateToContacts() {
        // Default empty implementation
    }

    /**
     * Navigate to Settings activity
     * Override this in child classes with specific implementation
     */
    protected void navigateToSettings() {
        // Default empty implementation
    }

    /**
     * Navigate to Help activity
     * Override this in child classes with specific implementation
     */
    protected void navigateToHelp() {
        // Default empty implementation
    }

    /**
     * Common method to find bottom navigation views
     * Call this in child class's initializeUIComponents() method
     */
    protected void findBottomNavViews() {
        View includeBottomNav = findViewById(R.id.includeBottomNav);
        if (includeBottomNav != null) {
            navContacts = includeBottomNav.findViewById(R.id.navContacts);
            navSettings = includeBottomNav.findViewById(R.id.navSettings);
            navHelp = includeBottomNav.findViewById(R.id.navHelp);
        }
    }

    /**
     * Setup back press handling
     * Call this in child activities' onCreate() after super.onCreate()
     */
    protected void setupBackPressHandling() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // Handle back press in child activities
                onCustomBackPressed();
            }
        });
    }

    /**
     * Override this in child activities for custom back press handling
     */
    protected void onCustomBackPressed() {
        // Default implementation - just finish activity
        finish();
    }
}