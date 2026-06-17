package mgks.os.swv;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.ValueCallback;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    private final Functions fns = new Functions(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(TAG, "onCreate called");

        SWVContext.asw_view = new WebView(this);
        SWVContext.asw_view.setId(R.id.msw_view);

        setContentView(R.layout.activity_main);

        if (getIntent() != null && getIntent().getData() != null) {
            String uriString = getIntent().getData().toString();
            SWVConfiguration.asw_url = uriString;
            Log.d(TAG, "Intent URI: " + uriString);
        }

        initializeWebView();

        if (savedInstanceState == null) {
            Log.d(TAG, "onCreate: savedInstanceState is null, loading URL");
            SWVContext.asw_view.loadUrl(SWVConfiguration.asw_url);
        } else {
            Log.d(TAG, "onCreate: restored WebView state");
            SWVContext.asw_view.restoreState(savedInstanceState);
        }

        handleIntent(getIntent());
    }

    private void initializeWebView() {
        SWVContext.init(this, SWVContext.asw_view, fns);

        Playground playground = new Playground(this, SWVContext.asw_view, fns);
        SWVContext.getPluginManager().setPlayground(playground);

        WebSettings webSettings = SWVContext.asw_view.getSettings();

        if (SWVContext.OVERRIDE_USER_AGENT || SWVContext.POSTFIX_USER_AGENT) {
            String userAgent = webSettings.getUserAgentString();
            if (SWVContext.OVERRIDE_USER_AGENT) {
                userAgent = SWVContext.CUSTOM_USER_AGENT;
            }
            if (SWVContext.POSTFIX_USER_AGENT) {
                userAgent = userAgent + " " + SWVContext.USER_AGENT_POSTFIX;
            }
            webSettings.setUserAgentString(userAgent);
        }

        webSettings.setJavaScriptEnabled(true);
        webSettings.setSaveFormData(SWVContext.ASWP_SFORM);
        webSettings.setSupportZoom(SWVContext.ASWP_ZOOM);

        // ===== SAKTI: PENGATURAN RESPONSIF GAMBAR & AUTO-FIT =====
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false); 
        // =========================================================

        webSettings.setAllowFileAccess(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);

        if (SWVContext.ASWP_CACHE) {
            webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        } else {
            webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        }

        Map<String, String> headers = new HashMap<>();
        if (SWVContext.ADD_CUSTOM_HEADER) {
            headers.put(SWVContext.CUSTOM_HEADER_KEY, SWVContext.CUSTOM_HEADER_VALUE);
        }

        SWVContext.asw_view.setWebViewClient(new MSWWebViewClient(this, headers));
        SWVContext.asw_view.setWebChromeClient(new MSWWebChromeClient(this));

        if (SWVContext.ASWP_CLEAR_CACHE) {
            SWVContext.asw_view.clearCache(true);
        }

        SWVContext.asw_view.setDownloadListener((url, userAgent, contentDisposition, mimeType, contentLength) -> {
            Log.d(TAG, "Download requested for URL: " + url);
            if (SWVContext.getPluginManager().hasPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                SWVContext.getPluginManager().downloadFile(this, url, userAgent, contentDisposition, mimeType);
            } else {
                SWVContext.getPluginManager().requestPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE, SWVContext.getPluginManager().getPermissionRequestCode(android.Manifest.permission.WRITE_EXTERNAL_STORAGE));
            }
        });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (intent != null && intent.getExtras() != null) {
            String url = intent.getExtras().getString("url");
            if (url != null && !url.isEmpty()) {
                Log.d(TAG, "handleIntent: Loading URL from intent: " + url);
                SWVContext.asw_view.loadUrl(url);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        Log.d(TAG, "onActivityResult called: requestCode=" + requestCode + ", resultCode=" + resultCode);

        ValueCallback<Uri[]> asw_file_message = SWVContext.getPluginManager().getFileMessageCallback();
        if (asw_file_message != null) {
            Uri[] results = null;
            if (resultCode == RESULT_OK && intent != null) {
                String dataString = intent.getDataString();
                if (dataString != null) {
                    results = new Uri[]{Uri.parse(dataString)};
                } else if (intent.getClipData() != null) {
                    int count = intent.getClipData().getItemCount();
                    results = new Uri[count];
                    for (int i = 0; i < count; i++) {
                        results[i] = intent.getClipData().getItemAt(i).getUri();
                    }
                }
            }
            asw_file_message.onReceiveValue(results);
            SWVContext.getPluginManager().setFileMessageCallback(null);
        }
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Log.d(TAG, "onConfigurationChanged");
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.d(TAG, "onSaveInstanceState");
        SWVContext.asw_view.saveState(outState);
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        Log.d(TAG, "onRestoreInstanceState");
        SWVContext.asw_view.restoreState(savedInstanceState);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onBackPressed() {
        if (SWVContext.asw_view.canGoBack()) {
            SWVContext.asw_view.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
        SWVContext.asw_view.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        SWVContext.asw_view.onResume();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.d(TAG, "onRequestPermissionsResult called");

        // ANTI-BLANK AUTO REFRESH: Otomatis reload saat izin diberikan pertama kali!
        if (grantResults.length > 0 && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            if (SWVContext.asw_view != null) {
                SWVContext.asw_view.reload();
            }
        } else {
            Toast.makeText(this, R.string.loc_perm_text, Toast.LENGTH_LONG).show();
        }
    }
}
