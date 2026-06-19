package mgks.os.swv;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;
import androidx.core.view.WindowCompat;
import com.journeyapps.barcodescanner.ScanOptions;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    static Functions fns = new Functions();
    private PermissionManager permissionManager;

    @SuppressLint({"SetJavaScriptEnabled", "JavascriptInterface"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        super.onCreate(savedInstanceState);
        SplashScreen.installSplashScreen(this);
        
        setContentView(SWVContext.ASWV_LAYOUT == 1 ? R.layout.drawer_main : R.layout.activity_main);
        
        SWVContext.asw_view = findViewById(R.id.msw_view);
        SWVContext.asw_view.setVisibility(View.INVISIBLE);

        permissionManager = new PermissionManager(this);
        SWVContext.setAppContext(getApplicationContext());
        
        initializeWebView();
        SWVContext.init(this, SWVContext.asw_view, fns);

        // Handle tombol back
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (SWVContext.asw_view.canGoBack()) SWVContext.asw_view.goBack();
                else finish();
            }
        });

        // Load URL Instan
        fns.aswm_view(SWVContext.ASWV_URL, false, 0, this);
        
        // Izin langsung tembak
        permissionManager.requestInitialPermissions();
    }

    private void initializeWebView() {
        WebSettings webSettings = SWVContext.asw_view.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setRenderPriority(WebSettings.RenderPriority.HIGH);
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        SWVContext.asw_view.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        
        SWVContext.asw_view.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                View welcomeScreen = findViewById(R.id.msw_welcome);
                if (welcomeScreen != null) welcomeScreen.setVisibility(View.GONE);
                SWVContext.asw_view.setVisibility(View.VISIBLE);
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        SWVContext.asw_view.onResume();
    }

    @Override
    public boolean onKeyDown(int keyCode, @NonNull KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && SWVContext.asw_view.canGoBack()) {
            SWVContext.asw_view.goBack();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}
