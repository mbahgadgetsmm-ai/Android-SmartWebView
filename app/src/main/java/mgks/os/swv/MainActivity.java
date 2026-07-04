package mgks.os.swv;

/*
  Smart WebView v8 - MBAH GADGET SUPER FAST (4-PERMISSION NORMAL MODE)
  FIXED: 100% INTERNAL PAYMENTS, 4 PERMISSIONS AT STARTUP (NORMAL & STABLE), HARDWARE ACCELERATION ENABLED.
*/

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.DownloadManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.SearchManager;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.webkit.ConsoleMessage;
import android.webkit.CookieManager;
import android.webkit.GeolocationPermissions;
import android.webkit.JavascriptInterface;
import android.webkit.ServiceWorkerClient;
import android.webkit.ServiceWorkerController;
import android.webkit.SslErrorHandler;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SearchView;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.graphics.Insets;
import androidx.core.splashscreen.SplashScreen;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.navigation.NavigationView;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;
import com.onesignal.OneSignal;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Objects;
import java.util.regex.Matcher;

import mgks.os.swv.plugins.QRScannerPlugin;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    private static final String TAG = "MainActivity";
    private boolean isFirstLaunchScanCheck = true;

    static Functions fns = new Functions();
    private FileProcessing fileProcessing;
    private PermissionManager permissionManager;
    private ActivityResultLauncher<Intent> fileUploadLauncher;
    private ActivityResultLauncher<ScanOptions> qrScannerLauncher;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        SWVContext.getPluginManager().onActivityResult(requestCode, resultCode, intent);
    }

    @SuppressLint({"SetJavaScriptEnabled", "WrongViewCast", "JavascriptInterface"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (SWVContext.ASWP_BLOCK_SCREENSHOTS) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        }
        
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (SWVContext.asw_view != null) {
                    String currentUrl = SWVContext.asw_view.getUrl();
                    if (currentUrl == null || currentUrl.equals(SWVContext.ASWV_URL) || currentUrl.equals(SWVContext.ASWV_URL + "/")) {
                        moveTaskToBack(true); 
                    } else {
                        if (SWVContext.asw_view.canGoBack()) {
                            SWVContext.asw_view.goBack(); 
                        }
                    }
                }
            }
        });

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        super.onCreate(savedInstanceState);
        androidx.core.splashscreen.SplashScreen.installSplashScreen(this);

        final View content = findViewById(android.R.id.content);
        permissionManager = new PermissionManager(this);

        fileUploadLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                Uri[] results = null;
                if (result.getResultCode() == Activity.RESULT_CANCELED) {
                    if (SWVContext.asw_file_path != null) {
                        SWVContext.asw_file_path.onReceiveValue(null);
                        SWVContext.asw_file_path = null;
                    }
                    return;
                }

                if (result.getResultCode() == Activity.RESULT_OK) {
                    if (null == SWVContext.asw_file_path) return;
                    Intent data = result.getData();
                    if (data != null && (data.getDataString() != null || data.getClipData() != null)) {
                        ClipData clipData = data.getClipData();
                        if (clipData != null) {
                            final int numSelectedFiles = clipData.getItemCount();
                            results = new Uri[numSelectedFiles];
                            for (int i = 0; i < numSelectedFiles; i++) {
                                results[i] = clipData.getItemAt(i).getUri();
                            }
                        } else if (data.getDataString() != null) {
                            results = new Uri[]{Uri.parse(data.getDataString())};
                        }
                    }
                    if (results == null) {
                        if (SWVContext.asw_pcam_message != null) {
                            results = new Uri[]{Uri.parse(SWVContext.asw_pcam_message)};
                        } else if (SWVContext.asw_vcam_message != null) {
                            results = new Uri[]{Uri.parse(SWVContext.asw_vcam_message)};
                        }
                    }
                }

                if (SWVContext.asw_file_path != null) {
                    SWVContext.asw_file_path.onReceiveValue(results);
                    SWVContext.asw_file_path = null;
                }
                SWVContext.asw_pcam_message = null;
                SWVContext.asw_vcam_message = null;
            }
        );
      qrScannerLauncher = registerForActivityResult(new ScanContract(),
                result -> {
                    PluginInterface plugin = SWVContext.getPluginManager().getPluginInstance("QRScannerPlugin");
                    if (plugin instanceof QRScannerPlugin) {
                        ((QRScannerPlugin) plugin).handleScanResult(result);
                    }
                }
        );

        SWVContext.setAppContext(getApplicationContext());
        fileProcessing = new FileProcessing(this, fileUploadLauncher);

        setupLayout();
        initializeWebView();

        SWVContext.loadPlugins(this);
        SWVContext.init(this, SWVContext.asw_view, fns);

        PluginInterface qrPlugin = SWVContext.getPluginManager().getPluginInstance("QRScannerPlugin");
        if (qrPlugin instanceof QRScannerPlugin) {
            ((QRScannerPlugin) qrPlugin).setLauncher(qrScannerLauncher);
        }

        if (savedInstanceState == null) {
            setupFeatures();
            handleIncomingIntents();
        }

        ViewCompat.setOnApplyWindowInsetsListener(content, (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(insets.left, insets.top, insets.right, insets.bottom);
            return windowInsets;
        });
    }

    public void setWindowSecure(boolean secure) {
        runOnUiThread(() -> {
            if (secure) getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
            else getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE);
        });
    }

    private void setupLayout() {
        if (SWVContext.ASWV_LAYOUT == 1) {
            setContentView(R.layout.drawer_main);
            MaterialToolbar toolbar = findViewById(R.id.toolbar);
            if (SWVContext.ASWP_DRAWER_HEADER) {
                findViewById(R.id.app_bar).setVisibility(View.VISIBLE);
                setSupportActionBar(toolbar);
                Objects.requireNonNull(getSupportActionBar()).setDisplayShowTitleEnabled(false);
                DrawerLayout drawer = findViewById(R.id.drawer_layout);
                ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.open, R.string.close);
                drawer.addDrawerListener(toggle);
                toggle.syncState();
            } else {
                findViewById(R.id.app_bar).setVisibility(View.GONE);
            }
            NavigationView navigationView = findViewById(R.id.nav_view);
            navigationView.setNavigationItemSelectedListener(this);
        } else {
            setContentView(R.layout.activity_main);
        }
        SWVContext.asw_view = findViewById(R.id.msw_view);
        if (SWVContext.asw_view != null) {
            SWVContext.asw_view.setVisibility(View.INVISIBLE);
        }
    }

    private void initializeWebView() {
        Playground playground = new Playground(this, SWVContext.asw_view, fns);
        SWVContext.getPluginManager().setPlayground(playground);
        WebSettings webSettings = SWVContext.asw_view.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setLoadsImagesAutomatically(true);
        webSettings.setRenderPriority(WebSettings.RenderPriority.HIGH);
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        
        // ⚡ SUPER CEPAT: AKSELERASI HARDWARE PERANGKAT AKTIF
        SWVContext.asw_view.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        
        webSettings.setSupportZoom(true);          
        webSettings.setBuiltInZoomControls(true);   
        webSettings.setDisplayZoomControls(false); 
        webSettings.setLoadWithOverviewMode(true);   
        webSettings.setUseWideViewPort(true);        
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowFileAccessFromFileURLs(true);
        webSettings.setAllowUniversalAccessFromFileURLs(true);
        webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            webSettings.setSafeBrowsingEnabled(false);
        }

        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        cookieManager.setAcceptThirdPartyCookies(SWVContext.asw_view, true);

        SWVContext.asw_view.setWebViewClient(new WebViewCallback());
        SWVContext.asw_view.setWebChromeClient(createWebChromeClient());
        setupDownloadListener();
    }

    private void setupDownloadListener() {
        SWVContext.asw_view.setDownloadListener((url, userAgent, contentDisposition, mimeType, contentLength) -> {
            try {
                DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
                String fileName = URLUtil.guessFileName(url, contentDisposition, mimeType);
                String finalMimeType = mimeType;
                if (finalMimeType == null || finalMimeType.equalsIgnoreCase("application/octet-stream") || fileName.endsWith(".bin")) {
                    if (url.contains("qris") || url.contains("deposit") || url.contains("invoice") || url.contains("gate")) {
                        finalMimeType = "image/png";
                        fileName = "QRIS_Mbah_Gadget_" + System.currentTimeMillis() + ".png";
                    } else if (url.contains(".apk")) {
                        finalMimeType = "application/vnd.android.package-archive";
                        fileName = URLUtil.guessFileName(url, contentDisposition, finalMimeType);
                    } else {
                        finalMimeType = "image/png";
                        if (fileName.endsWith(".bin")) fileName = fileName.replace(".bin", ".png");
                    }
                }
                request.setMimeType(finalMimeType);
                String cookies = CookieManager.getInstance().getCookie(url);
                request.addRequestHeader("cookie", cookies);
                request.addRequestHeader("User-Agent", userAgent);
                request.setDescription("Mendownload berkas...");
                request.setTitle(fileName);
                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);
                request.allowScanningByMediaScanner();
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                if (dm != null) {
                    dm.enqueue(request);
                    Toast.makeText(this, "Mendownload: " + fileName, Toast.LENGTH_LONG).show();
                }
            } catch (Exception e) { Log.e(TAG, "Download Error", e); }
        });
    }

    private WebChromeClient createWebChromeClient() {
        return new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView w, ValueCallback<Uri[]> f, FileChooserParams p) { return fileProcessing.onShowFileChooser(w, f, p); }
            @Override
            public void onProgressChanged(WebView view, int p) {
                if (p > 40) {
                    final View welcomeScreen = findViewById(R.id.msw_welcome);
                    if (SWVContext.asw_view != null && welcomeScreen != null && welcomeScreen.getVisibility() == View.VISIBLE) {
                        SWVContext.asw_view.setVisibility(View.VISIBLE);
                        welcomeScreen.setVisibility(View.GONE);
                    }
                }
            }
            @Override
            public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
                if (permissionManager.isLocationPermissionGranted()) callback.invoke(origin, true, false);
                else permissionManager.requestInitialPermissions();
            }
        };
    }

    private void setupFeatures() {
        setupSwipeRefresh();
        if (permissionManager != null) {
            permissionManager.requestInitialPermissions();
        }
        try { OneSignal.initWithContext(this); OneSignal.setAppId("e722a15b-0b07-4c82-a934-fcd0735704a2"); } catch (Exception e) { Log.e(TAG, "OneSignal Init Error", e); }
    }

    private void setupSwipeRefresh() {
        final SwipeRefreshLayout pullRefresh = findViewById(R.id.pullfresh);
        if (pullRefresh != null) { pullRefresh.setRefreshing(false); pullRefresh.setEnabled(false); }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        if (SWVContext.ASWV_LAYOUT == 1) {
            DrawerLayout drawer = findViewById(R.id.drawer_layout);
            if (drawer != null) drawer.closeDrawer(GravityCompat.START);
        }
        return true;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (SWVContext.asw_view != null) {
            SWVContext.asw_view.onResume();
            if (isFirstLaunchScanCheck) {
                isFirstLaunchScanCheck = false;
                SWVContext.asw_view.loadUrl(SWVContext.ASWV_URL); 
            } else {
                final View welcomeScreen = findViewById(R.id.msw_welcome);
                if (welcomeScreen != null) welcomeScreen.setVisibility(View.GONE);
                SWVContext.asw_view.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (SWVContext.asw_view != null) SWVContext.asw_view.onPause();
    }

    private class WebViewCallback extends WebViewClient {
        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) { super.onPageStarted(view, url, favicon); }
        @Override
        public void onPageFinished(WebView view, String url) {
            final View welcomeScreen = findViewById(R.id.msw_welcome);
            if (SWVContext.asw_view != null && welcomeScreen != null) { SWVContext.asw_view.setVisibility(View.VISIBLE); welcomeScreen.setVisibility(View.GONE); }
            view.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT);
            if (!url.startsWith("file://") && SWVContext.ASWV_GTAG != null && !SWVContext.ASWV_GTAG.isEmpty()) fns.inject_gtag(view, SWVContext.ASWV_GTAG);
        }

        @Override
        @SuppressLint("WebViewClientOnReceivedSslError")
        public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
            handler.proceed(); 
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            String url = request.getUrl().toString();
            
            if (url.startsWith("whatsapp:") || url.startsWith("intent:") || url.startsWith("tel:") || url.startsWith("mailto:") || url.startsWith("sms:")) {
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    view.getContext().startActivity(intent);
                    return true;
                } catch (Exception e) {
                    Log.e(TAG, "Gagal meluncurkan intent sistem luar: " + e.getMessage());
                    return true;
                }
            }
            
            if (url.startsWith("https://")) {
                // 🚀 USER AGENT FAKER AUTOMATION: Jika mendeteksi domain tiktok, paksa WebView menyamar jadi Chrome Android Asli
                if (url.contains("tiktok.com")) {
                    view.getSettings().setUserAgentString("Mozilla/5.0 (Linux; Android 13; SM-S901B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/112.0.0.0 Mobile Safari/537.36");
                } else {
                    // Jika kembali ke web Anda, kembalikan ke format default swv.properties
                    view.getSettings().setUserAgentString(null);
                }
                
                view.post(() -> view.loadUrl(url));
                return true; 
            }
            
            return false;
        }

        @Override
        public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
            if (request.isForMainFrame()) {
                view.post(() -> {
                    if (SWVContext.ASWV_OFFLINE_URL != null && !SWVContext.ASWV_OFFLINE_URL.isEmpty()) view.loadUrl(SWVContext.ASWV_OFFLINE_URL);
                    else view.loadUrl("file:///android_asset/error.html");
                });
            }
            super.onReceivedError(view, request, error);
        }
    }

    private void handleIncomingIntents() { fns.aswm_view(SWVContext.ASWV_URL, false, 0, this); }

    @Override
    public boolean onKeyDown(int keyCode, @NonNull KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (SWVContext.asw_view != null && SWVContext.asw_view.canGoBack()) { SWVContext.asw_view.goBack(); }
            else { moveTaskToBack(true); }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}
