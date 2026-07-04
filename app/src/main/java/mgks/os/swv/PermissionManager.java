package mgks.os.swv;

/*
  Smart WebView v8 - MBAH GADGET 4-BATCH PERMISSION ENGINE (ALL IN INITIAL)
*/

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.util.ArrayList;
import java.util.List;

public class PermissionManager {

    private static final String TAG = "PermissionManager";

    public static final int INITIAL_REQUEST_CODE = 100;
    public static final int CAMERA_REQUEST_CODE = 101;
    public static final int STORAGE_REQUEST_CODE = 102;

    private final Activity activity;

    public PermissionManager(Activity activity) {
        this.activity = activity;
    }

    /**
     * MENEMBAK 4 IZIN SEKALIGUS DI AWAL PASCA INSTALASI!
     * Mencakup: Lokasi, Notifikasi, KAMERA, serta Media Foto & Video.
     */
    public void requestInitialPermissions() {
        List<String> permissionsToRequest = new ArrayList<>();

        // 1. Masukkan Izin Lokasi
        if (!isLocationPermissionGranted()) {
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION);
            permissionsToRequest.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        }

        // 2. Masukkan Izin Notifikasi (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !isNotificationPermissionGranted()) {
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS);
        }

        // 3. Masukkan Izin Kamera (Biar langsung sah di awal!)
        if (!isCameraPermissionGranted()) {
            permissionsToRequest.add(Manifest.permission.CAMERA);
        }

        // 4. Masukkan Izin Media Galeri Foto & Video
        if (!isStoragePermissionGranted()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permissionsToRequest.add(Manifest.permission.READ_MEDIA_IMAGES);
                permissionsToRequest.add(Manifest.permission.READ_MEDIA_VIDEO);
            } else {
                permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
        }

        // Eksekusi: Hantam semua popup beruntun di detik pertama aplikasi dibuka
        if (!permissionsToRequest.isEmpty()) {
            Log.d(TAG, "Mbah Gadget memicu ALL BATCH request di awal: " + permissionsToRequest);
            ActivityCompat.requestPermissions(activity, permissionsToRequest.toArray(new String[0]), INITIAL_REQUEST_CODE);
        } else {
            Log.d(TAG, "Semua 4 izin utama sudah aman dan disetujui.");
        }
    }

    public void requestCameraPermission() {
        if (!isCameraPermissionGranted()) {
            List<String> permissions = new ArrayList<>();
            permissions.add(Manifest.permission.CAMERA);
            ActivityCompat.requestPermissions(activity, permissions.toArray(new String[0]), CAMERA_REQUEST_CODE);
        }
    }

    public boolean isLocationPermissionGranted() {
        return ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    public boolean isNotificationPermissionGranted() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    public boolean isCameraPermissionGranted() {
        return ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    public boolean isStoragePermissionGranted() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED &&
                   ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED;
        }
        return ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }
}
