package activities;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import utils.BaseActivity;
import utils.DataManager;
import com.example.silentguardian.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Set;

public class Activity_Add_Contact extends BaseActivity {

    // UI Components
    private ImageButton btnBack;
    private TextInputLayout inputLayoutName, inputLayoutPhone, inputLayoutRelationship;
    private TextInputEditText etFullName, etPhoneNumber;
    private AutoCompleteTextView actvRelationship;
    private MaterialButton btnSaveContact;
    private TextView tvAddPhoto;
    private ImageView ivContactPhoto;

    // Data Variables
    private boolean isEditMode = false;
    private int contactIndex = -1;
    private String originalName = "";
    private String originalPhone = "";

    // Camera/Gallery variables (Kept for future use)
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_IMAGE_GALLERY = 2;
    private static final int REQUEST_CAMERA_PERMISSION = 100;
    private static final int REQUEST_STORAGE_PERMISSION = 101;

    private String currentPhotoPath;
    private Uri photoUri;

    // Relationship options
    private String[] relationshipOptions = {
            "Select Relationship",
            "Family",
            "Friend",
            "Colleague",
            "Neighbor",
            "Doctor",
            "Emergency Service",
            "Other"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_contact);

        initializeUIComponents();
        setupRelationshipDropdown();
        checkEditMode();
        setupAllEventListeners();
        highlightCurrentNavItem();
        setupBackButtonHandler();
        setupPhoneNumberFormatting();
    }

    /**
     * Setup modern back gesture handling
     */
    private void setupBackButtonHandler() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                goBackToContacts();
            }
        });
    }

    /**
     * Initialize all UI components
     */
    private void initializeUIComponents() {
        // Navigation
        btnBack = findViewById(R.id.btnBack);

        // Form fields
        inputLayoutName = findViewById(R.id.inputLayoutName);
        inputLayoutPhone = findViewById(R.id.inputLayoutPhone);
        inputLayoutRelationship = findViewById(R.id.inputLayoutRelationship);

        etFullName = findViewById(R.id.etFullName);
        etPhoneNumber = findViewById(R.id.etPhoneNumber);
        actvRelationship = findViewById(R.id.actvRelationship);

        // Buttons
        btnSaveContact = findViewById(R.id.btnSaveContact);
        tvAddPhoto = findViewById(R.id.tvAddPhoto);
        ivContactPhoto = findViewById(R.id.ivContactPhoto);

        // Bottom Navigation (from BaseActivity)
        findBottomNavViews();
    }

    /**
     * Setup relationship dropdown with options
     */
    private void setupRelationshipDropdown() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                relationshipOptions
        );

        actvRelationship.setAdapter(adapter);
        actvRelationship.setText(relationshipOptions[0], false);
    }

    /**
     * Check if we're in edit mode and load contact data
     */
    private void checkEditMode() {
        Intent intent = getIntent();

        if (intent != null && intent.hasExtra("EDIT_MODE")) {
            isEditMode = intent.getBooleanExtra("EDIT_MODE", false);
            contactIndex = intent.getIntExtra("CONTACT_INDEX", -1);

            if (isEditMode && contactIndex != -1) {
                originalName = intent.getStringExtra("CONTACT_NAME");
                originalPhone = intent.getStringExtra("CONTACT_PHONE");

                etFullName.setText(originalName);
                etPhoneNumber.setText(originalPhone);

                btnSaveContact.setText("Update Contact");

                Toast.makeText(this, "Editing contact: " + originalName, Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Setup all event listeners using inner anonymous classes
     */
    private void setupAllEventListeners() {
        // Back button
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                goBackToContacts();
            }
        });

        // Save button
        btnSaveContact.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveContact();
            }
        });

        // Add photo text - Coming Soon feature
        tvAddPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showComingSoonMessage();
            }
        });

        // Bottom Navigation (from BaseActivity)
        setupBottomNavigation();
    }

    /**
     * Show coming soon message for contact photos feature
     */
    private void showComingSoonMessage() {
        Toast.makeText(this,
                "Contact photos feature will be available in the next version!",
                Toast.LENGTH_LONG).show();
    }

    /**
     * Check and request camera permission (Kept for future use)
     */
    private boolean checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    REQUEST_CAMERA_PERMISSION);
            return false;
        }
        return true;
    }

    /**
     * Check and request storage permission (Kept for future use)
     */
    private boolean checkStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // For Android 13+, use READ_MEDIA_IMAGES
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_MEDIA_IMAGES},
                        REQUEST_STORAGE_PERMISSION);
                return false;
            }
        } else {
            // For older versions
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        REQUEST_STORAGE_PERMISSION);
                return false;
            }
        }
        return true;
    }

    /**
     * Handle permission results (Kept for future use)
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Future: dispatchTakePictureIntent();
                showComingSoonMessage();
            } else {
                Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == REQUEST_STORAGE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Future: openGallery();
                showComingSoonMessage();
            } else {
                Toast.makeText(this, "Storage permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Setup phone number formatting (real-time)
     */
    private void setupPhoneNumberFormatting() {
        etPhoneNumber.addTextChangedListener(new TextWatcher() {
            boolean isFormatting;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Not used
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Not used
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (isFormatting) return;

                isFormatting = true;

                // Remove all non-digits and plus
                String digits = s.toString().replaceAll("[^0-9+]", "");

                // Format as user types
                if (digits.startsWith("+")) {
                    // Already has country code
                    if (digits.length() >= 3) {
                        String formatted = digits.substring(0, 3);
                        if (digits.length() > 3) {
                            formatted += " " + digits.substring(3, Math.min(6, digits.length()));
                            if (digits.length() > 6) {
                                formatted += " " + digits.substring(6, Math.min(digits.length(), 13));
                            }
                        }
                        s.replace(0, s.length(), formatted);
                    }
                } else if (digits.length() >= 10) {
                    // Add Pakistan country code by default
                    String formatted = "+92 " + digits;
                    s.replace(0, s.length(), formatted);
                }

                isFormatting = false;
            }
        });
    }

    /**
     * Highlight current screen in bottom navigation
     */
    private void highlightCurrentNavItem() {
        resetAllBottomNavItems();
        // For Add Contact screen, highlight Contacts to show where we came from
        if (navContacts != null) {
            highlightBottomNavItem(navContacts);
        }
    }

    /**
     * Save contact data
     */
    private void saveContact() {
        String name = etFullName.getText().toString().trim();
        String phone = etPhoneNumber.getText().toString().trim();
        String relationship = actvRelationship.getText().toString().trim();

        if (!validateInputs(name, phone, relationship)) {
            return;
        }

        boolean success = saveContactData(name, phone, relationship);

        if (success) {
            Intent resultIntent = new Intent();
            setResult(RESULT_OK, resultIntent);
            finish();
        }
    }

    /**
     * Validate form inputs
     */
    private boolean validateInputs(String name, String phone, String relationship) {
        boolean isValid = true;

        // Validate name
        if (TextUtils.isEmpty(name)) {
            inputLayoutName.setError("Name is required");
            isValid = false;
        } else if (name.length() < 2) {
            inputLayoutName.setError("Name must be at least 2 characters");
            isValid = false;
        } else {
            inputLayoutName.setError(null);
        }

        // Validate phone
        if (TextUtils.isEmpty(phone)) {
            inputLayoutPhone.setError("Phone number is required");
            isValid = false;
        } else {
            // Format and validate phone number
            String formattedPhone = formatPhoneNumber(phone);

            if (!isValidPhoneNumber(formattedPhone)) {
                inputLayoutPhone.setError("Enter a valid phone number with country code (e.g., +92 300 1234567)");
                isValid = false;
            } else {
                inputLayoutPhone.setError(null);
                // Update the edit text with formatted number
                etPhoneNumber.setText(formattedPhone);
            }
        }

        // Validate relationship
        if (relationship.equals("Select Relationship")) {
            actvRelationship.setError("Please select a relationship");
            isValid = false;
        } else {
            actvRelationship.setError(null);
        }

        return isValid;
    }

    /**
     * Format phone number
     */
    private String formatPhoneNumber(String phone) {
        // Remove all non-digit characters except +
        String digitsOnly = phone.replaceAll("[^0-9+]", "");

        // If starts with 0, remove it (for local numbers)
        if (digitsOnly.startsWith("0")) {
            digitsOnly = digitsOnly.substring(1);
        }

        // Add country code if not present
        if (!digitsOnly.startsWith("+")) {
            // Default to Pakistan code +92
            digitsOnly = "+92" + digitsOnly;
        }

        // Format with spaces for readability (e.g., +92 300 1234567)
        if (digitsOnly.length() > 3) {
            String countryCode = digitsOnly.substring(0, 3);
            String rest = digitsOnly.substring(3);

            if (rest.length() > 0) {
                StringBuilder formatted = new StringBuilder(countryCode);
                formatted.append(" ");

                // Format rest of the number
                if (rest.length() <= 3) {
                    formatted.append(rest);
                } else {
                    formatted.append(rest.substring(0, 3));
                    if (rest.length() > 3) {
                        formatted.append(" ").append(rest.substring(3));
                    }
                }
                return formatted.toString();
            }
        }

        return phone; // Return original if formatting fails
    }

    /**
     * Check if phone number is valid
     */
    private boolean isValidPhoneNumber(String phone) {
        // Remove spaces for validation
        String digitsOnly = phone.replaceAll("[^0-9+]", "");

        // Check for valid country code format
        if (!digitsOnly.startsWith("+")) {
            return false;
        }

        // Minimum length: +92 (3) + rest (at least 7) = 10
        return digitsOnly.length() >= 10;
    }

    /**
     * Save contact data using DataManager
     */
    private boolean saveContactData(String name, String phone, String relationship) {
        try {
            DataManager dataManager = new DataManager(this);
            Set<String> contactsSet = dataManager.getContactsSet();

            // Remove old contact if editing
            if (isEditMode && !originalName.isEmpty()) {
                String oldContactData = originalName + "|" + originalPhone;
                contactsSet.remove(oldContactData);
            }

            // Add new contact
            String contactData = name + "|" + phone;
            contactsSet.add(contactData);

            // Save using DataManager
            dataManager.saveContactsSet(contactsSet);

            String message = isEditMode ? "Contact updated successfully" : "Contact added successfully";
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();

            return true;

        } catch (Exception e) {
            Toast.makeText(this, "Error saving contact: " + e.getMessage(), Toast.LENGTH_LONG).show();
            return false;
        }
    }

    /**
     * Navigate back to contacts screen
     */
    private void goBackToContacts() {
        String currentName = etFullName.getText().toString().trim();
        String currentPhone = etPhoneNumber.getText().toString().trim();

        boolean hasChanges = !currentName.equals(originalName) || !currentPhone.equals(originalPhone);

        if (hasChanges) {
            showUnsavedChangesDialog();
        } else {
            setResult(RESULT_CANCELED);
            finish();
        }
    }

    /**
     * Show unsaved changes confirmation dialog
     */
    private void showUnsavedChangesDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Unsaved Changes");
        builder.setMessage("You have unsaved changes. Are you sure you want to discard them?");
        builder.setPositiveButton("DISCARD", new android.content.DialogInterface.OnClickListener() {
            @Override
            public void onClick(android.content.DialogInterface dialog, int which) {
                setResult(RESULT_CANCELED);
                finish();
            }
        });
        builder.setNegativeButton("CANCEL", new android.content.DialogInterface.OnClickListener() {
            @Override
            public void onClick(android.content.DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.show();
    }

    // ========== OVERRIDE BASEACTIVITY NAVIGATION METHODS ==========

    @Override
    protected void navigateToContacts() {
        goBackToContacts();
    }

    @Override
    protected void navigateToSettings() {
        String currentName = etFullName.getText().toString().trim();
        String currentPhone = etPhoneNumber.getText().toString().trim();

        boolean hasChanges = !currentName.equals(originalName) || !currentPhone.equals(originalPhone);

        if (hasChanges) {
            showUnsavedChangesDialog();
        } else {
            Intent intent = new Intent(Activity_Add_Contact.this, Activity_Settings.class);
            startActivity(intent);
            finish();
        }
    }

    @Override
    protected void navigateToHelp() {
        String currentName = etFullName.getText().toString().trim();
        String currentPhone = etPhoneNumber.getText().toString().trim();

        boolean hasChanges = !currentName.equals(originalName) || !currentPhone.equals(originalPhone);

        if (hasChanges) {
            showUnsavedChangesDialog();
        } else {
            Intent intent = new Intent(Activity_Add_Contact.this, Activity_Help.class);
            startActivity(intent);
            finish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        highlightCurrentNavItem();
    }
}