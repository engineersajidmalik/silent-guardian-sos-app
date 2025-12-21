package activities;

import android.os.Bundle;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import com.example.silentguardian.R;

public class WebViewActivity extends AppCompatActivity {

    private WebView webView;
    private TextView tvTitle;
    private ImageView btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_webview);

        initializeUIComponents();
        setupWebView();
        loadContentFromIntent();
        setupBackButtonHandler();
        setupEventListeners();
    }

    /**
     * Initialize all UI components
     */
    private void initializeUIComponents() {
        tvTitle = findViewById(R.id.tvTitle);
        webView = findViewById(R.id.webView);
        btnBack = findViewById(R.id.btnBack);
    }

    /**
     * Setup WebView configuration
     */
    private void setupWebView() {
        // Configure WebView
        webView.setWebViewClient(new WebViewClient());
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);

        // Enable zoom controls
        webView.getSettings().setSupportZoom(true);
        webView.getSettings().setBuiltInZoomControls(true);
        webView.getSettings().setDisplayZoomControls(false);
    }

    /**
     * Load content from intent
     */
    private void loadContentFromIntent() {
        // Get data from intent
        String title = getIntent().getStringExtra("title");
        String url = getIntent().getStringExtra("url");

        if (title != null) {
            tvTitle.setText(title);
        }

        if (url != null) {
            webView.loadUrl(url);
        }
    }

    /**
     * Setup modern back gesture handling
     */
    private void setupBackButtonHandler() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                handleBackNavigation();
            }
        });
    }

    /**
     * Setup event listeners
     */
    private void setupEventListeners() {
        // Back button in toolbar
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleBackNavigation();
            }
        });
    }

    /**
     * Handle back navigation (WebView history or activity finish)
     */
    private void handleBackNavigation() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            finish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (webView != null) {
            webView.onResume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (webView != null) {
            webView.onPause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (webView != null) {
            webView.destroy();
        }
    }
}