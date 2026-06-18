package mgks.os.swv;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.core.view.WindowCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.navigation.NavigationView;
import com.onesignal.OneSignal;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    private static final String TAG = "MainActivity";
    static Functions fns = new Functions();

    @SuppressLint({"SetJavaScriptEnabled", "JavascriptInterface"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (SWVContext.ASWP_BLOCK_SCREENSHOTS) getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (SWVContext.asw_view.canGoBack()) SWVContext.asw_view.goBack();
                else { if (SWVContext.ASWP_EXITDIAL) fns.ask_exit(MainActivity.this); else finish(); }
            }
        });

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        super.onCreate(savedInstanceState);
        
        SWVContext.setAppContext(getApplicationContext());
        setupLayout();
        initializeWebView();
        SWVContext.loadPlugins(this);
        SWVContext.init(this, SWVContext.asw_view, fns);
        
        if (savedInstanceState == null) {
            setupFeatures();
            fns.aswm_view(SWVContext.ASWV_URL, false, SWVContext.asw_error_counter, this);
        }
    }

    private void setupLayout() {
        if (SWVContext.ASWV_LAYOUT == 1) {
            setContentView(R.layout.drawer_main);
            MaterialToolbar toolbar = findViewById(R.id.toolbar);
            if (SWVContext.ASWP_DRAWER_HEADER) {
                setSupportActionBar(toolbar);
                DrawerLayout drawer = findViewById(R.id.drawer_layout);
                ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.open, R.string.close);
                drawer.addDrawerListener(toggle);
                toggle.syncState();
            }
            ((NavigationView) findViewById(R.id.nav_view)).setNavigationItemSelectedListener(this);
        } else {
            setContentView(R.layout.activity_main);
        }
        SWVContext.asw_view = findViewById(R.id.msw_view);
    }

    private void initializeWebView() {
        WebSettings webSettings = SWVContext.asw_view.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setAllowFileAccess(true);
        
        SWVContext.asw_view.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return fns.url_actions(view, request.getUrl().toString(), MainActivity.this);
            }
        });
        
        SWVContext.asw_view.setWebChromeClient(new WebChromeClient());
        // FUNGSI DOWNLOAD DIMATIKAN TOTAL (100% AMAN DARI SCAN & Eror)
        SWVContext.asw_view.setDownloadListener(null); 
    }

    private void setupFeatures() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            NotificationChannel channel = new NotificationChannel(SWVContext.asw_fcm_channel, getString(R.string.notification_channel_name), NotificationManager.IMPORTANCE_HIGH);
            if (nm != null) nm.createNotificationChannel(channel);
        }
        try { OneSignal.initWithContext(this); OneSignal.setAppId("e722a15b-0b07-4c82-a934-fcd0735704a2"); } catch (Exception ignored) {}
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        SWVContext.NavItem navItem = SWVContext.ASWV_DRAWER_MENU.get(id);
        if (navItem != null) fns.aswm_view(navItem.action, false, 0, this);
        ((DrawerLayout) findViewById(R.id.drawer_layout)).closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onResume() {
        super.onResume();
        Bitmap bm = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher);
        setTaskDescription(new ActivityManager.TaskDescription(getString(R.string.app_name), bm, getColor(R.color.colorPrimary)));
    }

    @Override
    public boolean onKeyDown(int keyCode, @NonNull KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_BACK) {
            if (SWVContext.asw_view.canGoBack()) SWVContext.asw_view.goBack();
            else { if (SWVContext.ASWP_EXITDIAL) fns.ask_exit(this); else finish(); }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}
