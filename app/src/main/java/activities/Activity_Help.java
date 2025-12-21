package activities;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.OnBackPressedCallback;

import utils.BaseActivity;
import com.example.silentguardian.R;

public class Activity_Help extends BaseActivity {

    // UI Components
    private TextView tvAppVersion, tvBuildNumber;

    // FAQ Items
    private View faqItem1, faqItem2, faqItem3;
    private TextView tvFaqAnswer1, tvFaqAnswer2, tvFaqAnswer3;
    private ImageView ivFaqArrow1, ivFaqArrow2, ivFaqArrow3;

    // Contact Support
    private View layoutEmailSupport, layoutWebsite;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help);

        initializeUIComponents();
        setupAllEventListeners();
        highlightCurrentNavItem();
        setupBackButtonHandler();
        loadAppInfo();
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
        tvAppVersion = findViewById(R.id.tvAppVersion);
        tvBuildNumber = findViewById(R.id.tvBuildNumber);

        // FAQ Items
        faqItem1 = findViewById(R.id.faqItem1);
        faqItem2 = findViewById(R.id.faqItem2);
        faqItem3 = findViewById(R.id.faqItem3);

        tvFaqAnswer1 = findViewById(R.id.tvFaqAnswer1);
        tvFaqAnswer2 = findViewById(R.id.tvFaqAnswer2);
        tvFaqAnswer3 = findViewById(R.id.tvFaqAnswer3);

        ivFaqArrow1 = findViewById(R.id.ivFaqArrow1);
        ivFaqArrow2 = findViewById(R.id.ivFaqArrow2);
        ivFaqArrow3 = findViewById(R.id.ivFaqArrow3);

        // Contact Support
        layoutEmailSupport = findViewById(R.id.layoutEmailSupport);
        layoutWebsite = findViewById(R.id.layoutWebsite);

        // Bottom Navigation (from BaseActivity)
        findBottomNavViews();
    }

    /**
     * Setup all event listeners
     */
    private void setupAllEventListeners() {
        // FAQ Item 1
        faqItem1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleFAQ(1);
            }
        });

        // FAQ Item 2
        faqItem2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleFAQ(2);
            }
        });

        // FAQ Item 3
        faqItem3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleFAQ(3);
            }
        });

        // Email Support
        layoutEmailSupport.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendEmail();
            }
        });

        // Website
        layoutWebsite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openWebsite();
            }
        });

        // Bottom Navigation (from BaseActivity)
        setupBottomNavigation();
    }

    /**
     * Toggle FAQ item expand/collapse
     */
    private void toggleFAQ(int faqNumber) {
        TextView answerView = null;
        ImageView arrowView = null;

        switch (faqNumber) {
            case 1:
                answerView = tvFaqAnswer1;
                arrowView = ivFaqArrow1;
                break;
            case 2:
                answerView = tvFaqAnswer2;
                arrowView = ivFaqArrow2;
                break;
            case 3:
                answerView = tvFaqAnswer3;
                arrowView = ivFaqArrow3;
                break;
        }

        if (answerView != null && arrowView != null) {
            if (answerView.getVisibility() == View.VISIBLE) {
                // Collapse
                answerView.setVisibility(View.GONE);
                arrowView.setRotation(0);
            } else {
                // Expand
                answerView.setVisibility(View.VISIBLE);
                arrowView.setRotation(180);
            }
        }
    }

    /**
     * Load app version and build info
     */
    private void loadAppInfo() {
        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);

            // Version name
            String versionName = pInfo.versionName;
            tvAppVersion.setText(versionName);

            // Build number (version code)
            String versionCode = String.valueOf(pInfo.versionCode);
            tvBuildNumber.setText(versionCode);

        } catch (PackageManager.NameNotFoundException e) {
            tvAppVersion.setText("1.0.0");
            tvBuildNumber.setText("1");
        }
    }

    /**
     * Send email to support
     */
    private void sendEmail() {
        try {
            Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
            emailIntent.setData(Uri.parse("mailto:support@silentguardian.com"));
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Silent Guardian Support");
            emailIntent.putExtra(Intent.EXTRA_TEXT, "Hello Silent Guardian Support,\n\n");

            if (emailIntent.resolveActivity(getPackageManager()) != null) {
                startActivity(emailIntent);
            } else {
                Toast.makeText(this, "No email app found", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error opening email", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Open website
     */
    private void openWebsite() {
        try {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://www.silentguardian.com"));

            if (browserIntent.resolveActivity(getPackageManager()) != null) {
                startActivity(browserIntent);
            } else {
                Toast.makeText(this, "No browser app found", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error opening website", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Navigate back to main screen
     */
    private void goBackToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    /**
     * Highlight current screen in bottom navigation
     */
    private void highlightCurrentNavItem() {
        resetAllBottomNavItems();
        if (navHelp != null) {
            highlightBottomNavItem(navHelp);
        }
    }

    // ========== OVERRIDE BASEACTIVITY NAVIGATION METHODS ==========

    @Override
    protected void navigateToContacts() {
        Intent intent = new Intent(this, Activity_Contacts.class);
        startActivity(intent);
        finish();
    }

    @Override
    protected void navigateToSettings() {
        Intent intent = new Intent(this, Activity_Settings.class);
        startActivity(intent);
        finish();
    }

    @Override
    protected void navigateToHelp() {
        // Already in help, do nothing
    }

    @Override
    protected void onResume() {
        super.onResume();
        highlightCurrentNavItem();
    }
}