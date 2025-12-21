package activities;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;

import utils.BaseActivity;
import utils.DataManager;
import com.example.silentguardian.R;

import java.util.HashSet;
import java.util.Set;

public class Activity_Contacts extends BaseActivity {

    // UI Components
    private LinearLayout cardContact1, cardContact2, cardContact3;
    private TextView tvName1, tvName2, tvName3;
    private TextView tvPhone1, tvPhone2, tvPhone3;
    private ImageView btnEdit1, btnEdit2, btnEdit3;
    private ImageView btnDelete1, btnDelete2, btnDelete3;
    private com.google.android.material.floatingactionbutton.FloatingActionButton fabAddContact;

    // Contact data arrays
    private String[] contactNames = new String[3];
    private String[] contactPhones = new String[3];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contacts);

        initializeUIComponents();
        loadSavedContacts();
        setupAllEventListeners();
        updateContactDisplays();
        highlightCurrentNavItem();
    }

    /**
     * Initialize all UI components
     */
    private void initializeUIComponents() {
        // Contact Cards
        cardContact1 = findViewById(R.id.cardContact1);
        cardContact2 = findViewById(R.id.cardContact2);
        cardContact3 = findViewById(R.id.cardContact3);

        // Contact Names
        tvName1 = findViewById(R.id.tvName1);
        tvName2 = findViewById(R.id.tvName2);
        tvName3 = findViewById(R.id.tvName3);

        // Contact Phones
        tvPhone1 = findViewById(R.id.tvPhone1);
        tvPhone2 = findViewById(R.id.tvPhone2);
        tvPhone3 = findViewById(R.id.tvPhone3);

        // Edit Buttons
        btnEdit1 = findViewById(R.id.btnEdit1);
        btnEdit2 = findViewById(R.id.btnEdit2);
        btnEdit3 = findViewById(R.id.btnEdit3);

        // Delete Buttons
        btnDelete1 = findViewById(R.id.btnDelete1);
        btnDelete2 = findViewById(R.id.btnDelete2);
        btnDelete3 = findViewById(R.id.btnDelete3);

        // Add Contact Button
        fabAddContact = findViewById(R.id.fabAddContact);

        // Bottom Navigation (from BaseActivity)
        findBottomNavViews();
    }

    /**
     * Load saved contacts using DataManager
     */
    private void loadSavedContacts() {
        DataManager dataManager = new DataManager(this);
        contactNames = dataManager.getContactNames();
        contactPhones = dataManager.getContactPhones();
    }

    /**
     * Setup all event listeners using inner anonymous classes
     */
    private void setupAllEventListeners() {
        // ========== CARD CLICK LISTENERS ==========
        cardContact1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showContactDetails(0);
            }
        });

        cardContact2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showContactDetails(1);
            }
        });

        cardContact3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showContactDetails(2);
            }
        });

        // ========== EDIT BUTTON LISTENERS ==========
        btnEdit1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editContact(0);
            }
        });

        btnEdit2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editContact(1);
            }
        });

        btnEdit3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editContact(2);
            }
        });

        // ========== DELETE BUTTON LISTENERS ==========
        btnDelete1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteContact(0);
            }
        });

        btnDelete2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteContact(1);
            }
        });

        btnDelete3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteContact(2);
            }
        });

        // ========== ADD CONTACT BUTTON ==========
        fabAddContact.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addNewContact();
            }
        });

        // ========== BOTTOM NAVIGATION ==========
        setupBottomNavigation();
    }

    /**
     * Update UI with contact data
     */
    private void updateContactDisplays() {
        boolean hasContacts = false;

        // Update contact 1
        if (contactNames[0] != null && !contactNames[0].isEmpty()) {
            tvName1.setText(contactNames[0]);
            tvPhone1.setText(contactPhones[0]);
            cardContact1.setVisibility(View.VISIBLE);
            hasContacts = true;
        } else {
            cardContact1.setVisibility(View.GONE);
        }

        // Update contact 2
        if (contactNames[1] != null && !contactNames[1].isEmpty()) {
            tvName2.setText(contactNames[1]);
            tvPhone2.setText(contactPhones[1]);
            cardContact2.setVisibility(View.VISIBLE);
            hasContacts = true;
        } else {
            cardContact2.setVisibility(View.GONE);
        }

        // Update contact 3
        if (contactNames[2] != null && !contactNames[2].isEmpty()) {
            tvName3.setText(contactNames[2]);
            tvPhone3.setText(contactPhones[2]);
            cardContact3.setVisibility(View.VISIBLE);
            hasContacts = true;
        } else {
            cardContact3.setVisibility(View.GONE);
        }

        // Show instruction if no contacts
        if (!hasContacts) {
            Toast.makeText(this,
                    "No emergency contacts\nTap + to add contacts",
                    Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Highlight current screen in bottom navigation
     */
    private void highlightCurrentNavItem() {
        resetAllBottomNavItems();
        if (navContacts != null) {
            highlightBottomNavItem(navContacts);
        }
    }

    // ========== CONTACT OPERATIONS ==========

    /**
     * Show contact details dialog
     */
    private void showContactDetails(int index) {
        if (contactNames[index] == null || contactNames[index].isEmpty()) {
            Toast.makeText(this, "No contact at this position", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Contact Details");
        builder.setMessage("Name: " + contactNames[index] + "\nPhone: " + contactPhones[index]);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.setNeutralButton("Edit", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                editContact(index);
            }
        });
        builder.show();
    }

    /**
     * Edit a contact
     */
    private void editContact(int index) {
        if (contactNames[index] == null || contactNames[index].isEmpty()) {
            Toast.makeText(this, "No contact to edit", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(Activity_Contacts.this, Activity_Add_Contact.class);
        intent.putExtra("EDIT_MODE", true);
        intent.putExtra("CONTACT_INDEX", index);
        intent.putExtra("CONTACT_NAME", contactNames[index]);
        intent.putExtra("CONTACT_PHONE", contactPhones[index]);
        startActivityForResult(intent, 100);
    }

    /**
     * Delete a contact with confirmation
     */
    private void deleteContact(int index) {
        if (contactNames[index] == null || contactNames[index].isEmpty()) {
            Toast.makeText(this, "No contact to delete", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Delete Contact");
        builder.setMessage("Delete " + contactNames[index] + "?");
        builder.setPositiveButton("DELETE", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                deleteContactConfirmed(index);
            }
        });
        builder.setNegativeButton("CANCEL", null);
        builder.show();
    }

    /**
     * Confirm and delete contact using DataManager
     */
    private void deleteContactConfirmed(int index) {
        DataManager dataManager = new DataManager(this);
        dataManager.deleteContact(contactNames[index], contactPhones[index]);

        // Update local arrays
        contactNames[index] = "";
        contactPhones[index] = "";

        updateContactDisplays();
        Toast.makeText(this, "Contact deleted", Toast.LENGTH_SHORT).show();
    }

    /**
     * Add a new contact
     */
    private void addNewContact() {
        DataManager dataManager = new DataManager(this);

        if (dataManager.isMaxContactsReached()) {
            showMaxContactsDialog();
            return;
        }

        // Find empty slot
        int emptySlot = findEmptyContactSlot();

        if (emptySlot != -1) {
            Intent intent = new Intent(Activity_Contacts.this, Activity_Add_Contact.class);
            intent.putExtra("EDIT_MODE", false);
            intent.putExtra("CONTACT_INDEX", emptySlot);
            startActivityForResult(intent, 100);
        }
    }

    /**
     * Show maximum contacts reached dialog
     */
    private void showMaxContactsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Maximum Contacts Reached");
        builder.setMessage("You can only store 3 emergency contacts.\nDelete one before adding a new contact.");
        builder.setPositiveButton("OK", null);
        builder.show();
    }

    /**
     * Find empty contact slot
     */
    private int findEmptyContactSlot() {
        for (int i = 0; i < 3; i++) {
            if (contactNames[i] == null || contactNames[i].isEmpty()) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Save contacts to storage using DataManager
     */
    private void saveContactsToStorage() {
        DataManager dataManager = new DataManager(this);
        Set<String> contactsSet = new HashSet<>();

        for (int i = 0; i < 3; i++) {
            if (contactNames[i] != null && !contactNames[i].isEmpty() &&
                    contactPhones[i] != null && !contactPhones[i].isEmpty()) {
                contactsSet.add(contactNames[i] + "|" + contactPhones[i]);
            }
        }

        dataManager.saveContactsSet(contactsSet);
        Toast.makeText(this, "Saved " + contactsSet.size() + " contacts", Toast.LENGTH_SHORT).show();
    }

    // ========== OVERRIDE BASEACTIVITY NAVIGATION METHODS ==========

    @Override
    protected void navigateToContacts() {
        refreshContactsList();
    }

    @Override
    protected void navigateToSettings() {
        Intent intent = new Intent(Activity_Contacts.this, Activity_Settings.class);
        startActivity(intent);
        finish();
    }

    @Override
    protected void navigateToHelp() {
        Intent intent = new Intent(Activity_Contacts.this, Activity_Help.class);
        startActivity(intent);
        finish();
    }

    /**
     * Refresh contacts list
     */
    private void refreshContactsList() {
        loadSavedContacts();
        updateContactDisplays();
        Toast.makeText(this, "Contacts refreshed", Toast.LENGTH_SHORT).show();
    }

    // ========== ACTIVITY LIFECYCLE ==========

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 100 && resultCode == RESULT_OK) {
            loadSavedContacts();
            updateContactDisplays();
            Toast.makeText(this, "Contact saved", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadSavedContacts();
        updateContactDisplays();
        highlightCurrentNavItem();
    }
}