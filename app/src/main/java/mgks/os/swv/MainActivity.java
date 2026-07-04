package mgks.os.swv;

/*
  Smart WebView v8 - MBAH GADGET SUPER FAST (4-PERMISSION NORMAL MODE)
  FIXED: 100% INTERNAL PAYMENTS, PAYDISINI INTEGRATION, CHROMIUM HARD-REFRESH RENDER GUARD (NO MANUALLY BACK NEEDED).
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
import android.content.SharedPreferences;
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
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Objects;
import java.util.regex.Matcher;

import mgks.os.swv.plugins.QRScannerPlugin;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    private static final String TAG = "MainActivity";
    private boolean isFirstLaunchScanCheck = true;
    private SharedPreferences sharedPrefs; 

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

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (SWVContext.asw_view != null) {
            String lastUrl = sharedPrefs.getString("last_payment_url", null);
            if (lastUrl != null) {
                sharedPrefs.edit().remove("last_payment_url").apply();
                SWVContext.asw_view.loadUrl(lastUrl);
            } else if (SWVContext.asw_view.getUrl() == null || SWVContext.asw_view.getUrl().equals("about:blank")) {
                SWVContext.asw_view.loadUrl(SWVContext.ASWV_URL);
            }
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (SWVContext.asw_view != null) {
            SWVContext.asw_view.saveState(outState);
        }
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
        sharedPrefs = getSharedPreferences("MbahGadgetPrefs", Context.MODE_PRIVATE);

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

        if (savedInstanceState != null && SWVContext.asw_view != null) {
            SWVContext.asw_view.restoreState(savedInstanceState);
            isFirstLaunchScanCheck = false;
        }

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
        
        webSettings.setDatabaseEnabled(true);
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        
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

    // 👑 JALUR AMAN: Menyentak paksa siklus render grafis Chromium (Babat Habis Layar Putih)
    @Override
    public void onResume() {
        super.onResume();
        if (SWVContext.asw_view != null) {
            final SwipeRefreshLayout pullRefresh = findViewById(R.id.pullfresh);
            if (pullRefresh != null) {
                pullRefresh.setRefreshing(false);
                pullRefresh.setEnabled(false);
                pullRefresh.destroyDrawingCache(); 
            }

            SWVContext.asw_view.onResume();
            SWVContext.asw_view.resumeTimers(); 

            SWVContext.asw_view.setVisibility(View.VISIBLE);
            SWVContext.asw_view.requestFocus();
            SWVContext.asw_view.requestFocusFromTouch();

            // Siasat Jitu: Paksa matikan hardware acceleration sebentar saat resume untuk membersihkan cache putih hantu
            SWVContext.asw_view.setLayerType(View.LAYER_TYPE_SOFTWARE, null);

            final View welcomeScreen = findViewById(R.id.msw_welcome);
            if (welcomeScreen != null) {
                welcomeScreen.setVisibility(View.GONE);
            }
            
            if (permissionManager != null) {
                permissionManager.requestInitialPermissions();
            }

            if (isFirstLaunchScanCheck) {
                isFirstLaunchScanCheck = false;
                String lastUrl = sharedPrefs.getString("last_payment_url", null);
                if (lastUrl != null) {
                    sharedPrefs.edit().remove("last_payment_url").apply(); 
                    SWVContext.asw_view.loadUrl(lastUrl);
                } else {
                    SWVContext.asw_view.loadUrl(SWVContext.ASWV_URL);
                }
            } else {
                if (SWVContext.asw_view.getUrl() == null || SWVContext.asw_view.getUrl().equals("about:blank")) {
                    String lastUrl = sharedPrefs.getString("last_payment_url", null);
                    if (lastUrl != null) {
                        sharedPrefs.edit().remove("last_payment_url").apply();
                        SWVContext.asw_view.loadUrl(lastUrl);
                    } else {
                        SWVContext.asw_view.loadUrl(SWVContext.ASWV_URL);
                    }
                } else {
                    // JURUS PAMUNGKAS: Lakukan penyegaran paksa layer internal WebView
                    SWVContext.asw_view.clearFocus();
                    SWVContext.asw_view.requestFocus();
                }
            }

            // Bangunkan kembali akselerasi grafis GPU 150 milidetik kemudian agar halaman rendering normal otomatis
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (SWVContext.asw_view != null) {
                    SWVContext.asw_view.setLayerType(View.LAYER_TYPE_HARDWARE, null);
                    SWVContext.asw_view.invalidate();
                    SWVContext.asw_view.requestLayout();
                }
            }, 150);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (SWVContext.asw_view != null) {
            SWVContext.asw_view.onPause();
        }
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
            
            if (url.startsWith("http://") || url.startsWith("https://")) {
                if (url.contains("checkout") || url.contains("pay") || url.contains("tripay") || url.contains("duitku") || url.contains("xendit") || url.contains("midtrans")) {
                    sharedPrefs.edit().putString("last_payment_url", url).apply();
                }
            }

            if (url.startsWith("whatsapp:") || url.startsWith("tel:") || url.startsWith("mailto:") || url.startsWith("sms:") || 
                url.startsWith("dana:") || url.startsWith("danaid:") || url.startsWith("ovo:") || url.startsWith("gopay:") || url.startsWith("shopeepay:")) {
                try {
                    if (view.getUrl() != null && !view.getUrl().equals("about:blank")) {
                        sharedPrefs.edit().putString("last_payment_url", view.getUrl()).apply();
                    }
                    
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
                    view.getContext().startActivity(intent);
                    return true; 
                } catch (Exception e) {
                    try {
                        if (url.startsWith("dana:") || url.startsWith("danaid:")) {
                            Intent marketIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=id.dana"));
                            view.getContext().startActivity(marketIntent);
                        } else if (url.startsWith("ovo:")) {
                            Intent marketIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=id.co.ovo"));
                            view.getContext().startActivity(marketIntent);
                        }
                    } catch (Exception marketEx) {
                        Log.e(TAG, "Gagal membuka store: " + marketEx.getMessage());
                    }
                    return true; 
                }
            }

            if (url.startsWith("http://") || url.startsWith("https://")) {
                if (url.contains("tiktok.com") || url.contains("facebook.com") || url.contains("instagram.com") || 
                    url.contains("shopee") || url.contains("x.com") || url.contains("youtube.com") || url.contains("snackvideo")) {
                    view.getSettings().setUserAgentString("Mozilla/5.0 (Linux; Android 13; SM-S901B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/112.0.0.0 Mobile Safari/537.36");
                } else {
                    view.getSettings().setUserAgentString(null);
                }
                view.post(() -> view.loadUrl(url));
                return true;
            }

            try {
                if (url.startsWith("intent:")) {
                    Intent intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
                    if (intent != null) {
                        PackageManager packageManager = view.getContext().getPackageManager();
                        if (intent.resolveActivity(packageManager) != null) {
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            view.getContext().startActivity(intent);
                            return true;
                        } else {
                            String fallbackUrl = intent.getStringExtra("browser_fallback_url");
                            if (fallbackUrl != null) {
                                view.loadUrl(fallbackUrl);
                                return true;
                            }
                        }
                    }
                }
            } catch (URISyntaxException e) {
                Log.e(TAG, "Kesalahan intent: " + e.getMessage());
            }

            return false;
        }

        @Override
        public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
            if (request.isForMainFrame()) {
                String failingUrl = request.getUrl().toString();
                
                if (failingUrl.contains("mbahgadget.co.id")) {
                    view.post(() -> {
                        if (SWVContext.ASWV_OFFLINE_URL != null && !SWVContext.ASWV_OFFLINE_URL.isEmpty()) {
                            view.loadUrl(SWVContext.ASWV_OFFLINE_URL);
                        } else {
                            String htmlData = "<html><head><meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
                                    "<style>body { font-family: sans-serif; text-align: center; padding-top: 60px; color: #333; background-color:#f9f9f9; }" +
                                    ".btn { display: inline-block; padding: 14px 28px; margin-top: 25px; background-color: #00C853; color: white; text-decoration: none; border-radius: 8px; font-weight: bold; border: none; cursor: pointer; box-shadow: 0 4px 6px rgba(0,0,0,0.1); }</style>" +
                                    "</head><body>" +
                                    "<h2>Koneksi Internet Terputus</h2>" +
                                    "<p>Pastikan paket data atau Wi-Fi Anda aktif, lalu coba lagi.</p>" +
                                    "<button class='btn' onclick='window.location.href=\"" + SWVContext.ASWV_URL + "\"'>Muat Ulang Toko</button>" +
                                    "</body></html>";
                            view.loadDataWithBaseURL(null, htmlData, "text/html", "UTF-8", null);
                        }
                    });
                } else {
                    Log.d(TAG, "Abaikan pemicu error untuk tautan luar: " + failingUrl);
                }
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
