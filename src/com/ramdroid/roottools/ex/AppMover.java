package com.ramdroid.roottools.ex;

import android.util.Log;
import com.stericson.RootTools.RootTools;
import com.stericson.RootTools.RootToolsException;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeoutException;

/**
 * Moves an app from one partition to another.
 *
 * You could use the following high level functions:
 *
 * 1. Install as system app / Update system app
 *
 *    AppMover.installSystemApp("com.example.myapp");
 *
 * 2. Remove a system app (move back to DATA, if APK on DATA was not updated)
 *
 *    AppMover.removeSystemApp("com.example.myapp");
 *
 * If you need a different behaviour you can use the moveAppEx function.
 *
 * Note:
 * Before moving apps you should call appFitsOnPartition(...) to make sure the
 * APK fits on the target partition.
 */
public class AppMover {

    private static final String TAG = "AppMover";

    public static final String PARTITION_DATA           = "data";
    public static final String PARTITION_SYSTEM         = "system";

    public static final int FLAG_OVERWRITE              = 1;

    public static final int ERROR_NONE                  = 0;
    public static final int ERROR_BUSYBOX               = 1;
    public static final int ERROR_NOT_EXISTING          = 2;
    public static final int ERROR_INSUFFICIENT_SPACE    = 3;
    public static final int ERROR_REMOUNT_SYSTEM        = 4;

    /**
     * Check if an app is installed on a particular partition or not.
     *
     * When searching for an APK on the SYSTEM partition then root access is not required and we can just use
     * Java functions to parse the files.
     *
     * When searching for an APK on the DATA partition then we have no read access and need to launch a busybox
     * command instead.
     *
     * @param packageName Package name of the App e.g. com.example.myapp
     * @param partition PARTITION_DATA or PARTITION_SYSTEM
     * @return the error code or ERROR_NONE if OK
     */
    public static int appExistsOnPartition(String packageName, String partition) {
        int errorCode = ERROR_NOT_EXISTING;
        if (partition.equals(PARTITION_SYSTEM)) {
            File root = new File("/" + partition + "/app/");
            File[] files = root.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (f.getName().contains(packageName)) {
                        Log.d(TAG, "Found " + f.getPath());
                        errorCode = ERROR_NONE;
                        break;
                    }
                }
            }
        }
        else
        {
            try {
                List<String> result = RootTools.sendShell("busybox ls /" + partition + "/app | grep " + packageName, -1);
                Log.d(TAG, "Results: " + result.size());
                for (String r : result) {
                    Log.d(TAG, "Result " + r);
                    if (r.contains(packageName)) {
                        errorCode = ERROR_NONE;
                    }
                    else if (r.contains("fail")) {
                        errorCode = ERROR_BUSYBOX;
                    }
                }
            } catch (IOException e) {
                errorCode = ERROR_BUSYBOX;
                e.printStackTrace();
            } catch (RootToolsException e) {
                errorCode = ERROR_BUSYBOX;
                e.printStackTrace();
            } catch (TimeoutException e) {
                errorCode = ERROR_BUSYBOX;
                e.printStackTrace();
            }
        }
        return errorCode;
    }

    /**
     * Checks if the APK fits on the specified partition.
     *
     * @param packageName Package name of the App e.g. com.example.myapp
     * @param partition PARTITION_DATA or PARTITION_SYSTEM
     * @return the error code or ERROR_NONE if OK
     */
    public static int appFitsOnPartition(String packageName, String partition) {
        int errorCode = ERROR_INSUFFICIENT_SPACE;
        long freeDiskSpace = RootTools.getSpace("/" + partition);
        long apkSpace = 0;

        String appPartition = PARTITION_DATA;
        if (partition.equals(PARTITION_DATA)) {
            appPartition = PARTITION_SYSTEM;
        }

        File apk1 = new File("/" + appPartition + "/app/" + packageName + "-1.apk");
        File apk2 = new File("/" + appPartition + "/app/" + packageName + "-2.apk");
        apkSpace = apk1.length() / 1024;
        if (apkSpace < 1) {
            apkSpace = apk2.length() / 1024;
        }

        Log.d(TAG, "Available disk space on " + partition + ": " + freeDiskSpace);
        Log.d(TAG, "Required disk space for APK: " + apkSpace);

        if ((apkSpace > 0) && (freeDiskSpace > apkSpace)) {
            errorCode = ERROR_NONE;
        }
        return errorCode;
    }

    /**
     * Moves an APK from DATA to SYSTEM partition and updates the APK if already existing.
     *
     * @param packageName Package name of the App e.g. com.example.myapp
     * @return the error code or ERROR_NONE if OK
     */
    public static int installSystemApp(String packageName) {
        return moveAppEx(packageName, PARTITION_DATA, PARTITION_SYSTEM, FLAG_OVERWRITE);
    }

    /**
     * Moves an APK from SYSTEM to DATA partition. If the APK was updated in the DATA partition then this APK
     * remains untouched and only the APK on system is removed.
     *
     * @param packageName Package name of the App e.g. com.example.myapp
     * @return the error code or ERROR_NONE if OK
     */
    public static int removeSystemApp(String packageName) {
        return moveAppEx(packageName, PARTITION_SYSTEM, PARTITION_DATA, 0);
    }

    public static int moveAppEx(String packageName, String sourcePartition, String targetPartition, int flags) {
        int errorCode = ERROR_NONE;
        boolean needRemountSystem = (sourcePartition.equals(PARTITION_SYSTEM) || targetPartition.equals(PARTITION_SYSTEM));
        if (needRemountSystem) {
            if (!RootTools.remount("/system", "RW")) {
                errorCode = ERROR_REMOUNT_SYSTEM;
            }
        }

        if (errorCode == ERROR_NONE) {
            // install or remove system app
            String shellCmd = "busybox mv /" + sourcePartition + "/app/" + packageName + "*.apk /" + targetPartition + "/app/";
            boolean forceMove = true;
            if ((flags & FLAG_OVERWRITE) != FLAG_OVERWRITE) {
                errorCode = appExistsOnPartition(packageName, sourcePartition);
                if (errorCode == AppMover.ERROR_NONE) {
                    // App is already existing in target partition, so just remove it from source partition
                    shellCmd = "busybox rm /" + sourcePartition + "/app/" + packageName + "*.apk";
                    forceMove = false;
                }
            }
            if (forceMove) {
                // App is only existing in system directory, so move it back to data directory
                shellCmd = "busybox mv /" + sourcePartition + "/app/" + packageName + "*.apk /" + targetPartition + "/app/";
            }
            Log.d(TAG, shellCmd);

            if (errorCode == ERROR_NONE) {
                // move APK to system partition or vice versa
                try {
                    List<String> result = RootTools.sendShell(shellCmd, -1);
                    for (String r : result) {
                        Log.d(TAG, "Result " + r);
                    }
                } catch (IOException e) {
                    errorCode = ERROR_BUSYBOX;
                    e.printStackTrace();
                } catch (RootToolsException e) {
                    errorCode = ERROR_BUSYBOX;
                    e.printStackTrace();
                } catch (TimeoutException e) {
                    errorCode = ERROR_BUSYBOX;
                    e.printStackTrace();
                }

                if (needRemountSystem) {
                    // mount R/O
                    RootTools.remount("/system", "RO");
                }
            }
        }
        return errorCode;
    }
}
