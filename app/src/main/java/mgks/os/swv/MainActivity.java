package mgks.os.swv;

/*
  Smart WebView v8 - MBAH GADGET TURBO CACHE BUILD
  VERSION: UNIVERSAL AUTO REFRESH
  ✅ HANYA CEK 1 DOMAIN! (mbahgadget.co.id)
  ✅ Semua selain itu = APLIKASI LUAR → AUTO REFRESH!
  ✅ Login AMAN! Tidak kena refresh!
*/

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.GeolocationPermissions;
import android.webkit.SslErrorHandler;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.graphics.Insets;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.navigation.NavigationView;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;
import com.onesignal.OneSignal;

import java.util.Objects;

import mgks.os.swv.plugins.QRScannerPlugin;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    private static final String TAG = "MainActivity";
    private static final boolean DEBUG = false;
    
    private boolean isFirstLaunchScanCheck = true;
    private boolean isNetworkAvailable = true;
    
    // ✅ Flag untuk deteksi kembali dari aplikasi luar
    private boolean isReturningFromExternalApp = false;

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
                handleBackPressed();
            }
        });

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        super.onCreate(savedInstanceState);
        androidx.core.splashscreen.SplashScreen.installSplashScreen(this);

        final View content = findViewById(android.R.id.content);
        permissionManager = new PermissionManager(this);

        setupFileUploadLauncher();
        setupQRScannerLauncher();

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

    // ==================== SETUP LAUNCHERS ====================
    
    private void setupFileUploadLauncher() {
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
    }

    private void setupQRScannerLauncher() {
        qrScannerLauncher = registerForActivityResult(new ScanContract(),
                result -> {
                    PluginInterface plugin = SWVContext.getPluginManager().getPluginInstance("QRScannerPlugin");
                    if (plugin instanceof QRScannerPlugin) {
                        ((QRScannerPlugin) plugin).handleScanResult(result);
                    }
                }
        );
    }

    // ==================== LAYOUT SETUP ====================

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

    // ==================== WEBVIEW INITIALIZATION ====================

    @SuppressLint("SetJavaScriptEnabled")
    private void initializeWebView() {
        Playground playground = new Playground(this, SWVContext.asw_view, fns);
        SWVContext.getPluginManager().setPlayground(playground);
        
        WebSettings webSettings = SWVContext.asw_view.getSettings();
        
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setLoadsImagesAutomatically(true);
        webSettings.setRenderPriority(WebSettings.RenderPriority.HIGH);
        webSettings.setSupportMultipleWindows(true);
        webSettings.setDatabaseEnabled(true);
        
        try {
            webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        } catch (Exception e) {
            if (DEBUG) Log.e(TAG, "Cache Init Error", e);
        }
        
        // 🔒 KEAMANAN
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowFileAccessFromFileURLs(false);
        webSettings.setAllowUniversalAccessFromFileURLs(false);
        webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        
        webSettings.setSupportZoom(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUseWideViewPort(true);
        
        SWVContext.asw_view.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        SWVContext.asw_view.setWebViewClient(new WebViewCallback());
        SWVContext.asw_view.setWebChromeClient(createWebChromeClient());
        setupDownloadListener();
        checkNetworkAvailability();
    }

    private void checkNetworkAvailability() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            isNetworkAvailable = activeNetwork != null && activeNetwork.isConnectedOrConnecting();
        }
    }

    // ==================== DOWNLOAD LISTENER ====================

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
                request.setDescription(getString(R.string.dl_downloading));
                request.setTitle(fileName);
                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);
                request.allowScanningByMediaScanner();
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                
                DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                if (dm != null) {
                    dm.enqueue(request);
                    Toast.makeText(this, getString(R.string.dl_downloading2) + " " + fileName, Toast.LENGTH_LONG).show();
                }
            } catch (Exception e) {
                if (DEBUG) Log.e(TAG, "Download Error", e);
                Toast.makeText(this, R.string.went_wrong, Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ==================== WEB CHROME CLIENT ====================

    private WebChromeClient createWebChromeClient() {
        return new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView w, ValueCallback<Uri[]> f, FileChooserParams p) {
                return fileProcessing.onShowFileChooser(w, f, p);
            }
            
            @Override
            public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, android.os.Message resultMsg) {
                WebView newWebView = new WebView(MainActivity.this);
                newWebView.setWebViewClient(new WebViewClient() {
                    @Override
                    public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) {
                        String url = r.getUrl().toString();
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        MainActivity.this.startActivity(intent);
                        return true;
                    }
                });
                WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
                transport.setWebView(newWebView);
                resultMsg.sendToTarget();
                return true;
            }

            @Override
            public void onProgressChanged(WebView view, int progress) {
                if (progress > 35) {
                    final View welcomeScreen = findViewById(R.id.msw_welcome);
                    if (SWVContext.asw_view != null && welcomeScreen != null && welcomeScreen.getVisibility() == View.VISIBLE) {
                        SWVContext.asw_view.setVisibility(View.VISIBLE);
                        welcomeScreen.setVisibility(View.GONE);
                    }
                }
            }
            
            @Override
            public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
                if (permissionManager.isLocationPermissionGranted()) {
                    callback.invoke(origin, true, false);
                } else {
                    permissionManager.requestInitialPermissions();
                }
            }
        };
    }

    // ==================== FEATURES SETUP ====================

    private void setupFeatures() {
        setupSwipeRefresh();
        if (permissionManager != null) {
            permissionManager.requestInitialPermissions();
        }
        
        try {
            OneSignal.initWithContext(this);
            String oneSignalAppId = getString(R.string.onesignal_app_id);
            if (!oneSignalAppId.isEmpty() && !oneSignalAppId.equals("YOUR_ONESIGNAL_APP_ID")) {
                OneSignal.setAppId(oneSignalAppId);
            }
        } catch (Exception e) {
            if (DEBUG) Log.e(TAG, "OneSignal Init Error", e);
        }
    }

    private void setupSwipeRefresh() {
        final SwipeRefreshLayout pullRefresh = findViewById(R.id.pullfresh);
        if (pullRefresh != null) {
            pullRefresh.setRefreshing(false);
            pullRefresh.setEnabled(false);
        }
    }

    // ==================== BACK BUTTON HANDLER ====================

    private void handleBackPressed() {
        if (SWVContext.asw_view != null) {
            if (SWVContext.asw_view.canGoBack()) {
                SWVContext.asw_view.goBack();
                return;
            }
            
            String currentUrl = SWVContext.asw_view.getUrl();
            if (currentUrl != null && !currentUrl.contains("mbahgadget.co.id")) {
                SWVContext.asw_view.loadUrl("https://mbahgadget.co.id");
                return;
            }
        }
        
        moveTaskToBack(true);
    }

    // ==================== ✅ UNIVERSAL: CEK APLIKASI LUAR ====================

    private boolean isExternalAppUrl(String url) {
        if (url == null || url.isEmpty()) return false;
        
        // ✅ UNIVERSAL: HANYA CEK 1 DOMAIN!
        // Jika BUKAN mbahgadget.co.id = APLIKASI LUAR!
        boolean isMbahGadget = url.contains("mbahgadget.co.id");
        boolean isInternalFile = url.startsWith("file://");
        boolean isBlank = url.equals("about:blank");
        
        // ❌ JANGAN REFRESH jika internal
        // ✅ REFRESH jika external
        return !isMbahGadget && !isInternalFile && !isBlank;
    }

    // ==================== ON RESUME ====================

    @Override
    public void onResume() {
        super.onResume();
        
        if (SWVContext.asw_view != null) {
            SWVContext.asw_view.onResume();
            checkNetworkAvailability();
            
            // ✅ KEMBALI DARI APLIKASI LUAR?
            if (isReturningFromExternalApp) {
                isReturningFromExternalApp = false;
                
                String currentUrl = SWVContext.asw_view.getUrl();
                
                // ✅ UNIVERSAL DETECTION!
                if (isExternalAppUrl(currentUrl)) {
                    // 🔄 REFRESH! (Kembali dari aplikasi luar)
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        if (SWVContext.asw_view != null) {
                            SWVContext.asw_view.reload();
                            if (DEBUG) Log.d(TAG, "🔄 Auto Refresh - Kembali dari aplikasi luar: " + currentUrl);
                        }
                    }, 300);
                } else {
                    // ❌ JANGAN REFRESH! (Internal)
                    if (DEBUG) Log.d(TAG, "⛔ Skip refresh - Internal: " + currentUrl);
                    
                    if (SWVContext.asw_view.getVisibility() != View.VISIBLE) {
                        SWVContext.asw_view.setVisibility(View.VISIBLE);
                    }
                }
            }
            
            if (isFirstLaunchScanCheck) {
                isFirstLaunchScanCheck = false;
                SWVContext.asw_view.loadUrl("https://mbahgadget.co.id");
            } else {
                final View welcomeScreen = findViewById(R.id.msw_welcome);
                if (welcomeScreen != null) {
                    welcomeScreen.setVisibility(View.GONE);
                }
                
                if (SWVContext.asw_view.getVisibility() != View.VISIBLE) {
                    SWVContext.asw_view.setVisibility(View.VISIBLE);
                }

                clearLoadingOverlays();
            }
        }
    }

    // ==================== ON PAUSE ====================

    @Override
    public void onPause() {
        super.onPause();
        
        if (SWVContext.asw_view != null) {
            SWVContext.asw_view.onPause();
            
            // ✅ Tandai bahwa aplikasi keluar (ke background)
            isReturningFromExternalApp = true;
            if (DEBUG) Log.d(TAG, "📤 Keluar dari APK");
        }
    }

    // ==================== CLEAR LOADING OVERLAYS ====================

    private void clearLoadingOverlays() {
        if (SWVContext.asw_view != null) {
            SWVContext.asw_view.evaluateJavascript(
                "(function() {" +
                "   var elements = document.getElementsByTagName('*');" +
                "   for (var i = 0; i < elements.length; i++) {" +
                "       var el = elements[i];" +
                "       var text = el.innerText || '';" +
                "       if (text.toLowerCase().includes('please wait') || " +
                "           text.toLowerCase().includes('mohon tunggu') || " +
                "           text.toLowerCase().includes('loading')) {" +
                "           el.style.display = 'none';" +
                "           el.style.visibility = 'hidden';" +
                "       }" +
                "   }" +
                "   var loaders = document.querySelectorAll('[class*=\"loading\"], [id*=\"loading\"], [class*=\"overlay\"], [id*=\"overlay\"], [class*=\"preloader\"], [id*=\"preloader\"]');" +
                "   for (var j = 0; j < loaders.length; j++) {" +
                "       loaders[j].style.display = 'none';" +
                "       loaders[j].style.visibility = 'hidden';" +
                "   }" +
                "})();", 
                null
            );
        }
    }

    // ==================== ON DESTROY ====================

    @Override
    protected void onDestroy() {
        if (SWVContext.asw_view != null) {
            try {
                SWVContext.asw_view.loadUrl("about:blank");
                SWVContext.asw_view.clearHistory();
                SWVContext.asw_view.clearCache(true);
                SWVContext.asw_view.clearFormData();
                SWVContext.asw_view.clearSslPreferences();
                
                if (SWVContext.asw_view.getParent() != null) {
                    ((ViewGroup) SWVContext.asw_view.getParent()).removeView(SWVContext.asw_view);
                }
                
                SWVContext.asw_view.removeAllViews();
                SWVContext.asw_view.destroy();
                SWVContext.asw_view = null;
            } catch (Exception e) {
                if (DEBUG) Log.e(TAG, "Error destroying WebView", e);
            }
        }
        
        SWVContext.setAppContext(null);
        System.gc();
        
        super.onDestroy();
    }

    // ==================== WEB VIEW CLIENT ====================

    private class WebViewCallback extends WebViewClient {
        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            if (DEBUG) Log.d(TAG, "Page started: " + url);
        }
        
        @Override
        public void onPageFinished(WebView view, String url) {
            final View welcomeScreen = findViewById(R.id.msw_welcome);
            if (SWVContext.asw_view != null && welcomeScreen != null) {
                SWVContext.asw_view.setVisibility(View.VISIBLE);
                welcomeScreen.setVisibility(View.GONE);
            }
            
            view.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT);
            
            if (!url.startsWith("file://") && SWVContext.ASWV_GTAG != null && !SWVContext.ASWV_GTAG.isEmpty()) {
                fns.inject_gtag(view, SWVContext.ASWV_GTAG);
            }
            
            if (DEBUG) Log.d(TAG, "Page finished: " + url);
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            String url = request.getUrl().toString();
            
            if (DEBUG) Log.d(TAG, "Override URL: " + url);
            
            // 1. Tangani Skema Pembayaran Khusus (dana://, shopeeid://, gojek://)
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                try {
                    Intent intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
                    if (intent != null) {
                        if (view.getContext().getPackageManager().resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY) != null) {
                            view.getContext().startActivity(intent);
                        } else {
                            String fallbackUrl = intent.getStringExtra("browser_fallback_url");
                            if (fallbackUrl != null) {
                                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(fallbackUrl));
                                view.getContext().startActivity(browserIntent);
                            }
                        }
                        return true;
                    }
                } catch (Exception e) {
                    if (DEBUG) Log.e(TAG, "Intent Error: " + e.getMessage());
                }
            }

            // 2. PAYDISINI & TRIPAY (proses di dalam WebView)
            if (url.contains("paydisini.co.id") || url.contains("tripay.co.id")) {
                final View welcomeScreen = findViewById(R.id.msw_welcome);
                if (welcomeScreen != null) welcomeScreen.setVisibility(View.GONE);
                view.loadUrl(url);
                return true;
            }

            // 3. UNIVERSAL: Domain Luar (buka di browser/aplikasi eksternal)
            if (!url.contains("mbahgadget.co.id") && !url.startsWith("file://")) {
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    if (intent.resolveActivity(view.getContext().getPackageManager()) != null) {
                        view.getContext().startActivity(intent);
                    } else {
                        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                        browserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        view.getContext().startActivity(browserIntent);
                    }
                    return true;
                } catch (Exception e) {
                    if (DEBUG) Log.e(TAG, "Deep Link Error: " + e.getMessage());
                }
            }
            
            view.loadUrl(url);
            return true;
        }

        @Override
        public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
            if (request.isForMainFrame()) {
                checkNetworkAvailability();
                
                if (!isNetworkAvailable) {
                    view.post(() -> {
                        if (SWVContext.ASWV_OFFLINE_URL != null && !SWVContext.ASWV_OFFLINE_URL.isEmpty()) {
                            view.loadUrl(SWVContext.ASWV_OFFLINE_URL);
                        } else {
                            view.loadUrl("file:///android_asset/offline.html");
                        }
                        Toast.makeText(MainActivity.this, R.string.msg_no_internet, Toast.LENGTH_LONG).show();
                    });
                } else {
                    view.post(() -> {
                        if (SWVContext.ASWV_OFFLINE_URL != null && !SWVContext.ASWV_OFFLINE_URL.isEmpty()) {
                            view.loadUrl(SWVContext.ASWV_OFFLINE_URL);
                        } else {
                            view.loadUrl("file:///android_asset/error.html");
                        }
                        Toast.makeText(MainActivity.this, R.string.went_wrong, Toast.LENGTH_SHORT).show();
                    });
                }
            }
            super.onReceivedError(view, request, error);
        }

        @Override
        public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
            if (DEBUG) {
                Log.w(TAG, "SSL Error: " + error.toString());
                handler.proceed();
            } else {
                handler.cancel();
                view.loadUrl("file:///android_asset/error.html");
                Toast.makeText(MainActivity.this, R.string.went_wrong, Toast.LENGTH_LONG).show();
            }
        }
    }

    // ==================== NAVIGATION ====================

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        if (SWVContext.ASWV_LAYOUT == 1) {
            DrawerLayout drawer = findViewById(R.id.drawer_layout);
            if (drawer != null) {
                drawer.closeDrawer(GravityCompat.START);
            }
        }
        return true;
    }

    // ==================== INTENT HANDLER ====================

    private void handleIncomingIntents() {
        fns.aswm_view("https://mbahgadget.co.id", false, 0, this);
    }

    // ==================== KEY EVENT ====================

    @Override
    public boolean onKeyDown(int keyCode, @NonNull KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            handleBackPressed();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    // ==================== UTILITY METHODS ====================

    public void setWindowSecure(boolean secure) {
        runOnUiThread(() -> {
            if (secure) {
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
            } else {
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE);
            }
        });
    }
}
