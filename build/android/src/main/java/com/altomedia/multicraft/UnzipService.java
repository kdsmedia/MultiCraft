package com.altomedia.multicraft;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;
import androidx.core.app.NotificationCompat;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

// Previously extended IntentService, which is deprecated and does not support
// the Android 14+ (targetSdk 34+) foreground-service type requirement. This
// is now a plain Service that promotes itself to a foreground service (type
// dataSync) so long-running extraction is not killed by the system, and works
// on Android 8 through 16.
public class UnzipService extends Service {
    public static final String ACTION_UPDATE = "com.altomedia.multicraft.UPDATE";
    public static final String EXTRA_KEY_IN_FILE = "file";
    public static final String EXTRA_KEY_IN_LOCATION = "location";
    public static final String ACTION_PROGRESS = "progress";
    public final String TAG = UnzipService.class.getSimpleName();
    private static final String CHANNEL_ID = "MultiCraft channel";
    private static final int NOTIFICATION_ID = 1;

    private NotificationManager mNotifyManager;
    private Thread mWorker;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Promote to a foreground service BEFORE starting heavy work. On
        // Android 14+ (targetSdk 34+) a foreground service that declares
        // foregroundServiceType="dataSync" must be started with that type.
        Notification notification = buildNotification();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }

        final Intent workIntent = intent;
        mWorker = new Thread(new Runnable() {
            @Override
            public void run() {
                unzip(workIntent);
                stopSelf();
            }
        });
        mWorker.start();
        // START_NOT_STICKY: if the process is killed, do not restart the
        // extraction automatically (the user can re-trigger it).
        return START_NOT_STICKY;
    }

    private Notification buildNotification() {
        String name = "com.altomedia.multicraft";
        String description = "notifications from MultiCraft";
        if (mNotifyManager == null) {
            mNotifyManager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel mChannel = mNotifyManager.getNotificationChannel(CHANNEL_ID);
            if (mChannel == null) {
                mChannel = new NotificationChannel(CHANNEL_ID, name,
                        NotificationManager.IMPORTANCE_LOW);
                mChannel.setDescription(description);
                mChannel.setSound(null, null);
                mChannel.enableLights(false);
                mChannel.enableVibration(false);
                mNotifyManager.createNotificationChannel(mChannel);
            }
        }
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID);
        builder.setContentTitle(getString(R.string.notification_title))
                .setSmallIcon(R.drawable.update)
                .setContentText(getString(R.string.notification_description))
                .setOngoing(true);
        return builder.build();
    }

    private void isDir(String dir, String unzipLocation) {
        File f = new File(unzipLocation + dir);
        if (!f.isDirectory()) {
            f.mkdirs();
        }
    }

    private void unzip(Intent intent) {
        String[] file = intent.getStringArrayExtra(EXTRA_KEY_IN_FILE);
        String location = intent.getStringExtra(EXTRA_KEY_IN_LOCATION);
        int per = 0;
        int size = getSummarySize(file);
        for (String f : file) {
            try {
                try {
                    FileInputStream fin = new FileInputStream(f);
                    ZipInputStream zin = new ZipInputStream(fin);
                    ZipEntry ze;
                    while ((ze = zin.getNextEntry()) != null) {
                        if (ze.isDirectory()) {
                            per++;
                            isDir(ze.getName(), location);
                        } else {
                            per++;
                            int progress = 100 * per / size;
                            // send update
                            publishProgress(progress);
                            FileOutputStream f_out = new FileOutputStream(location + ze.getName());
                            byte[] buffer = new byte[8192];
                            int len;
                            while ((len = zin.read(buffer)) != -1) {
                                f_out.write(buffer, 0, len);
                            }
                            f_out.close();
                            zin.closeEntry();
                            f_out.close();
                        }
                    }
                    zin.close();
                } catch (FileNotFoundException e) {
                    Log.e(TAG, e.getMessage());
                }
            } catch (IOException e) {
                Log.e(TAG, e.getMessage());
            }
        }
    }

    private void publishProgress(int progress) {
        Intent intentUpdate = new Intent(ACTION_UPDATE);
        intentUpdate.putExtra(ACTION_PROGRESS, progress);
        sendBroadcast(intentUpdate);
    }

    private int getSummarySize(String[] zips) {
        int size = 0;
        for (String z : zips) {
            try {
                ZipFile zipSize = new ZipFile(z);
                size += zipSize.size();
            } catch (IOException e) {
                Log.e(TAG, e.getLocalizedMessage());
            }
        }
        return size;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mNotifyManager.cancel(NOTIFICATION_ID);
        publishProgress(-1);
    }
}