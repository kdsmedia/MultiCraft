package com.altomedia.multicraft;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.preference.PreferenceManager;
import androidx.core.app.ActivityCompat;

import java.util.ArrayList;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.POST_NOTIFICATIONS;
import static com.altomedia.multicraft.PreferencesHelper.getLaunchTimes;

class PermissionManager {
    static ArrayList<String> permissionsToRequest;
    static ArrayList<String> permissionsRejected;
    private Activity activity;
    private SharedPreferences sharedPreferences;

    PermissionManager(Activity activity) {
        this.activity = activity;
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(activity);
    }

    String[] requestPermissions() {
        ArrayList<String> permissions = new ArrayList<>();
        // WRITE_EXTERNAL_STORAGE is intentionally NOT requested anymore:
        // since the game stores data in Context.getExternalFilesDir(null)
        // (app-specific storage), no storage permission is needed on
        // Android 5.0+ (API 21+). On Android 11+ (API 30+) the system
        // auto-denies WRITE_EXTERNAL_STORAGE for app-specific storage, which
        // previously caused the app to loop forever on the permission prompt
        // and appear to hang / force-close.
        permissions.add(ACCESS_COARSE_LOCATION);
        // Android 13+ (API 33+): POST_NOTIFICATIONS is a runtime permission.
        // It is needed by the UnzipService foreground notification. On older
        // versions it is a normal permission (granted at install).
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(POST_NOTIFICATIONS);
        }
        //filter out the permissions we have already accepted
        permissionsToRequest = findUnAskedPermissions(permissions);
        //get the permissions we have asked for before but are not granted..
        //we will store this in a global list to access later.
        permissionsRejected = findRejectedPermissions(permissions);
        if (permissionsToRequest.size() > 0) {//we need to ask for permissions
            for (String perm : permissionsToRequest) {
                markAsAsked(perm);
            }
            return permissionsToRequest.toArray(new String[permissionsToRequest.size()]);
        } else if (permissionsRejected.size() > 0 && getLaunchTimes() % 3 == 0) {
            return permissionsRejected.toArray(new String[permissionsRejected.size()]);
        }
        return new String[]{};
    }

    boolean hasPermission(String permission) {
        return ActivityCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED;
    }

    private boolean shouldWeAsk(String permission) {
        return sharedPreferences.getBoolean(permission, true);
    }

    private void markAsAsked(String permission) {
        sharedPreferences.edit().putBoolean(permission, false).apply();
    }

    private ArrayList<String> findUnAskedPermissions(ArrayList<String> wanted) {
        ArrayList<String> result = new ArrayList<>();

        for (String perm : wanted) {
            if (!hasPermission(perm) && shouldWeAsk(perm)) {
                result.add(perm);
            }
        }

        return result;
    }

    private ArrayList<String> findRejectedPermissions(ArrayList<String> wanted) {
        ArrayList<String> result = new ArrayList<>();

        for (String perm : wanted) {
            if (!hasPermission(perm) && !shouldWeAsk(perm)) {
                result.add(perm);
            }
        }

        return result;
    }
}

